package com.project.busfinder.dataInsertion;

import org.json.JSONArray;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.*;

import static com.project.busfinder.helperFunctions.getUniqueIdentifer.GetUniqueIdentifier;


public class addRoutes {
    public static final String DB_URL = "jdbc:sqlite:data/databases/routes.db";

    public static void main(String[] args) {


        //handleStopRefs("data/stoprefs", DB_URL);
        handlePolylines("data/encoded_polylines", DB_URL);
        handleStopRefs("data/stoprefs", DB_URL);

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            removeIncompleteData(conn);
        } catch (SQLException e) {
            System.err.println("Error while removing incomplete data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void handlePolylines(String directoryPath, String dbUrl) {

        File directory = new File(directoryPath);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                createRoutesTable(conn);
                for (File file : files) {
                    processPolylines(file, conn);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void handleStopRefs(String directoryPath, String dbUrl) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        System.out.println(files.length);
        if (files != null) {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                createRoutesTable(conn);
                for (File file : files) {
                    processStopRefs(file, conn);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void processPolylines(File file, Connection conn) throws Exception {

        System.out.println("Processing polylines...");
        String uniqueIdentifier = GetUniqueIdentifier(file.getName());
        String jsonContent = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        JSONArray jsonArray = new JSONArray(jsonContent);

        String sql = "INSERT INTO routes (route_id, polyline_data) VALUES (?, ?) "
                + "ON CONFLICT(route_id) DO UPDATE SET "
                + "polyline_data = EXCLUDED.polyline_data";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uniqueIdentifier);
            pstmt.setString(2, jsonArray.toString());
            pstmt.executeUpdate();
        }
    }

    static void processStopRefs(File file, Connection conn) throws Exception {

        String uniqueIdentifier = GetUniqueIdentifier(file.getName());
        String jsonContent = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        JSONArray jsonArray = new JSONArray(jsonContent);
        String sql = "INSERT INTO routes (route_id, stop_point_refs) VALUES (?, ?) "
                + "ON CONFLICT(route_id) DO UPDATE SET "
                + "stop_point_refs = EXCLUDED.stop_point_refs";
        //String sql = "MERGE INTO routes (route_id, stop_point_refs) KEY (route_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uniqueIdentifier);
            pstmt.setString(2, jsonArray.toString());
            pstmt.executeUpdate();
        }
    }

    static void createRoutesTable(Connection conn) throws Exception {
        // ensure the table can store both polyline data and stop point references
        String createTableSQL = "CREATE TABLE IF NOT EXISTS routes ("
                + "route_id TEXT PRIMARY KEY, "
                + "polyline_data TEXT, "
                + "stop_point_refs TEXT)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }
    //
    static void removeIncompleteData(Connection conn) throws SQLException {
        String sql = "DELETE FROM routes WHERE polyline_data IS NULL OR stop_point_refs IS NULL";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int affectedRows = pstmt.executeUpdate();
            System.out.println("Removed " + affectedRows + " incomplete records from the database.");
        }
    }

}

