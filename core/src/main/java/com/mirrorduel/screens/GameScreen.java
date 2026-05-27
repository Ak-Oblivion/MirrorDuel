package com.mirrorduel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.mirrorduel.MirrorDuelGame;
import com.mirrorduel.core.GameManager;
import com.mirrorduel.core.GameState;
import com.mirrorduel.rendering.GameRenderer;

/**
 * The main gameplay screen.
 * Delegates all rendering to GameRenderer and input to GameManager.
 */
public class GameScreen extends BaseScreen {

    private final GameManager  gm;
    private final GameRenderer renderer;

    public GameScreen(MirrorDuelGame game) {
        super(game);
        this.gm       = game.gameManager;
        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.gameRenderer = renderer;

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int[] tile = renderer.screenToTile(screenX, screenY);
                if (tile != null) {
                    gm.handleTileClick(tile[0], tile[1]);
                }
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.R:
                        if (gm.getGameState() == GameState.GAME_OVER) {
                            game.gameManager = new GameManager();
                            game.gameManager.initialize();
                            game.setScreen(new GameScreen(game));
                        } else {
                            gm.handleRotateKey();
                        }
                        return true;
                    case Input.Keys.ESCAPE:
                        game.setScreen(new MainMenuScreen(game));
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        gm.update(delta);
        renderer.render(gm, delta);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}
