package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import db.DBConnection;

public class CalendarTrackerFrame extends JFrame {

    DashboardFrame dashboard;
    int studentId;
    YearMonth currentMonth;
    JLabel monthLabel;
    JLabel streakLabel;
    JPanel calendarGrid;
    JPanel detailsPanel;
    Map<Integer, DayStats> monthStats;

    public CalendarTrackerFrame(DashboardFrame dashboard, int studentId) {

        this.dashboard = dashboard;
        this.studentId = studentId;
        this.currentMonth = YearMonth.now();
        this.monthStats = new HashMap<>();

        setTitle("Monthly Tracker");
        setSize(900, 620);
        setMinimumSize(new Dimension(720, 520));
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Monthly Productivity Tracker", "Click any day to review how productive that date was."), BorderLayout.NORTH);

        JSplitPane content = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        content.setResizeWeight(0.68);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        content.setBackground(UITheme.BACKGROUND);
        content.setContinuousLayout(true);
        content.setDividerSize(8);
        add(content, BorderLayout.CENTER);

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setBackground(UITheme.BACKGROUND);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setBackground(UITheme.BACKGROUND);
        rightWrap.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));

        content.setLeftComponent(leftWrap);
        content.setRightComponent(rightWrap);

        JPanel calendarCard = UITheme.createCard();
        calendarCard.setLayout(new BorderLayout(0, 14));
        leftWrap.add(calendarCard, BorderLayout.CENTER);

        JPanel monthHeader = new JPanel(new BorderLayout(0, 8));
        monthHeader.setOpaque(false);

        JPanel monthNav = new JPanel(new GridBagLayout());
        monthNav.setOpaque(false);

        JButton prevBtn = UITheme.dateNavButton("< Previous", "previous", 124);
        JButton nextBtn = UITheme.dateNavButton("Next >", "next", 124);
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 20));
        monthLabel.setForeground(UITheme.TEXT);
        streakLabel = new JLabel("", SwingConstants.CENTER);
        streakLabel.setFont(new Font("Arial", Font.BOLD, 13));
        streakLabel.setForeground(UITheme.ACCENT);

        GridBagConstraints navGbc = new GridBagConstraints();
        navGbc.gridy = 0;
        navGbc.insets = new Insets(0, 0, 0, 10);
        navGbc.fill = GridBagConstraints.VERTICAL;
        monthNav.add(prevBtn, navGbc);

        navGbc.gridx = 1;
        navGbc.weightx = 1;
        navGbc.fill = GridBagConstraints.BOTH;
        monthNav.add(monthLabel, navGbc);

        navGbc.gridx = 2;
        navGbc.weightx = 0;
        navGbc.insets = new Insets(0, 0, 0, 0);
        navGbc.fill = GridBagConstraints.VERTICAL;
        monthNav.add(nextBtn, navGbc);
        monthHeader.add(monthNav, BorderLayout.NORTH);
        monthHeader.add(streakLabel, BorderLayout.SOUTH);
        calendarCard.add(monthHeader, BorderLayout.NORTH);

        calendarGrid = new JPanel(new GridLayout(0, 7, 8, 8));
        calendarGrid.setOpaque(false);
        calendarCard.add(calendarGrid, BorderLayout.CENTER);

        detailsPanel = UITheme.createCard();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setMinimumSize(new Dimension(240, 220));
        rightWrap.add(detailsPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        bottomPanel.setBackground(UITheme.BACKGROUND);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        JButton backBtn = actionButton(UITheme.secondaryButton("Back"));
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadMonth();
        });

        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadMonth();
        });

        backBtn.addActionListener(e -> {
            dashboard.setVisible(true);
            this.dispose();
        });

        loadMonth();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadMonth() {
        monthStats = fetchMonthStats();
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        streakLabel.setText(getStreakSummary());
        buildCalendar();
        showDetails(LocalDate.now().getMonth().equals(currentMonth.getMonth()) && LocalDate.now().getYear() == currentMonth.getYear()
                ? LocalDate.now()
                : currentMonth.atDay(1));
    }

    private Map<Integer, DayStats> fetchMonthStats() {
        Map<Integer, DayStats> stats = new HashMap<>();

        try {
            Connection con = DBConnection.getConnection();

            String query =
                    "SELECT EXTRACT(DAY FROM dl.log_date) AS day_no, " +
                    "NVL(SUM(CASE WHEN c.category_name='Productive' THEN dl.hours_spent ELSE 0 END),0) AS productive, " +
                    "NVL(SUM(CASE WHEN c.category_name='Unproductive' THEN dl.hours_spent ELSE 0 END),0) AS unproductive " +
                    "FROM daily_log dl " +
                    "JOIN activity a ON dl.activity_id = a.activity_id " +
                    "JOIN category c ON a.category_id = c.category_id " +
                    "WHERE dl.student_id=? AND EXTRACT(MONTH FROM dl.log_date)=? AND EXTRACT(YEAR FROM dl.log_date)=? " +
                    "GROUP BY EXTRACT(DAY FROM dl.log_date)";

            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, studentId);
            pst.setInt(2, currentMonth.getMonthValue());
            pst.setInt(3, currentMonth.getYear());
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int day = rs.getInt("day_no");
                double productive = rs.getDouble("productive");
                double unproductive = rs.getDouble("unproductive");
                stats.put(day, new DayStats(productive, unproductive));
            }

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(this, "Tracker Failed", "Unable to load monthly tracker data.");
        }

        return stats;
    }

    private void buildCalendar() {
        calendarGrid.removeAll();

        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : weekdays) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setForeground(UITheme.MUTED);
            calendarGrid.add(label);
        }

        int firstDayOffset = currentMonth.atDay(1).getDayOfWeek().getValue() % 7;
        for (int i = 0; i < firstDayOffset; i++) {
            calendarGrid.add(createEmptyCell());
        }

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            calendarGrid.add(createDayButton(date, monthStats.get(day)));
        }

        int usedCells = 7 + firstDayOffset + currentMonth.lengthOfMonth();
        int trailingCells = (7 - (usedCells % 7)) % 7;
        for (int i = 0; i < trailingCells; i++) {
            calendarGrid.add(createEmptyCell());
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private JPanel createEmptyCell() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private JButton createDayButton(LocalDate date, DayStats stats) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        button.setBackground(getDayColor(stats));

        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.LEFT);
        dayLabel.setFont(new Font("Arial", Font.BOLD, 15));
        dayLabel.setForeground(UITheme.TEXT);

        JLabel scoreLabel = new JLabel(getDayText(stats), SwingConstants.RIGHT);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        scoreLabel.setForeground(isProductiveDay(stats) ? UITheme.SUCCESS : UITheme.MUTED);

        button.add(dayLabel, BorderLayout.NORTH);
        button.add(scoreLabel, BorderLayout.SOUTH);
        button.addActionListener(e -> showDetails(date));

        return button;
    }

    private Color getDayColor(DayStats stats) {
        if (stats == null || stats.total() == 0) {
            return new Color(248, 250, 252);
        }

        double score = stats.score();
        if (score >= 70) {
            return new Color(219, 242, 222);
        }
        if (score >= 45) {
            return new Color(255, 238, 205);
        }
        return new Color(255, 224, 224);
    }

    private String getDayText(DayStats stats) {
        if (stats == null || stats.total() == 0) {
            return "No log";
        }

        return String.format("%.0f%%", stats.score());
    }

    private void showDetails(LocalDate date) {
        detailsPanel.removeAll();

        DayStats stats = monthStats.get(date.getDayOfMonth());
        if (stats == null) {
            stats = new DayStats(0, 0);
        }

        JLabel title = new JLabel(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel status = new JLabel(getStatusText(stats));
        status.setFont(new Font("Arial", Font.BOLD, 14));
        status.setForeground(getStatusColor(stats));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(stats.score()));
        bar.setStringPainted(true);
        bar.setString(String.format("%.1f%% productive", stats.score()));
        bar.setForeground(getStatusColor(stats));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea summary = new JTextArea(
                "Productive: " + stats.productive + " hrs\n" +
                "Unproductive: " + stats.unproductive + " hrs\n" +
                "Total tracked: " + stats.total() + " hrs\n\n" +
                "Productive streak through this day: " + getStreakEndingAt(date.getDayOfMonth()) + " day(s)\n\n" +
                getAdvice(stats)
        );
        summary.setEditable(false);
        summary.setOpaque(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setFont(new Font("Arial", Font.PLAIN, 13));
        summary.setForeground(UITheme.MUTED);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsPanel.add(title);
        detailsPanel.add(Box.createVerticalStrut(8));
        detailsPanel.add(status);
        detailsPanel.add(Box.createVerticalStrut(14));
        detailsPanel.add(bar);
        detailsPanel.add(Box.createVerticalStrut(14));
        detailsPanel.add(summary);

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private String getStatusText(DayStats stats) {
        if (stats.total() == 0) {
            return "No activity logged";
        }
        if (stats.score() >= 70) {
            return "Strong productive day";
        }
        if (stats.score() >= 45) {
            return "Balanced day";
        }
        return "Needs recovery";
    }

    private Color getStatusColor(DayStats stats) {
        if (stats.total() == 0) {
            return UITheme.MUTED;
        }
        if (stats.score() >= 70) {
            return UITheme.SUCCESS;
        }
        if (stats.score() >= 45) {
            return new Color(245, 124, 0);
        }
        return UITheme.DANGER;
    }

    private String getAdvice(DayStats stats) {
        if (stats.total() == 0) {
            return "Add a daily log for this date to see productivity feedback.";
        }
        if (stats.score() >= 70) {
            return "Great balance. Try to repeat the same schedule pattern.";
        }
        if (stats.score() >= 45) {
            return "Good base. Move one low-value hour into study, exercise, or sleep.";
        }
        return "Reduce distractions on the next day and protect one focused study block first.";
    }

    private String getStreakSummary() {
        int best = getBestStreak();
        int active = currentMonth.equals(YearMonth.now())
                ? getStreakEndingAt(LocalDate.now().getDayOfMonth())
                : getBestStreak();

        if (best == 0) {
            return "No productive streak yet this month. A day with 70%+ productivity starts one.";
        }

        return "Current streak: " + active + " day(s)   |   Best this month: " + best + " day(s)";
    }

    private int getBestStreak() {
        int best = 0;
        int current = 0;

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            if (isProductiveDay(monthStats.get(day))) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }

        return best;
    }

    private int getStreakEndingAt(int endDay) {
        int streak = 0;

        for (int day = Math.min(endDay, currentMonth.lengthOfMonth()); day >= 1; day--) {
            if (isProductiveDay(monthStats.get(day))) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    private boolean isProductiveDay(DayStats stats) {
        return stats != null && stats.total() > 0 && stats.score() >= 70;
    }

    private JButton navButton(JButton button, int width) {
        Dimension size = new Dimension(width, 48);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setMargin(new Insets(8, 10, 8, 10));
        return button;
    }

    private JButton actionButton(JButton button) {
        Dimension size = new Dimension(112, 38);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setMargin(new Insets(8, 10, 8, 10));
        return button;
    }

    private static class DayStats {
        double productive;
        double unproductive;

        DayStats(double productive, double unproductive) {
            this.productive = productive;
            this.unproductive = unproductive;
        }

        double total() {
            return productive + unproductive;
        }

        double score() {
            return total() == 0 ? 0 : (productive / total()) * 100;
        }
    }
}
