package com.mirrorduel.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * Manages spark/particle effects for:
 *   - Laser reflection points  (small coloured sparks)
 *   - Crystal explosion        (large burst)
 *   - Idle crystal shimmer     (subtle ambient flicker)
 *
 * All particles live in 3-D world space.  The render() method uses a
 * ShapeRenderer whose projection matrix is set to the game camera's combined
 * matrix, so particles appear correctly situated on the board.  Because
 * ShapeRenderer.circle() operates in the x-y plane we project each particle
 * into screen-space first and draw there, using an orthographic-screen
 * projection as a fallback when a true world-space circle in the x-z plane
 * is not needed for this top-down-ish camera.
 *
 * Gravity pulls particles downward along the world Y axis at 2.5 m/s².
 * Particles are pooled to avoid per-frame allocation.
 */
public class ParticleSystem {

    // ------------------------------------------------------------------
    //  Particle data object
    // ------------------------------------------------------------------

    /** Plain-data value object pooled to avoid GC pressure. */
    public static class Particle implements Pool.Poolable {
        /** World-space position. */
        public float x, y, z;
        /** World-space velocity (units / second). */
        public float vx, vy, vz;
        /** Age and total lifetime (seconds). */
        public float life, maxLife;
        /** Visual radius in world units. */
        public float size;
        /** RGBA colour components in [0, 1]. */
        public float r, g, b, a;
        /** Whether this particle is currently in use. */
        public boolean active;

        @Override
        public void reset() {
            active  = false;
            life    = 0f;
            x = y = z = 0f;
            vx = vy = vz = 0f;
            size = 0f;
            r = g = b = a = 0f;
        }
    }

    // ------------------------------------------------------------------
    //  Constants
    // ------------------------------------------------------------------

    /** Hard cap on simultaneous live particles. */
    private static final int MAX_PARTICLES = 512;
    /** World-space Y of the board surface (particles spawn just above). */
    private static final float SPAWN_Y     = 0.12f;
    /** Gravity acceleration in world-units per second squared. */
    private static final float GRAVITY     = 2.5f;

    // ------------------------------------------------------------------
    //  Fields
    // ------------------------------------------------------------------

    /** All currently active particles. */
    private final Array<Particle> particles = new Array<>(false, MAX_PARTICLES);

    /** Object pool backing the particle list. */
    private final Pool<Particle> pool = new Pool<Particle>(MAX_PARTICLES) {
        @Override
        protected Particle newObject() {
            return new Particle();
        }
    };

    /** Shared ShapeRenderer used exclusively by this system. */
    private final ShapeRenderer shape;

    // ------------------------------------------------------------------
    //  Construction / disposal
    // ------------------------------------------------------------------

    public ParticleSystem() {
        shape = new ShapeRenderer();
        shape.setAutoShapeType(true);
    }

    // ------------------------------------------------------------------
    //  Particle emitters
    // ------------------------------------------------------------------

    /**
     * Emit a small burst of sparks at a laser-mirror reflection point.
     *
     * @param wx    World X of the reflection (tile centre: col + 0.5)
     * @param wz    World Z of the reflection (tile centre: row + 0.5)
     * @param color Laser colour to tint the sparks
     */
    public void addReflectionSparks(float wx, float wz, Color color) {
        int count = 8 + MathUtils.random(6);          // 8–14 sparks
        for (int i = 0; i < count; i++) {
            if (particles.size >= MAX_PARTICLES) break;

            Particle p = pool.obtain();
            p.active  = true;

            // Spawn at reflection height
            p.x = wx;  p.y = SPAWN_Y;  p.z = wz;

            // Random outward direction in the horizontal plane
            float angle = MathUtils.random(MathUtils.PI2);
            float speed = 0.8f + MathUtils.random(1.6f);
            p.vx = MathUtils.cos(angle) * speed * 0.5f;
            p.vy = 0.4f + MathUtils.random(0.8f);     // slight upward kick
            p.vz = MathUtils.sin(angle) * speed * 0.5f;

            p.life    = 0f;
            p.maxLife = 0.3f + MathUtils.random(0.4f); // 0.3 – 0.7 s
            p.size    = 0.04f + MathUtils.random(0.04f);

            p.r = color.r;  p.g = color.g;  p.b = color.b;  p.a = 1f;

            particles.add(p);
        }
    }

    /**
     * Emit a dramatic explosion burst when a crystal is destroyed.
     *
     * @param wx    World X of the crystal centre
     * @param wz    World Z of the crystal centre
     * @param color Laser colour that struck the crystal
     */
    public void addCrystalExplosion(float wx, float wz, Color color) {
        final int count = 60;
        for (int i = 0; i < count; i++) {
            if (particles.size >= MAX_PARTICLES) break;

            Particle p = pool.obtain();
            p.active = true;

            p.x = wx;  p.y = 0.3f;  p.z = wz;

            float angle = MathUtils.random(MathUtils.PI2);
            float elev  = MathUtils.random(-0.5f, 0.5f);
            float speed = 1f + MathUtils.random(3f);
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = elev * speed + 0.5f;
            p.vz = MathUtils.sin(angle) * speed;

            p.life    = 0f;
            p.maxLife = 0.6f + MathUtils.random(0.8f); // 0.6 – 1.4 s

            // Slightly randomise hue around the crystal's colour
            p.size = 0.05f + MathUtils.random(0.10f);
            p.r    = Math.min(1f, color.r + MathUtils.random(0.2f));
            p.g    = Math.min(1f, color.g + MathUtils.random(0.2f));
            p.b    = Math.min(1f, color.b + MathUtils.random(0.2f));
            p.a    = 1f;

            particles.add(p);
        }
    }

