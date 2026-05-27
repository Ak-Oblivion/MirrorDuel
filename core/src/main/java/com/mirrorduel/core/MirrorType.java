package com.mirrorduel.core;

/**
 * Mirror types and their reflection behaviour.
 *
 * SLASH  (/) rotates incoming direction 90° clockwise:
 *   RIGHT→DOWN, DOWN→LEFT, LEFT→UP, UP→RIGHT
 *
 * BACKSLASH (\) rotates incoming direction 90° counter-clockwise:
 *   RIGHT→UP, UP→LEFT, LEFT→DOWN, DOWN→RIGHT
 */
public enum MirrorType {

    SLASH {
        @Override
        public Direction reflect(Direction in) {
            switch (in) {
                case RIGHT: return Direction.DOWN;
                case DOWN:  return Direction.LEFT;
                case LEFT:  return Direction.UP;
                case UP:    return Direction.RIGHT;
                default:    return in;
            }
        }
        @Override public String getSymbol() { return "/"; }
        @Override public float getAngle()   { return 45f; }
    },

    BACKSLASH {
        @Override
        public Direction reflect(Direction in) {
            switch (in) {
                case RIGHT: return Direction.UP;
                case UP:    return Direction.LEFT;
                case LEFT:  return Direction.DOWN;
                case DOWN:  return Direction.RIGHT;
                default:    return in;
            }
        }
        @Override public String getSymbol() { return "\\"; }
        @Override public float getAngle()   { return -45f; }
    };

    public abstract Direction reflect(Direction incoming);
    public abstract String    getSymbol();
    /** Visual angle in degrees for rendering the mirror. */
    public abstract float     getAngle();

    public MirrorType toggled() {
        return this == SLASH ? BACKSLASH : SLASH;
    }
}
