package mirrorduel;

import mirrorduel.GameEnums.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class BoardPanel extends JPanel {
    static final int TILE = 70;
    static final int BOARD_PX = TILE * Board.SIZE;

    private static final Color TILE_LIGHT   = new Color(238, 238, 238);
    private static final Color TILE_DARK    = new Color(178, 194, 208);
    private static final Color LASER_RED    = new Color(220, 50, 50, 210);
    private static final Color LASER_BLUE   = new Color(60, 140, 220, 210);
    private static final Color SELECT_RING  = new Color(255, 200, 0, 220);
    private static final Color VALID_FILL   = new Color(120, 220, 140, 100);
    private static final Color VALID_RING   = new Color(80, 180, 100, 200);

    private final GameManager gm;

    public BoardPanel(GameManager gm) {
        this.gm = gm;
        setPreferredSize(new Dimension(BOARD_PX, BOARD_PX));
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = e.getX() / TILE;
                int row = e.getY() / TILE;
                if (col >= 0 && col < Board.SIZE && row >= 0 && row < Board.SIZE) {
                    boolean changed = gm.handleCellClick(row, col);
                    if (changed) {
                        repaint();
                        firePropertyChange("gameStateChanged", false, true);
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawGrid(g);
        if (gm.getBoard() != null) {
            drawValidMoveHints(g);
            drawLasers(g);
            drawPieces(g);
            drawSelectionHighlight(g);
        }
    }

    private void drawGrid(Graphics2D g) {
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++) {
                g.setColor((r + c) % 2 == 0 ? TILE_LIGHT : TILE_DARK);
                g.fillRect(c * TILE, r * TILE, TILE, TILE);
            }
    }

    private void drawValidMoveHints(Graphics2D g) {
        if (!gm.isAwaitingMove() || gm.getSelectedPiece() == null) return;
        Piece sel = gm.getSelectedPiece();
        int[] drs = {-1, 1, 0, 0};
        int[] dcs = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nr = sel.row + drs[i], nc = sel.col + dcs[i];
            if (nr >= 0 && nr < Board.SIZE && nc >= 0 && nc < Board.SIZE
                    && gm.getBoard().pieceAt(nr, nc) == null) {
                g.setColor(VALID_FILL);
                g.fillRoundRect(nc * TILE + 5, nr * TILE + 5, TILE - 10, TILE - 10, 8, 8);
                g.setColor(VALID_RING);
                g.setStroke(new BasicStroke(1.8f));
                g.drawRoundRect(nc * TILE + 5, nr * TILE + 5, TILE - 10, TILE - 10, 8, 8);
            }
        }
    }

    private void drawLasers(Graphics2D g) {
        drawBeam(g, gm.getBoard().getBeam(PlayerID.RED),  LASER_RED,  PlayerID.RED);
        drawBeam(g, gm.getBoard().getBeam(PlayerID.BLUE), LASER_BLUE, PlayerID.BLUE);
    }

    private void drawBeam(Graphics2D g, List<Point> beam, Color color, PlayerID player) {
        if (beam == null || beam.isEmpty()) return;
        Piece emitter = null;
        for (Piece p : gm.getBoard().piecesOf(player))
            if (p.type == PieceType.EMITTER) { emitter = p; break; }
        if (emitter == null) return;

        int sx = emitter.col * TILE + TILE / 2;
        int sy = emitter.row * TILE + TILE / 2;

        GeneralPath path = new GeneralPath();
        path.moveTo(sx, sy);
        for (Point pt : beam) path.lineTo(pt.x * TILE + TILE / 2, pt.y * TILE + TILE / 2);

        // Soft glow
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        // Core
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        // Bright centre
        g.setColor(new Color(255, 255, 255, 160));
        g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    private void drawPieces(Graphics2D g) {
        int pad = 5, size = TILE - pad * 2;
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = gm.getBoard().pieceAt(r, c);
                if (p != null) drawPiece(g, p, c * TILE + pad, r * TILE + pad, size);
            }
    }

    private void drawPiece(Graphics2D g, Piece p, int x, int y, int size) {
        boolean red = p.owner == PlayerID.RED;
        String imgName = switch (p.type) {
            case EMITTER -> red ? "emitter_red.png"  : "emitter_blue.png";
            case CRYSTAL -> red ? "crystal_red.png"  : "crystal_blue.png";
            case MIRROR_SLASH, MIRROR_BACKSLASH -> red ? "mirror_red.png" : "mirror_blue.png";
        };

        BufferedImage img = AssetLoader.scaled(imgName, size, size);
        if (img != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (p.type == PieceType.MIRROR_BACKSLASH) {
                g2.translate(x + size, y);
                g2.scale(-1, 1);
                g2.drawImage(img, 0, 0, size, size, null);
            } else {
                g2.drawImage(img, x, y, size, size, null);
            }
            g2.dispose();
        } else {
            drawFallback(g, p, x, y, size);
        }

        if (p.isMirror()) {
            g.setColor(new Color(255, 255, 255, 200));
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            String sym = p.type == PieceType.MIRROR_SLASH ? "/" : "\\";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(sym, x + (size - fm.stringWidth(sym)) / 2,
                              y + (size + fm.getAscent()) / 2 - 3);
        }
    }

    private void drawFallback(Graphics2D g, Piece p, int x, int y, int size) {
        boolean red = p.owner == PlayerID.RED;
        Color base   = red ? new Color(210, 65, 65)  : new Color(65, 130, 210);
        Color accent = red ? new Color(255, 130, 130) : new Color(130, 190, 255);
        switch (p.type) {
            case EMITTER -> {
                g.setColor(base); g.fillOval(x, y, size, size);
                g.setColor(accent); g.setStroke(new BasicStroke(2.5f));
                g.drawOval(x+4, y+4, size-8, size-8);
                g.setColor(new Color(0,0,0,160)); g.fillOval(x+size/3, y+size/3, size/3, size/3);
            }
            case CRYSTAL -> {
                g.setColor(base); g.fillOval(x, y, size, size);
                g.setColor(accent); g.setStroke(new BasicStroke(2f));
                g.drawOval(x+4, y+4, size-8, size-8);
                int cx=x+size/2, cy=y+size/2;
                int[] cx2={cx-10,cx-10,cx-5,cx,cx+5,cx+10,cx+10};
                int[] cy2={cy+5, cy-3, cy+2, cy-7, cy+2, cy-3, cy+5};
                g.setColor(accent); g.fillPolygon(cx2,cy2,7);
            }
            case MIRROR_SLASH, MIRROR_BACKSLASH -> {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(base);
                g2.translate(x+size/2, y+size/2);
                g2.rotate(Math.toRadians(p.type==PieceType.MIRROR_SLASH ? -35 : 35));
                g2.fillOval(-size/2, -size/6, size, size/3);
                g2.setColor(accent); g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(-size/2, -size/6, size, size/3);
                g2.dispose();
            }
        }
    }

    private void drawSelectionHighlight(Graphics2D g) {
        Piece sel = gm.getSelectedPiece();
        if (sel == null) return;
        g.setColor(SELECT_RING);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(sel.col*TILE+2, sel.row*TILE+2, TILE-4, TILE-4, 8, 8);
    }
}
