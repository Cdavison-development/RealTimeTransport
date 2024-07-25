package com.project.busfinder.helperFunctions;

import java.sql.*;
import java.util.ArrayList;

public class ReadFromDatabase {

    public static void main(String[] args) {
        readRoutes();
    }

    public static ArrayList<String> readRoutes(){
        Connection con = null;
        PreparedStatement p = null;
        ResultSet rs = null;
        ArrayList<String> routes = null;

        con = makeConnection.connect();

        try {


            String sql = "select route_id from routes";
            p = con.prepareStatement(sql);
            rs = p.executeQuery();

            routes = new ArrayList<String>();

            while (rs.next()) {

                String id = rs.getString("route_id");
                routes.add(id);
            }
        }

        catch (SQLException e) {

            System.out.println(e);
        }
        //System.out.println(routes);
        return routes;
    }
}
