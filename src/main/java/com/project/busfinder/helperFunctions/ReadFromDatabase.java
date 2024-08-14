package com.project.busfinder.helperFunctions;

import java.sql.*;
import java.util.ArrayList;

public class ReadFromDatabase {

    public static void main(String[] args) {
        readRoutes();
    }

    public static ArrayList<String> readRoutes() {
        Connection con = null;
        PreparedStatement p = null;
        ResultSet rs = null;
        ArrayList<String> routes = null;

        // establish a connection to the database using the connect method
        con = makeConnection.connect();

        try {
            // SQL query to select route IDs from the routes table
            String sql = "select route_id from routes";
            p = con.prepareStatement(sql);
            rs = p.executeQuery();

            // initialise the routes list to store the results
            routes = new ArrayList<>();

            // iterate through the result set and add each route ID to the routes list
            while (rs.next()) {
                String id = rs.getString("route_id");
                routes.add(id);
            }
        } catch (SQLException e) {
            System.out.println(e); // print the exception if an error occurs during the query
        }

        // return the list of route IDs
        return routes;
    }
}
