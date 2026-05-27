package com.mirrorduel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirrorduel.MirrorDuelGame;

/**
 * Rules / How-to-play screen.
 */
public class RulesScreen extends BaseScreen {

    private final SpriteBatch   batch;
    private final BitmapFont    fontTitle;
    private final BitmapFont    fontBody;
    private final BitmapFont    fontSmall;
    private final ShapeRenderer shape;

    public RulesScreen(MirrorDuelGame game) {
        super(game);
        batch = new SpriteBatch();
        shape = new ShapeRenderer();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(3.2f);
        fontTitle.setColor(new Color(0.3f, 0.8f, 1f, 1f));

        fontBody = new BitmapFont();
        fontBody.getData().setScale(1.5f);
        fontBody.setColor(new Color(0.85f, 0.9f, 1f, 1f));

        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.2f);
        fontSmall.setColor(new Color(0.6f, 0.7f, 0.9f, 1f));

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int k) {
                if (k == Input.Keys.ESCAPE || k == Input.Keys.BACK) {
                    game.setScreen(new MainMenuScreen(game));
                }
                return true;
            }
            @Override public boolean touchDown(int x, int y, int p, int b) {
                float W = Gdx.graphics.getWidth();
                float H = Gdx.graphics.getHeight();
                float my = H - y;
                float bx = (W - 220f) / 2f;
                if (x >= bx && x <= bx + 220 && my >= 35 && my <= 75) {
                    game.setScreen(new MainMenuScreen(game));
                }
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        clearScreen();
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Background grid
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.05f, 0.08f, 0.20f, 0.3f);
        for (float x = 0; x < W; x += 60) shape.rectLine(x, 0, x, H, 1);
        for (float y = 0; y < H; y += 60) shape.rectLine(0, y, W, y, 1);
        shape.end();

        batch.begin();

        // Title
        drawCentred(batch, fontTitle, "HOW TO PLAY", W / 2f, H - 55f);

        // Divider
        fillRect(batch, 80, H - 80f, W - 160f, 1.5f, new Color(0.3f, 0.7f, 1f, 0.4f));

        // Rules content
        String[] lines = {
            "OBJECTIVE",
            "  Redirect your laser beam to destroy the opponent's Core Crystal.",
            "",
            "TURNS",
            "  Players alternate turns. Each turn: perform ONE action.",
            "  Actions:  Move a mirror  |  Rotate a mirror",
            "",
            "HOW TO PLAY",
            "  1. Click on one of your mirrors to select it  (yellow highlight)",
            "  2a. Click the selected mirror again to ROTATE it (toggles / and \\)",
            "  2b. Click a blue-highlighted adjacent tile to MOVE the mirror there",
            "  Press R to ROTATE the selected mirror",
            "",
            "LASER RULES",
            "  / mirror:  RIGHT→DOWN  DOWN→LEFT  LEFT→UP  UP→RIGHT",
            "  \\ mirror: RIGHT→UP   UP→LEFT   LEFT→DOWN  DOWN→RIGHT",
            "  Lasers bounce off ALL mirrors (friendly or enemy).",
            "  Maximum 64 bounces per beam (cycle detection).",
            "",
            "WIN CONDITION",
            "  Your laser hits the OPPONENT'S Crystal = YOU WIN!",
            "",
            "COLORS",
            "  Player 1 = Electric Blue     Player 2 = Crimson Red",
        };

        float y = H - 110f;
        for (String line : lines) {
            if (line.startsWith("OBJECTIVE") || line.startsWith("TURNS") ||
                line.startsWith("HOW TO") || line.startsWith("LASER") ||
                line.startsWith("WIN") || line.startsWith("COLORS")) {
                fontBody.setColor(0.4f, 0.8f, 1f, 1f);
                fontBody.getData().setScale(1.6f);
            } else if (line.isEmpty()) {
                y -= 8f;
                continue;
            } else {
                fontBody.setColor(0.85f, 0.9f, 1f, 0.9f);
                fontBody.getData().setScale(1.35f);
            }
            fontBody.draw(batch, line, 90f, y);
            y -= 26f;
        }

        // Back button
        float bx = (W - 220f) / 2f;
        fillRect(batch, bx, 35, 220, 40, new Color(0.1f, 0.15f, 0.4f, 0.9f));
        fillRect(batch, bx, 35,     220, 2, new Color(0.3f, 0.6f, 1f, 0.8f));
        fillRect(batch, bx, 73,     220, 2, new Color(0.3f, 0.6f, 1f, 0.8f));
        fillRect(batch, bx, 35,     2, 40,  new Color(0.3f, 0.6f, 1f, 0.8f));
        fillRect(batch, bx + 218, 35, 2, 40, new Color(0.3f, 0.6f, 1f, 0.8f));

        fontSmall.setColor(0.7f, 0.9f, 1f, 1f);
        drawCentred(batch, fontSmall, "BACK (ESC)", W / 2f, 63f);

        batch.end();
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        fontTitle.dispose();
        fontBody.dispose();
        fontSmall.dispose();
    }
}
