package com.mirrorduel.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mirrorduel.MirrorDuelGame;

/**
 * Desktop entry point for Mirror Duel: Neon Protocol.
 *
 * Run with:
 *   ./gradlew desktop:run
 *
 * Or build a fat JAR:
 *   ./gradlew desktop:jar
 *   java -jar desktop/build/libs/MirrorDuel-desktop.jar
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Mirror Duel: Neon Protocol");
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(30);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4); // RGBA + depth + 4× MSAA
        config.useVsync(true);

        new Lwjgl3Application(new MirrorDuelGame(), config);
    }
}
