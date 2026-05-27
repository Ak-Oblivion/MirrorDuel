package com.mirrorduel;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mirrorduel.audio.AudioManager;
import com.mirrorduel.core.GameManager;
import com.mirrorduel.rendering.GameRenderer;
import com.mirrorduel.screens.MainMenuScreen;

/**
 * Root LibGDX application class for Mirror Duel: Neon Protocol.
 *
 * Shared resources (batch, font) are owned here so screens don't
 * have to each create their own.  Screens that need game-specific
 * resources access them via this class.
 */
public class MirrorDuelGame extends Game {

    // ---- Shared resources ----
    public SpriteBatch  batch;
    public BitmapFont   font;
    public AudioManager audioManager;

    // ---- Game-specific (created per session) ----
    public GameManager  gameManager;
    public GameRenderer gameRenderer;

    @Override
    public void create() {
        batch        = new SpriteBatch();
        font         = new BitmapFont();
        audioManager = new AudioManager();

        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render(); // delegates to current screen
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (gameRenderer  != null) gameRenderer.dispose();
        batch.dispose();
        font.dispose();
        audioManager.dispose();
    }
}
