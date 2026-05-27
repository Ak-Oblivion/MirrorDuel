package com.mirrorduel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirrorduel.MirrorDuelGame;

/**
 * Main menu screen with animated cyberpunk background.
 *
 * Layout (1280×720):
 *  - Animated neon grid background
 *  - Title: "MIRROR DUEL"
 *  - Sub: "NEON PROTOCOL"
 *  - Buttons: PLAY / RULES / EXIT
 */
public class MainMenuScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final BitmapFont  fontTitle;
    private final BitmapFont  fontSub;
    private final BitmapFont  fontBtn;
    private final ShapeRenderer shape;

    // Button areas  (x, y, w, h)
    private static final float BTN_W = 260f, BTN_H = 52f;
    private float btnX;

    // Animated background particles
    private static final int BG_PARTICLES = 80;
    private final float[] bpx = new float[BG_PARTICLES];
    private final float[] bpy = new float[BG_PARTICLES];
    private final float[] bpSpeed = new float[BG_PARTICLES];
    private final float[] bpSize  = new float[BG_PARTICLES];

    private int hoveredButton = -1; // 0=Play, 1=Rules, 2=Exit

    public MainMenuScreen(MirrorDuelGame game) {
        super(game);
        batch = new SpriteBatch();
        shape = new ShapeRenderer();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(5f);
        fontTitle.setColor(new Color(0.3f, 0.8f, 1f, 1f));

        fontSub = new BitmapFont();
        fontSub.getData().setScale(2f);
        fontSub.setColor(new Color(0.6f, 0.9f, 1f, 0.8f));

        fontBtn = new BitmapFont();
        fontBtn.getData().setScale(1.8f);

        // Init background particles
        for (int i = 0; i < BG_PARTICLES; i++) {
            bpx[i]     = MathUtils.random(1280f);
            bpy[i]     = MathUtils.random(720f);
            bpSpeed[i] = 15f + MathUtils.random(45f);
            bpSize[i]  = 1f + MathUtils.random(3f);
        }
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();
        btnX = (W - BTN_W) / 2f;
        float btnBaseY = H / 2f - 60f;

        // ---- Update background particles ----
        for (int i = 0; i < BG_PARTICLES; i++) {
            bpy[i] -= bpSpeed[i] * delta;
            if (bpy[i] < -4) { bpy[i] = H + 4; bpx[i] = MathUtils.random(W); }
        }

        // ---- Hover detection ----
        float mx = Gdx.input.getX();
        float my = H - Gdx.input.getY();
        hoveredButton = -1;
        for (int b = 0; b < 3; b++) {
            float by = btnBaseY - b * (BTN_H + 18f);
            if (mx >= btnX && mx <= btnX + BTN_W && my >= by && my <= by + BTN_H)
                hoveredButton = b;
        }

        // ---- Input ----
        if (Gdx.input.justTouched()) {
            if (hoveredButton == 0) startGame();
            else if (hoveredButton == 1) game.setScreen(new RulesScreen(game));
            else if (hoveredButton == 2) Gdx.app.exit();
        }

        clearScreen();

        // ---- Background grid ----
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Grid lines
        shape.setColor(0.08f, 0.12f, 0.30f, 0.4f);
        for (float x = 0; x < W; x += 60) shape.rectLine(x, 0, x, H, 1);
        for (float y = 0; y < H; y += 60) shape.rectLine(0, y, W, y, 1);

        // Floating particles
        for (int i = 0; i < BG_PARTICLES; i++) {
            float pulse = 0.4f + 0.6f * (float) Math.sin(stateTime * 2 + i);
            shape.setColor(0.2f, 0.6f, 1f, 0.5f * pulse);
            shape.circle(bpx[i], bpy[i], bpSize[i], 6);
        }
        shape.end();

        // ---- Text & buttons ----
        batch.begin();

        // Title glow layers
        fontTitle.setColor(0.1f, 0.5f, 0.9f, 0.3f);
        drawCentred(batch, fontTitle, "MIRROR DUEL", W / 2f + 3, H - 95);
        fontTitle.setColor(0.3f, 0.8f, 1f, 1f);
        drawCentred(batch, fontTitle, "MIRROR DUEL", W / 2f, H - 100);

        // Subtitle
        float sub_pulse = 0.7f + 0.3f * (float) Math.sin(stateTime * 2.5f);
        fontSub.setColor(0.6f, 0.9f, 1f, sub_pulse);
        drawCentred(batch, fontSub, "N E O N   P R O T O C O L", W / 2f, H - 160);

        // Separator line drawn as a batch rect
        float lineY = H - 180f;
        float lineW = 380f;
        fillRect(batch, (W - lineW) / 2f, lineY, lineW, 1.5f,
                new Color(0.3f, 0.7f, 1f, 0.5f));

        // Buttons
        String[] labels = {"PLAY", "RULES", "EXIT"};
        Color[] btnColors = {
            new Color(0.1f, 0.6f, 1f, 1f),
            new Color(0.5f, 0.5f, 0.9f, 1f),
            new Color(0.7f, 0.2f, 0.2f, 1f)
        };
        for (int b = 0; b < 3; b++) {
            float by = btnBaseY - b * (BTN_H + 18f);
            boolean hov = hoveredButton == b;
            float alpha = hov ? 1f : 0.7f;

            // Button background
            Color bc = btnColors[b];
            fillRect(batch, btnX, by, BTN_W, BTN_H,
                    new Color(bc.r * 0.15f, bc.g * 0.15f, bc.b * 0.15f, alpha * 0.85f));
            // Border
            fillRect(batch, btnX,            by,            BTN_W, 2,    new Color(bc.r, bc.g, bc.b, alpha));
            fillRect(batch, btnX,            by + BTN_H - 2, BTN_W, 2,   new Color(bc.r, bc.g, bc.b, alpha));
            fillRect(batch, btnX,            by,            2, BTN_H,    new Color(bc.r, bc.g, bc.b, alpha));
            fillRect(batch, btnX + BTN_W - 2, by,           2, BTN_H,    new Color(bc.r, bc.g, bc.b, alpha));

            float scale = hov ? 1.0f : 0.95f;
            fontBtn.getData().setScale(scale * 1.8f);
            fontBtn.setColor(hov ? Color.WHITE : new Color(bc.r, bc.g, bc.b, 0.9f));
            drawCentred(batch, fontBtn, labels[b], W / 2f, by + BTN_H / 2f + 10f);
        }

        // Footer
        fontBtn.getData().setScale(0.85f);
        fontBtn.setColor(0.3f, 0.4f, 0.6f, 0.7f);
        drawCentred(batch, fontBtn, "2-PLAYER LOCAL • LASER STRATEGY • NEON PROTOCOL v1.0", W / 2f, 22f);

        batch.end();
    }

    private void startGame() {
        game.gameManager = new com.mirrorduel.core.GameManager();
        game.gameManager.initialize();
        game.setScreen(new GameScreen(game));
    }

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        fontTitle.dispose();
        fontSub.dispose();
        fontBtn.dispose();
    }
}
