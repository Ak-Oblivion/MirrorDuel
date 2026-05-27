package com.mirrorduel.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import com.mirrorduel.core.Board;
import com.mirrorduel.core.Crystal;
import com.mirrorduel.core.Direction;
import com.mirrorduel.core.GameManager;
import com.mirrorduel.core.GameState;
import com.mirrorduel.core.LaserEmitter;
import com.mirrorduel.core.LaserSegment;
import com.mirrorduel.core.Mirror;
import com.mirrorduel.core.Player;
import com.mirrorduel.core.Tile;

import java.util.List;

/**
 * Main renderer for <em>Mirror Duel: Neon Protocol</em>.
 *
 * <h3>Render pipeline per frame</h3>
 * <ol>
 *   <li>{@link BloomEffect#captureBegin()} — redirect scene into off-screen FBO.</li>
 *   <li>3-D scene via {@link ModelBatch}:
 *       board tiles with highlight overlays, mirrors, crystals, emitters.</li>
 *   <li>Laser beams via {@link ShapeRenderer} with additive blending:
 *       three layered quads per segment (outer glow, mid glow, bright core).</li>
 *   <li>Particle system: spawn new bursts on game events, advance simulation,
 *       render with additive blending.</li>
 *   <li>{@link BloomEffect#captureEnd()} + {@link BloomEffect#render()} —
 *       bloom composite to the default framebuffer.</li>
 *   <li>2-D HUD overlay via {@link SpriteBatch}: turn indicator, player labels,
 *       control hints, and win overlay.</li>
 * </ol>
 *
 * <h3>Coordinate system</h3>
 * Board tile {@code (col, row)} has its world centre at
 * {@code (col + 0.5f, 0, row + 0.5f)}.  Y is the world up axis.  The camera
 * is positioned above and behind the board looking down at roughly 60 degrees.
 * Laser beams travel in the {@code y = 0.12} plane so they float just above
 * the tile surfaces.
 *
 * <h3>Thread safety</h3>
 * All methods must be called on the LibGDX GL/render thread.
 */
public class GameRenderer implements Disposable {

    // ══════════════════════════════════════════════════════════════════════
    //  Geometry constants
    // ══════════════════════════════════════════════════════════════════════

    /** World-unit side length of one board tile. */
    private static final float TILE        = 1.0f;
    /** Visible gap between adjacent tile boxes. */
    private static final float TILE_GAP    = 0.07f;
    /** Height of the tile box model. */
    private static final float TILE_H      = 0.08f;
    /** World Y at which piece centres sit above the tile surface. */
    private static final float OBJ_Y       = TILE_H / 2f + 0.13f;
    /** World Y of the laser beam plane. */
    private static final float LASER_Y     = 0.12f;

    // ══════════════════════════════════════════════════════════════════════
    //  Colour constants
    // ══════════════════════════════════════════════════════════════════════

    /** Checkerboard dark tile. */
    private static final Color TILE_A      = new Color(0.040f, 0.040f, 0.100f, 1f);
    /** Checkerboard light tile. */
    private static final Color TILE_B      = new Color(0.055f, 0.060f, 0.140f, 1f);
    /** Glow colour for highlighted (valid-move) tiles. */
    private static final Color HL_MOVE     = new Color(0.20f, 0.80f, 1.00f, 1f);
    /** Glow colour for the selected mirror's tile. */
    private static final Color HL_SEL      = new Color(1.00f, 0.95f, 0.20f, 1f);
    /** Player 0 (blue) accent colour used in the HUD. */
    private static final Color P0_HUD      = new Color(0.15f, 0.65f, 1.00f, 1f);
    /** Player 1 (red) accent colour used in the HUD. */
    private static final Color P1_HUD      = new Color(1.00f, 0.18f, 0.18f, 1f);
    /** Background clear colour (very dark navy). */
    private static final Color BG_COLOR    = new Color(0.018f, 0.018f, 0.042f, 1f);

    // ══════════════════════════════════════════════════════════════════════
    //  Camera default
    // ══════════════════════════════════════════════════════════════════════

    private static final float CAM_X    = 3.5f;
    private static final float CAM_Y    = 9.0f;
    private static final float CAM_Z    = 13.0f;
    private static final float LOOK_X   = 3.5f;
    private static final float LOOK_Z   = 3.5f;

