package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {

        Connection con = null;

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            /*
            * Database Configuration
            *
            * Before running the application, update the following
            * database URL, username, and password according to your
            * local Oracle Database setup.
            *
            * Example:
            * URL      : jdbc:oracle:thin:@localhost:1521:XE
            * Username : your_username
            * Password : your_password
            *
            * The credentials used during development have been removed
            * for security reasons and are not included in this repository.
            */
            con = DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521/XE",
                "username",
                "password"
            );

        } catch (Exception e) {
            System.out.println(e);
        }

        return con;
    }
}