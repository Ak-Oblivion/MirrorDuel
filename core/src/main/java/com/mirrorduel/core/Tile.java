package com.mirrorduel.core;

/**
 * A single cell on the 8×8 game board.
 * Holds at most one GamePiece at a time.
 */
public class Tile {

    private final int col;
    private final int row;
    private GamePiece piece;

    // Hover/selection state for rendering
    private boolean highlighted;
    private boolean selected;
    private float   highlightAlpha;

    public Tile(int col, int row) {
        this.col = col;
        this.row = row;
    }

    // ---- Piece management ----

    public boolean    isEmpty()    { return piece == null; }
    public boolean    hasMirror()  { return piece instanceof Mirror; }
    public boolean    hasCrystal() { return piece instanceof Crystal; }
    public boolean    hasEmitter() { return piece instanceof LaserEmitter; }

    public GamePiece   getPiece()   { return piece; }
    public Mirror      getMirror()  { return hasMirror()  ? (Mirror)      piece : null; }
    public Crystal     getCrystal() { return hasCrystal() ? (Crystal)     piece : null; }
    public LaserEmitter getEmitter(){ return hasEmitter() ? (LaserEmitter) piece : null; }

    public void setPiece(GamePiece p) { this.piece = p; }
    public void clearPiece()          { this.piece = null; }

    // ---- Position ----

    public int getCol() { return col; }
    public int getRow() { return row; }

    // ---- Visual state ----

    public void    setHighlighted(boolean h) { highlighted = h; }
    public void    setSelected   (boolean s) { selected    = s; }
    public boolean isHighlighted()           { return highlighted; }
    public boolean isSelected   ()           { return selected;    }

    public void  updateHighlightAlpha(float delta) {
        float target = (highlighted || selected) ? 1f : 0f;
        highlightAlpha += (target - highlightAlpha) * Math.min(delta * 8f, 1f);
    }
    public float getHighlightAlpha() { return highlightAlpha; }
}
