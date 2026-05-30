package mirrorduel;

import mirrorduel.GameEnums.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * GameWindow — minimalist UI matching the provided mockup.
 * Palette: off-white card (#EDEDED), grey board tiles, red/blue text accents.
 */
public class GameWindow extends JFrame {

    // ── Palette ──────────────────────────────────────────────────
    private static final Color BG          = new Color(30, 30, 30);      // outer bg
    private static final Color CARD_BG     = new Color(210, 210, 210);   // rounded card
    private static final Color BTN_BG      = new Color(188, 188, 188);   // button fill
    private static final Color BTN_FG      = new Color(90, 90, 90);      // button text/icon
    private static final Color RED_ACCENT  = new Color(210, 55, 55);
    private static final Color BLUE_ACCENT = new Color(55, 130, 210);
    private static final Color TEXT_DARK   = new Color(60, 60, 60);
    private static final Color TEXT_MID    = new Color(100, 100, 100);

    private static final int CARD_RADIUS = 22;

    private final GameManager gm = new GameManager();
    private final CardLayout  cards = new CardLayout();
    private final JPanel      root  = new JPanel(cards);

    private JPanel    menuPanel, rulesPanel, gamePanel, victoryPanel;
    private BoardPanel boardPanel;

    // Sidebar / game widgets
    private JLabel    turnLabel, playerLabel, instructionLabel;
    private JButton   rotateBtn;
    private JTextArea logArea;
    private JLabel    winnerLabel;

    public GameWindow() {
        super("Mirror Duel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG);

        buildMenuPanel();
        buildRulesPanel();
        buildGamePanel();
        buildVictoryPanel();

        root.add(menuPanel,    "MENU");
        root.add(rulesPanel,   "RULES");
        root.add(gamePanel,    "GAME");
        root.add(victoryPanel, "VICTORY");
        root.setBackground(BG);

        add(root);
        cards.show(root, "MENU");
        pack();
        setLocationRelativeTo(null);
    }

    // ══════════════════════════════════════════════════════════════
    //  MAIN MENU
    // ══════════════════════════════════════════════════════════════
    private void buildMenuPanel() {
        menuPanel = darkPanel(new GridBagLayout());
        menuPanel.setPreferredSize(new Dimension(480, 500));

        JPanel card = card(new GridBagLayout());
        card.setPreferredSize(new Dimension(320, 360));

        GridBagConstraints gbc = gbc();

        JLabel title = label("MIRROR DUEL", 32, Font.BOLD, RED_ACCENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(18, 20, 4, 20);
        card.add(title, gbc);

        JLabel sub = label("Laser Strategy · 2 Players", 12, Font.PLAIN, TEXT_MID);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1; gbc.insets = new Insets(0, 20, 18, 20);
        card.add(sub, gbc);

        card.add(sep(), gbc(0, 2, new Insets(0, 16, 14, 16)));

        JButton play = btn("▶  Play Game");
        play.addActionListener(e -> {
            gm.startNewGame(); refreshSidebar();
            cards.show(root, "GAME"); pack();
        });
        gbc.gridy = 3; gbc.insets = new Insets(6, 20, 6, 20); gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(play, gbc);

        JButton rules = btn("?  How to Play");
        rules.addActionListener(e -> cards.show(root, "RULES"));
        gbc.gridy = 4;
        card.add(rules, gbc);

        JButton quit = btn("✕  Quit");
        quit.addActionListener(e -> System.exit(0));
        gbc.gridy = 5; gbc.insets = new Insets(6, 20, 20, 20);
        card.add(quit, gbc);

        menuPanel.add(card);
    }

    // ══════════════════════════════════════════════════════════════
    //  RULES
    // ══════════════════════════════════════════════════════════════
    private void buildRulesPanel() {
        rulesPanel = darkPanel(new BorderLayout(0, 0));
        rulesPanel.setPreferredSize(new Dimension(480, 500));

        JPanel card = card(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));

        JLabel title = label("How to Play", 22, Font.BOLD, TEXT_DARK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        String txt =
            "OBJECTIVE\n" +
            "  Hit the enemy Core Crystal with your laser.\n\n" +
            "PIECES (each player)\n" +
            "  ◉  Emitter  — fires your laser\n" +
            "  ◈  Crystal  — protect this!\n" +
            "  /  Slash mirror  |  \\  Backslash mirror\n\n" +
            "EACH TURN — pick ONE action:\n" +
            "  1. Move a piece one tile (orthogonal)\n" +
            "  2. Rotate a mirror  / ↔ \\\n\n" +
            "REFLECTION  (Slash /)\n" +
            "  Left→Up  Right→Down  Up→Right  Down→Left\n\n" +
            "REFLECTION  (Backslash \\)\n" +
            "  Left→Down  Right→Up  Up→Left  Down→Right\n\n" +
            "CONTROLS\n" +
            "  Click piece → select (yellow ring)\n" +
            "  Click green tile → move there\n" +
            "  Press ROTATE → rotate selected mirror\n\n" +
            "WIN\n" +
            "  Laser reaches enemy crystal — instant win!";

        JTextArea ta = new JTextArea(txt);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setBackground(new Color(230, 230, 230));
        ta.setForeground(TEXT_DARK);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(new Color(195, 195, 195)));
        card.add(sp, BorderLayout.CENTER);

        JButton back = imgBtn("btn_mainmenu.png", "← Menu");
        back.addActionListener(e -> cards.show(root, "MENU"));
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bp.setOpaque(false);
        bp.add(back);
        card.add(bp, BorderLayout.SOUTH);

        JPanel wrap = darkPanel(new GridBagLayout());
        wrap.setPreferredSize(new Dimension(480, 500));
        wrap.add(card);
        rulesPanel = wrap;
    }

    // ══════════════════════════════════════════════════════════════
    //  GAME SCREEN
    // ══════════════════════════════════════════════════════════════
    private void buildGamePanel() {
        gamePanel = darkPanel(new GridBagLayout());

        boardPanel = new BoardPanel(gm);
        boardPanel.addPropertyChangeListener("gameStateChanged", e -> {
            refreshSidebar();
            if (gm.getScreen() == GameScreen.VICTORY) showVictory();
        });

        // Outer card wraps board + bottom bar
        int boardPx = BoardPanel.BOARD_PX;
        int cardW   = boardPx + 32;

        JPanel card = card(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        card.setPreferredSize(new Dimension(cardW, boardPx + 160));

        // ── Top: turn label ──
        playerLabel = label("RED'S TURN", 16, Font.BOLD, RED_ACCENT);
        playerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        turnLabel = label("Turn 1", 11, Font.PLAIN, TEXT_MID);
        turnLabel.setHorizontalAlignment(SwingConstants.CENTER);

        instructionLabel = label("Click a piece to select", 11, Font.PLAIN, TEXT_MID);
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel topBar = new JPanel();
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setOpaque(false);
        playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topBar.add(playerLabel);
        topBar.add(Box.createVerticalStrut(2));
        topBar.add(turnLabel);
        topBar.add(Box.createVerticalStrut(2));
        topBar.add(instructionLabel);

        card.add(topBar, BorderLayout.NORTH);

        // ── Centre: board ──
        JPanel boardWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boardWrap.setOpaque(false);
        boardWrap.add(boardPanel);
        card.add(boardWrap, BorderLayout.CENTER);

        // ── Bottom: rotate hint + buttons ──
        rotateBtn = btn("↺  Rotate Mirror");
        rotateBtn.setEnabled(false);
        rotateBtn.addActionListener(e -> {
            if (gm.handleRotate()) {
                refreshSidebar(); boardPanel.repaint();
                if (gm.getScreen() == GameScreen.VICTORY) showVictory();
            }
        });

        JButton menuBtn    = imgBtn("btn_mainmenu.png", "Main Menu");
        JButton restartBtn = imgBtn("btn_restart.png",  "Restart");

        menuBtn.addActionListener(e -> {
            gm.setScreen(GameScreen.MAIN_MENU);
            cards.show(root, "MENU"); pack();
        });
        restartBtn.addActionListener(e -> {
            gm.startNewGame(); refreshSidebar(); boardPanel.repaint();
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        btnRow.add(menuBtn);
        btnRow.add(restartBtn);

        JPanel bottomBar = new JPanel();
        bottomBar.setLayout(new BoxLayout(bottomBar, BoxLayout.Y_AXIS));
        bottomBar.setOpaque(false);
        rotateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomBar.add(Box.createVerticalStrut(4));
        bottomBar.add(rotateBtn);
        bottomBar.add(Box.createVerticalStrut(8));
        bottomBar.add(btnRow);

        card.add(bottomBar, BorderLayout.SOUTH);

        gamePanel.add(card);
    }

    // ══════════════════════════════════════════════════════════════
    //  VICTORY
    // ══════════════════════════════════════════════════════════════
    private void buildVictoryPanel() {
        victoryPanel = darkPanel(new GridBagLayout());
        victoryPanel.setPreferredSize(new Dimension(480, 400));

        JPanel card = card(new GridBagLayout());
        card.setPreferredSize(new Dimension(300, 280));

        GridBagConstraints gbc = gbc();

        JLabel crown = label("♛", 60, Font.PLAIN, RED_ACCENT);
        crown.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(20, 20, 6, 20);
        card.add(crown, gbc);

        winnerLabel = label("RED WINS!", 28, Font.BOLD, RED_ACCENT);
        winnerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1; gbc.insets = new Insets(0, 20, 4, 20);
        card.add(winnerLabel, gbc);

        JLabel sub = label("The laser found its mark.", 12, Font.PLAIN, TEXT_MID);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 2; gbc.insets = new Insets(0, 20, 14, 20);
        card.add(sub, gbc);

        card.add(sep(), gbc(0, 3, new Insets(0, 16, 14, 16)));

        JButton playAgain = imgBtn("btn_restart.png", "Play Again");
        playAgain.addActionListener(e -> {
            gm.startNewGame(); refreshSidebar(); boardPanel.repaint();
            cards.show(root, "GAME"); pack();
        });
        gbc.gridy = 4; gbc.insets = new Insets(4, 20, 6, 20); gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(playAgain, gbc);

        JButton menuBtn = imgBtn("btn_mainmenu.png", "Main Menu");
        menuBtn.addActionListener(e -> cards.show(root, "MENU"));
        gbc.gridy = 5; gbc.insets = new Insets(4, 20, 20, 20);
        card.add(menuBtn, gbc);

        victoryPanel.add(card);
    }

    private void showVictory() {
        PlayerID w = gm.getWinner();
        if (w == null) return;
        if (w == PlayerID.RED) {
            winnerLabel.setText("RED WINS!");
            winnerLabel.setForeground(RED_ACCENT);
            ((JLabel) ((JPanel) victoryPanel.getComponent(0))
                    .getComponent(0)).setForeground(RED_ACCENT);
        } else {
            winnerLabel.setText("BLUE WINS!");
            winnerLabel.setForeground(BLUE_ACCENT);
        }
        cards.show(root, "VICTORY"); pack();
    }

    // ══════════════════════════════════════════════════════════════
    //  Sidebar / state refresh
    // ══════════════════════════════════════════════════════════════
    public void refreshSidebar() {
        if (gm.getBoard() == null) return;
        PlayerID cp = gm.getCurrentPlayer();

        turnLabel.setText("Turn " + gm.getTurnNumber());

        if (cp == PlayerID.RED) {
            playerLabel.setText("RED'S TURN");
            playerLabel.setForeground(RED_ACCENT);
        } else {
            playerLabel.setText("BLUE'S TURN");
            playerLabel.setForeground(BLUE_ACCENT);
        }

        Piece sel = gm.getSelectedPiece();
        if (sel == null) {
            instructionLabel.setText("Click a piece to select");
            rotateBtn.setEnabled(false);
        } else if (sel.isMirror()) {
            String sym = sel.type == PieceType.MIRROR_SLASH ? "/" : "\\";
            instructionLabel.setText("Mirror " + sym + " selected — move or rotate");
            rotateBtn.setEnabled(sel.owner == cp);
        } else {
            instructionLabel.setText(sel.type + " selected — click green tile to move");
            rotateBtn.setEnabled(false);
        }

        boardPanel.repaint();
    }

    // ══════════════════════════════════════════════════════════════
    //  Widget factory helpers
    // ══════════════════════════════════════════════════════════════
    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG);
        return p;
    }

    /** Rounded card panel */
    private JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS);
            }
        };
        p.setOpaque(false);
        return p;
    }

    /** Plain text button styled like the mockup grey pill */
    private JButton btn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BTN_BG.darker() : BTN_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(BTN_FG);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(200, 38));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /**
     * Button that tries to use the provided image asset;
     * falls back to the grey pill button with fallbackText if image not found.
     */
    private JButton imgBtn(String assetName, String fallbackText) {
        BufferedImage img = AssetLoader.load(assetName);
        if (img != null) {
            // Scale to a comfortable button size (200×44)
            int bw = 200, bh = 44;
            BufferedImage scaled = AssetLoader.scaled(assetName, bw, bh);
            JButton b = new JButton(new ImageIcon(scaled)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isRollover()) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));
                    }
                    super.paintComponent(g);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            };
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(bw, bh));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
        return btn(fallbackText);
    }

    private JLabel label(String txt, int size, int style, Color color) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(180, 180, 180));
        s.setMaximumSize(new Dimension(260, 1));
        return s;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 20, 6, 20);
        return g;
    }

    private GridBagConstraints gbc(int x, int y, Insets ins) {
        GridBagConstraints g = gbc();
        g.gridx = x; g.gridy = y; g.insets = ins;
        return g;
    }
}
