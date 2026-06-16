package ui;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class UITheme {

    public static final Color BACKGROUND = new Color(241, 246, 249);
    public static final Color SURFACE = Color.WHITE;
    public static final Color PRIMARY = new Color(24, 45, 81);
    public static final Color ACCENT = new Color(20, 139, 140);
    public static final Color SUCCESS = new Color(36, 140, 82);
    public static final Color DANGER = new Color(211, 69, 76);
    public static final Color WARNING = new Color(238, 150, 48);
    public static final Color TEXT = new Color(29, 36, 49);
    public static final Color MUTED = new Color(93, 104, 121);
    public static final Color BORDER = new Color(214, 224, 232);

    public static JPanel createHeader(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, getWidth(), getHeight(), new Color(22, 128, 137)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(229, 244, 245));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(subtitleLabel, BorderLayout.SOUTH);

        return panel;
    }

    public static JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204, 218, 229)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
        return panel;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setMargin(new Insets(8, 14, 8, 14));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(250, 252, 254));
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(BORDER));
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setMargin(new Insets(8, 14, 8, 14));
        return button;
    }

    public static JButton dateNavButton(String text, String style, int width) {
        JButton button = new JButton(text);
        Color background;
        Color foreground = Color.WHITE;

        if ("previous".equalsIgnoreCase(style)) {
            background = new Color(67, 97, 238);
        } else if ("today".equalsIgnoreCase(style)) {
            background = new Color(20, 139, 140);
        } else {
            background = new Color(123, 92, 194);
        }

        final Color baseColor = background;
        final Color hoverColor = brighten(baseColor, 24);
        final Color pressedColor = darken(baseColor, 24);

        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setMargin(new Insets(8, 10, 8, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Dimension size = new Dimension(width, 48);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.getModel().addChangeListener(e -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(pressedColor);
            } else if (model.isRollover()) {
                button.setBackground(hoverColor);
            } else {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    private static Color brighten(Color color, int amount) {
        return new Color(
                Math.min(255, color.getRed() + amount),
                Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount)
        );
    }

    private static Color darken(Color color, int amount) {
        return new Color(
                Math.max(0, color.getRed() - amount),
                Math.max(0, color.getGreen() - amount),
                Math.max(0, color.getBlue() - amount)
        );
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setGridColor(new Color(232, 236, 241));
        table.setSelectionBackground(new Color(205, 218, 229));
        table.setSelectionForeground(TEXT);

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 13));
    }

    public static void styleField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    public static JScrollPane verticalScrollPane(Component content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    public static void showSuccess(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, createMessagePanel(title, message, SUCCESS), title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, createMessagePanel(title, message, DANGER), title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, createMessagePanel(title, message, ACCENT), title, JOptionPane.PLAIN_MESSAGE);
    }

    public static int showConfirm(Component parent, String title, String message, String yesText, String noText) {
        Object[] options = {yesText, noText};
        return JOptionPane.showOptionDialog(
                parent,
                createMessagePanel(title, message, ACCENT),
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
    }

    private static JPanel createMessagePanel(String title, String message, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(360, 96));

        JLabel marker = new JLabel();
        marker.setOpaque(true);
        marker.setBackground(accentColor);
        marker.setPreferredSize(new Dimension(8, 80));
        panel.add(marker, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(TEXT);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JTextArea messageText = new JTextArea(message);
        messageText.setEditable(false);
        messageText.setOpaque(false);
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setFont(new Font("Arial", Font.PLAIN, 13));
        messageText.setForeground(MUTED);
        messageText.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(messageText, BorderLayout.CENTER);
        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }
}
