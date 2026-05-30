package mirrorduel;

import mirrorduel.GameEnums.*;

public class Piece {
    public PieceType type;
    public PlayerID owner;
    public int row, col;

    public Piece(PieceType type, PlayerID owner, int row, int col) {
        this.type  = type;
        this.owner = owner;
        this.row   = row;
        this.col   = col;
    }

    public boolean isMirror() {
        return type == PieceType.MIRROR_SLASH || type == PieceType.MIRROR_BACKSLASH;
    }

    /** Rotate this mirror between slash and backslash */
    public void rotateMirror() {
        if (type == PieceType.MIRROR_SLASH)      type = PieceType.MIRROR_BACKSLASH;
        else if (type == PieceType.MIRROR_BACKSLASH) type = PieceType.MIRROR_SLASH;
    }

    public Direction reflectBeam(Direction incoming) {
        if (type == PieceType.MIRROR_SLASH)      return incoming.reflectSlash();
        if (type == PieceType.MIRROR_BACKSLASH)  return incoming.reflectBackslash();
        return incoming; // non-mirror pieces don't redirect
    }

    @Override
    public String toString() {
        return owner + " " + type + " @(" + row + "," + col + ")";
    }
}
