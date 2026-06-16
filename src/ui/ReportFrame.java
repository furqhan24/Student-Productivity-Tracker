package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import db.DBConnection;

public class ReportFrame extends JFrame {

    DashboardFrame dashboard;
    int studentId;
    java.sql.Date selectedDate;
    ResponsivePanel content;
    JLabel dateLabel;
    JLabel totalLabel;

    public ReportFrame(DashboardFrame dashboard, int studentId) {

        this.dashboard = dashboard;
        this.studentId = studentId;
        this.selectedDate = new java.sql.Date(System.currentTimeMillis());

        setTitle("Productivity Report");
        setSize(780, 560);
        setMinimumSize(new Dimension(640, 460));
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Productivity Report", "Compare your day-wise logged productivity with what you planned."), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setBackground(UITheme.BACKGROUND);
        center.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 22));
        center.add(createDateNavigator(), BorderLayout.NORTH);

        content = new ResponsivePanel(new BorderLayout(0, 16));
        content.setBackground(UITheme.BACKGROUND);
        center.add(UITheme.verticalScrollPane(content), BorderLayout.CENTER);
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

        loadReport();
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
            loadReport();
        });
        nextBtn.addActionListener(e -> moveSelectedDate(1));

        return datePanel;
    }

    private void loadReport() {
        content.removeAll();
        try {
            Connection con = DBConnection.getConnection();

            double[] totals = loadProductivityTotals(con);
            double productive = totals[0];
            double unproductive = totals[1];
            double total = productive + unproductive;
            double score = (total == 0) ? 0 : (productive / total) * 100;
            double[] allTimeTotals = loadAllTimeProductivityTotals(con);
            double allTimeTotal = allTimeTotals[0] + allTimeTotals[1];
            double allTimeScore = (allTimeTotal == 0) ? 0 : (allTimeTotals[0] / allTimeTotal) * 100;

            updateDateText(total);
            content.add(createSummaryPanel(productive, unproductive, score, allTimeTotals[0], allTimeTotals[1], allTimeScore), BorderLayout.NORTH);

            JPanel detailPanels = new JPanel();
            detailPanels.setOpaque(false);
            detailPanels.setLayout(new BoxLayout(detailPanels, BoxLayout.Y_AXIS));
            detailPanels.add(createAchievementPanel(con));
            detailPanels.add(Box.createVerticalStrut(14));
            detailPanels.add(createSubActivityPanel(con));
            content.add(detailPanels, BorderLayout.CENTER);

        } catch (Exception ex) {
            System.out.println(ex);
            updateDateText(0);
            content.add(messageCard("Unable to load report. Please check your database connection and normalized category table."), BorderLayout.CENTER);
        }

        content.revalidate();
        content.repaint();
    }

    private double[] loadProductivityTotals(Connection con) throws SQLException {
        String query =
                "SELECT c.category_name, SUM(dl.hours_spent) AS total_hours " +
                "FROM daily_log dl " +
                "JOIN activity a ON dl.activity_id = a.activity_id " +
                "JOIN category c ON a.category_id = c.category_id " +
                "WHERE dl.student_id=? AND TRUNC(dl.log_date)=TRUNC(?) " +
                "GROUP BY c.category_name";
        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, studentId);
        pst.setDate(2, selectedDate);
        ResultSet rs = pst.executeQuery();

        double productive = 0;
        double unproductive = 0;

        while (rs.next()) {
            String category = rs.getString("category_name");
            double hours = rs.getDouble("total_hours");

            if ("Productive".equalsIgnoreCase(category)) {
                productive = hours;
            } else if ("Unproductive".equalsIgnoreCase(category)) {
                unproductive = hours;
            }
        }

        return new double[]{productive, unproductive};
    }

    private double[] loadAllTimeProductivityTotals(Connection con) throws SQLException {
        String query =
                "SELECT c.category_name, SUM(dl.hours_spent) AS total_hours " +
                "FROM daily_log dl " +
                "JOIN activity a ON dl.activity_id = a.activity_id " +
                "JOIN category c ON a.category_id = c.category_id " +
                "WHERE dl.student_id=? " +
                "GROUP BY c.category_name";
        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, studentId);
        ResultSet rs = pst.executeQuery();

        double productive = 0;
        double unproductive = 0;

        while (rs.next()) {
            String category = rs.getString("category_name");
            double hours = rs.getDouble("total_hours");

            if ("Productive".equalsIgnoreCase(category)) {
                productive = hours;
            } else if ("Unproductive".equalsIgnoreCase(category)) {
                unproductive = hours;
            }
        }

        return new double[]{productive, unproductive};
    }

    private JPanel createSummaryPanel(double productive, double unproductive, double score, double allTimeProductive, double allTimeUnproductive, double allTimeScore) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(100, 196));
        statsPanel.add(createStatCard("Productive Hours", formatHours(productive), UITheme.SUCCESS));
        statsPanel.add(createStatCard("Unproductive Hours", formatHours(unproductive), UITheme.DANGER));
        statsPanel.add(createStatCard("Day Score", String.format("%.2f", score) + "%", UITheme.ACCENT));
        statsPanel.add(createStatCard("All-Time Score", String.format("%.2f", allTimeScore) + "%", UITheme.PRIMARY));
        panel.add(statsPanel, BorderLayout.NORTH);

        JPanel progressGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        progressGrid.setOpaque(false);
        progressGrid.add(createScoreCard("Day Balance", score, formatHours(productive + unproductive) + " tracked today"));
        progressGrid.add(createScoreCard("All-Time Balance", allTimeScore, formatHours(allTimeProductive + allTimeUnproductive) + " tracked overall"));
        panel.add(progressGrid, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createScoreCard(String title, double score, String subtitle) {
        JPanel progressCard = UITheme.createCard();
        progressCard.setLayout(new BorderLayout(0, 10));

        JLabel progressTitle = new JLabel(title);
        progressTitle.setFont(new Font("Arial", Font.BOLD, 16));
        progressTitle.setForeground(UITheme.TEXT);

        JLabel helperLabel = new JLabel(subtitle);
        helperLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        helperLabel.setForeground(UITheme.MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        textPanel.setOpaque(false);
        textPanel.add(progressTitle);
        textPanel.add(helperLabel);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(score));
        bar.setStringPainted(true);
        bar.setString(String.format("%.2f%% productive", score));
        bar.setForeground(score >= 70 ? UITheme.SUCCESS : score >= 45 ? UITheme.WARNING : UITheme.DANGER);
        bar.setBackground(new Color(225, 232, 238));
        bar.setPreferredSize(new Dimension(100, 22));

        progressCard.add(textPanel, BorderLayout.NORTH);
        progressCard.add(bar, BorderLayout.CENTER);
        return progressCard;
    }

    private JPanel createAchievementPanel(Connection con) throws SQLException {
        JPanel panel = UITheme.createCard();
        panel.setLayout(new BorderLayout(0, 12));

        JLabel title = new JLabel("Planned vs Achieved By Activity");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(UITheme.TEXT);
        panel.add(title, BorderLayout.NORTH);

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
        chartPanel.setOpaque(false);

        String query =
                "SELECT a.activity_name, c.category_name, " +
                "NVL(s.planned_hours, 0) AS planned_hours, " +
                "NVL(l.logged_hours, 0) AS logged_hours " +
                "FROM activity a " +
                "JOIN category c ON a.category_id = c.category_id " +
                "LEFT JOIN (SELECT activity_id, SUM(planned_hours) AS planned_hours FROM schedule WHERE student_id=? AND TRUNC(schedule_date)=TRUNC(?) GROUP BY activity_id) s " +
                "ON a.activity_id = s.activity_id " +
                "LEFT JOIN (SELECT activity_id, SUM(hours_spent) AS logged_hours FROM daily_log WHERE student_id=? AND TRUNC(log_date)=TRUNC(?) GROUP BY activity_id) l " +
                "ON a.activity_id = l.activity_id " +
                "WHERE NVL(s.planned_hours, 0) > 0 OR NVL(l.logged_hours, 0) > 0 " +
                "ORDER BY a.activity_name";

        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, studentId);
        pst.setDate(2, selectedDate);
        pst.setInt(3, studentId);
        pst.setDate(4, selectedDate);
        ResultSet rs = pst.executeQuery();

        boolean hasRows = false;
        while (rs.next()) {
            hasRows = true;
            String activity = rs.getString("activity_name");
            String category = rs.getString("category_name");
            double planned = rs.getDouble("planned_hours");
            double achieved = rs.getDouble("logged_hours");
            chartPanel.add(createAchievementRow(activity, category, planned, achieved));
            chartPanel.add(Box.createVerticalStrut(10));
        }

        if (!hasRows) {
            JLabel emptyLabel = new JLabel("No planned or logged activities for this day.");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            emptyLabel.setForeground(UITheme.MUTED);
            chartPanel.add(emptyLabel);
        }

        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSubActivityPanel(Connection con) throws SQLException {
        JPanel panel = UITheme.createCard();
        panel.setLayout(new BorderLayout(0, 12));

        JLabel title = new JLabel("Time Spent By SubActivity");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(UITheme.TEXT);
        panel.add(title, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        String query =
                "SELECT a.activity_name, NVL(sa.subactivity_name, 'General') AS subactivity_name, SUM(dl.hours_spent) AS total_hours " +
                "FROM daily_log dl " +
                "JOIN activity a ON dl.activity_id = a.activity_id " +
                "LEFT JOIN sub_activity sa ON dl.subactivity_id = sa.subactivity_id " +
                "WHERE dl.student_id=? AND TRUNC(dl.log_date)=TRUNC(?) " +
                "GROUP BY a.activity_name, NVL(sa.subactivity_name, 'General') " +
                "ORDER BY a.activity_name, total_hours DESC";

        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, studentId);
        pst.setDate(2, selectedDate);
        ResultSet rs = pst.executeQuery();

        boolean hasRows = false;
        while (rs.next()) {
            hasRows = true;
            listPanel.add(createSubActivityRow(
                    rs.getString("activity_name"),
                    rs.getString("subactivity_name"),
                    rs.getDouble("total_hours")
            ));
            listPanel.add(Box.createVerticalStrut(8));
        }

        if (!hasRows) {
            JLabel emptyLabel = new JLabel("No subactivity time recorded for this day.");
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            emptyLabel.setForeground(UITheme.MUTED);
            listPanel.add(emptyLabel);
        }

        panel.add(listPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSubActivityRow(String activity, String subActivity, double hours) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(new Color(248, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel nameLabel = new JLabel(activity + "  >  " + subActivity);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(UITheme.TEXT);
        row.add(nameLabel, BorderLayout.CENTER);

        JLabel hoursLabel = new JLabel(formatHours(hours));
        hoursLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        hoursLabel.setPreferredSize(new Dimension(90, 28));
        hoursLabel.setFont(new Font("Arial", Font.BOLD, 15));
        hoursLabel.setForeground(UITheme.ACCENT);
        row.add(hoursLabel, BorderLayout.EAST);

        return row;
    }

    private JPanel createAchievementRow(String activity, String category, double planned, double achieved) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(new Color(248, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JPanel labelPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        labelPanel.setOpaque(false);
        labelPanel.setPreferredSize(new Dimension(170, 46));

        JLabel activityLabel = new JLabel(activity);
        activityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        activityLabel.setForeground(UITheme.TEXT);

        JLabel hoursLabel = new JLabel(getHoursText(planned, achieved));
        hoursLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        hoursLabel.setForeground(UITheme.MUTED);

        labelPanel.add(activityLabel);
        labelPanel.add(hoursLabel);
        row.add(labelPanel, BorderLayout.WEST);

        boolean unplanned = planned == 0 && achieved > 0;
        double rawPercent = unplanned ? 100 : (achieved / planned) * 100;
        int percent = unplanned ? 100 : (int) Math.round(Math.min(rawPercent, 100));
        boolean exceeded = !unplanned && achieved > planned;

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setStringPainted(true);
        bar.setString(getAchievementStatus(category, rawPercent, exceeded, unplanned));
        bar.setForeground(getAchievementColor(category, rawPercent, exceeded, unplanned));
        bar.setBackground(new Color(230, 235, 240));
        bar.setPreferredSize(new Dimension(100, 16));
        row.add(bar, BorderLayout.CENTER);

        JLabel resultLabel = new JLabel(unplanned ? "Unplanned" : String.format("%.1f%%", rawPercent));
        resultLabel.setFont(new Font("Arial", Font.BOLD, 13));
        resultLabel.setForeground(getAchievementColor(category, rawPercent, exceeded, unplanned));
        resultLabel.setPreferredSize(new Dimension(105, 30));
        row.add(resultLabel, BorderLayout.EAST);

        return row;
    }

    private void moveSelectedDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        calendar.add(Calendar.DATE, days);
        selectedDate = new java.sql.Date(calendar.getTimeInMillis());
        loadReport();
    }

    private void updateDateText(double total) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy");
        dateLabel.setText(isToday(selectedDate) ? "Today's Report" : formatter.format(selectedDate));
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

    private String getHoursText(double planned, double achieved) {
        if (planned == 0 && achieved > 0) {
            return "Not planned | Done " + formatHours(achieved);
        }

        return "Planned " + formatHours(planned) + " | Done " + formatHours(achieved);
    }

    private String formatHours(double hours) {
        return hours == Math.floor(hours) ? String.valueOf((int) hours) + " hrs" : String.format("%.1f hrs", hours);
    }

    private String getAchievementStatus(String category, double percent, boolean exceeded, boolean unplanned) {
        if (unplanned && "Unproductive".equalsIgnoreCase(category)) {
            return "Unplanned distraction";
        }

        if (unplanned) {
            return "Unplanned productive work";
        }

        if (exceeded && "Unproductive".equalsIgnoreCase(category)) {
            return "Over limit";
        }

        if (exceeded) {
            return "Exceeded target";
        }

        if (percent >= 80) {
            return "On track";
        }

        if (percent >= 45) {
            return "Partly done";
        }

        return "Needs work";
    }

    private Color getAchievementColor(String category, double percent, boolean exceeded, boolean unplanned) {
        if (unplanned && "Unproductive".equalsIgnoreCase(category)) {
            return UITheme.DANGER;
        }

        if (unplanned) {
            return UITheme.ACCENT;
        }

        if (exceeded && "Unproductive".equalsIgnoreCase(category)) {
            return UITheme.DANGER;
        }

        if (exceeded) {
            return UITheme.SUCCESS;
        }

        if (percent >= 80) {
            return UITheme.SUCCESS;
        }

        if (percent >= 45) {
            return new Color(245, 124, 0);
        }

        return UITheme.DANGER;
    }

    private JPanel createStatCard(String label, String value, Color color) {
        JPanel panel = UITheme.createCard();
        panel.setLayout(new GridLayout(2, 1, 0, 4));

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

    private JPanel messageCard(String message) {
        JPanel panel = UITheme.createCard();
        panel.add(new JLabel(message));
        return panel;
    }
}
