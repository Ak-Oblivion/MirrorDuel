package com.mirrorduel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirrorduel.MirrorDuelGame;
import com.mirrorduel.core.GameManager;
import com.mirrorduel.core.Player;

/**
 * Victory screen shown after a player wins.
 * Features cinematic particle rain and winner announcement.
 */
public class VictoryScreen extends BaseScreen {

    private final SpriteBatch   batch;
    private final BitmapFont    fontHuge;
    private final BitmapFont    fontMed;
    private final BitmapFont    fontSmall;
    private final ShapeRenderer shape;

    private final int    winner;
    private final Color  winColor;
    private final String winName;

    // Celebration particles
    private static final int P_COUNT = 150;
    private final float[] px    = new float[P_COUNT];
    private final float[] py    = new float[P_COUNT];
    private final float[] pvx   = new float[P_COUNT];
    private final float[] pvy   = new float[P_COUNT];
    private final float[] psize = new float[P_COUNT];
    private final float[] ph    = new float[P_COUNT]; // hue
    private final float[] plife = new float[P_COUNT];

    public VictoryScreen(MirrorDuelGame game, int winner) {
        super(game);
        this.winner   = winner;
        Player wp     = game.gameManager.getPlayers()[winner];
        this.winColor = wp.getColor();
        this.winName  = wp.getName();

        batch = new SpriteBatch();
        shape = new ShapeRenderer();

        fontHuge = new BitmapFont();
        fontHuge.getData().setScale(6f);
        fontMed  = new BitmapFont();
        fontMed.getData().setScale(2.2f);
        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.4f);

        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // Spawn confetti
        for (int i = 0; i < P_COUNT; i++) {
            px[i]    = MathUtils.random(W);
            py[i]    = H + MathUtils.random(H);
            pvx[i]   = MathUtils.random(-60f, 60f);
            pvy[i]   = -(60f + MathUtils.random(120f));
            psize[i] = 4f + MathUtils.random(8f);
            ph[i]    = MathUtils.random(360f);
            plife[i] = MathUtils.random(1f);
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int k) {
                if (k == Input.Keys.R || k == Input.Keys.ENTER) restart();
                if (k == Input.Keys.ESCAPE) goMenu();
                return true;
            }
            @Override public boolean touchDown(int x, int y, int p, int b) {
                restart(); return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // Update confetti
        for (int i = 0; i < P_COUNT; i++) {
            px[i] += pvx[i] * delta;
            py[i] += pvy[i] * delta;
            plife[i] += delta;
            if (py[i] < -20) {
                px[i] = MathUtils.random(W);
                py[i] = H + 20;
                plife[i] = 0;
            }
        }

        clearScreen();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ---- Confetti ----
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < P_COUNT; i++) {
            float t   = (float)(Math.sin(ph[i] / 60f * Math.PI * 2));
            float r   = 0.5f + 0.5f * t;
            float g   = 0.5f + 0.5f * (float)Math.cos(ph[i] / 40f * Math.PI * 2);
            float b   = 0.5f + 0.5f * (float)Math.sin(ph[i] / 30f * Math.PI * 2 + 1);
            float a   = Math.min(1f, plife[i] * 2f) * 0.85f;
            shape.setColor(r, g, b, a);
            shape.rect(px[i], py[i], psize[i], psize[i] * 0.5f);
        }
        shape.end();

        // ---- Text ----
        batch.begin();

        // Glow
        fontHuge.setColor(winColor.r * 0.4f, winColor.g * 0.4f, winColor.b * 0.4f,
                          0.4f + 0.3f * (float)Math.sin(stateTime * 3));
        drawCentred(batch, fontHuge, winName + "  WINS!", W / 2f + 4, H / 2f + 130);
        fontHuge.setColor(winColor);
        drawCentred(batch, fontHuge, winName + "  WINS!", W / 2f, H / 2f + 130);

        // Sub
        fontMed.setColor(0.8f, 0.9f, 1f, 0.9f);
        drawCentred(batch, fontMed, "Crystal Destroyed!", W / 2f, H / 2f + 30f);

        float blink = (stateTime % 1.2f) < 0.8f ? 1f : 0f;
        fontSmall.setColor(0.7f, 0.7f, 1f, blink);
        drawCentred(batch, fontSmall, "[ CLICK or press R to PLAY AGAIN ]", W / 2f, H / 2f - 40f);
        fontSmall.setColor(0.4f, 0.4f, 0.7f, 0.7f);
        drawCentred(batch, fontSmall, "[ ESC for Main Menu ]", W / 2f, H / 2f - 75f);

        batch.end();
    }

    private void restart() {
        game.gameManager = new GameManager();
        game.gameManager.initialize();
        game.setScreen(new GameScreen(game));
    }
    private void goMenu() {
        game.setScreen(new MainMenuScreen(game));
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        fontHuge.dispose();
        fontMed.dispose();
        fontSmall.dispose();
    }
}
