package mirrorduel;

import mirrorduel.GameEnums.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class GameWindow extends JFrame {
    private final GameManager gm = new GameManager();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // Panels
    private JPanel menuPanel, rulesPanel, gamePanel, victoryPanel;
    private BoardPanel boardPanel;

    // Sidebar widgets
    private JLabel turnLabel, playerLabel, instructionLabel;
    private JButton rotateBtn;
    private JTextArea logArea;

    // Victory widgets
    private JLabel winnerLabel;

    public GameWindow() {
        super("Mirror Duel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        buildMenuPanel();
        buildRulesPanel();
        buildGamePanel();
        buildVictoryPanel();

        root.add(menuPanel,   "MENU");
        root.add(rulesPanel,  "RULES");
        root.add(gamePanel,   "GAME");
        root.add(victoryPanel,"VICTORY");

        add(root);
        cards.show(root, "MENU");
        pack();
        setLocationRelativeTo(null);
    }

    // ──────────────────────────────────────────────────────────────
    //  MAIN MENU
    // ──────────────────────────────────────────────────────────────
    private void buildMenuPanel() {
        menuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 14, 30), 0, getHeight(), new Color(25, 35, 65));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        menuPanel.setLayout(new GridBagLayout());
        menuPanel.setPreferredSize(new Dimension(700, 560));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 20, 12, 20);
        gbc.gridx = 0;

        // Title
        JLabel title = new JLabel("MIRROR DUEL");
        title.setFont(new Font("Serif", Font.BOLD, 54));
        title.setForeground(new Color(220, 180, 80));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        gbc.gridy = 0;
        menuPanel.add(title, gbc);

        JLabel subtitle = new JLabel("Laser Strategy for Two Players");
        subtitle.setFont(new Font("Serif", Font.ITALIC, 18));
        subtitle.setForeground(new Color(150, 170, 210));
        gbc.gridy = 1;
        menuPanel.add(subtitle, gbc);

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 100, 150));
        sep.setPreferredSize(new Dimension(320, 2));
        gbc.gridy = 2;
        menuPanel.add(sep, gbc);

        // Buttons
        JButton playBtn = styledButton("⚔  Play Game", new Color(60, 180, 80), new Color(40, 140, 60));
        playBtn.addActionListener(e -> {
            gm.startNewGame();
            refreshSidebar();
            cards.show(root, "GAME");
            pack();
        });
        gbc.gridy = 3;
        menuPanel.add(playBtn, gbc);

        JButton rulesBtn = styledButton("📖  How to Play", new Color(60, 120, 200), new Color(40, 90, 160));
        rulesBtn.addActionListener(e -> cards.show(root, "RULES"));
        gbc.gridy = 4;
        menuPanel.add(rulesBtn, gbc);

        JButton quitBtn = styledButton("✕  Quit", new Color(160, 60, 60), new Color(120, 40, 40));
        quitBtn.addActionListener(e -> System.exit(0));
        gbc.gridy = 5;
        menuPanel.add(quitBtn, gbc);

        // Laser art decoration
        JLabel deco = new JLabel("— ⟩⟩ ◈ ⟩⟩ —");
        deco.setFont(new Font("Monospaced", Font.PLAIN, 22));
        deco.setForeground(new Color(80, 200, 240, 180));
        gbc.gridy = 6;
        menuPanel.add(deco, gbc);
    }

    // ──────────────────────────────────────────────────────────────
    //  RULES SCREEN
    // ──────────────────────────────────────────────────────────────
    private void buildRulesPanel() {
        rulesPanel = new JPanel(new BorderLayout(10, 10));
        rulesPanel.setBackground(new Color(12, 18, 35));
        rulesPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        rulesPanel.setPreferredSize(new Dimension(700, 560));

        JLabel title = new JLabel("HOW TO PLAY  —  Mirror Duel");
        title.setFont(new Font("Serif", Font.BOLD, 28));
        title.setForeground(new Color(220, 180, 80));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        rulesPanel.add(title, BorderLayout.NORTH);

        String rules =
            "OBJECTIVE\n" +
            "  Direct your laser beam to hit the opponent's Core Crystal.\n\n" +
            "PIECES (each player has):\n" +
            "  ◉  Laser Emitter  — fires a beam across the board\n" +
            "  ◈  Core Crystal   — protect this at all costs!\n" +
            "  /  Slash Mirror   — reflects beams along the / diagonal\n" +
            "  \\  Backslash Mirror — reflects beams along the \\ diagonal\n\n" +
            "TURN ACTIONS (pick exactly ONE per turn):\n" +
            "  1. Rotate a mirror  (/ ↔ \\)\n" +
            "  2. Move a mirror or emitter one tile orthogonally\n\n" +
            "LASER REFLECTION RULES:\n" +
            "  Slash  /  :  Left→Up  |  Right→Down  |  Up→Right  |  Down→Left\n" +
            "  Back   \\  :  Left→Down | Right→Up  |  Up→Left  |  Down→Right\n\n" +
            "HOW TO PLAY:\n" +
            "  • Click a piece to select it (yellow highlight).\n" +
            "  • Green squares show valid move destinations.\n" +
            "  • Click a green square to move, or press ROTATE to rotate a mirror.\n" +
            "  • The laser fires automatically after every move.\n" +
            "  • The beam path is always visible — no hidden information.\n\n" +
            "WIN CONDITION:\n" +
            "  Your laser beam reaches the enemy's Core Crystal — instant win!\n\n" +
            "STRATEGY TIPS:\n" +
            "  • Build multi-mirror beam chains to attack from unexpected angles.\n" +
            "  • Move mirrors to block incoming beams.\n" +
            "  • Create 'forced response' situations where the opponent\n" +
            "    must defend but leaves another angle open.";

        JTextArea ta = new JTextArea(rules);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setBackground(new Color(18, 26, 50));
        ta.setForeground(new Color(200, 215, 240));
        ta.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 130), 1));
        rulesPanel.add(sp, BorderLayout.CENTER);

        JButton back = styledButton("← Back to Menu", new Color(80, 80, 140), new Color(55, 55, 110));
        back.addActionListener(e -> cards.show(root, "MENU"));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        btnPanel.add(back);
        rulesPanel.add(btnPanel, BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────────────
    //  GAME SCREEN
    // ──────────────────────────────────────────────────────────────
    private void buildGamePanel() {
        gamePanel = new JPanel(new BorderLayout(0, 0));
        gamePanel.setBackground(new Color(10, 14, 28));

        // Board
        boardPanel = new BoardPanel(gm);
        boardPanel.addPropertyChangeListener("gameStateChanged", evt -> {
            refreshSidebar();
            if (gm.getScreen() == GameScreen.VICTORY) {
                showVictory();
            }
        });
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        // Sidebar
        JPanel sidebar = buildSidebar();
        gamePanel.add(sidebar, BorderLayout.EAST);

        // Row labels (numbers) on left
        JPanel rowLabels = buildRowLabels();
        gamePanel.add(rowLabels, BorderLayout.WEST);

        // Col labels (letters) on bottom
        JPanel colLabels = buildColLabels();
        gamePanel.add(colLabels, BorderLayout.SOUTH);
    }

    private JPanel buildSidebar() {
        JPanel sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBackground(new Color(14, 20, 40));
        sb.setPreferredSize(new Dimension(200, 0));
        sb.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(40, 60, 110)));

        sb.add(Box.createVerticalStrut(18));

        // Title
        JLabel t = new JLabel("MIRROR DUEL");
        t.setFont(new Font("Serif", Font.BOLD, 18));
        t.setForeground(new Color(220, 180, 80));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        sb.add(t);

        sb.add(Box.createVerticalStrut(14));
        sb.add(hsep());

        // Turn info
        turnLabel = new JLabel("Turn 1");
        turnLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        turnLabel.setForeground(new Color(180, 200, 230));
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sb.add(Box.createVerticalStrut(10));
        sb.add(turnLabel);

        playerLabel = new JLabel("RED's Turn");
        playerLabel.setFont(new Font("Serif", Font.BOLD, 20));
        playerLabel.setForeground(new Color(240, 90, 90));
        playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sb.add(Box.createVerticalStrut(4));
        sb.add(playerLabel);

        sb.add(Box.createVerticalStrut(10));
        sb.add(hsep());

        // Instruction
        instructionLabel = new JLabel("<html><center>Click a piece<br>to select it</center></html>");
        instructionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instructionLabel.setForeground(new Color(160, 180, 220));
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sb.add(Box.createVerticalStrut(10));
        sb.add(instructionLabel);

        sb.add(Box.createVerticalStrut(12));

        // Rotate button
        rotateBtn = styledButton("↺ Rotate Mirror", new Color(160, 100, 40), new Color(120, 75, 25));
        rotateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rotateBtn.setMaximumSize(new Dimension(175, 40));
        rotateBtn.setEnabled(false);
        rotateBtn.addActionListener(e -> {
            boolean changed = gm.handleRotate();
            if (changed) {
                refreshSidebar();
                boardPanel.repaint();
                if (gm.getScreen() == GameScreen.VICTORY) showVictory();
            }
        });
        sb.add(rotateBtn);

        sb.add(Box.createVerticalStrut(12));
        sb.add(hsep());

        // Move log
        JLabel logTitle = new JLabel("Move Log");
        logTitle.setFont(new Font("Monospaced", Font.BOLD, 12));
        logTitle.setForeground(new Color(140, 160, 200));
        logTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sb.add(Box.createVerticalStrut(8));
        sb.add(logTitle);

        logArea = new JTextArea(10, 14);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(new Color(10, 14, 28));
        logArea.setForeground(new Color(130, 160, 200));
        logArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 100)));
        logScroll.setMaximumSize(new Dimension(185, 140));
        sb.add(Box.createVerticalStrut(4));
        sb.add(logScroll);

        sb.add(Box.createVerticalStrut(10));
        sb.add(hsep());

        // Menu button
        JButton menuBtn = styledButton("☰ Main Menu", new Color(60, 70, 110), new Color(40, 50, 85));
        menuBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuBtn.setMaximumSize(new Dimension(175, 38));
        menuBtn.addActionListener(e -> {
            gm.setScreen(GameScreen.MAIN_MENU);
            cards.show(root, "MENU");
            pack();
        });
        sb.add(Box.createVerticalStrut(6));
        sb.add(menuBtn);

        JButton restartBtn = styledButton("↺ Restart", new Color(50, 110, 90), new Color(35, 80, 65));
        restartBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        restartBtn.setMaximumSize(new Dimension(175, 38));
        restartBtn.addActionListener(e -> {
            gm.startNewGame();
            refreshSidebar();
            boardPanel.repaint();
        });
        sb.add(Box.createVerticalStrut(6));
        sb.add(restartBtn);

        sb.add(Box.createVerticalGlue());
        return sb;
    }

    private JPanel buildRowLabels() {
        JPanel p = new JPanel(new GridLayout(8, 1));
        p.setBackground(new Color(10, 14, 28));
        p.setPreferredSize(new Dimension(24, Board.SIZE * 72));
        for (int r = 0; r < Board.SIZE; r++) {
            JLabel lbl = new JLabel(String.valueOf(8 - r), SwingConstants.CENTER);
            lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lbl.setForeground(new Color(100, 130, 180));
            p.add(lbl);
        }
        return p;
    }

    private JPanel buildColLabels() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(10, 14, 28));

        JPanel inner = new JPanel(new GridLayout(1, 8));
        inner.setBackground(new Color(10, 14, 28));
        String[] letters = {"A","B","C","D","E","F","G","H"};
        for (String l : letters) {
            JLabel lbl = new JLabel(l, SwingConstants.CENTER);
            lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lbl.setForeground(new Color(100, 130, 180));
            lbl.setPreferredSize(new Dimension(72, 22));
            inner.add(lbl);
        }
        // offset for row label width
        p.add(Box.createHorizontalStrut(24), BorderLayout.WEST);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // ──────────────────────────────────────────────────────────────
    //  VICTORY SCREEN
    // ──────────────────────────────────────────────────────────────
    private void buildVictoryPanel() {
        victoryPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(5, 8, 20), 0, getHeight(), new Color(20, 30, 60));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        victoryPanel.setLayout(new GridBagLayout());
        victoryPanel.setPreferredSize(new Dimension(700, 560));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(14, 20, 14, 20);
        gbc.gridx = 0;

        JLabel vcrown = new JLabel("♛");
        vcrown.setFont(new Font("Serif", Font.PLAIN, 80));
        vcrown.setForeground(new Color(220, 180, 80));
        gbc.gridy = 0;
        victoryPanel.add(vcrown, gbc);

        winnerLabel = new JLabel("RED WINS!");
        winnerLabel.setFont(new Font("Serif", Font.BOLD, 52));
        winnerLabel.setForeground(new Color(240, 90, 90));
        gbc.gridy = 1;
        victoryPanel.add(winnerLabel, gbc);

        JLabel sub = new JLabel("The laser found its mark.");
        sub.setFont(new Font("Serif", Font.ITALIC, 20));
        sub.setForeground(new Color(160, 180, 220));
        gbc.gridy = 2;
        victoryPanel.add(sub, gbc);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 100, 150));
        sep.setPreferredSize(new Dimension(320, 2));
        gbc.gridy = 3;
        victoryPanel.add(sep, gbc);

        JButton playAgain = styledButton("⚔  Play Again", new Color(60, 180, 80), new Color(40, 140, 60));
        playAgain.addActionListener(e -> {
            gm.startNewGame();
            refreshSidebar();
            boardPanel.repaint();
            cards.show(root, "GAME");
            pack();
        });
        gbc.gridy = 4;
        victoryPanel.add(playAgain, gbc);

        JButton menuBtn = styledButton("☰  Main Menu", new Color(60, 80, 150), new Color(40, 60, 120));
        menuBtn.addActionListener(e -> cards.show(root, "MENU"));
        gbc.gridy = 5;
        victoryPanel.add(menuBtn, gbc);
    }

    private void showVictory() {
        PlayerID w = gm.getWinner();
        if (w == null) return;
        if (w == PlayerID.RED) {
            winnerLabel.setText("RED WINS!");
            winnerLabel.setForeground(new Color(240, 90, 90));
        } else {
            winnerLabel.setText("BLUE WINS!");
            winnerLabel.setForeground(new Color(80, 160, 255));
        }
        cards.show(root, "VICTORY");
        pack();
    }

    // ──────────────────────────────────────────────────────────────
    //  Sidebar refresh
    // ──────────────────────────────────────────────────────────────
    public void refreshSidebar() {
        if (gm.getBoard() == null) return;
        PlayerID cp = gm.getCurrentPlayer();

        turnLabel.setText("Turn " + gm.getTurnNumber());

        if (cp == PlayerID.RED) {
            playerLabel.setText("● RED's Turn");
            playerLabel.setForeground(new Color(240, 90, 90));
        } else {
            playerLabel.setText("● BLUE's Turn");
            playerLabel.setForeground(new Color(80, 160, 255));
        }

        Piece sel = gm.getSelectedPiece();
        if (sel == null) {
            instructionLabel.setText("<html><center>Click a piece<br>to select it</center></html>");
            rotateBtn.setEnabled(false);
        } else if (sel.isMirror()) {
            String sym = sel.type == PieceType.MIRROR_SLASH ? "/" : "\\";
            instructionLabel.setText("<html><center>Selected mirror " + sym +
                    "<br>Click green square to move<br>or press Rotate</center></html>");
            rotateBtn.setEnabled(sel.owner == cp);
        } else {
            instructionLabel.setText("<html><center>Selected " + sel.type +
                    "<br>Click green square<br>to move it</center></html>");
            rotateBtn.setEnabled(false);
        }

        // Update log
        StringBuilder sb = new StringBuilder();
        for (int i = gm.getMoveLog().size() - 1; i >= 0; i--)
            sb.append(gm.getMoveLog().get(i)).append("\n");
        logArea.setText(sb.toString());

        boardPanel.repaint();
    }

    // ──────────────────────────────────────────────────────────────
    //  UI helpers
    // ──────────────────────────────────────────────────────────────
    private JButton styledButton(String text, Color bg, Color bgHover) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? bgHover : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(200, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JSeparator hsep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(50, 70, 120));
        s.setMaximumSize(new Dimension(185, 2));
        return s;
    }
}