    // ══════════════════════════════════════════════════════════════════════
    //  Laser beam layers  {halfWidth, alphaMultiplier}
    // ══════════════════════════════════════════════════════════════════════

    private static final float[][] LASER_LAYERS = {
        { 0.200f, 0.18f },   // outer corona
        { 0.095f, 0.42f },   // mid glow
        { 0.030f, 0.96f },   // bright core
    };

    // ══════════════════════════════════════════════════════════════════════
    //  3-D pipeline
    // ══════════════════════════════════════════════════════════════════════

    private final ModelBatch       modelBatch;
    private final Environment      environment;
    private final PerspectiveCamera camera;

    // Shared geometry models
    private Model tileModel;        // base tile box
    private Model tileHighModel;    // highlight overlay (slightly raised)
    private Model mirrorModel;      // thin elongated bar
    private Model crystalModel;     // low-poly sphere
    private Model emitterModel;     // body box
    private Model emitterBarrel;    // cylinder barrel pointing in fire direction

    /** Tracks all models for safe disposal. */
    private final Array<Disposable> ownedModels = new Array<>();

    // Pre-built tile model instances (board is static; only materials change)
    private ModelInstance[][] tileInst;      // [col][row]
    private ModelInstance[][] tileHighInst;  // [col][row] highlight overlay

    // ══════════════════════════════════════════════════════════════════════
    //  2-D / ShapeRenderer
    // ══════════════════════════════════════════════════════════════════════

    private final ShapeRenderer shapes;

    // ══════════════════════════════════════════════════════════════════════
    //  Post-processing
    // ══════════════════════════════════════════════════════════════════════

    private BloomEffect bloom;

    // ══════════════════════════════════════════════════════════════════════
    //  Particles
    // ══════════════════════════════════════════════════════════════════════

    private final ParticleSystem particles;

    // ══════════════════════════════════════════════════════════════════════
    //  HUD
    // ══════════════════════════════════════════════════════════════════════

    private final SpriteBatch  uiBatch;
    /** Small body font (scaled 1.3×). */
    private final BitmapFont   fontSmall;
    /** Medium font for turn indicator (scaled 1.9×). */
    private final BitmapFont   fontMed;
    /** Large display font for win overlay (scaled 3.0×). */
    private final BitmapFont   fontLarge;
    /** Reusable layout object — avoids per-frame allocation for text metrics. */
    private final GlyphLayout  layout = new GlyphLayout();

    // ══════════════════════════════════════════════════════════════════════
    //  Picking scratch objects (reused each frame)
    // ══════════════════════════════════════════════════════════════════════

    /** The y = 0 world plane used for ray–board intersection. */
    private final Plane   boardPlane = new Plane(Vector3.Y, 0f);
    /** Intersection point populated by {@link #screenToTile}. */
    private final Vector3 hitPoint   = new Vector3();

    // ══════════════════════════════════════════════════════════════════════
    //  Runtime state
    // ══════════════════════════════════════════════════════════════════════

    /** Viewport width in pixels. */
    private int   vw;
    /** Viewport height in pixels. */
    private int   vh;
    /** Accumulated wall-clock time since construction; drives all animations. */
    private float stateTime;
    /** Camera shake magnitude; decays toward zero at {@code 4 units/s}. */
    private float camShake;
    /** Lazily created 1×1 white texture for HUD solid-colour fills. */
    private Texture whiteTex;

