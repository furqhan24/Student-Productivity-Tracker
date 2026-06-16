package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import db.DBConnection;

public class RecoveryFrame extends JFrame {

    DashboardFrame dashboard;
    int studentId;
    java.sql.Date selectedDate;
    ResponsivePanel contentPanel;
    JLabel dateLabel;
    JLabel totalLabel;

    public RecoveryFrame(DashboardFrame dashboard, int studentId) {

        this.dashboard = dashboard;
        this.studentId = studentId;
        this.selectedDate = new java.sql.Date(System.currentTimeMillis());

        setTitle("Recovery Plan");
        setSize(820, 620);
        setMinimumSize(new Dimension(700, 520));
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Personal Recovery Plan", "A day-wise plan based on logged productive and unproductive hours."), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setBackground(UITheme.BACKGROUND);
        center.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 22));
        center.add(createDateNavigator(), BorderLayout.NORTH);

        contentPanel = new ResponsivePanel(new BorderLayout(0, 16));
        contentPanel.setBackground(UITheme.BACKGROUND);
        center.add(UITheme.verticalScrollPane(contentPanel), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JButton backBtn = actionButton(UITheme.secondaryButton("Back"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        bottomPanel.setBackground(UITheme.BACKGROUND);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        backBtn.addActionListener(e -> {
            dashboard.setVisible(true);
            this.dispose();
        });

        loadRecoveryPlan();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createDateNavigator() {
        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setBackground(UITheme.BACKGROUND);

        JButton previousBtn = UITheme.dateNavButton("< Previous", "previous", 124);
        JButton todayBtn = UITheme.dateNavButton("Today", "today", 92);
        JButton nextBtn = UITheme.dateNavButton("Next >", "next", 124);

        JPanel dateTextPanel = UITheme.createCard();
        dateTextPanel.setLayout(new BorderLayout(0, 4));
        dateTextPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 217, 226)),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        dateLabel = new JLabel();
        dateLabel.setFont(new Font("Arial", Font.BOLD, 19));
        dateLabel.setForeground(UITheme.TEXT);
        totalLabel = new JLabel();
        totalLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        totalLabel.setForeground(UITheme.MUTED);
        dateTextPanel.add(dateLabel, BorderLayout.NORTH);
        dateTextPanel.add(totalLabel, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.fill = GridBagConstraints.VERTICAL;
        datePanel.add(previousBtn, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        datePanel.add(dateTextPanel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        datePanel.add(todayBtn, gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        datePanel.add(nextBtn, gbc);

        previousBtn.addActionListener(e -> moveSelectedDate(-1));
        todayBtn.addActionListener(e -> {
            selectedDate = new java.sql.Date(System.currentTimeMillis());
            loadRecoveryPlan();
        });
        nextBtn.addActionListener(e -> moveSelectedDate(1));

        return datePanel;
    }

    private void loadRecoveryPlan() {
        contentPanel.removeAll();
        try {
            Connection con = DBConnection.getConnection();

            String query =
                    "SELECT " +
                    "NVL(SUM(CASE WHEN c.category_name='Productive' THEN dl.hours_spent ELSE 0 END),0) AS productive, " +
                    "NVL(SUM(CASE WHEN c.category_name='Unproductive' THEN dl.hours_spent ELSE 0 END),0) AS unproductive " +
                    "FROM daily_log dl " +
                    "JOIN activity a ON dl.activity_id = a.activity_id " +
                    "JOIN category c ON a.category_id = c.category_id " +
                    "WHERE dl.student_id=? AND TRUNC(dl.log_date)=TRUNC(?)";

            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, studentId);
            pst.setDate(2, selectedDate);
            ResultSet rs = pst.executeQuery();

            double productive = 0;
            double unproductive = 0;

            if (rs.next()) {
                productive = rs.getDouble("productive");
                unproductive = rs.getDouble("unproductive");
            }

            double total = productive + unproductive;
            double score = (total == 0) ? 0 : (productive / total) * 100;
            String[][] topUnproductiveSubActivities = loadTopUnproductiveSubActivities(con);

            updateDateText(total);
            contentPanel.add(createTopPanel(productive, unproductive, score), BorderLayout.NORTH);
            contentPanel.add(createRecommendationsPanel(productive, unproductive, score, topUnproductiveSubActivities), BorderLayout.CENTER);

        } catch (Exception ex) {
            System.out.println(ex);
            updateDateText(0);
            contentPanel.add(createMessageCard("Unable to load recovery plan. Please check your database connection and normalized category table."), BorderLayout.CENTER);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createTopPanel(double productive, double unproductive, double score) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(100, 210));

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(100, 92));
        statsPanel.add(createStatCard("Productive", formatHours(productive), UITheme.SUCCESS));
        statsPanel.add(createStatCard("Unproductive", formatHours(unproductive), UITheme.DANGER));
        statsPanel.add(createStatCard("Score", String.format("%.1f", score) + "%", UITheme.ACCENT));
        panel.add(statsPanel, BorderLayout.NORTH);

        JPanel progressCard = createPlainCard(new BorderLayout(0, 10));
        JLabel progressTitle = new JLabel("Day Productivity Balance");
        progressTitle.setFont(new Font("Arial", Font.BOLD, 16));
        progressTitle.setForeground(UITheme.TEXT);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int) Math.round(score));
        progressBar.setStringPainted(true);
        progressBar.setString(String.format("%.1f%% productive", score));
        progressBar.setForeground(getScoreColor(score));
        progressBar.setBackground(new Color(230, 235, 240));
        progressBar.setPreferredSize(new Dimension(100, 28));

        progressCard.add(progressTitle, BorderLayout.NORTH);
        progressCard.add(progressBar, BorderLayout.CENTER);
        panel.add(progressCard, BorderLayout.CENTER);

        return panel;
    }

    private String[][] loadTopUnproductiveSubActivities(Connection con) throws SQLException {
        String query =
                "SELECT NVL(sa.subactivity_name, a.activity_name) AS subactivity_name, SUM(dl.hours_spent) AS total_hours " +
                "FROM daily_log dl " +
                "JOIN activity a ON dl.activity_id = a.activity_id " +
                "JOIN category c ON a.category_id = c.category_id " +
                "LEFT JOIN sub_activity sa ON dl.subactivity_id = sa.subactivity_id " +
                "WHERE dl.student_id=? AND TRUNC(dl.log_date)=TRUNC(?) AND c.category_name='Unproductive' " +
                "GROUP BY NVL(sa.subactivity_name, a.activity_name) " +
                "ORDER BY total_hours DESC";

        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, studentId);
        pst.setDate(2, selectedDate);
        ResultSet rs = pst.executeQuery();

        java.util.List<String[]> rows = new java.util.ArrayList<>();
        while (rs.next() && rows.size() < 2) {
            double hours = rs.getDouble("total_hours");
            double suggestedReduction = rows.isEmpty() ? Math.min(2, hours) : Math.min(1, hours);
            rows.add(new String[]{rs.getString("subactivity_name"), formatHours(hours), formatHours(suggestedReduction)});
        }

        return rows.toArray(new String[0][0]);
    }

    private JPanel createRecommendationsPanel(double productive, double unproductive, double score, String[][] topUnproductiveSubActivities) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(100, 300));

        JPanel headerCard = createPlainCard(new BorderLayout(12, 0));

        JLabel marker = new JLabel();
        marker.setOpaque(true);
        marker.setBackground(getScoreColor(score));
        marker.setPreferredSize(new Dimension(8, 54));
        headerCard.add(marker, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setOpaque(false);

        JLabel title = new JLabel("Recommended Recovery Actions");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT);

        JTextArea subtitle = new JTextArea(getPriorityMessage(productive, unproductive, score));
        subtitle.setEditable(false);
        subtitle.setOpaque(false);
        subtitle.setLineWrap(true);
        subtitle.setWrapStyleWord(true);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(UITheme.MUTED);

        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.CENTER);
        headerCard.add(textPanel, BorderLayout.CENTER);
        panel.add(headerCard, BorderLayout.NORTH);

        JPanel actionGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        actionGrid.setOpaque(false);
        actionGrid.setPreferredSize(new Dimension(100, 210));

        String[] actions = buildActions(productive, unproductive, score, topUnproductiveSubActivities);
        actionGrid.add(createActionCard("Focus Block", actions[0], UITheme.ACCENT));
        actionGrid.add(createActionCard("Distraction Limit", actions[1], UITheme.DANGER));
        actionGrid.add(createActionCard("Next Day Target", actions[2], UITheme.SUCCESS));
        actionGrid.add(createActionCard("Evening Review", actions[3], new Color(245, 124, 0)));

        panel.add(actionGrid, BorderLayout.CENTER);
        return panel;
    }

    private void moveSelectedDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        calendar.add(Calendar.DATE, days);
        selectedDate = new java.sql.Date(calendar.getTimeInMillis());
        loadRecoveryPlan();
    }

    private void updateDateText(double total) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy");
        dateLabel.setText(isToday(selectedDate) ? "Today's Recovery Plan" : formatter.format(selectedDate));
        totalLabel.setText(formatHours(total) + " tracked for this day");
    }

    private boolean isToday(java.sql.Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date).equals(formatter.format(new java.util.Date()));
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

    private JPanel createPlainCard(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        return panel;
    }

    private JPanel createStatCard(String label, String value, Color color) {
        JPanel panel = createPlainCard(new GridLayout(2, 1, 0, 4));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 22));
        valueLabel.setForeground(color);

        JLabel labelLabel = new JLabel(label);
        labelLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        labelLabel.setForeground(UITheme.MUTED);

        panel.add(valueLabel);
        panel.add(labelLabel);
        return panel;
    }

    private JPanel createActionCard(String title, String text, Color color) {
        JPanel card = createPlainCard(new BorderLayout(0, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        titleLabel.setForeground(color);

        JLabel body = new JLabel("<html><div style='width:220px;'>" + text + "</div></html>");
        body.setFont(new Font("Arial", Font.PLAIN, 13));
        body.setForeground(UITheme.TEXT);
        body.setVerticalAlignment(SwingConstants.TOP);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMessageCard(String message) {
        JPanel panel = createPlainCard(new BorderLayout());
        JLabel label = new JLabel(message);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private String getPriorityMessage(double productive, double unproductive, double score) {
        if (productive + unproductive == 0) {
            return "No logs for this day yet. Start with one focused study block and one honest daily log.";
        }

        if (unproductive > productive) {
            return "Priority: move " + String.format("%.1f", unproductive - productive) + " hour(s) from distraction into useful work or rest on the next day.";
        }

        if (score >= 70) {
            return "This was a productive day. Keep the same rhythm steady without overloading tomorrow.";
        }

        return "This day was close to a strong balance. One extra focused hour would make it noticeably better.";
    }

    private String[] buildActions(double productive, double unproductive, double score, String[][] topUnproductiveSubActivities) {
        if (productive + unproductive == 0) {
            return new String[]{
                    "Log this day's activities first so the recovery plan can become accurate.",
                    "Keep entertainment fixed to a small time block after your main task.",
                    "Plan one study block, one break, and a fixed sleep time for the next day.",
                    "At night, write what helped and what distracted you most."
            };
        }

        if (score < 45) {
            String distractionAdvice = buildSubActivityReductionAdvice(topUnproductiveSubActivities, "Put social media and gaming after the first study block, not before it.");
            return new String[]{
                    "Use 25 minute study sessions with 5 minute breaks. Start with just two rounds.",
                    distractionAdvice,
                    "Aim for at least 5 productive hours on your next active day.",
                    "Find the biggest repeated distraction and remove it for the first hour tomorrow."
            };
        }

        if (score < 70) {
            String distractionAdvice = buildSubActivityReductionAdvice(topUnproductiveSubActivities, "Keep unproductive activities under 2 hours and stop when the limit is reached.");
            return new String[]{
                    "Add one extra focused hour tomorrow in the subject or task that matters most.",
                    distractionAdvice,
                    "Protect sleep and exercise so productivity does not depend only on willpower.",
                    "Review the daily log and replace one low-value activity with a useful one."
            };
        }

        return new String[]{
                "Keep the same start time and repeat the schedule pattern that worked.",
                "Use breaks intentionally so productive time stays fresh instead of forced.",
                "Avoid adding too much extra work. Maintain the balance you already built.",
                "Check your tracker weekly so small slips are corrected early."
        };
    }

    private String buildSubActivityReductionAdvice(String[][] topUnproductiveSubActivities, String fallback) {
        if (topUnproductiveSubActivities.length == 0) {
            return fallback;
        }

        String first = topUnproductiveSubActivities[0][0] + " by " + topUnproductiveSubActivities[0][2] + " (" + topUnproductiveSubActivities[0][1] + " logged)";
        if (topUnproductiveSubActivities.length == 1) {
            return "Reduce " + first + " and allocate that time to Study.";
        }

        String second = topUnproductiveSubActivities[1][0] + " by " + topUnproductiveSubActivities[1][2] + " (" + topUnproductiveSubActivities[1][1] + " logged)";
        return "Reduce " + first + " and " + second + ", then allocate that time to Study.";
    }

    private String formatHours(double hours) {
        return hours == Math.floor(hours) ? String.valueOf((int) hours) + " hrs" : String.format("%.1f hrs", hours);
    }

    private Color getScoreColor(double score) {
        if (score >= 70) {
            return UITheme.SUCCESS;
        }
        if (score >= 45) {
            return new Color(245, 124, 0);
        }
        return UITheme.DANGER;
    }
}
