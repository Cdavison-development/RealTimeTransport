package com.project.busfinder.helperFunctions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class makeConnection {

    static Connection conn = null;

    public static Connection connect() {

        try {
            // db parameters
            String url = "jdbc:sqlite:data\\databases\\routes.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");
            return conn;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
}