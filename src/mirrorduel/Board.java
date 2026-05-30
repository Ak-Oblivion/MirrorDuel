package mirrorduel;

import mirrorduel.GameEnums.*;
import java.awt.Point;
import java.util.*;

public class Board {
    public static final int SIZE = 8;

    // [row][col] → piece or null
    private final Piece[][] grid = new Piece[SIZE][SIZE];

    // Laser beam segments for each player: list of (row,col) cells the beam passes through
    private final List<Point> redBeam  = new ArrayList<>();
    private final List<Point> blueBeam = new ArrayList<>();

    // Whether each player's laser has hit the enemy crystal
    private boolean redHitsTarget  = false;
    private boolean blueHitsTarget = false;

    public Board() {
        setupInitial();
    }

    // ──────────────────────────────────────────────────────────────
    //  Initial layout
    // ──────────────────────────────────────────────────────────────
    private void setupInitial() {
        // RED player: emitter top-left corner, crystal top-right corner, mirrors scattered top half
        place(new Piece(PieceType.EMITTER, PlayerID.RED,  0, 0));
        place(new Piece(PieceType.CRYSTAL, PlayerID.RED,  0, 7));

        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.RED, 1, 2));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.RED, 2, 5));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.RED, 3, 1));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.RED, 1, 6));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.RED, 3, 4));

        // BLUE player: emitter bottom-right, crystal bottom-left, mirrors scattered bottom half
        place(new Piece(PieceType.EMITTER, PlayerID.BLUE, 7, 7));
        place(new Piece(PieceType.CRYSTAL, PlayerID.BLUE, 7, 0));

        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.BLUE, 6, 5));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.BLUE, 5, 2));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.BLUE, 4, 6));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.BLUE, 6, 1));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.BLUE, 4, 3));

        recalculateLasers();
    }

    // ──────────────────────────────────────────────────────────────
    //  Accessors
    // ──────────────────────────────────────────────────────────────
    public Piece pieceAt(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return null;
        return grid[row][col];
    }

    public List<Point> getBeam(PlayerID p) {
        return p == PlayerID.RED ? redBeam : blueBeam;
    }

    public boolean laserHitsTarget(PlayerID p) {
        return p == PlayerID.RED ? redHitsTarget : blueHitsTarget;
    }

    /** All pieces for a given player */
    public List<Piece> piecesOf(PlayerID p) {
        List<Piece> list = new ArrayList<>();
        for (Piece[] row : grid)
            for (Piece pc : row)
                if (pc != null && pc.owner == p) list.add(pc);
        return list;
    }

    // ──────────────────────────────────────────────────────────────
    //  Placement helpers
    // ──────────────────────────────────────────────────────────────
    private void place(Piece p) {
        grid[p.row][p.col] = p;
    }

    private void remove(Piece p) {
        grid[p.row][p.col] = null;
    }

    // ──────────────────────────────────────────────────────────────
    //  Move validation
    // ──────────────────────────────────────────────────────────────
    public boolean canMovePieceTo(Piece p, int newRow, int newCol) {
        if (newRow < 0 || newRow >= SIZE || newCol < 0 || newCol >= SIZE) return false;
        // Must be orthogonally adjacent (1 step)
        int dr = Math.abs(newRow - p.row);
        int dc = Math.abs(newCol - p.col);
        if (!((dr == 1 && dc == 0) || (dr == 0 && dc == 1))) return false;
        // Destination must be empty
        return grid[newRow][newCol] == null;
    }

    // ──────────────────────────────────────────────────────────────
    //  Actions
    // ──────────────────────────────────────────────────────────────
    /** Move a piece (emitter or mirror) one tile orthogonally */
    public boolean movePiece(Piece p, int newRow, int newCol) {
        if (!canMovePieceTo(p, newRow, newCol)) return false;
        remove(p);
        p.row = newRow;
        p.col = newCol;
        place(p);
        recalculateLasers();
        return true;
    }

    /** Rotate a mirror piece */
    public boolean rotateMirror(Piece p) {
        if (!p.isMirror()) return false;
        p.rotateMirror();
        recalculateLasers();
        return true;
    }

    // ──────────────────────────────────────────────────────────────
    //  Laser tracing
    // ──────────────────────────────────────────────────────────────
    public void recalculateLasers() {
        traceBeam(PlayerID.RED,  redBeam,  false);
        traceBeam(PlayerID.BLUE, blueBeam, false);
        redHitsTarget  = checkHitsTarget(PlayerID.RED,  redBeam);
        blueHitsTarget = checkHitsTarget(PlayerID.BLUE, blueBeam);
    }

    private void traceBeam(PlayerID shooter, List<Point> beam, boolean dummy) {
        beam.clear();

        // Find emitter
        Piece emitter = null;
        for (Piece[] row : grid)
            for (Piece pc : row)
                if (pc != null && pc.owner == shooter && pc.type == PieceType.EMITTER) {
                    emitter = pc; break;
                }
        if (emitter == null) return;

        // Emitter always fires RIGHT for RED, LEFT for BLUE
        Direction dir = (shooter == PlayerID.RED) ? Direction.RIGHT : Direction.LEFT;

        int row = emitter.row;
        int col = emitter.col;

        Set<String> visited = new HashSet<>(); // prevent infinite loops

        while (true) {
            row += dir.dy();
            col += dir.dx();

            if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) break;

            String key = row + "," + col + "," + dir;
            if (visited.contains(key)) break; // loop guard
            visited.add(key);

            beam.add(new Point(col, row)); // stored as (col=x, row=y)

            Piece pc = grid[row][col];
            if (pc == null) continue;

            if (pc.isMirror()) {
                dir = pc.reflectBeam(dir);
            } else {
                // Hit a non-mirror piece — stop
                break;
            }
        }
    }

    private boolean checkHitsTarget(PlayerID shooter, List<Point> beam) {
        PlayerID enemy = shooter.opponent();
        if (beam.isEmpty()) return false;
        Point last = beam.get(beam.size() - 1);
        Piece pc = grid[last.y][last.x]; // y=row, x=col
        return pc != null && pc.type == PieceType.CRYSTAL && pc.owner == enemy;
    }

    /** Returns the winner if a laser hits a crystal, else null */
    public PlayerID checkWinner() {
        if (redHitsTarget)  return PlayerID.RED;
        if (blueHitsTarget) return PlayerID.BLUE;
        return null;
    }

    // Deep copy for AI / preview use
    public Board copy() {
        Board b = new Board();
        // clear the board
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                b.grid[r][c] = null;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] != null) {
                    Piece p = grid[r][c];
                    b.grid[r][c] = new Piece(p.type, p.owner, p.row, p.col);
                }
        b.recalculateLasers();
        return b;
    }
}
