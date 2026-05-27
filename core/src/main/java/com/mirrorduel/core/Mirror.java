package com.mirrorduel.core;

/** A reflective mirror piece owned by a player. Can be moved or rotated. */
public class Mirror extends GamePiece {

    private MirrorType type;

    // --- Visual animation ---
    private float visualAngle;   // current rendered angle (degrees)
    private float targetAngle;   // angle we're animating toward
    private static final float ROTATE_SPEED = 360f; // degrees per second

    public Mirror(int col, int row, int owner, MirrorType type) {
        super(col, row, owner);
        this.type        = type;
        this.visualAngle = type.getAngle();
        this.targetAngle = type.getAngle();
    }

    /** Toggle mirror type (SLASH ↔ BACKSLASH) and start rotation animation. */
    public void toggleType() {
        type        = type.toggled();
        targetAngle = type.getAngle();
    }

    /** Advance rotation animation. */
    public void update(float delta) {
        if (visualAngle != targetAngle) {
            float diff  = targetAngle - visualAngle;
            float step  = ROTATE_SPEED * delta;
            if (Math.abs(diff) <= step) {
                visualAngle = targetAngle;
            } else {
                visualAngle += Math.signum(diff) * step;
            }
        }
    }

    public MirrorType getType()        { return type; }
    public float      getVisualAngle() { return visualAngle; }
    public boolean    isAnimating()    { return visualAngle != targetAngle; }

    /** Reflect a laser beam direction through this mirror. */
    public Direction reflect(Direction incoming) {
        return type.reflect(incoming);
    }
}
