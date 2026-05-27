package com.mirrorduel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Traces both players' laser beams through the board after every action.
 *
 * Reflection rules:
 *   SLASH  (/): RIGHT→DOWN, DOWN→LEFT, LEFT→UP,  UP→RIGHT
 *   BACKSLASH (\): RIGHT→UP, UP→LEFT,  LEFT→DOWN, DOWN→RIGHT
 */
@SuppressWarnings("unchecked")
public class LaserSystem {

    private static final int MAX_BOUNCES = 64;

    private final List<LaserSegment>[] segments = new List[2];
    private int winner = -1;  // -1 = no winner yet

    public LaserSystem() {
        segments[0] = new ArrayList<>();
        segments[1] = new ArrayList<>();
    }

    /**
     * Re-traces both laser beams and checks win conditions.
     * Call this after every board action.
     */
    public void update(Board board, Player[] players) {
        winner = -1;
        for (int p = 0; p < 2; p++) {
            segments[p].clear();
            segments[p].addAll(trace(board, players[p].getEmitter(), p));

            // Check if any terminal segment hit the opponent's crystal
            for (LaserSegment seg : segments[p]) {
                if (seg.hitCrystal) {
                    winner = p;
                }
            }
        }
    }

    /** Trace a single player's laser path and return the segment list. */
    private List<LaserSegment> trace(Board board, LaserEmitter emitter, int playerIndex) {
        List<LaserSegment> path = new ArrayList<>();

        int       col = emitter.getCol();
        int       row = emitter.getRow();
        Direction dir = emitter.getDirection();
        int       bounces = 0;

        while (bounces++ < MAX_BOUNCES) {
            int nc = col + dir.dc;
            int nr = row + dir.dr;

            LaserSegment seg = new LaserSegment(col, row, nc, nr, dir);

            if (!board.isValid(nc, nr)) {
                // Exited the board
                seg.isTerminal = true;
                path.add(seg);
                break;
            }

            Tile tile = board.getTile(nc, nr);

            if (tile.hasMirror()) {
                // Hit a mirror – add segment to mirror centre, then reflect
                path.add(seg);
                dir = tile.getMirror().reflect(dir);
                col = nc;
                row = nr;

            } else if (tile.hasCrystal()) {
                Crystal crystal = tile.getCrystal();
                if (crystal.getOwner() != playerIndex) {
                    // Hit the OPPONENT's crystal ➜ win condition
                    seg.hitCrystal = true;
                    seg.isTerminal = true;
                    path.add(seg);
                    break;
                } else {
                    // Hit own crystal – stop beam harmlessly
                    seg.isTerminal = true;
                    path.add(seg);
                    break;
                }

            } else if (tile.hasEmitter()) {
                // Hit any emitter – stop
                seg.isTerminal = true;
                path.add(seg);
                break;

            } else {
                // Empty tile – continue straight
                path.add(seg);
                col = nc;
                row = nr;
            }
        }

        return path;
    }

    public List<LaserSegment> getSegments(int playerIndex) {
        return segments[playerIndex];
    }

    /** @return index of winner (0 or 1), or -1 if no winner yet. */
    public int getWinner() { return winner; }
}
