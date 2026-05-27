# MIRROR DUEL: NEON PROTOCOL

> A polished 2-player cyberpunk laser-mirror strategy game — Java + LibGDX

---

## 🎮 Gameplay

Two players battle on an 8×8 neon grid. Redirect your laser beam through mirrors to destroy the opponent's **Core Crystal**.

| Feature  | Detail |
|----------|--------|
| Players  | 2 (local multiplayer) |
| Type     | Turn-based strategy |
| Grid     | 8×8 |
| Win Con  | Hit opponent's Core Crystal with your laser |

---

## ⚡ Quick Start

**Prerequisites:** Java 8+ · Gradle (wrapper included)

```bash
# Run the game
./gradlew desktop:run          # Linux/macOS
gradlew.bat desktop:run        # Windows

# Build a fat JAR
./gradlew desktop:jar
java -jar desktop/build/libs/MirrorDuel-desktop.jar
```

---

## 🎯 Controls

| Action | Input |
|--------|-------|
| Select mirror | Left-click your mirror |
| Move mirror | Click on a highlighted (blue) adjacent tile |
| Rotate mirror | Click selected mirror again **or** press `R` |
| Deselect | Click anywhere else |
| Restart (after win) | Press `R` |
| Back to menu | Press `ESC` |

---

## 🔭 Mirror Reflection Rules

```
/ (SLASH)      RIGHT→DOWN  DOWN→LEFT   LEFT→UP    UP→RIGHT
\ (BACKSLASH)  RIGHT→UP    UP→LEFT     LEFT→DOWN  DOWN→RIGHT
```

Lasers reflect off **all** mirrors on the board — friendly or enemy.

---

## 🗂 Project Structure

```
MirrorDuel/
├── core/src/main/java/com/mirrorduel/
│   ├── MirrorDuelGame.java        ← LibGDX Game root
│   ├── core/                      ← Game logic (laser tracing, board, pieces)
│   ├── rendering/                 ← 3D renderer, bloom, particles
│   ├── screens/                   ← Menu, Game, Victory, Rules screens
│   └── audio/                     ← Sound stub
├── desktop/                       ← LWJGL3 desktop launcher
└── assets/                        ← Game assets (shaders, fonts, sounds)
```

---

## 🌈 Visual Features

- **LibGDX 3D** — perspective camera, 3D tile geometry, floating pieces
- **Bloom** — two-pass Gaussian blur post-processing
- **Layered laser glow** — 3 additive blend layers (outer glow → mid → core)
- **Crystal pulse** — idle bob + scale animation, explosion on destroy
- **Camera drift** — subtle cinematic pan; shake on crystal hit
- **Particle sparks** — burst at reflection points and crystal explosions

---

*Built with LibGDX 1.12.1 · Java 8+ · OpenGL · Neon Protocol v1.0*
