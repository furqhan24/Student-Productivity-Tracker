package main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import db.DBConnection;

public class LoginTest {

    public static void main(String[] args) {

        String email = "zeyi@qq.com";
        String password = "1234";

        try {

            Connection con = DBConnection.getConnection();

            String query = "SELECT * FROM student WHERE email=? AND password=?";

            PreparedStatement pst = con.prepareStatement(query);

            pst.setString(1, email);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                System.out.println("Login Successful!");
            } else {
                System.out.println("Invalid Credentials!");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}