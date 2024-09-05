package com.project.busfinder.helperFunctions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class makeConnection {

    static Connection conn = null;

    public static Connection connect() {

        try {
            // database connection URL
            String url = "jdbc:sqlite:data\\databases\\routes.db";
            // establish a connection to the SQLite database

            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");
            return conn;
        } catch (SQLException e) {

            System.out.println(e.getMessage()); // print the error message if connection fails

        }
        return conn;
    }
}