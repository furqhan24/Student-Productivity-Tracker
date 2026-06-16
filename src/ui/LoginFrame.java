package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

import db.DBConnection;

public class LoginFrame extends JFrame implements ActionListener {

    JTextField emailField;
    JPasswordField passwordField;
    JButton loginBtn, registerBtn;

    public LoginFrame() {

        setTitle("Login");
        setSize(460, 420);
        setMinimumSize(new Dimension(380, 360));
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Student Productivity", "Sign in to manage your schedule, logs, reports, and recovery plan."), BorderLayout.NORTH);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.BACKGROUND);

        JPanel card = UITheme.createCard();
        card.setLayout(new GridLayout(5, 1, 8, 8));
        card.setPreferredSize(new Dimension(340, 220));
        card.setMaximumSize(new Dimension(420, 260));

        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 13));
        emailField = new JTextField();
        UITheme.styleField(emailField);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Arial", Font.BOLD, 13));
        passwordField = new JPasswordField();
        UITheme.styleField(passwordField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);
        loginBtn = UITheme.primaryButton("Login");
        registerBtn = UITheme.secondaryButton("Register");
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        card.add(emailLabel);
        card.add(emailField);
        card.add(passLabel);
        card.add(passwordField);
        card.add(buttonPanel);
        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);

        loginBtn.addActionListener(this);
        registerBtn.addActionListener(e -> {
            new RegisterFrame(this);
            this.setVisible(false);
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {

        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            UITheme.showInfo(this, "Missing Details", "Please enter both your email and password before logging in.");
            return;
        }

        try {
            Connection con = DBConnection.getConnection();

            String query = "SELECT student_id, password FROM student WHERE email=?";
            PreparedStatement pst = con.prepareStatement(query);

            pst.setString(1, email);

            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                int choice = UITheme.showConfirm(
                        this,
                        "Account Not Found",
                        "No student account exists for this email. Would you like to register a new account now?",
                        "Register",
                        "Try Again"
                );

                if (choice == JOptionPane.YES_OPTION) {
                    new RegisterFrame(this);
                    this.setVisible(false);
                }
                return;
            }

            String actualPassword = rs.getString("password");

            if (password.equals(actualPassword)) {
                int studentId = rs.getInt("student_id");
                UITheme.showSuccess(this, "Login Successful", "Welcome back. Your dashboard is ready.");
                new DashboardFrame(studentId);
                this.dispose();
            } else {
                UITheme.showError(this, "Incorrect Password", "The email is correct, but the password does not match. Please try again.");
                passwordField.setText("");
                passwordField.requestFocus();
            }

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(this, "Login Failed", "Please check the database connection and try again.");
        }
    }
}
