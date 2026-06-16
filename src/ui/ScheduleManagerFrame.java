package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.sql.*;

import db.DBConnection;

public class ScheduleManagerFrame extends JFrame {

    DashboardFrame dashboard;
    int studentId;
    DefaultTableModel model;
    JPanel listPanel;
    JLabel dateLabel;
    JLabel totalLabel;
    java.sql.Date selectedDate;
    int selectedRow = -1;

    public ScheduleManagerFrame(DashboardFrame dashboard, int studentId) {

        this.dashboard = dashboard;
        this.studentId = studentId;

        setTitle("Schedule Manager");
        setSize(760, 520);
        setMinimumSize(new Dimension(620, 420));
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Schedule Manager", "Plan your day with a strict 24-hour daily limit."), BorderLayout.NORTH);

        selectedDate = new java.sql.Date(System.currentTimeMillis());

        String[] columns = {"ID", "Activity", "SubActivity", "Hours", "Date"};
        model = new DefaultTableModel(columns, 0);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setBackground(UITheme.BACKGROUND);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 10, 22));

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

        listPanel = new JPanel();
        listPanel.setBackground(UITheme.BACKGROUND);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane sp = UITheme.verticalScrollPane(listPanel);
        centerPanel.add(datePanel, BorderLayout.NORTH);
        centerPanel.add(sp, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        bottomPanel.setBackground(UITheme.BACKGROUND);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        JButton addBtn = actionButton(UITheme.primaryButton("Add"));
        JButton updateBtn = actionButton(UITheme.secondaryButton("Update"));
        JButton deleteBtn = actionButton(UITheme.secondaryButton("Delete"));
        JButton refreshBtn = actionButton(UITheme.secondaryButton("Refresh"));
        JButton backBtn = actionButton(UITheme.secondaryButton("Back"));

        bottomPanel.add(addBtn);
        bottomPanel.add(updateBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(refreshBtn);
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> showEntryDialog("Add Schedule", -1, null, null, ""));
        updateBtn.addActionListener(e -> updateSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
        previousBtn.addActionListener(e -> moveSelectedDate(-1));
        todayBtn.addActionListener(e -> {
            selectedDate = new java.sql.Date(System.currentTimeMillis());
            loadData();
        });
        nextBtn.addActionListener(e -> moveSelectedDate(1));
        backBtn.addActionListener(e -> {
            dashboard.setVisible(true);
            this.dispose();
        });

        loadData();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadData() {
        try {
            Connection con = DBConnection.getConnection();

            String query =
                    "SELECT MIN(s.schedule_id) AS schedule_id, a.activity_name, NVL(sa.subactivity_name, 'General') AS subactivity_name, " +
                    "SUM(s.planned_hours) AS planned_hours, TRUNC(s.schedule_date) AS schedule_date " +
                    "FROM schedule s " +
                    "JOIN activity a ON s.activity_id = a.activity_id " +
                    "LEFT JOIN sub_activity sa ON s.subactivity_id = sa.subactivity_id " +
                    "WHERE s.student_id=? AND TRUNC(s.schedule_date)=TRUNC(?) " +
                    "GROUP BY a.activity_name, NVL(sa.subactivity_name, 'General'), TRUNC(s.schedule_date) " +
                    "ORDER BY a.activity_name, subactivity_name";

            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, studentId);
            pst.setDate(2, selectedDate);
            ResultSet rs = pst.executeQuery();

            model.setRowCount(0);
            selectedRow = -1;

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("schedule_id"),
                        rs.getString("activity_name"),
                        rs.getString("subactivity_name"),
                        rs.getDouble("planned_hours"),
                        rs.getDate("schedule_date")
                });
            }

            renderCards();

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(this, "Load Failed", "Unable to load schedule data from the database.");
        }
    }

    private void renderCards() {
        listPanel.removeAll();
        double totalHours = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            totalHours += Double.parseDouble(String.valueOf(model.getValueAt(i, 3)));
        }

        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy");
        dateLabel.setText(isToday(selectedDate) ? "Today's Plan" : formatter.format(selectedDate));
        totalLabel.setText(formatHours(totalHours) + " planned out of 24 hours");

        if (model.getRowCount() == 0) {
            JPanel emptyCard = createDisplayCard("No schedule planned", "Add a plan for this day to start shaping it.", "0h", false);
            listPanel.add(emptyCard);
        } else {
            for (int i = 0; i < model.getRowCount(); i++) {
                JPanel card = createDisplayCard(
                        String.valueOf(model.getValueAt(i, 1)),
                        String.valueOf(model.getValueAt(i, 2)) + " - " + progressText(Double.parseDouble(String.valueOf(model.getValueAt(i, 3)))),
                        formatHours(Double.parseDouble(String.valueOf(model.getValueAt(i, 3)))),
                        i == selectedRow
                );
                final int rowIndex = i;
                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedRow = rowIndex;
                        renderCards();
                    }
                });
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(10));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createDisplayCard(String title, String subtitle, String hours, boolean selected) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(14, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        card.setPreferredSize(new Dimension(420, 92));
        card.setBackground(selected ? new Color(249, 252, 255) : UITheme.SURFACE);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? UITheme.ACCENT : UITheme.BORDER, selected ? 2 : 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel marker = new JLabel();
        marker.setOpaque(true);
        marker.setBackground(selected ? UITheme.ACCENT : new Color(130, 160, 185));
        marker.setPreferredSize(new Dimension(8, 52));
        card.add(marker, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new BorderLayout(0, 5));
        textPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 17));
        titleLabel.setForeground(UITheme.TEXT);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(UITheme.MUTED);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(subtitleLabel, BorderLayout.SOUTH);
        card.add(textPanel, BorderLayout.CENTER);

        JLabel hoursLabel = new JLabel(hours);
        hoursLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        hoursLabel.setPreferredSize(new Dimension(78, 52));
        hoursLabel.setFont(new Font("Arial", Font.BOLD, 22));
        hoursLabel.setForeground(UITheme.ACCENT);
        card.add(hoursLabel, BorderLayout.EAST);
        return card;
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

    private void moveSelectedDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        calendar.add(Calendar.DATE, days);
        selectedDate = new java.sql.Date(calendar.getTimeInMillis());
        loadData();
    }

    private boolean isToday(java.sql.Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date).equals(formatter.format(new java.util.Date()));
    }

    private String formatHours(double hours) {
        return hours == Math.floor(hours) ? String.valueOf((int) hours) + "h" : String.valueOf(hours) + "h";
    }

    private String progressText(double hours) {
        int percent = (int) Math.round((hours / 24.0) * 100);
        return percent + "% of the day planned for this activity";
    }

    private void showEntryDialog(String title, int recordId, String selectedActivity, String selectedSubActivity, String selectedHours) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BACKGROUND);

        JPanel form = UITheme.createCard();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JComboBox<String> activityBox = new JComboBox<>(loadActivityOptions());
        activityBox.setEditable(false);
        activityBox.setPreferredSize(new Dimension(220, 28));

        JComboBox<String> subActivityBox = new JComboBox<>();
        subActivityBox.setEditable(true);
        subActivityBox.setPreferredSize(new Dimension(220, 28));
        reloadSubActivityOptions(subActivityBox, String.valueOf(activityBox.getSelectedItem()), selectedSubActivity);

        if (selectedActivity != null) {
            activityBox.setSelectedItem(selectedActivity);
            activityBox.setEnabled(false);
            reloadSubActivityOptions(subActivityBox, selectedActivity, selectedSubActivity);
            subActivityBox.setEnabled(false);
        }

        
        JTextField hoursField = new JTextField(selectedHours);
        UITheme.styleField(hoursField);

        JButton submitBtn = UITheme.primaryButton(recordId == -1 ? "Add" : "Update");


        form.add(createFormRow("Activity:", activityBox));


        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("SubActivity:", subActivityBox));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Hours:", hoursField));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(submitBtn);
        form.add(Box.createVerticalStrut(14));
        form.add(buttonRow);

        dialog.add(form, BorderLayout.NORTH);

        activityBox.addActionListener(e -> reloadSubActivityOptions(subActivityBox, String.valueOf(activityBox.getSelectedItem()), null));

        submitBtn.addActionListener(ev -> {
            try {
                String selectedActivityValue = String.valueOf(activityBox.getSelectedItem());
                String activity = selectedActivityValue;
                String subActivity = String.valueOf(subActivityBox.getEditor().getItem()).trim();
                double hours = Double.parseDouble(hoursField.getText().trim());

                if (hours <= 0 || hours > 24) {
                    UITheme.showInfo(dialog, "Invalid Hours", "Hours must be between 1 and 24.");
                    return;
                }
                if (subActivity.isEmpty()) {
                    UITheme.showInfo(dialog, "SubActivity Required", "Select or type a subactivity before saving.");
                    return;
                }

                String customCategory = null;

                if (recordId == -1) {
                    addOrMerge(activity, subActivity, customCategory, hours, dialog);
                } else {
                    updateRecord(recordId, hours, dialog);
                }

            } catch (NumberFormatException ex) {
                UITheme.showError(dialog, "Invalid Number", "Enter a valid number.");
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addOrMerge(String activity, String subActivity, String customCategory, double hours, JDialog dialog) {
        try {
            Connection con = DBConnection.getConnection();
            String existingCategory = customCategory != null ? getActivityCategory(con, activity) : null;
            int activityId = getOrCreateActivityId(con, activity, customCategory);
            int subActivityId = getOrCreateSubActivityId(con, activityId, subActivity);

            if (existingCategory != null && customCategory != null && !existingCategory.equalsIgnoreCase(customCategory)) {
                UITheme.showInfo(dialog, "Preserved Category",
                        "The activity \"" + activity + "\" already exists and is marked as " + existingCategory + ". That category is preserved.");
            }

            double scheduledToday = getTotalHoursForDate(con, selectedDate, -1);
            if (scheduledToday + hours > 24) {
                UITheme.showInfo(dialog, "Daily Limit Reached", "This day already has " + scheduledToday + " scheduled hours. Total scheduled hours cannot exceed 24.");
                return;
            }

            String existingQuery =
                    "SELECT MIN(schedule_id) AS schedule_id, NVL(SUM(planned_hours), 0) AS total_hours " +
                    "FROM schedule WHERE student_id=? AND activity_id=? AND subactivity_id=? AND TRUNC(schedule_date)=TRUNC(?)";
            PreparedStatement existingPst = con.prepareStatement(existingQuery);
            existingPst.setInt(1, studentId);
            existingPst.setInt(2, activityId);
            existingPst.setInt(3, subActivityId);
            existingPst.setDate(4, selectedDate);
            ResultSet existingRs = existingPst.executeQuery();

            int existingId = 0;
            double existingHours = 0;
            if (existingRs.next()) {
                existingId = existingRs.getInt("schedule_id");
                existingHours = existingRs.getDouble("total_hours");
            }

            if (existingId > 0) {
                String updateQuery = "UPDATE schedule SET planned_hours=? WHERE schedule_id=? AND student_id=?";
                PreparedStatement updatePst = con.prepareStatement(updateQuery);
                updatePst.setDouble(1, existingHours + hours);
                updatePst.setInt(2, existingId);
                updatePst.setInt(3, studentId);
                updatePst.executeUpdate();

                removeDuplicateRows(con, existingId, activityId, subActivityId, selectedDate);
                UITheme.showSuccess(dialog, "Time Combined", "This subactivity already existed on this day, so its time was combined.");
            } else {
                String insertQuery = "INSERT INTO schedule (schedule_id, student_id, activity_id, schedule_date, planned_hours, subactivity_id) VALUES (schedule_seq.NEXTVAL, ?, ?, ?, ?, ?)";
                PreparedStatement insertPst = con.prepareStatement(insertQuery);
                insertPst.setInt(1, studentId);
                insertPst.setInt(2, activityId);
                insertPst.setDate(3, selectedDate);
                insertPst.setDouble(4, hours);
                insertPst.setInt(5, subActivityId);
                insertPst.executeUpdate();

                UITheme.showSuccess(dialog, "Schedule Added", "The schedule item was added successfully.");
            }

            dialog.dispose();
            loadData();

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(dialog, "Save Failed", "Unable to save the schedule item.");
        }
    }

    private void updateSelected() {
        int row = selectedRow;
        if (row == -1) {
            UITheme.showInfo(this, "No Plan Selected", "Select a schedule card before clicking Update Selected.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String activity = String.valueOf(model.getValueAt(row, 1));
        String subActivity = String.valueOf(model.getValueAt(row, 2));
        String hours = String.valueOf(model.getValueAt(row, 3));
        showEntryDialog("Update Schedule", id, activity, subActivity, hours);
    }

    private void updateRecord(int recordId, double hours, JDialog dialog) {
        try {
            Connection con = DBConnection.getConnection();

            String infoQuery = "SELECT activity_id FROM schedule WHERE schedule_id=? AND student_id=?";
            PreparedStatement infoPst = con.prepareStatement(infoQuery);
            infoPst.setInt(1, recordId);
            infoPst.setInt(2, studentId);
            ResultSet infoRs = infoPst.executeQuery();

            if (!infoRs.next()) {
                UITheme.showError(dialog, "Not Found", "Selected schedule was not found.");
                return;
            }

            int activityId = infoRs.getInt("activity_id");
            Date scheduleDate = getScheduleDate(con, recordId);
            double otherHours = getTotalHoursForDate(con, scheduleDate, recordId);

            if (otherHours + hours > 24) {
                UITheme.showInfo(dialog, "Daily Limit Reached", "Other scheduled items already use " + otherHours + " hours. Total scheduled hours cannot exceed 24.");
                return;
            }

            String updateQuery = "UPDATE schedule SET planned_hours=? WHERE schedule_id=? AND student_id=?";
            PreparedStatement updatePst = con.prepareStatement(updateQuery);
            updatePst.setDouble(1, hours);
            updatePst.setInt(2, recordId);
            updatePst.setInt(3, studentId);
            updatePst.executeUpdate();

            Integer subActivityId = getScheduleSubActivityId(con, recordId);
            removeDuplicateRows(con, recordId, activityId, subActivityId, scheduleDate);

            UITheme.showSuccess(dialog, "Schedule Updated", "The selected schedule time was updated successfully.");
            dialog.dispose();
            loadData();

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(dialog, "Update Failed", "Unable to update the selected schedule item.");
        }
    }

    private void deleteSelected() {
        int row = selectedRow;

        if (row == -1) {
            UITheme.showInfo(this, "No Plan Selected", "Select a schedule card before deleting.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);

        try {
            Connection con = DBConnection.getConnection();

            String infoQuery = "SELECT activity_id, subactivity_id, TRUNC(schedule_date) AS schedule_date FROM schedule WHERE schedule_id=? AND student_id=?";
            PreparedStatement infoPst = con.prepareStatement(infoQuery);
            infoPst.setInt(1, id);
            infoPst.setInt(2, studentId);
            ResultSet infoRs = infoPst.executeQuery();

            if (!infoRs.next()) {
                UITheme.showError(this, "Not Found", "Selected schedule was not found.");
                return;
            }

            String q = "DELETE FROM schedule WHERE student_id=? AND activity_id=? AND (subactivity_id=? OR (subactivity_id IS NULL AND ? IS NULL)) AND TRUNC(schedule_date)=TRUNC(?)";
            PreparedStatement pst = con.prepareStatement(q);
            pst.setInt(1, studentId);
            pst.setInt(2, infoRs.getInt("activity_id"));
            Integer subActivityId = (Integer) infoRs.getObject("subactivity_id");
            setNullableInt(pst, 3, subActivityId);
            setNullableInt(pst, 4, subActivityId);
            pst.setDate(5, infoRs.getDate("schedule_date"));
            pst.executeUpdate();

            UITheme.showSuccess(this, "Schedule Deleted", "The selected schedule entry was deleted.");
            loadData();

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(this, "Delete Failed", "Unable to delete the selected schedule item.");
        }
    }

    private int getActivityId(Connection con, String activity) throws SQLException {
        String q = "SELECT activity_id FROM activity WHERE activity_name=?";
        PreparedStatement pst = con.prepareStatement(q);
        pst.setString(1, activity);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }

        throw new SQLException("Activity not found: " + activity);
    }

    private String getActivityCategory(Connection con, String activity) throws SQLException {
        String q =
                "SELECT c.category_name " +
                "FROM activity a JOIN category c ON a.category_id = c.category_id " +
                "WHERE LOWER(a.activity_name)=LOWER(?)";
        PreparedStatement pst = con.prepareStatement(q);
        pst.setString(1, activity.trim());
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getString("category_name");
        }

        return null;
    }

    private int getOrCreateActivityId(Connection con, String activity, String customCategory) throws SQLException {
        String findQuery = "SELECT activity_id FROM activity WHERE LOWER(activity_name)=LOWER(?)";
        PreparedStatement findPst = con.prepareStatement(findQuery);
        findPst.setString(1, activity);
        ResultSet rs = findPst.executeQuery();

        if (rs.next()) {
            return rs.getInt("activity_id");
        }

        if (customCategory == null) {
            throw new SQLException("Activity not found: " + activity);
        }

        String idQuery = "SELECT NVL(MAX(activity_id), 0) + 1 FROM activity";
        PreparedStatement idPst = con.prepareStatement(idQuery);
        ResultSet idRs = idPst.executeQuery();

        int activityId = 1;
        if (idRs.next()) {
            activityId = idRs.getInt(1);
        }

        int categoryId = getCategoryId(con, customCategory);

        String insertQuery = "INSERT INTO activity (activity_id, activity_name, category_id) VALUES (?, ?, ?)";
        PreparedStatement insertPst = con.prepareStatement(insertQuery);
        insertPst.setInt(1, activityId);
        insertPst.setString(2, activity);
        insertPst.setInt(3, categoryId);
        insertPst.executeUpdate();

        return activityId;
    }

    private int getCategoryId(Connection con, String categoryName) throws SQLException {
        String findQuery = "SELECT category_id FROM category WHERE LOWER(category_name)=LOWER(?)";
        PreparedStatement findPst = con.prepareStatement(findQuery);
        findPst.setString(1, categoryName);
        ResultSet rs = findPst.executeQuery();

        if (rs.next()) {
            return rs.getInt("category_id");
        }

        String idQuery = "SELECT NVL(MAX(category_id), 0) + 1 FROM category";
        PreparedStatement idPst = con.prepareStatement(idQuery);
        ResultSet idRs = idPst.executeQuery();

        int categoryId = 1;
        if (idRs.next()) {
            categoryId = idRs.getInt(1);
        }

        String insertQuery = "INSERT INTO category (category_id, category_name) VALUES (?, ?)";
        PreparedStatement insertPst = con.prepareStatement(insertQuery);
        insertPst.setInt(1, categoryId);
        insertPst.setString(2, categoryName);
        insertPst.executeUpdate();

        return categoryId;
    }

    private boolean isExistingActivityOption(JComboBox<String> activityBox, String value) {
        String normalizedValue = normalizeValue(value);
        for (int i = 0; i < activityBox.getItemCount(); i++) {
            String item = activityBox.getItemAt(i);
            if (!"Other".equalsIgnoreCase(item) && normalizeValue(item).equals(normalizedValue)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String getExistingActivityItem(JComboBox<String> activityBox, String value) {
        String normalizedValue = normalizeValue(value);
        for (int i = 0; i < activityBox.getItemCount(); i++) {
            String item = activityBox.getItemAt(i);
            if (!"Other".equalsIgnoreCase(item) && normalizeValue(item).equals(normalizedValue)) {
                return item;
            }
        }
        return value;
    }

    private String[] loadActivityOptions() {
        try {
            Connection con = DBConnection.getConnection();
            String query = "SELECT activity_name FROM activity ORDER BY activity_name";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            java.util.List<String> activities = new java.util.ArrayList<>();
            while (rs.next()) {
                activities.add(rs.getString("activity_name"));
            }

            return activities.toArray(new String[0]);
        } catch (Exception ex) {
            System.out.println(ex);
            return new String[]{"Study", "Exercise", "Sleep", "Social Media", "Gaming"};
        }
    }

    private void reloadSubActivityOptions(JComboBox<String> subActivityBox, String activity, String selectedSubActivity) {
        subActivityBox.removeAllItems();
        String[] subActivities = loadSubActivityOptions(activity);
        for (String subActivity : subActivities) {
            subActivityBox.addItem(subActivity);
        }

        if (selectedSubActivity != null) {
            subActivityBox.setSelectedItem(selectedSubActivity);
        } else if (subActivityBox.getItemCount() > 0) {
            subActivityBox.setSelectedIndex(0);
        }
    }

    private String[] loadSubActivityOptions(String activity) {
        try {
            Connection con = DBConnection.getConnection();
            int activityId = getActivityId(con, activity);
            String query = "SELECT subactivity_name FROM sub_activity WHERE activity_id=? ORDER BY subactivity_name";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, activityId);
            ResultSet rs = pst.executeQuery();

            java.util.List<String> subActivities = new java.util.ArrayList<>();
            while (rs.next()) {
                subActivities.add(rs.getString("subactivity_name"));
            }

            if (subActivities.isEmpty()) {
                subActivities.add("General");
            }
            return subActivities.toArray(new String[0]);
        } catch (Exception ex) {
            System.out.println(ex);
            return new String[]{"General"};
        }
    }

    private int getOrCreateSubActivityId(Connection con, int activityId, String subActivity) throws SQLException {
        String findQuery = "SELECT subactivity_id FROM sub_activity WHERE activity_id=? AND LOWER(subactivity_name)=LOWER(?)";
        PreparedStatement findPst = con.prepareStatement(findQuery);
        findPst.setInt(1, activityId);
        findPst.setString(2, subActivity.trim());
        ResultSet rs = findPst.executeQuery();

        if (rs.next()) {
            return rs.getInt("subactivity_id");
        }

        String idQuery = "SELECT NVL(MAX(subactivity_id), 0) + 1 FROM sub_activity";
        PreparedStatement idPst = con.prepareStatement(idQuery);
        ResultSet idRs = idPst.executeQuery();

        int subActivityId = 1;
        if (idRs.next()) {
            subActivityId = idRs.getInt(1);
        }

        String insertQuery = "INSERT INTO sub_activity (subactivity_id, subactivity_name, activity_id) VALUES (?, ?, ?)";
        PreparedStatement insertPst = con.prepareStatement(insertQuery);
        insertPst.setInt(1, subActivityId);
        insertPst.setString(2, subActivity.trim());
        insertPst.setInt(3, activityId);
        insertPst.executeUpdate();

        return subActivityId;
    }

    private Integer getScheduleSubActivityId(Connection con, int recordId) throws SQLException {
        String query = "SELECT subactivity_id FROM schedule WHERE schedule_id=? AND student_id=?";
        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, recordId);
        pst.setInt(2, studentId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return (Integer) rs.getObject("subactivity_id");
        }

        return null;
    }

    private Date getScheduleDate(Connection con, int recordId) throws SQLException {
        String query = "SELECT TRUNC(schedule_date) AS schedule_date FROM schedule WHERE schedule_id=? AND student_id=?";
        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, recordId);
        pst.setInt(2, studentId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getDate("schedule_date");
        }

        throw new SQLException("Schedule date not found for id: " + recordId);
    }

    private double getTotalHoursForDate(Connection con, Date date, int excludeId) throws SQLException {
        PreparedStatement pst;
        if (date == null) {
            String query =
                    "SELECT NVL(SUM(planned_hours), 0) FROM schedule " +
                    "WHERE student_id=? AND TRUNC(schedule_date)=TRUNC(SYSDATE) AND schedule_id<>?";
            pst = con.prepareStatement(query);
            pst.setInt(1, studentId);
            pst.setInt(2, excludeId);
        } else {
            String query =
                    "SELECT NVL(SUM(planned_hours), 0) FROM schedule " +
                    "WHERE student_id=? AND TRUNC(schedule_date)=TRUNC(?) AND schedule_id<>?";
            pst = con.prepareStatement(query);
            pst.setInt(1, studentId);
            pst.setDate(2, date);
            pst.setInt(3, excludeId);
        }
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getDouble(1);
        }

        return 0;
    }

    private double getTotalHoursForDateExcludingActivity(Connection con, Date date, int activityId) throws SQLException {
        String query =
                "SELECT NVL(SUM(planned_hours), 0) FROM schedule " +
                "WHERE student_id=? AND TRUNC(schedule_date)=TRUNC(?) AND activity_id<>?";
        PreparedStatement pst = con.prepareStatement(query);
        pst.setInt(1, studentId);
        pst.setDate(2, date);
        pst.setInt(3, activityId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getDouble(1);
        }

        return 0;
    }

    private void removeDuplicateRows(Connection con, int keepId, int activityId, Integer subActivityId, Date date) throws SQLException {
        PreparedStatement deletePst;
        if (date == null) {
            String deleteQuery =
                    "DELETE FROM schedule " +
                    "WHERE student_id=? AND activity_id=? AND (subactivity_id=? OR (subactivity_id IS NULL AND ? IS NULL)) AND TRUNC(schedule_date)=TRUNC(SYSDATE) AND schedule_id<>?";
            deletePst = con.prepareStatement(deleteQuery);
            deletePst.setInt(1, studentId);
            deletePst.setInt(2, activityId);
            setNullableInt(deletePst, 3, subActivityId);
            setNullableInt(deletePst, 4, subActivityId);
            deletePst.setInt(5, keepId);
        } else {
            String deleteQuery =
                    "DELETE FROM schedule " +
                    "WHERE student_id=? AND activity_id=? AND (subactivity_id=? OR (subactivity_id IS NULL AND ? IS NULL)) AND TRUNC(schedule_date)=TRUNC(?) AND schedule_id<>?";
            deletePst = con.prepareStatement(deleteQuery);
            deletePst.setInt(1, studentId);
            deletePst.setInt(2, activityId);
            setNullableInt(deletePst, 3, subActivityId);
            setNullableInt(deletePst, 4, subActivityId);
            deletePst.setDate(5, date);
            deletePst.setInt(6, keepId);
        }
        deletePst.executeUpdate();
    }

    private void setNullableInt(PreparedStatement pst, int index, Integer value) throws SQLException {
        if (value == null) {
            pst.setNull(index, Types.INTEGER);
        } else {
            pst.setInt(index, value);
        }
    }
    private JPanel createFormRow(String labelText, JComponent field) {
    JPanel panel = new JPanel(new BorderLayout(10, 0));
    panel.setOpaque(false);

    JLabel label = new JLabel(labelText);
    label.setPreferredSize(new Dimension(120, 25));
    label.setFont(new Font("Arial", Font.BOLD, 13));

    panel.add(label, BorderLayout.WEST);
    panel.add(field, BorderLayout.CENTER);

    return panel;
}
}
