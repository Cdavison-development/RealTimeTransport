package com.project.busfinder.dataInsertion;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.Map;


import static com.project.busfinder.helperFunctions.getUniqueIdentifer.GetUniqueIdentifier;
import static com.project.busfinder.util.XMLtoTimetableFunctions.*;



/**
 *
 *  Remember to convert all XML files from UTF-8-BOM to just UTF-8
 *
 *  find all journey codes -> convert journey codes to journeyRef -> use to get departure time ->
 *  parse journey pattern sections and store
 *
 *
 */


public class addTimes {

    public static final String DB_URL = "jdbc:sqlite:data/databases/routes.db";

    public static void main(String[] args) {
        String folderPath = "data/routes";
        try {
            createJourneyTable(DB_URL);
            processAllFilesInFolder(folderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The Main function for processing Files, utilises all functions within the addTimes and XMLtoTimetableFunctions classes
     * iterates over each file and applies read, parse and write functions.
     *
     * sequence of operations:
     *        find all journey codes (journey codes are within the same block as Journey Ref, which is used to find route timetable,
     *        and is returned by the API, so bus data can be retrieved based on this code)
     *
     *        -> convert journey codes to Journey References ( Journey references are used to find the full timetable/schedule)
     *
     *        -> use to get departure time (departure time is in the same xml block, using the runTime tag, we can simulate bus
     *        travel by adding runTime, which is the time between stops, to departure time, therefore optimising calls somewhat
     *        by predicting bus location using this, rather than constant API calls.
     *
     *        -> parse journey sections and store.
     *
     * @param folderPath path the XML files are stored in
     *
     */
    private static void processAllFilesInFolder(String folderPath) throws IOException, SQLException {
        Path dir = Paths.get(folderPath);
        System.out.println("Processing folder: " + folderPath);
        // Use try-with-resources to ensure that the directory stream is closed properly after use.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
            // Iterate over each folder in file.
            for (Path entry : stream) {
                // Get filePath String.
                String filePath = entry.toString();
                System.out.println("Processing file: " + filePath);
                // Get all Journey Codes for a given XML file.
                var JourneyCodeList = findAllJourneyCodes(filePath);
                System.out.println("Found Journey Codes: " + JourneyCodeList);
                // Initialise empty journey Refs, outside for loops so can be re-initialised for each file.
                String journeyRefs = "";
                for (String JourneyCode : JourneyCodeList) {
                    System.out.println("Processing Journey Code: " + JourneyCode);
                    // Use Journey codes to find the Journey Ref, which is a unique identifier for certain journey on a bus route.
                    journeyRefs = findVehicleJourneyRef(filePath, JourneyCode);
                    System.out.println("Found journey refs : " + journeyRefs);
                }
                if (journeyRefs != null) {
                    // Will be used for predicting where we expect a bus to be.
                    String departureTime = getDepartureTime(filePath, journeyRefs);
                    if (departureTime != null) {
                        // Write all the data to sqlite.
                        List<Map<String, String>> journeyPatternSections = parseJourneyPatternSections(filePath, departureTime);
                        writeLinksToDatabase(filePath,journeyPatternSections);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            System.out.println("Error processing files in the directory: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * Creates Table in the specified database. Table columns are outlined.
     * We use autoincrement as a unique identifier for a row because route_id cannot be unique due to the
     * one-to-many relationship between a bus route and number of different routes that a bus route can take having to be
     * written as a one-to-one format
     *
     * example:
     *          |route_id|Journey_pattern|TO      |FROM   |ExpectedTime|
     *          |route_1 | journey1      |example|example|  example    |
     *          |route_1 | journey2      |example|example|  example    |
     *
     *
     *
     * @param fileName - database filePath/URL
     */
    public static void createJourneyTable(String fileName) {
        // Attempt connection.
        try (Connection conn = DriverManager.getConnection(fileName)) {
            if (conn != null) {
                try (Statement stmt = conn.createStatement()) {
                    // SQL statement for creating a new table
                    String sql = "CREATE TABLE IF NOT EXISTS journey_patterns (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "route_id INTEGER," +
                            "JourneyPattern TEXT," +
                            "To_stop TEXT," +
                            "From_stop TEXT," +
                            "ExpectedTime TEXT)";
                    stmt.execute(sql);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     *
     * Writes data from the link parameter to the database
     *
     *
     * @param link - Map that stores all data for one journey.
     * @param conn - database connection.
     * @param file - file name for Unique identifier column.
     * @throws Exception
     */
    private static void insertLinkData(Map<String, String> link, Connection conn, File file) throws Exception {

        // Get bus route name from file.
        String uniqueIdentifier = GetUniqueIdentifier(file.getName());

        // Despite 'journey patterns' throwing an error in the IDE, it is not erroneous.
        // Specify columns for insertion.
        String sql = "INSERT INTO journey_patterns (route_id, JourneyPattern, To_stop, From_stop,ExpectedTime) VALUES (?, ?, ?, ?,?);";
        // Attempt to insert files into database.
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uniqueIdentifier);
            pstmt.setString(2, link.get("JourneyPatternRef"));
            pstmt.setString(3, link.get("To_StopPointRef"));
            pstmt.setString(4, link.get("From_StopPointRef"));
            pstmt.setString(5, link.get("UpdatedDepartureTime"));
            pstmt.executeUpdate();
        }
    }

    /**
     *
     *  Performs the insertion on data for each item in the data
     *
     *
     * @param filePath - path to file in use.
     * @param timingLinks - the read and parsed data from the XML.
     * @throws Exception
     */
    public static void writeLinksToDatabase(String filePath,List<Map<String, String>> timingLinks) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            for (Map<String, String> link : timingLinks) {
                insertLinkData(link, conn, new File(filePath));
                System.out.println(link);
            }
        } catch (Exception e) {
            System.out.println("Database write error: " + e.getMessage());
        }
    }


}
