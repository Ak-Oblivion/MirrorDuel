package mirrorduel;

import mirrorduel.GameEnums.*;
import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private Board board;
    private PlayerID currentPlayer;
    private PlayerID winner;
    private GameScreen screen;

    // Selected piece for move/rotate actions
    private Piece selectedPiece;
    private boolean awaitingMoveTarget; // true when user has picked a piece and is choosing destination

    // Move history for display
    private final List<String> moveLog = new ArrayList<>();
    private int turnNumber = 1;

    public GameManager() {
        screen = GameScreen.MAIN_MENU;
    }

    public void startNewGame() {
        board = new Board();
        currentPlayer = PlayerID.RED;
        winner = null;
        screen = GameScreen.PLAYING;
        selectedPiece = null;
        awaitingMoveTarget = false;
        moveLog.clear();
        turnNumber = 1;
    }

    // ──────────────────────────────────────────────────────────────
    //  Input handling from BoardPanel click
    // ──────────────────────────────────────────────────────────────
    /**
     * Called when player clicks a board cell.
     * Returns true if the board changed (needs repaint).
     */
    public boolean handleCellClick(int row, int col) {
        if (screen != GameScreen.PLAYING || winner != null) return false;

        Piece clickedPiece = board.pieceAt(row, col);

        if (awaitingMoveTarget) {
            // Second click — destination
            if (clickedPiece == null) {
                // Attempt to move selected piece here
                boolean moved = board.movePiece(selectedPiece, row, col);
                if (moved) {
                    log("Move " + selectedPiece.type + " to (" + row + "," + col + ")");
                    endTurn();
                    return true;
                }
            }
            // Clicking same piece cancels selection
            if (clickedPiece == selectedPiece) {
                selectedPiece = null;
                awaitingMoveTarget = false;
                return true;
            }
            // Clicking another own piece switches selection
            if (clickedPiece != null && clickedPiece.owner == currentPlayer) {
                selectedPiece = clickedPiece;
                return true;
            }
            return false;
        }

        // First click — select a piece
        if (clickedPiece != null && clickedPiece.owner == currentPlayer) {
            selectedPiece = clickedPiece;
            awaitingMoveTarget = true;
            return true;
        }
        return false;
    }

    /**
     * Called when "Rotate" button is pressed.
     * Rotates the currently selected mirror (if any).
     */
    public boolean handleRotate() {
        if (screen != GameScreen.PLAYING || winner != null) return false;
        if (selectedPiece == null || !selectedPiece.isMirror()) return false;
        if (selectedPiece.owner != currentPlayer) return false;

        board.rotateMirror(selectedPiece);
        log("Rotate mirror at (" + selectedPiece.row + "," + selectedPiece.col + ")");
        selectedPiece = null;
        awaitingMoveTarget = false;
        endTurn();
        return true;
    }

    private void endTurn() {
        selectedPiece = null;
        awaitingMoveTarget = false;

        PlayerID w = board.checkWinner();
        if (w != null) {
            winner = w;
            screen = GameScreen.VICTORY;
            return;
        }

        currentPlayer = currentPlayer.opponent();
        turnNumber++;
    }

    private void log(String msg) {
        moveLog.add("T" + turnNumber + " [" + currentPlayer + "] " + msg);
        if (moveLog.size() > 20) moveLog.remove(0);
    }

    // ──────────────────────────────────────────────────────────────
    //  Getters
    // ──────────────────────────────────────────────────────────────
    public Board getBoard()              { return board; }
    public PlayerID getCurrentPlayer()   { return currentPlayer; }
    public PlayerID getWinner()          { return winner; }
    public GameScreen getScreen()        { return screen; }
    public Piece getSelectedPiece()      { return selectedPiece; }
    public boolean isAwaitingMove()      { return awaitingMoveTarget; }
    public List<String> getMoveLog()     { return moveLog; }
    public int getTurnNumber()           { return turnNumber; }

    public void setScreen(GameScreen s)  { this.screen = s; }
}
