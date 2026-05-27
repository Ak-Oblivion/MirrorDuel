package com.mirrorduel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The 8×8 game board. Manages tile content and piece placement.
 */
public class Board {

    public static final int SIZE = 8;

    private final Tile[][] tiles = new Tile[SIZE][SIZE];

    public Board() {
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++)
                tiles[c][r] = new Tile(c, r);
    }

    // ---- Tile access ----

    public Tile getTile(int col, int row) {
        return tiles[col][row];
    }

    public boolean isValid(int col, int row) {
        return col >= 0 && col < SIZE && row >= 0 && row < SIZE;
    }

    // ---- Piece management ----

    public void placePiece(GamePiece piece, int col, int row) {
        tiles[col][row].setPiece(piece);
        piece.setPosition(col, row);
    }

    public void removePiece(int col, int row) {
        tiles[col][row].clearPiece();
    }

    /**
     * Move the piece at (fromCol, fromRow) to (toCol, toRow).
     * Returns true on success, false if destination is occupied or invalid.
     */
    public boolean movePiece(int fromCol, int fromRow, int toCol, int toRow) {
        if (!isValid(toCol, toRow)) return false;
        if (!tiles[toCol][toRow].isEmpty()) return false;
        GamePiece piece = tiles[fromCol][fromRow].getPiece();
        if (piece == null) return false;
        tiles[fromCol][fromRow].clearPiece();
        tiles[toCol][toRow].setPiece(piece);
        piece.setPosition(toCol, toRow);
        return true;
    }

    public boolean canMoveTo(int col, int row) {
        return isValid(col, row) && tiles[col][row].isEmpty();
    }

    // ---- Queries ----

    /** Returns all mirrors owned by the given player. */
    public List<Mirror> getMirrors(int owner) {
        List<Mirror> list = new ArrayList<>();
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++)
                if (tiles[c][r].hasMirror() &&
                    tiles[c][r].getMirror().getOwner() == owner)
                    list.add(tiles[c][r].getMirror());
        return list;
    }

    /** Returns all empty tiles adjacent (4-directional) to (col, row). */
    public List<int[]> getAdjacentEmpty(int col, int row) {
        List<int[]> result = new ArrayList<>();
        int[][] offsets = {{0,1},{0,-1},{1,0},{-1,0}};
        for (int[] off : offsets) {
            int nc = col + off[0], nr = row + off[1];
            if (isValid(nc, nr) && tiles[nc][nr].isEmpty())
                result.add(new int[]{nc, nr});
        }
        return result;
    }

    /** Clear all highlight/selection state on every tile. */
    public void clearHighlights() {
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++) {
                tiles[c][r].setHighlighted(false);
                tiles[c][r].setSelected(false);
            }
    }

    public void update(float delta) {
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++)
                tiles[c][r].updateHighlightAlpha(delta);
    }
}
