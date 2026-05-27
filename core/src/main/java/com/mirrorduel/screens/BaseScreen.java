package com.mirrorduel.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.Gdx;
import com.mirrorduel.MirrorDuelGame;

/** Shared base for all screens – holds common utilities. */
public abstract class BaseScreen extends ScreenAdapter {

    protected final MirrorDuelGame game;
    protected final GlyphLayout layout = new GlyphLayout();
    protected float stateTime;

    // One reusable 1×1 white texture for solid-colour drawing
    private static Texture whitePixel;

    public BaseScreen(MirrorDuelGame game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
    }

    /** Draw centred text at (cx, y) using the given font and batch. */
    protected void drawCentred(SpriteBatch batch, BitmapFont font,
                                String text, float cx, float y) {
        layout.setText(font, text);
        font.draw(batch, text, cx - layout.width / 2f, y);
    }

    /** Fill a screen rectangle with a solid colour (requires batch.begin()). */
    protected void fillRect(SpriteBatch batch, float x, float y,
                             float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(getWhitePixel(), x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** Standard clear with near-black cyberpunk bg. */
    protected void clearScreen() {
        Gdx.gl.glClearColor(0.015f, 0.015f, 0.04f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    protected static Texture getWhitePixel() {
        if (whitePixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whitePixel = new Texture(pm);
            pm.dispose();
        }
        return whitePixel;
    }
}
