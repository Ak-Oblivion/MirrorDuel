package com.mirrorduel.core;

/** Top-level application state machine. */
public enum GameState {
    MENU,
    RULES,
    PLAYING,
    ANIMATING,   // mid-turn animation (laser propagation, etc.)
    GAME_OVER
}
