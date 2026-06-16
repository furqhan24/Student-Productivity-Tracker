package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

import db.DBConnection;

public class RegisterFrame extends JFrame implements ActionListener {

    LoginFrame loginFrame;
    JTextField nameField, emailField;
    JPasswordField passwordField, confirmPasswordField;
    JButton registerBtn, backBtn;

    public RegisterFrame(LoginFrame loginFrame) {

        this.loginFrame = loginFrame;

        setTitle("Register");
        setSize(480, 500);
        setMinimumSize(new Dimension(400, 430));
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UITheme.BACKGROUND);

        add(UITheme.createHeader("Create Account", "Register once, then keep your productivity data private to your account."), BorderLayout.NORTH);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.BACKGROUND);

        JPanel card = UITheme.createCard();
        card.setLayout(new GridLayout(9, 1, 8, 8));
        card.setPreferredSize(new Dimension(360, 300));
        card.setMaximumSize(new Dimension(440, 340));

        nameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();

        UITheme.styleField(nameField);
        UITheme.styleField(emailField);
        UITheme.styleField(passwordField);
        UITheme.styleField(confirmPasswordField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);
        registerBtn = UITheme.primaryButton("Register");
        backBtn = UITheme.secondaryButton("Back");
        buttonPanel.add(registerBtn);
        buttonPanel.add(backBtn);

        card.add(fieldLabel("Name"));
        card.add(nameField);
        card.add(fieldLabel("Email"));
        card.add(emailField);
        card.add(fieldLabel("Password"));
        card.add(passwordField);
        card.add(fieldLabel("Confirm Password"));
        card.add(confirmPasswordField);
        card.add(buttonPanel);

        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);

        registerBtn.addActionListener(this);
        backBtn.addActionListener(e -> backToLogin());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setForeground(UITheme.TEXT);
        return label;
    }

    public void actionPerformed(ActionEvent e) {

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            UITheme.showInfo(this, "Missing Details", "Please fill all registration fields before creating your account.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            UITheme.showError(this, "Password Mismatch", "Password and confirm password must be exactly the same.");
            return;
        }

        try {
            Connection con = DBConnection.getConnection();

            String checkQuery = "SELECT email FROM student WHERE email=?";
            PreparedStatement checkPst = con.prepareStatement(checkQuery);
            checkPst.setString(1, email);

            ResultSet rs = checkPst.executeQuery();

            if (rs.next()) {
                UITheme.showInfo(this, "Email Already Registered", "This email already has an account. Go back and login instead.");
                return;
            }

            String idQuery = "SELECT NVL(MAX(student_id), 0) + 1 FROM student";
            PreparedStatement idPst = con.prepareStatement(idQuery);
            ResultSet idRs = idPst.executeQuery();

            int studentId = 1;
            if (idRs.next()) {
                studentId = idRs.getInt(1);
            }

            String insertQuery =
                    "INSERT INTO student (student_id, name, email, password) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement insertPst = con.prepareStatement(insertQuery);
            insertPst.setInt(1, studentId);
            insertPst.setString(2, name);
            insertPst.setString(3, email);
            insertPst.setString(4, password);

            insertPst.executeUpdate();

            UITheme.showSuccess(this, "Registration Successful", "Your account was created. Please login with your new credentials.");
            backToLogin();

        } catch (Exception ex) {
            System.out.println(ex);
            UITheme.showError(this, "Registration Failed", "Unable to create the account. Please check the database connection and table columns.");
        }
    }

    private void backToLogin() {
        loginFrame.setVisible(true);
        this.dispose();
    }
}
