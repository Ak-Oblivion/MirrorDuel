package com.mirrorduel.core;

/** The laser cannon that fires a beam at the start of each trace. */
public class LaserEmitter extends GamePiece {

    private Direction direction;
    private float     chargeTime;

    public LaserEmitter(int col, int row, int owner, Direction direction) {
        super(col, row, owner);
        this.direction = direction;
    }

    public void update(float delta) {
        chargeTime += delta;
    }

    public Direction getDirection()        { return direction; }
    public void      setDirection(Direction d) { direction = d; }

    /** Glow intensity for the charge animation [0.5, 1.0]. */
    public float getChargeGlow() {
        return 0.5f + 0.5f * (float) Math.abs(Math.sin(chargeTime * 4.0));
    }
}
