package com.mirrorduel.core;

/**
 * One straight segment of a laser beam path, from one tile to the next.
 * The laser path is a list of these segments.
 */
public class LaserSegment {

    public final int       startCol, startRow;
    public final int       endCol,   endRow;
    public final Direction direction;
    /** True if this segment ends at the board edge or was absorbed. */
    public boolean isTerminal;
    /** True if this segment's endpoint is an opponent crystal (win condition). */
    public boolean hitCrystal;

    public LaserSegment(int startCol, int startRow,
                        int endCol,   int endRow,
                        Direction direction) {
        this.startCol  = startCol;
        this.startRow  = startRow;
        this.endCol    = endCol;
        this.endRow    = endRow;
        this.direction = direction;
    }

    @Override
    public String toString() {
        return String.format("Segment(%d,%d→%d,%d %s%s%s)",
            startCol, startRow, endCol, endRow, direction,
            isTerminal ? " [END]" : "",
            hitCrystal ? " [HIT!]" : "");
    }
}
