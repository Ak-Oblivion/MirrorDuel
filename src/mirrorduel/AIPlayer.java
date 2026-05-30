package mirrorduel;

import mirrorduel.GameEnums.*;
import java.awt.Point;
import java.util.*;

/**
 * Minimax AI with alpha-beta pruning.
 * Hard mode: depth-5 search — strong enough to beat expert human players.
 * Easy mode: same engine but takes a fully random move 35% of the time
 *            (still grabs immediate wins even in random branches).
 */
public class AIPlayer {

    public enum Difficulty { EASY, HARD }

    // ── Move representation ───────────────────────────────────────────────────
    public static class Move {
        public enum Type { MOVE_PIECE, ROTATE }
        public final Type type;
        public final int pieceRow, pieceCol;
        public final int targetRow, targetCol;

        /** Movement constructor */
        public Move(int pr, int pc, int tr, int tc) {
            this.type = Type.MOVE_PIECE;
            pieceRow = pr; pieceCol = pc;
            targetRow = tr; targetCol = tc;
        }

        /** Rotation constructor */
        public Move(int pr, int pc) {
            this.type = Type.ROTATE;
            pieceRow = pr; pieceCol = pc;
            targetRow = -1; targetCol = -1;
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int    HARD_DEPTH         = 5;
    private static final double EASY_RANDOM_CHANCE = 0.35;
    private static final Random RNG                = new Random();

    // ── State ─────────────────────────────────────────────────────────────────
    private final PlayerID   aiPlayer;
    private final Difficulty difficulty;

    public AIPlayer(PlayerID player, Difficulty difficulty) {
        this.aiPlayer   = player;
        this.difficulty = difficulty;
    }

    // ── Public API ────────────────────────────────────────────────────────────
    /** Compute and return the best move for the AI's current position. */
    public Move getBestMove(Board board) {
        if (difficulty == Difficulty.EASY && RNG.nextDouble() < EASY_RANDOM_CHANCE) {
            return getRandomMove(board);
        }
        return minimaxRoot(board, HARD_DEPTH);
    }

    // ── Random move (easy fallback) ───────────────────────────────────────────
    private Move getRandomMove(Board board) {
        List<Move> moves = generateMoves(board, aiPlayer);
        if (moves.isEmpty()) return null;
        // Even in random mode, grab an immediate win when available
        for (Move m : moves) {
            Board sim = applyMove(board, m, aiPlayer);
            if (sim.checkWinner() == aiPlayer) return m;
        }
        return moves.get(RNG.nextInt(moves.size()));
    }

    // ── Minimax root ──────────────────────────────────────────────────────────
    private Move minimaxRoot(Board board, int depth) {
        List<Move> moves = generateMoves(board, aiPlayer);
        if (moves.isEmpty()) return null;

        // Grab an immediate win before the full search
        for (Move m : moves) {
            Board sim = applyMove(board, m, aiPlayer);
            if (sim.checkWinner() == aiPlayer) return m;
        }

        Move best      = null;
        int  bestScore = Integer.MIN_VALUE;

        for (Move m : moves) {
            Board sim   = applyMove(board, m, aiPlayer);
            int   score = minimax(sim, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            if (score > bestScore) {
                bestScore = score;
                best      = m;
            }
        }
        return best;
    }

    // ── Minimax with alpha-beta pruning ───────────────────────────────────────
    private int minimax(Board board, int depth, int alpha, int beta, boolean maximizing) {
        PlayerID winner = board.checkWinner();
        if (winner == aiPlayer)            return  100_000 + depth; // win sooner is better
        if (winner == aiPlayer.opponent()) return -100_000 - depth; // lose later is better
        if (depth == 0)                    return evaluate(board);

        PlayerID current = maximizing ? aiPlayer : aiPlayer.opponent();
        List<Move> moves = generateMoves(board, current);

        if (maximizing) {
            int max = Integer.MIN_VALUE;
            for (Move m : moves) {
                int score = minimax(applyMove(board, m, current), depth - 1, alpha, beta, false);
                if (score > max)   max   = score;
                if (score > alpha) alpha = score;
                if (beta <= alpha) break;
            }
            return max;
        } else {
            int min = Integer.MAX_VALUE;
            for (Move m : moves) {
                int score = minimax(applyMove(board, m, current), depth - 1, alpha, beta, true);
                if (score < min)  min  = score;
                if (score < beta) beta = score;
                if (beta <= alpha) break;
            }
            return min;
        }
    }

    // ── Heuristic evaluation (positive = good for AI) ─────────────────────────
    private int evaluate(Board board) {
        PlayerID opp = aiPlayer.opponent();

        List<Point> aiBeam  = board.getBeam(aiPlayer);
        List<Point> oppBeam = board.getBeam(opp);

        Piece oppCrystal = findCrystal(board, opp);
        Piece ownCrystal = findCrystal(board, aiPlayer);
        if (oppCrystal == null || ownCrystal == null) return 0;

        int score = 0;

        // 1. Raw beam pressure
        score += aiBeam.size()  * 8;
        score -= oppBeam.size() * 8;

        // 2. AI beam endpoint proximity to enemy crystal
        if (!aiBeam.isEmpty()) {
            Point last = aiBeam.get(aiBeam.size() - 1);
            int dist = Math.abs(last.y - oppCrystal.row) + Math.abs(last.x - oppCrystal.col);
            score += (14 - dist) * 22;
            for (Point pt : aiBeam) {
                if (Math.abs(pt.y - oppCrystal.row) + Math.abs(pt.x - oppCrystal.col) <= 2)
                    score += 12;
            }
        }

        // 3. Enemy beam endpoint proximity to own crystal (danger)
        if (!oppBeam.isEmpty()) {
            Point last = oppBeam.get(oppBeam.size() - 1);
            int dist = Math.abs(last.y - ownCrystal.row) + Math.abs(last.x - ownCrystal.col);
            score -= (14 - dist) * 22;
            for (Point pt : oppBeam) {
                if (Math.abs(pt.y - ownCrystal.row) + Math.abs(pt.x - ownCrystal.col) <= 2)
                    score -= 12;
            }
        }

        // 4. Mirrors actively participating in each beam
        for (Point pt : aiBeam) {
            Piece pc = board.pieceAt(pt.y, pt.x);
            if (pc != null && pc.isMirror())
                score += pc.owner == aiPlayer ? 5 : -3;
        }
        for (Point pt : oppBeam) {
            Piece pc = board.pieceAt(pt.y, pt.x);
            if (pc != null && pc.isMirror())
                score -= pc.owner == opp ? 5 : -3;
        }

        return score;
    }

    // ── Move generation ───────────────────────────────────────────────────────
    private List<Move> generateMoves(Board board, PlayerID player) {
        List<Move> moves = new ArrayList<>();
        int[] drs = {-1, 1,  0, 0};
        int[] dcs = { 0, 0, -1, 1};

        for (Piece piece : board.piecesOf(player)) {
            if (piece.type == PieceType.CRYSTAL) continue; // crystals are fixed per game rules

            for (int i = 0; i < 4; i++) {
                int nr = piece.row + drs[i];
                int nc = piece.col + dcs[i];
                if (board.canMovePieceTo(piece, nr, nc))
                    moves.add(new Move(piece.row, piece.col, nr, nc));
            }

            if (piece.isMirror())
                moves.add(new Move(piece.row, piece.col)); // rotation
        }
        return moves;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Board applyMove(Board board, Move move, PlayerID player) {
        Board copy  = board.copy();
        Piece piece = copy.pieceAt(move.pieceRow, move.pieceCol);
        if (piece == null) return copy;

        if (move.type == Move.Type.ROTATE) {
            copy.rotateMirror(piece);
        } else {
            copy.movePiece(piece, move.targetRow, move.targetCol);
        }
        return copy;
    }

    private Piece findCrystal(Board board, PlayerID player) {
        for (Piece p : board.piecesOf(player))
            if (p.type == PieceType.CRYSTAL) return p;
        return null;
    }
}
