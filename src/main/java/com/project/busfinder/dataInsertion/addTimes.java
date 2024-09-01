package com.project.busfinder.dataInsertion;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.*;

import static com.project.busfinder.util.ProcessXML.processXMLAndInsertData;
import static com.project.busfinder.util.ProcessXML.processXMLtoJourneyCode;

public class addTimes {

    public static void main(String[] args) {
        String folderPath = "data/routetest";
        String dbPath = "jdbc:sqlite:data/databases/routes.db";

        try {

            Connection conn = DriverManager.getConnection(dbPath);
            createJourneyCodeTable(conn);
            createJourneyRouteTable(conn);

            File folder = new File(folderPath);
            File[] listOfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));

            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    System.out.println(file.getName());
                    if (file.isFile()) {
                        processXMLFile(file.getPath(), conn);
                    }
                }
            }


            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void createJourneyCodeTable(Connection conn) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS journeyCode_test " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "journey_code TEXT NOT NULL, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "days_of_week TEXT NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    public static void createJourneyRouteTable(Connection conn) throws SQLException {

        String createSaturdayTableSQL = "CREATE TABLE IF NOT EXISTS saturday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";

        String createSundayTableSQL = "CREATE TABLE IF NOT EXISTS sunday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";

        String createMondayTableSQL = "CREATE TABLE IF NOT EXISTS monday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";
        String createTuesdayTableSQL = "CREATE TABLE IF NOT EXISTS tuesday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";
        String createWednesdayTableSQL = "CREATE TABLE IF NOT EXISTS wednesday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";
        String createThursdayTableSQL = "CREATE TABLE IF NOT EXISTS thursday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";
        String createFridayTableSQL = "CREATE TABLE IF NOT EXISTS friday_routes " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id TEXT NOT NULL, " +
                "journey_pattern_ref TEXT NOT NULL, " +
                "Vehicle_journey_code TEXT NOT NULL, " +
                "from_stop TEXT NOT NULL, " +
                "to_stop TEXT NOT NULL, " +
                "day_of_week TEXT NOT NULL, " +
                "departure_time TEXT NOT NULL, " +
                "date DATE NOT NULL)";

        try (Statement stmt = conn.createStatement()) {
            //stmt.execute(createWeekdayTableSQL);  // create the saturday routes table
            stmt.execute(createSaturdayTableSQL);  // create the sunday routes table
            stmt.execute(createSundayTableSQL);
            stmt.execute(createMondayTableSQL);
            stmt.execute(createTuesdayTableSQL);
            stmt.execute(createWednesdayTableSQL);
            stmt.execute(createThursdayTableSQL);
            stmt.execute(createFridayTableSQL);


        }
    }


    public static void processXMLFile(String filePath, Connection conn) {
        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);  // parse the XML file into a Document object
            doc.getDocumentElement().normalize();

            processXMLAndInsertData(doc, conn, filePath);  // process the document and insert data into the database
            //processXMLtoJourneyCode(doc, conn, filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
