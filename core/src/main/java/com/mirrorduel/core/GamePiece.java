package com.mirrorduel.core;

/** Base class for every object that occupies a tile on the board. */
public abstract class GamePiece {

    protected int col;
    protected int row;
    protected int owner; // 0 = Player 1 (Blue), 1 = Player 2 (Red)

    public GamePiece(int col, int row, int owner) {
        this.col   = col;
        this.row   = row;
        this.owner = owner;
    }

    public int  getCol()   { return col; }
    public int  getRow()   { return row; }
    public int  getOwner() { return owner; }

    public void setCol(int col) { this.col = col; }
    public void setRow(int row) { this.row = row; }
    public void setPosition(int col, int row) { this.col = col; this.row = row; }
}
