package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DashboardFrame extends JFrame {

    int studentId;
    JPanel grid;

    public DashboardFrame(int studentId) {

        this.studentId = studentId;

        setTitle("Dashboard");
        setSize(900, 600);
        setMinimumSize(new Dimension(520, 460));
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Productivity Dashboard", "Choose a workspace and keep every hour intentional."), BorderLayout.NORTH);

        grid = new JPanel(new GridLayout(0, 3, 18, 18));
        grid.setBackground(UITheme.BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(26, 30, 18, 30));

        grid.add(createDashboardCard(
                "Schedule Planner",
                "Plan your day before it starts.",
                "Open Schedule",
                () -> {
                    new ScheduleManagerFrame(this, studentId);
                    this.setVisible(false);
                }
        ));

        grid.add(createDashboardCard(
                "Daily Log",
                "Record what actually happened.",
                "Open Log",
                () -> {
                    new DailyLogManagerFrame(this, studentId);
                    this.setVisible(false);
                }
        ));

        grid.add(createDashboardCard(
                "Productivity Report",
                "Check your productive balance.",
                "View Report",
                () -> {
                    new ReportFrame(this, studentId);
                    this.setVisible(false);
                }
        ));

        grid.add(createDashboardCard(
                "Recovery Plan",
                "Get suggestions for improvement.",
                "View Plan",
                () -> {
                    new RecoveryFrame(this, studentId);
                    this.setVisible(false);
                }
        ));

        grid.add(createDashboardCard(
                "Monthly Tracker",
                "View each day's productivity score.",
                "Open Tracker",
                () -> {
                    new CalendarTrackerFrame(this, studentId);
                    this.setVisible(false);
                }
        ));

        add(grid, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        bottomPanel.setBackground(UITheme.BACKGROUND);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        JButton logoutBtn = UITheme.secondaryButton("Logout");
        logoutBtn.setPreferredSize(new Dimension(112, 38));
        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        logoutBtn.addActionListener(e -> {
            new LoginFrame();
            this.dispose();
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateDashboardColumns();
            }
        });
        updateDashboardColumns();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void updateDashboardColumns() {
        int width = getContentPane().getWidth();
        int columns;

        if (width >= 820) {
            columns = 3;
        } else if (width >= 620) {
            columns = 2;
        } else {
            columns = 1;
        }

        GridLayout layout = (GridLayout) grid.getLayout();
        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
            grid.revalidate();
            grid.repaint();
        }
    }

    private JPanel createDashboardCard(String title, String detail, String actionText, Runnable action) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, 14));
        card.setBackground(new Color(252, 254, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 218, 229)),
                BorderFactory.createEmptyBorder(0, 0, 16, 0)
        ));

        JLabel colorBand = new JLabel();
        colorBand.setOpaque(true);
        colorBand.setBackground(getCardAccent(title));
        colorBand.setPreferredSize(new Dimension(100, 8));
        card.add(colorBand, BorderLayout.NORTH);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 19));
        titleLabel.setForeground(UITheme.TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea detailLabel = new JTextArea(detail);
        detailLabel.setEditable(false);
        detailLabel.setOpaque(false);
        detailLabel.setLineWrap(true);
        detailLabel.setWrapStyleWord(true);
        detailLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        detailLabel.setForeground(UITheme.MUTED);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(detailLabel);

        JButton button = UITheme.primaryButton(actionText);
        button.setPreferredSize(new Dimension(100, 38));
        button.addActionListener(e -> action.run());

        JPanel buttonWrap = new JPanel(new BorderLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        buttonWrap.add(button, BorderLayout.CENTER);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(buttonWrap, BorderLayout.SOUTH);

        return card;
    }

    private Color getCardAccent(String title) {
        if (title.contains("Schedule")) {
            return UITheme.ACCENT;
        }
        if (title.contains("Log")) {
            return UITheme.SUCCESS;
        }
        if (title.contains("Report")) {
            return UITheme.PRIMARY;
        }
        if (title.contains("Recovery")) {
            return UITheme.WARNING;
        }
        return new Color(113, 86, 180);
    }
}
