package com.mirrorduel.core;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Central game controller.
 *
 * Turn flow:
 *   1. Player clicks on their own mirror → it becomes selected (highlighted move targets shown)
 *   2a. Player clicks on an empty highlighted tile → mirror moves there
 *   2b. Player clicks on the selected mirror again → it rotates
 *   3. Laser is re-traced; if a crystal is hit the game ends.
 *   4. Turn passes to the other player.
 */
public class GameManager {

    private Board        board;
    private Player[]     players;
    private LaserSystem  laserSystem;
    private int          currentPlayer; // 0 or 1
    private GameState    state;

    // Selection state
    private Mirror  selectedMirror;
    private int     selectedCol = -1;
    private int     selectedRow = -1;

    // Win/end-game state
    private int   winner  = -1;
    private float endTimer = 0f;  // counts up after win detected

    // Action-ready flag (laser just updated, ready for render flash)
    private boolean actionJustTaken;

    public GameManager() {
        state = GameState.MENU;
    }

    /** Build the initial board and start a new game. */
    public void initialize() {
        board       = new Board();
        laserSystem = new LaserSystem();
        players     = new Player[2];
        winner      = -1;
        endTimer    = 0f;
        selectedMirror = null;
        selectedCol    = -1;
        selectedRow    = -1;
        currentPlayer  = 0;
        state          = GameState.PLAYING;

        // ---- Player 1 (Blue) ----
        players[0] = new Player(0, "PLAYER 1", Player.P1_COLOR, Player.P1_LASER_COLOR);

        LaserEmitter e0 = new LaserEmitter(0, 4, 0, Direction.RIGHT);
        Crystal      c0 = new Crystal(7, 5, 0);
        List<Mirror> m0 = new ArrayList<>();
        m0.add(new Mirror(2, 2, 0, MirrorType.BACKSLASH));
        m0.add(new Mirror(5, 6, 0, MirrorType.SLASH));
        m0.add(new Mirror(1, 6, 0, MirrorType.SLASH));

        players[0].setEmitter(e0);
        players[0].setCrystal(c0);
        players[0].setMirrors(m0);

        board.placePiece(e0, 0, 4);
        board.placePiece(c0, 7, 5);
        for (Mirror m : m0) board.placePiece(m, m.getCol(), m.getRow());

        // ---- Player 2 (Red) ----
        players[1] = new Player(1, "PLAYER 2", Player.P2_COLOR, Player.P2_LASER_COLOR);

        LaserEmitter e1 = new LaserEmitter(7, 3, 1, Direction.LEFT);
        Crystal      c1 = new Crystal(0, 2, 1);
        List<Mirror> m1 = new ArrayList<>();
        m1.add(new Mirror(5, 1, 1, MirrorType.BACKSLASH));
        m1.add(new Mirror(2, 5, 1, MirrorType.SLASH));
        m1.add(new Mirror(6, 1, 1, MirrorType.BACKSLASH));

        players[1].setEmitter(e1);
        players[1].setCrystal(c1);
        players[1].setMirrors(m1);

        board.placePiece(e1, 7, 3);
        board.placePiece(c1, 0, 2);
        for (Mirror m : m1) board.placePiece(m, m.getCol(), m.getRow());

        // Trace initial laser paths
        laserSystem.update(board, players);
        actionJustTaken = false;
    }

    // ------------------------------------------------------------------ //
    //  Update
    // ------------------------------------------------------------------ //

    public void update(float delta) {
        if (state == GameState.GAME_OVER) {
            endTimer += delta;
        }

        // Update piece animations
        if (board != null) {
            board.update(delta);
            for (Player p : players) {
                p.getEmitter().update(delta);
                p.getCrystal().update(delta);
                for (Mirror m : p.getMirrors()) m.update(delta);
            }
        }
        actionJustTaken = false;
    }

    // ------------------------------------------------------------------ //
    //  Input
    // ------------------------------------------------------------------ //

    /**
     * Handle a tile click from the player.
     * @param col  clicked column
     * @param row  clicked row
     */
    public void handleTileClick(int col, int row) {
        if (state != GameState.PLAYING) return;
        if (!board.isValid(col, row)) return;

        Tile tile = board.getTile(col, row);

        // Case 1 – clicking on a highlighted empty tile → MOVE
        if (tile.isHighlighted() && selectedMirror != null && tile.isEmpty()) {
            performMove(selectedCol, selectedRow, col, row);
            return;
        }

        // Case 2 – clicking on the selected mirror again → ROTATE
        if (tile.isSelected() && selectedMirror != null) {
            performRotate();
            return;
        }

        // Case 3 – clicking on a different own mirror → SELECT it
        if (tile.hasMirror() && tile.getMirror().getOwner() == currentPlayer) {
            selectMirror(col, row);
            return;
        }

        // Case 4 – clicking elsewhere → DESELECT
        clearSelection();
    }

    /** Player manually rotates the selected mirror (keyboard shortcut). */
    public void handleRotateKey() {
        if (state == GameState.PLAYING && selectedMirror != null) {
            performRotate();
        }
    }

    // ------------------------------------------------------------------ //
    //  Actions
    // ------------------------------------------------------------------ //

    private void selectMirror(int col, int row) {
        clearSelection();
        selectedMirror = board.getTile(col, row).getMirror();
        selectedCol    = col;
        selectedRow    = row;

        board.getTile(col, row).setSelected(true);

        // Highlight adjacent empty tiles as valid move targets
        for (int[] pos : board.getAdjacentEmpty(col, row)) {
            board.getTile(pos[0], pos[1]).setHighlighted(true);
        }
    }

    private void clearSelection() {
        selectedMirror = null;
        selectedCol    = -1;
        selectedRow    = -1;
        board.clearHighlights();
    }

    private void performMove(int fromCol, int fromRow, int toCol, int toRow) {
        board.movePiece(fromCol, fromRow, toCol, toRow);
        clearSelection();
        afterAction();
    }

    private void performRotate() {
        selectedMirror.toggleType();
        clearSelection();
        afterAction();
    }

    /** Called after any board-changing action to update laser and check win. */
    private void afterAction() {
        laserSystem.update(board, players);
        actionJustTaken = true;

        int w = laserSystem.getWinner();
        if (w >= 0) {
            winner = w;
            players[1 - w].getCrystal().destroy(); // destroy the loser's crystal
            state  = GameState.GAME_OVER;
        } else {
            // Next player's turn
            currentPlayer = 1 - currentPlayer;
        }
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //

    public Board        getBoard()              { return board; }
    public Player[]     getPlayers()            { return players; }
    public Player       getCurrentPlayer()      { return players[currentPlayer]; }
    public int          getCurrentPlayerIndex() { return currentPlayer; }
    public LaserSystem  getLaserSystem()        { return laserSystem; }
    public GameState    getGameState()          { return state; }
    public int          getWinner()             { return winner; }
    public float        getEndTimer()           { return endTimer; }
    public boolean      actionJustTaken()       { return actionJustTaken; }

    public Mirror  getSelectedMirror() { return selectedMirror; }
    public int     getSelectedCol()    { return selectedCol; }
    public int     getSelectedRow()    { return selectedRow; }

    public boolean isHighlightedTile(int col, int row) {
        return board.isValid(col, row) && board.getTile(col, row).isHighlighted();
    }

    public void setState(GameState s) { state = s; }
}