    // ══════════════════════════════════════════════════════════════════════
    //  Construction
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Allocate all GPU resources for the renderer.
     * Must be called on the GL render thread.
     *
     * @param width  Initial viewport width in pixels
     * @param height Initial viewport height in pixels
     */
    public GameRenderer(int width, int height) {
        vw = width;
        vh = height;

        // ── Camera ──────────────────────────────────────────────────────
        camera = new PerspectiveCamera(60f, width, height);
        resetCamera();

        // ── Lighting ────────────────────────────────────────────────────
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight,
                0.07f, 0.07f, 0.14f, 1f));
        // Cool directional fill from upper-left
        environment.add(new DirectionalLight()
                .set(new Color(0.25f, 0.35f, 0.70f, 1f),
                     new Vector3(-0.4f, -1f, -0.5f).nor()));
        // Blue overhead point light — illuminates the board surface
        environment.add(new PointLight()
                .set(new Color(0.12f, 0.35f, 0.90f, 1f),
                     new Vector3(0f, 6f, 0f), 120f));
        // Warm red fill from far corner — visually separates the two sides
        environment.add(new PointLight()
                .set(new Color(0.50f, 0.06f, 0.06f, 1f),
                     new Vector3(7f, 6f, 7f), 120f));

        // ── Core objects ────────────────────────────────────────────────
        modelBatch = new ModelBatch();
        shapes     = new ShapeRenderer();
        shapes.setAutoShapeType(true);
        particles  = new ParticleSystem();
        uiBatch    = new SpriteBatch();

        // ── Fonts ────────────────────────────────────────────────────────
        fontSmall = new BitmapFont(); fontSmall.getData().setScale(1.3f);
        fontMed   = new BitmapFont(); fontMed  .getData().setScale(1.9f);
        fontLarge = new BitmapFont(); fontLarge.getData().setScale(3.0f);

        // ── Post-processing ──────────────────────────────────────────────
        bloom = new BloomEffect(width, height);

        // ── Scene geometry ───────────────────────────────────────────────
        buildModels();
        buildTileInstances();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Model factory
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Build the shared {@link Model} objects used by all board pieces.
     * Called once during construction.
     */
    private void buildModels() {
        ModelBuilder mb = new ModelBuilder();
        final long VA = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        float ts = TILE - TILE_GAP;  // effective tile render size

        // ── Board tile (dark flat box) ───────────────────────────────────
        tileModel = mb.createBox(ts, TILE_H, ts,
            new Material(
                ColorAttribute.createDiffuse(TILE_A),
                ColorAttribute.createEmissive(0.05f, 0.06f, 0.18f, 1f)
            ), VA);
        ownedModels.add(tileModel);

        // ── Tile highlight overlay (slightly larger, emissive only) ──────
        tileHighModel = mb.createBox(ts + 0.05f, TILE_H + 0.012f, ts + 0.05f,
            new Material(
                ColorAttribute.createDiffuse(Color.CLEAR),
                ColorAttribute.createEmissive(HL_MOVE),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 0.55f)
            ), VA);
        ownedModels.add(tileHighModel);

        // ── Mirror (thin elongated bar, rotated to 45° or -45° in code) ─
        mirrorModel = mb.createBox(0.88f, 0.065f, 0.13f,
            new Material(
                ColorAttribute.createDiffuse(0.55f, 0.85f, 1.00f, 1f),
                ColorAttribute.createEmissive(0.20f, 0.50f, 0.75f, 1f)
            ), VA);
        ownedModels.add(mirrorModel);

        // ── Crystal (sphere approximating a polyhedral gem) ─────────────
        crystalModel = mb.createSphere(0.52f, 0.72f, 0.52f, 8, 8,
            new Material(
                ColorAttribute.createDiffuse(0.70f, 0.85f, 1.00f, 1f),
                ColorAttribute.createEmissive(0.25f, 0.30f, 0.85f, 1f)
            ), VA);
        ownedModels.add(crystalModel);

        // ── Emitter body (low angular box) ───────────────────────────────
        emitterModel = mb.createBox(0.68f, 0.38f, 0.68f,
            new Material(
                ColorAttribute.createDiffuse(0.08f, 0.15f, 0.35f, 1f),
                ColorAttribute.createEmissive(0.04f, 0.18f, 0.55f, 1f)
            ), VA);
        ownedModels.add(emitterModel);

        // ── Emitter barrel (cylinder pointing in the firing direction) ───
        emitterBarrel = mb.createCylinder(0.14f, 0.48f, 0.14f, 8,
            new Material(
                ColorAttribute.createDiffuse(0.10f, 0.20f, 0.45f, 1f),
                ColorAttribute.createEmissive(0.10f, 0.40f, 0.90f, 1f)
            ), VA);
        ownedModels.add(emitterBarrel);
    }

    /**
     * Pre-build one {@link ModelInstance} per tile for the static board grid.
     * These instances are created once and reused every frame — only their
     * {@link Material} attributes are mutated at runtime.
     */
    private void buildTileInstances() {
        tileInst     = new ModelInstance[Board.SIZE][Board.SIZE];
        tileHighInst = new ModelInstance[Board.SIZE][Board.SIZE];

        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                float wx = c + 0.5f;
                float wz = r + 0.5f;

                tileInst[c][r] = new ModelInstance(tileModel);
                tileInst[c][r].transform.setToTranslation(wx, 0f, wz);

                tileHighInst[c][r] = new ModelInstance(tileHighModel);
                tileHighInst[c][r].transform.setToTranslation(wx, 0.006f, wz);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Main render entry point
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render a complete frame.  Must be called once per
     * {@code ApplicationListener.render()} tick.
     *
     * @param gm    The authoritative game state — must not be null
     * @param delta Frame delta time in seconds
     */
    public void render(GameManager gm, float delta) {
        if (gm == null) return;
        stateTime += delta;

        // Initiate a camera-shake burst when the game ends
        if (gm.getGameState() == GameState.GAME_OVER && gm.getEndTimer() < 0.4f) {
            if (camShake < 0.12f) camShake = 0.12f;
        }
        animateCamera(delta);

        // ── Bloom capture begin ──────────────────────────────────────────
        if (bloom.isEnabled()) {
            bloom.captureBegin();
        } else {
            Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        }

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);

        // ── Pass A: 3-D scene ────────────────────────────────────────────
        modelBatch.begin(camera);
        renderTiles(gm);
        renderPieces(gm);
        modelBatch.end();

        // ── Pass B: Laser beams (no depth write, additive blend) ─────────
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        renderLasers(gm);
        Gdx.gl.glDepthMask(true);

        // ── Pass C: Particles ────────────────────────────────────────────
        spawnParticles(gm);
        particles.update(delta);
        particles.render(camera);

        // ── Bloom composite ──────────────────────────────────────────────
        if (bloom.isEnabled()) {
            bloom.captureEnd();
            bloom.render();
        }

        // ── Pass D: 2-D HUD (always on top of bloom) ─────────────────────
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        renderHUD(gm);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Board / tile rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render the 8×8 tile grid.
     * Pre-built instances are reused; only diffuse/emissive material
     * attributes are updated per frame to avoid GC pressure.
     */
    private void renderTiles(GameManager gm) {
        Board board = gm.getBoard();

        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                Tile  tile = board.getTile(c, r);

                // Checkerboard base
                Color base = ((c + r) % 2 == 0) ? TILE_A : TILE_B;
                tileInst[c][r].materials.get(0)
                        .set(ColorAttribute.createDiffuse(base.r, base.g, base.b, 1f));
                modelBatch.render(tileInst[c][r], environment);

                // Highlight / selection glow overlay (fades in/out via alpha)
                float alpha = tile.getHighlightAlpha();
                if (alpha > 0.01f) {
                    Color gc = tile.isSelected() ? HL_SEL : HL_MOVE;
                    tileHighInst[c][r].materials.get(0).set(
                            ColorAttribute.createEmissive(
                                    gc.r * alpha, gc.g * alpha, gc.b * alpha, 1f),
                            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE,
                                    alpha * 0.65f));
                    modelBatch.render(tileHighInst[c][r], environment);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Piece rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render all pieces (emitters, crystals, mirrors) for both players.
     * {@link ModelInstance} objects are created per piece per frame; with
     * only ~10 pieces total this never becomes a performance bottleneck.
     */
    private void renderPieces(GameManager gm) {
        for (Player player : gm.getPlayers()) {
            Color pc = player.getColor();
            renderEmitter(player, pc);
            renderCrystal(player, pc);
            renderMirrors(player, pc);
        }
    }

    /** Render the laser emitter body and directional barrel for one player. */
    private void renderEmitter(Player player, Color pc) {
        LaserEmitter em   = player.getEmitter();
        float        glow = em.getChargeGlow(); // [0.5, 1.0] sine-pulsed

        // Body
        ModelInstance body = new ModelInstance(emitterModel);
        body.materials.get(0).set(
                ColorAttribute.createDiffuse(pc.r * 0.25f, pc.g * 0.25f, pc.b * 0.25f, 1f),
                ColorAttribute.createEmissive(pc.r * glow,  pc.g * glow,  pc.b * glow,  1f));
        body.transform.setToTranslation(em.getCol() + 0.5f, OBJ_Y - 0.05f, em.getRow() + 0.5f);
        modelBatch.render(body, environment);

        // Barrel — positioned and oriented to face the firing direction
        ModelInstance barrel = new ModelInstance(emitterBarrel);
        float barrelGlow = glow * 1.4f;
        barrel.materials.get(0).set(
                ColorAttribute.createDiffuse(pc.r * 0.15f, pc.g * 0.15f, pc.b * 0.15f, 1f),
                ColorAttribute.createEmissive(pc.r * barrelGlow, pc.g * barrelGlow,
                        pc.b * barrelGlow, 1f));

        // Compute barrel tip offset based on direction
        Direction dir  = em.getDirection();
        float     bx   = em.getCol() + 0.5f;
        float     bz   = em.getRow() + 0.5f;
        float     offX = 0f, offZ = 0f;
        float     rotZ = 0f;   // rotation around Z axis to tip barrel
        if      (dir == Direction.RIGHT) { offX =  0.28f; rotZ =  90f; }
        else if (dir == Direction.LEFT)  { offX = -0.28f; rotZ = -90f; }
        else if (dir == Direction.UP)    { offZ = -0.28f; rotZ =   0f; }
        else if (dir == Direction.DOWN)  { offZ =  0.28f; rotZ = 180f; }

        barrel.transform
                .setToTranslation(bx + offX, OBJ_Y + 0.05f, bz + offZ)
                .rotate(Vector3.Z, rotZ);
        modelBatch.render(barrel, environment);
    }

    /** Render the crystal for one player, with idle pulse and explode animation. */
    private void renderCrystal(Player player, Color pc) {
        Crystal cr = player.getCrystal();
        if (cr.isDestroyed()) return;

        ModelInstance inst  = new ModelInstance(crystalModel);
        float         pulse = cr.getPulseScale();
        float         bright = cr.getPulseBrightness();
        float         exP    = cr.getExplodeProgress();

        // Colour shifts to hot orange/white while exploding
        Color emissive;
        if (cr.isExploding()) {
            emissive = new Color(1f, 0.5f + exP * 0.5f, exP * 0.3f, 1f);
        } else {
            emissive = new Color(pc.r * bright, pc.g * bright, pc.b * bright, 1f);
        }

        inst.materials.get(0).set(
                ColorAttribute.createDiffuse(pc.r * 0.35f, pc.g * 0.35f, pc.b * 0.35f, 1f),
                ColorAttribute.createEmissive(emissive));

        // Scale swells dramatically during explosion
        float scale = pulse * (cr.isExploding() ? (1f + exP * 3f) : 1f);
        // Gentle vertical bob — offset by player index to avoid sync
        float bob   = 0.06f * MathUtils.sin(stateTime * 2.2f + player.getIndex() * MathUtils.PI);

        inst.transform
                .setToTranslation(cr.getCol() + 0.5f, 0.34f + bob, cr.getRow() + 0.5f)
                .scale(scale, scale, scale);
        modelBatch.render(inst, environment);
    }

    /** Render all mirrors for one player. */
    private void renderMirrors(Player player, Color pc) {
        for (Mirror m : player.getMirrors()) {
            ModelInstance inst = new ModelInstance(mirrorModel);
            inst.materials.get(0).set(
                    ColorAttribute.createDiffuse(0.50f, 0.80f, 0.95f, 1f),
                    ColorAttribute.createEmissive(pc.r * 0.28f, pc.g * 0.28f, pc.b * 0.28f, 1f));
            // getVisualAngle() smoothly animates toward the target angle
            inst.transform
                    .setToTranslation(m.getCol() + 0.5f, OBJ_Y, m.getRow() + 0.5f)
                    .rotate(Vector3.Y, m.getVisualAngle());
            modelBatch.render(inst, environment);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Laser beam rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render all laser segments for both players.
     *
     * <p>Each segment is drawn as three overlapping quad layers:
     * a wide outer corona, a medium mid-glow, and a tight bright core.
     * The additive blend mode causes layers to accumulate, producing a
     * convincing neon-light effect without any custom shaders.</p>
     *
     * <p>A per-player sine pulse keeps the two beams visually distinct even
     * when their colours are similar.</p>
     */
    private void renderLasers(GameManager gm) {
        if (gm.getLaserSystem() == null) return;

        shapes.setProjectionMatrix(camera.combined);

        for (int p = 0; p < 2; p++) {
            Color lc    = gm.getPlayers()[p].getLaserColor();
            float pulse = 0.72f + 0.28f * MathUtils.sin(stateTime * 9f + p * MathUtils.PI);

            List<LaserSegment> segs = gm.getLaserSystem().getSegments(p);
            for (LaserSegment seg : segs) {
                float x1 = seg.startCol + 0.5f;
                float z1 = seg.startRow + 0.5f;
                float x2 = seg.endCol   + 0.5f;
                float z2 = seg.endRow   + 0.5f;

                for (float[] layer : LASER_LAYERS) {
                    float halfW = layer[0];
                    float am    = layer[1];
                    // Bright core gets a white-shifted tint
                    Color col = (halfW < 0.05f)
                        ? new Color(lc.r * 0.5f + 0.5f,
                                    lc.g * 0.5f + 0.5f,
                                    lc.b * 0.5f + 0.5f, 1f)
                        : lc;
                    drawBeamQuad(x1, z1, x2, z2, col, halfW, am * pulse);
                }
            }
        }
    }

    /**
     * Draw one laser beam layer as a world-space quad.
     *
     * <p>A perpendicular offset in the x-z plane gives the segment width.
     * Two triangles are submitted to the {@link ShapeRenderer}, which is
     * configured with the 3-D camera's combined projection matrix.
     * Passing {@code (worldX, worldZ)} as the {@code (x, y)} parameters of
     * {@link ShapeRenderer#triangle} correctly places vertices on the board
     * surface because the camera matrix maps world z onto screen y.</p>
     *
     * @param x1        Segment start world X
     * @param z1        Segment start world Z
     * @param x2        Segment end world X
     * @param z2        Segment end world Z
     * @param color     Beam colour
     * @param halfWidth Half-width of the quad in world units
     * @param alpha     Layer alpha
     */
    private void drawBeamQuad(float x1, float z1, float x2, float z2,
                               Color color, float halfWidth, float alpha) {
        float dx  = x2 - x1;
        float dz  = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4f) return;

        // Perpendicular unit vector in the x-z plane, scaled to halfWidth
        float px = -dz / len * halfWidth;
        float pz =  dx / len * halfWidth;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(color.r, color.g, color.b, alpha);
        // Quad as two CCW triangles:  v0---v3
        //                             |    |
        //                             v1---v2
        shapes.triangle(
                x1 - px, z1 - pz,   // v0
                x1 + px, z1 + pz,   // v1
                x2 + px, z2 + pz);  // v2
        shapes.triangle(
                x1 - px, z1 - pz,   // v0
                x2 + px, z2 + pz,   // v2
                x2 - px, z2 - pz);  // v3
        shapes.end();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Particle spawning
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Spawn new particle bursts when a game action was just taken.
     * Reflection sparks appear at every mirror that deflected a beam;
     * a full explosion burst appears when a crystal is struck.
     */
    private void spawnParticles(GameManager gm) {
        if (!gm.actionJustTaken()) return;
        if (gm.getLaserSystem() == null) return;

        for (int p = 0; p < 2; p++) {
            Color              lc   = gm.getPlayers()[p].getLaserColor();
            List<LaserSegment> segs = gm.getLaserSystem().getSegments(p);

            for (LaserSegment seg : segs) {
                if (!gm.getBoard().isValid(seg.endCol, seg.endRow)) continue;
                Tile t = gm.getBoard().getTile(seg.endCol, seg.endRow);

                if (t.hasMirror()) {
                    particles.addReflectionSparks(
                            seg.endCol + 0.5f, seg.endRow + 0.5f, lc);
                }
                if (seg.hitCrystal) {
                    particles.addCrystalExplosion(
                            seg.endCol + 0.5f, seg.endRow + 0.5f, lc);
                    camShake = 0.35f;
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HUD rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render all screen-space UI elements on top of the 3-D scene.
     * Uses a standard {@link SpriteBatch} with an orthographic projection.
     */
    private void renderHUD(GameManager gm) {
        float W = vw;
        float H = vh;
        uiBatch.getProjectionMatrix().setToOrtho2D(0, 0, W, H);
        uiBatch.begin();

        // ── Turn indicator (centred, current player's colour) ────────────
        if (gm.getGameState() == GameState.PLAYING) {
            Player cur  = gm.getCurrentPlayer();
            Color  pc   = cur.getColor();
            fontMed.setColor(pc);
            String txt  = cur.getName() + " — YOUR TURN";
            layout.setText(fontMed, txt);
            fontMed.draw(uiBatch, txt, (W - layout.width) / 2f, H - 14f);
        }

        // ── Player name labels (top-left and top-right corners) ──────────
        Player p0 = gm.getPlayers()[0];
        Player p1 = gm.getPlayers()[1];

        fontSmall.getData().setScale(1.3f);
        fontSmall.setColor(P0_HUD);
        fontSmall.draw(uiBatch, p0.getName() + " ◀ BLUE", 14f, H - 14f);

        fontSmall.setColor(P1_HUD);
        String p1Label = p1.getName() + " ▶ RED";
        layout.setText(fontSmall, p1Label);
        fontSmall.draw(uiBatch, p1Label, W - layout.width - 14f, H - 14f);

        // ── Selection feedback (shown when a mirror is selected) ─────────
        if (gm.getSelectedMirror() != null) {
            Color  cc    = gm.getCurrentPlayer().getColor();
            float  pulse = 0.85f + 0.15f * MathUtils.sin(stateTime * 5f);
            fontSmall.getData().setScale(1.25f);
            fontSmall.setColor(cc.r, cc.g, cc.b, pulse);
            fontSmall.draw(uiBatch,
                    "Mirror selected — click again to ROTATE, or move to a blue tile",
                    14f, 44f);
        }

        // ── Control hints (bottom, smaller) ─────────────────────────────
        fontSmall.getData().setScale(1.1f);
        fontSmall.setColor(0.45f, 0.50f, 0.75f, 0.85f);
        String hint = "Click mirror to SELECT  |  Click again to ROTATE  |  "
                    + "Click blue tile to MOVE  |  R = rotate  |  ESC = menu";
        layout.setText(fontSmall, hint);
        fontSmall.draw(uiBatch, hint, (W - layout.width) / 2f, 18f);

        // Restore scale before win overlay (which uses its own scales)
        fontSmall.getData().setScale(1.3f);

        // ── Win overlay ──────────────────────────────────────────────────
        if (gm.getGameState() == GameState.GAME_OVER) {
            drawWinOverlay(gm, W, H);
        }

        uiBatch.end();
    }

    /**
     * Render the translucent win screen.
     *
     * <p>The overlay fades in over the first 0.7 s after the game ends.
     * Text is revealed in two stages:
     * the dim panel appears first, then the title and subtitle fade in
     * once the panel is at roughly 40% of full opacity.</p>
     */
    private void drawWinOverlay(GameManager gm, float W, float H) {
        int winnerIdx = gm.getWinner();
        if (winnerIdx < 0 || winnerIdx > 1) return;

        Player winner = gm.getPlayers()[winnerIdx];
        Color  pc     = winner.getColor();
        float  t      = Math.min(gm.getEndTimer() / 0.7f, 1f);
        float  alpha  = t * t; // ease-in quad

        // Semi-transparent dark panel
        uiBatch.setColor(0f, 0f, 0f, alpha * 0.60f);
        uiBatch.draw(getWhiteTex(), 0f, 0f, W, H);
        uiBatch.setColor(Color.WHITE);

        if (alpha > 0.4f) {
            float reveal = (alpha - 0.4f) / 0.6f; // re-normalised [0,1]

            // ── Glow shadow layer ─────────────────────────────────────
            String title = winner.getName() + "  WINS!";
            fontLarge.getData().setScale(3.0f);
            fontLarge.setColor(pc.r * 0.4f, pc.g * 0.4f, pc.b * 0.4f,
                    reveal * (0.5f + 0.5f * MathUtils.sin(stateTime * 4f)));
            layout.setText(fontLarge, title);
            float tx = (W - layout.width) / 2f;
            float ty = H / 2f + 80f;
            fontLarge.draw(uiBatch, title, tx + 3f, ty - 3f);

            // ── Main title ────────────────────────────────────────────
            fontLarge.setColor(pc.r, pc.g, pc.b, reveal);
            fontLarge.draw(uiBatch, title, tx, ty);

            // ── Subtitle and restart prompt ───────────────────────────
            if (reveal > 0.6f) {
                float subAlpha = (reveal - 0.6f) / 0.4f;

                fontMed.getData().setScale(1.9f);
                fontMed.setColor(0.85f, 0.90f, 1.00f, subAlpha);
                String sub = "Crystal Destroyed!";
                layout.setText(fontMed, sub);
                fontMed.draw(uiBatch, sub, (W - layout.width) / 2f, H / 2f + 18f);

                // Blink the restart prompt
                float blink = (stateTime % 1.4f) < 0.9f ? subAlpha : 0f;
                fontSmall.getData().setScale(1.4f);
                fontSmall.setColor(0.70f, 0.80f, 1.00f, blink);
                String restart = "[ Press R to play again  |  ESC for menu ]";
                layout.setText(fontSmall, restart);
                fontSmall.draw(uiBatch, restart, (W - layout.width) / 2f, H / 2f - 30f);
                fontSmall.getData().setScale(1.3f); // restore
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Camera management
    // ══════════════════════════════════════════════════════════════════════

    /** Reset camera to the standard board-overview position. */
    private void resetCamera() {
        camera.position.set(CAM_X, CAM_Y, CAM_Z);
        camera.lookAt(LOOK_X, 0f, LOOK_Z);
        camera.up.set(Vector3.Y);
        camera.near = 0.5f;
        camera.far  = 100f;
        camera.update();
    }

    /**
     * Apply subtle idle camera drift and decay any active shake.
     * The drift uses independent sine/cosine oscillators so the movement
     * feels organic rather than periodic.
     */
    private void animateCamera(float delta) {
        float t  = stateTime * 0.04f;
        float cx = CAM_X + MathUtils.sin(t)         * 0.4f;
        float cz = CAM_Z + MathUtils.cos(t * 0.7f)  * 0.3f;

        if (camShake > 0f) {
            camShake = Math.max(0f, camShake - delta * 4f);
            cx += MathUtils.random(-camShake, camShake) * 0.6f;
            cz += MathUtils.random(-camShake, camShake) * 0.6f;
        }

        camera.position.set(cx, CAM_Y, cz);
        camera.lookAt(LOOK_X, 0f, LOOK_Z);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Screen → tile picking
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Convert a screen-space mouse/touch position to board tile coordinates.
     *
     * <p>Builds a pick ray from the perspective camera through the given screen
     * pixel, intersects it with the {@code y = 0} world plane, and converts the
     * resulting world position to {@code (col, row)} by flooring.</p>
     *
     * @param screenX Pixel X from the left edge (LibGDX convention)
     * @param screenY Pixel Y from the top edge (LibGDX convention)
     * @return {@code int[]{col, row}} if the click landed on the board,
     *         {@code null} if it missed.
     */
    public int[] screenToTile(int screenX, int screenY) {
        Ray ray = camera.getPickRay(screenX, screenY);
        if (Intersector.intersectRayPlane(ray, boardPlane, hitPoint)) {
            int col = (int) Math.floor(hitPoint.x);
            int row = (int) Math.floor(hitPoint.z);
            if (col >= 0 && col < Board.SIZE && row >= 0 && row < Board.SIZE) {
                return new int[]{ col, row };
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public accessors
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Expose the particle system so external callers (e.g. the game screen)
     * can trigger custom effects directly.
     *
     * @return The shared {@link ParticleSystem}
     */
    public ParticleSystem getParticles() {
        return particles;
    }

    /**
     * Expose the bloom effect so callers can toggle or tune it at runtime.
     *
     * @return The {@link BloomEffect} instance (may be disabled if FBO creation failed)
     */
    public BloomEffect getBloom() {
        return bloom;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Resize
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Adapt the renderer to a new viewport size.
     * Must be called from {@code ApplicationListener.resize()}.
     *
     * @param width  New viewport width in pixels
     * @param height New viewport height in pixels
     */
    public void resize(int width, int height) {
        vw = width;
        vh = height;
        camera.viewportWidth  = width;
        camera.viewportHeight = height;
        camera.update();
        if (bloom != null) bloom.resize(width, height);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Lazily create and return a 1×1 white {@link Texture} used for
     * solid-colour HUD fills via {@link SpriteBatch#draw}.
     */
    private Texture getWhiteTex() {
        if (whiteTex == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whiteTex = new Texture(pm);
            pm.dispose();
        }
        return whiteTex;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Disposal
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Release all GPU resources owned by this renderer.
     * Must be called when the game screen is hidden or the application exits.
     * The renderer must not be used after this call.
     */
    @Override
    public void dispose() {
        modelBatch.dispose();
        shapes.dispose();
        uiBatch.dispose();
        fontSmall.dispose();
        fontMed.dispose();
        fontLarge.dispose();
        bloom.dispose();
        particles.dispose();
        for (Disposable d : ownedModels) d.dispose();
        if (whiteTex != null) whiteTex.dispose();
    }
}
