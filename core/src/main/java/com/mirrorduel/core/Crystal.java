package com.mirrorduel.core;

/** A player's Core Crystal – the target the opponent must destroy. */
public class Crystal extends GamePiece {

    private boolean destroyed;
    private float   pulseTime;

    // Destruction animation
    private boolean exploding;
    private float   explodeTimer;
    public static final float EXPLODE_DURATION = 1.2f;

    public Crystal(int col, int row, int owner) {
        super(col, row, owner);
    }

    public void update(float delta) {
        pulseTime += delta;
        if (exploding) {
            explodeTimer += delta;
            if (explodeTimer >= EXPLODE_DURATION) {
                exploding  = false;
                destroyed  = true;
            }
        }
    }

    /** Trigger the destruction sequence. */
    public void destroy() {
        if (!destroyed && !exploding) {
            exploding = true;
        }
    }

    public boolean isDestroyed()  { return destroyed; }
    public boolean isExploding()  { return exploding; }
    public float   getExplodeProgress() {
        return exploding ? Math.min(explodeTimer / EXPLODE_DURATION, 1f) : 0f;
    }

    /** Returns a scale multiplier [0.8, 1.0] for the idle pulse animation. */
    public float getPulseScale() {
        return 0.85f + 0.15f * (float) Math.sin(pulseTime * 3.0);
    }

    /** Returns brightness [0, 1] that pulses with the scale. */
    public float getPulseBrightness() {
        return 0.6f + 0.4f * (float) Math.sin(pulseTime * 3.0 + Math.PI / 4);
    }
}