    /**
     * Emit a single subtle shimmer particle around an idle crystal for ambient
     * life.  Call every few frames from the crystal's world position.
     *
     * @param wx    World X of the crystal
     * @param wz    World Z of the crystal
     * @param color Crystal's player colour
     */
    public void addCrystalShimmer(float wx, float wz, Color color) {
        if (particles.size >= MAX_PARTICLES) return;

        Particle p = pool.obtain();
        p.active = true;

        // Spawn in a small random ring around the crystal
        float angle  = MathUtils.random(MathUtils.PI2);
        float radius = 0.1f + MathUtils.random(0.2f);
        p.x = wx + MathUtils.cos(angle) * radius;
        p.y = 0.3f + MathUtils.random(0.3f);
        p.z = wz + MathUtils.sin(angle) * radius;

        // Slow drift upward
        p.vx = MathUtils.random(-0.1f, 0.1f);
        p.vy = 0.1f + MathUtils.random(0.2f);
        p.vz = MathUtils.random(-0.1f, 0.1f);

        p.life    = 0f;
        p.maxLife = 0.5f + MathUtils.random(0.5f);
        p.size    = 0.02f + MathUtils.random(0.02f);
        p.r = color.r;  p.g = color.g;  p.b = color.b;  p.a = 0.8f;

        particles.add(p);
    }

    // ------------------------------------------------------------------
    //  Update
    // ------------------------------------------------------------------

    /**
     * Advance all active particles by {@code delta} seconds.
     * Dead particles are freed back to the pool.
     * Must be called once per frame before {@link #render}.
     *
     * @param delta Frame delta time in seconds
     */
    public void update(float delta) {
        for (int i = particles.size - 1; i >= 0; i--) {
            Particle p = particles.get(i);

            if (!p.active) {
                particles.removeIndex(i);
                pool.free(p);
                continue;
            }

            p.life += delta;
            if (p.life >= p.maxLife) {
                p.active = false;
                particles.removeIndex(i);
                pool.free(p);
                continue;
            }

            // Integrate position
            p.x  += p.vx * delta;
            p.y  += p.vy * delta;
            p.z  += p.vz * delta;

            // Apply gravity
            p.vy -= GRAVITY * delta;

            // Fade alpha linearly with remaining lifetime
            p.a = 1f - (p.life / p.maxLife);
        }
    }

    // ------------------------------------------------------------------
    //  Render
    // ------------------------------------------------------------------

    /**
     * Render all active particles as small lit circles.
     *
     * Additive blending is used so overlapping particles accumulate brightness,
     * reinforcing the neon aesthetic.
     *
     * The ShapeRenderer is configured with the 3-D camera's combined projection
     * matrix.  Particles are drawn as circles in the X-Z plane (Y is the up
     * axis), which is correct for a top-down-ish board camera.  Because
     * {@link ShapeRenderer#circle} uses its first two parameters as X and Y of
     * the local coordinate system, and our camera looks down from above, we
     * map world X → shape X and world Z → shape Y so particles project onto
     * the board surface correctly.
     *
     * <p>Call this AFTER {@code ModelBatch.end()} and BEFORE any
     * {@code SpriteBatch} calls so blending state is consistent.</p>
     *
     * @param camera The game's {@link PerspectiveCamera}
     */
    public void render(PerspectiveCamera camera) {
        if (particles.size == 0) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // additive for neon glow

        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0, n = particles.size; i < n; i++) {
            Particle p = particles.get(i);
            if (!p.active) continue;

            // Quadratic alpha fall-off gives a sharper, punchier look
            float alpha = p.a * p.a;
            shape.setColor(p.r, p.g, p.b, alpha);

            // Draw circle centred at (p.x, p.z) in the board's x-z plane.
            // 6 segments is sufficient for small, fast-moving sparks.
            shape.circle(p.x, p.z, p.size, 6);
        }

        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ------------------------------------------------------------------
    //  Accessors / utilities
    // ------------------------------------------------------------------

    /** @return The number of currently active particles (useful for debug HUD). */
    public int getParticleCount() {
        return particles.size;
    }

    /**
     * Immediately remove all active particles.
     * Call this when resetting or restarting the game.
     */
    public void clear() {
        for (int i = 0, n = particles.size; i < n; i++) {
            pool.free(particles.get(i));
        }
        particles.clear();
    }

    /** Release all native resources held by this system. */
    public void dispose() {
        clear();
        shape.dispose();
    }
}
