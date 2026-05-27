package com.mirrorduel.core;

/** Cardinal directions for laser beam travel and grid movement. */
public enum Direction {
    UP(0, 1),
    DOWN(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    /** Column delta (positive = right). */
    public final int dc;
    /** Row delta (positive = up). */
    public final int dr;

    Direction(int dc, int dr) {
        this.dc = dc;
        this.dr = dr;
    }

    public Direction opposite() {
        switch (this) {
            case UP:    return DOWN;
            case DOWN:  return UP;
            case LEFT:  return RIGHT;
            case RIGHT: return LEFT;
            default:    return this;
        }
    }
}
