package com.mirrorduel.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Two-pass separable Gaussian bloom post-processor.
 *
 * <h3>Pipeline</h3>
 * <ol>
 *   <li><b>Capture</b> – the entire 3-D scene is rendered into an off-screen
 *       FBO ({@link #sceneFbo}) at full resolution.</li>
 *   <li><b>Threshold</b> – a fragment shader discards pixels below a luminance
 *       threshold, writing only bright (glowing) pixels at half resolution into
 *       {@link #blurFboH}.</li>
 *   <li><b>Horizontal blur</b> – a 9-tap Gaussian kernel is applied
 *       horizontally; result stored in {@link #blurFboV}.</li>
 *   <li><b>Vertical blur</b> – same kernel applied vertically; result stored
 *       back in {@link #blurFboH}.</li>
 *   <li><b>Composite</b> – the full-res scene and the half-res bloom buffer are
 *       additively combined and drawn to the default framebuffer.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 *   bloom.captureBegin();
 *   // ... render 3-D scene, lasers, particles ...
 *   bloom.captureEnd();
 *   bloom.render();   // composites result to screen
 * }</pre>
 *
 * <p>If FBO creation fails (unsupported hardware) the effect is automatically
 * disabled and the caller should render the scene directly to the screen.</p>
 */
public class BloomEffect {

    // ------------------------------------------------------------------
    //  FBOs
    // ------------------------------------------------------------------

    /** Full-resolution scene capture target. */
    private FrameBuffer sceneFbo;
    /**
     * Half-resolution intermediate buffer.
     * Re-used as: (a) threshold output, (b) final vertical-blur output.
     */
    private FrameBuffer blurFboH;
    /** Half-resolution horizontal-blur output. */
    private FrameBuffer blurFboV;

    // ------------------------------------------------------------------
    //  Shaders
    // ------------------------------------------------------------------

    private final ShaderProgram thresholdShader;
    private final ShaderProgram blurShader;
    private final ShaderProgram compositeShader;

    // ------------------------------------------------------------------
    //  SpriteBatch for full-screen quads
    // ------------------------------------------------------------------

    /** Dedicated batch – avoids interfering with the game's main SpriteBatch. */
    private final SpriteBatch fullBatch = new SpriteBatch();

    // ------------------------------------------------------------------
    //  State
    // ------------------------------------------------------------------

    private int  width, height;
    private boolean enabled = true;

    // ------------------------------------------------------------------
    //  Tunable parameters
    // ------------------------------------------------------------------

    /**
     * Luminance threshold in [0, 1].
     * Pixels brighter than this value are kept in the bloom buffer.
     * Lower values create wider, dreamier bloom; higher values restrict
     * bloom to only the brightest highlights.
     */
    private float threshold     = 0.4f;

    /**
     * How strongly the bloom buffer is added back on top of the scene.
     * 1.0 = subtle glow; 2.0+ = vivid neon corona.
     */
    private float bloomStrength = 1.5f;

    // ------------------------------------------------------------------
    //  GLSL source strings
    // ------------------------------------------------------------------

    /**
     * Standard pass-through vertex shader compatible with LibGDX's
     * {@link SpriteBatch} vertex layout.
     */
    private static final String VERT =
        "attribute vec4 a_position;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "varying   vec2 v_texCoords;\n" +
        "uniform   mat4 u_projTrans;\n" +
        "void main() {\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = u_projTrans * a_position;\n" +
        "}\n";

    /**
     * Luminance-threshold fragment shader.
     * Pixels whose perceived brightness exceeds {@code u_threshold} are passed
     * through unchanged; all others are replaced with transparent black.
     * Uses the BT.709 luminance coefficients.
     */
    private static final String THRESHOLD_FRAG =
        "#ifdef GL_ES\n"                                                          +
        "precision mediump float;\n"                                              +
        "#endif\n"                                                                +
        "varying   vec2      v_texCoords;\n"                                      +
        "uniform   sampler2D u_texture;\n"                                        +
        "uniform   float     u_threshold;\n"                                      +
        "void main() {\n"                                                         +
        "    vec4  col        = texture2D(u_texture, v_texCoords);\n"             +
        "    float brightness = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));\n"   +
        "    gl_FragColor = (brightness > u_threshold) ? col : vec4(0.0);\n"     +
        "}\n";

    /**
     * Separable 9-tap Gaussian blur fragment shader.
     * Set {@code u_texOffset} to {@code (1/width, 0)} for the horizontal pass
     * and {@code (0, 1/height)} for the vertical pass.
     *
     * <p>Kernel weights (sum ≈ 1.0):</p>
     * <pre>0.051, 0.0918, 0.1227, 0.1945, 0.2270, 0.1945, 0.1227, 0.0918, 0.051</pre>
     */
    private static final String BLUR_FRAG =
        "#ifdef GL_ES\n"                                                              +
        "precision mediump float;\n"                                                  +
        "#endif\n"                                                                    +
        "varying   vec2      v_texCoords;\n"                                          +
        "uniform   sampler2D u_texture;\n"                                            +
        "uniform   vec2      u_texOffset;\n"                                          +
        "void main() {\n"                                                             +
        "    vec4 s = vec4(0.0);\n"                                                   +
        "    s += texture2D(u_texture, v_texCoords - u_texOffset * 4.0) * 0.0510;\n" +
        "    s += texture2D(u_texture, v_texCoords - u_texOffset * 3.0) * 0.0918;\n" +
        "    s += texture2D(u_texture, v_texCoords - u_texOffset * 2.0) * 0.1227;\n" +
        "    s += texture2D(u_texture, v_texCoords - u_texOffset)       * 0.1945;\n" +
        "    s += texture2D(u_texture, v_texCoords)                     * 0.2270;\n" +
        "    s += texture2D(u_texture, v_texCoords + u_texOffset)       * 0.1945;\n" +
        "    s += texture2D(u_texture, v_texCoords + u_texOffset * 2.0) * 0.1227;\n" +
        "    s += texture2D(u_texture, v_texCoords + u_texOffset * 3.0) * 0.0918;\n" +
        "    s += texture2D(u_texture, v_texCoords + u_texOffset * 4.0) * 0.0510;\n" +
        "    gl_FragColor = s;\n"                                                     +
        "}\n";

    /**
     * Additive composite fragment shader.
     * {@code u_texture} (unit 0) = full-resolution scene.
     * {@code u_bloom}   (unit 1) = blurred bright-pass buffer.
     * Output = scene + bloom * bloomStrength, clamped to [0,1].
     */
    private static final String COMPOSITE_FRAG =
        "#ifdef GL_ES\n"                                                              +
        "precision mediump float;\n"                                                  +
        "#endif\n"                                                                    +
        "varying   vec2      v_texCoords;\n"                                          +
        "uniform   sampler2D u_texture;\n"                                            +
        "uniform   sampler2D u_bloom;\n"                                              +
        "uniform   float     u_bloomStrength;\n"                                      +
        "void main() {\n"                                                             +
        "    vec4 scene = texture2D(u_texture, v_texCoords);\n"                       +
        "    vec4 bloom = texture2D(u_bloom,   v_texCoords);\n"                       +
        "    gl_FragColor = clamp(scene + bloom * u_bloomStrength, 0.0, 1.0);\n"      +
        "}\n";

    // ------------------------------------------------------------------
    //  Construction
    // ------------------------------------------------------------------

    /**
     * Create a bloom effect sized for the current viewport.
     *
     * @param width  Initial viewport width in pixels
     * @param height Initial viewport height in pixels
     */
    public BloomEffect(int width, int height) {
        ShaderProgram.pedantic = false;   // don't crash on unused uniforms

        thresholdShader = new ShaderProgram(VERT, THRESHOLD_FRAG);
        if (!thresholdShader.isCompiled()) {
            Gdx.app.error("BloomEffect", "Threshold shader:\n" + thresholdShader.getLog());
        }

        blurShader = new ShaderProgram(VERT, BLUR_FRAG);
        if (!blurShader.isCompiled()) {
            Gdx.app.error("BloomEffect", "Blur shader:\n" + blurShader.getLog());
        }

        compositeShader = new ShaderProgram(VERT, COMPOSITE_FRAG);
        if (!compositeShader.isCompiled()) {
            Gdx.app.error("BloomEffect", "Composite shader:\n" + compositeShader.getLog());
        }

        resize(width, height);
    }

    // ------------------------------------------------------------------
    //  Resize
    // ------------------------------------------------------------------

    /**
     * Recreate FBOs to match the new viewport dimensions.
     * Must be called from {@code ApplicationListener.resize()}.
     *
     * @param w New viewport width in pixels
     * @param h New viewport height in pixels
     */
    public void resize(int w, int h) {
        this.width  = w;
        this.height = h;
        disposeFbos();

        try {
            sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888, w,     h,     false);
            blurFboH = new FrameBuffer(Pixmap.Format.RGBA8888, w / 2, h / 2, false);
            blurFboV = new FrameBuffer(Pixmap.Format.RGBA8888, w / 2, h / 2, false);
            enabled  = true;
        } catch (Exception e) {
            enabled = false;
            Gdx.app.log("BloomEffect",
                "FrameBuffer creation failed – bloom disabled: " + e.getMessage());
        }

        // Keep the batch's projection in sync with the new dimensions
        fullBatch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
    }

    // ------------------------------------------------------------------
    //  Scene capture
    // ------------------------------------------------------------------

    /**
     * Redirect subsequent rendering into the internal scene FBO.
     * The FBO is cleared to opaque black before any rendering begins.
     *
     * <p>Must be paired with a corresponding {@link #captureEnd()} call.</p>
     */
    public void captureBegin() {
        if (!enabled) return;
        sceneFbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Stop redirecting rendering; subsequent draws target the default
     * framebuffer again.
     */
    public void captureEnd() {
        if (!enabled) return;
        sceneFbo.end();
    }

    // ------------------------------------------------------------------
    //  Bloom pipeline
    // ------------------------------------------------------------------

    /**
     * Execute the bloom pipeline and composite the result to the screen.
     *
     * <p>Must be called after {@link #captureEnd()} with no active FBO.</p>
     */
    public void render() {
        if (!enabled) return;

        Texture scene = sceneFbo.getColorBufferTexture();
        scene.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // ----------------------------------------------------------------
        // Pass 1 — Luminance threshold  (full-res → half-res)
        //   Writes only bright pixels into blurFboH at half dimensions.
        // ----------------------------------------------------------------
        blurFboH.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fullBatch.setShader(thresholdShader);
        thresholdShader.setUniformf("u_threshold", threshold);
        fullBatch.begin();
        fullBatch.draw(scene, 0f, 0f, width / 2f, height / 2f);
        fullBatch.end();

        blurFboH.end();

        // ----------------------------------------------------------------
        // Pass 2 — Horizontal Gaussian blur  (half-res)
        //   Reads blurFboH, writes horizontally blurred result to blurFboV.
        // ----------------------------------------------------------------
        blurFboV.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fullBatch.setShader(blurShader);
        blurShader.setUniformf("u_texOffset", 1f / (width * 0.5f), 0f);
        fullBatch.begin();
        fullBatch.draw(blurFboH.getColorBufferTexture(), 0f, 0f, width / 2f, height / 2f);
        fullBatch.end();

        blurFboV.end();

        // ----------------------------------------------------------------
        // Pass 3 — Vertical Gaussian blur  (half-res)
        //   Reads blurFboV, writes vertically blurred result back to blurFboH.
        //   blurFboH is now the final bloom texture.
        // ----------------------------------------------------------------
        blurFboH.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        blurShader.setUniformf("u_texOffset", 0f, 1f / (height * 0.5f));
        fullBatch.begin();
        fullBatch.draw(blurFboV.getColorBufferTexture(), 0f, 0f, width / 2f, height / 2f);
        fullBatch.end();

        blurFboH.end();

        // ----------------------------------------------------------------
        // Pass 4 — Composite: scene + bloom → default framebuffer
        // ----------------------------------------------------------------
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Texture bloom = blurFboH.getColorBufferTexture();
        bloom.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Bind bloom to texture unit 1 first, then restore unit 0.
        // SpriteBatch always uses unit 0, so 0 must be active when begin() is called.
        bloom.bind(1);
        scene.bind(0);

        fullBatch.setShader(compositeShader);
        compositeShader.setUniformi("u_bloom", 1);
        compositeShader.setUniformf("u_bloomStrength", bloomStrength);

        fullBatch.begin();
        fullBatch.draw(scene, 0f, 0f, width, height);
        fullBatch.end();

        // Restore the batch's default shader so subsequent draw calls are unaffected
        fullBatch.setShader(null);
    }

    // ------------------------------------------------------------------
    //  Parameter accessors
    // ------------------------------------------------------------------

    /** @return Luminance threshold for bright-pass filter (default 0.4). */
    public float getThreshold()           { return threshold; }
    /** @return Bloom add-on strength applied during composite (default 1.5). */
    public float getBloomStrength()       { return bloomStrength; }
    /** @return {@code true} if the effect is active (FBOs created successfully). */
    public boolean isEnabled()            { return enabled; }

    /**
     * Set the luminance threshold for the bright-pass filter.
     * @param threshold Value in [0, 1]; lower = more of the scene glows.
     */
    public void setThreshold(float threshold) {
        this.threshold = Math.max(0f, Math.min(1f, threshold));
    }

    /**
     * Set how strongly the bloom buffer is blended back onto the scene.
     * @param bloomStrength Multiplier; 0 = disabled, 1 = subtle, 2+ = intense.
     */
    public void setBloomStrength(float bloomStrength) {
        this.bloomStrength = Math.max(0f, bloomStrength);
    }

    // ------------------------------------------------------------------
    //  Disposal
    // ------------------------------------------------------------------

    /** Dispose only the FBOs (called before recreating them on resize). */
    private void disposeFbos() {
        if (sceneFbo != null) { sceneFbo.dispose(); sceneFbo = null; }
        if (blurFboH != null) { blurFboH.dispose(); blurFboH = null; }
        if (blurFboV != null) { blurFboV.dispose(); blurFboV = null; }
    }

    /**
     * Release all GPU resources held by this effect.
     * After calling this the object must not be used again.
     */
    public void dispose() {
        disposeFbos();
        fullBatch.dispose();
        thresholdShader.dispose();
        blurShader.dispose();
        compositeShader.dispose();
    }
}
