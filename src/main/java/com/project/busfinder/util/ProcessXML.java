package com.project.busfinder.util;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.xml.stream.*;

import static com.project.busfinder.helperFunctions.getUniqueIdentifer.GetUniqueIdentifier;

public class ProcessXML {

    public static void main(String[] args) {
        String Filename = "data/routes/AMSY_G10S_AMSYPC000114157610B_20240123_-_1808719.xml";
        try {

            File inputFile = new File(Filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();


            Connection conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
            Statement stmt = conn.createStatement();
            String createTableSQL = "CREATE TABLE IF NOT EXISTS journey_test " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "journey_code TEXT NOT NULL, " +
                    "route_id TEXT NOT NULL, " +
                    "journey_pattern_ref TEXT NOT NULL, " +
                    "Vehicle_journey_code TEXT NOT NULL, " +
                    "from_stop TEXT NOT NULL, " +
                    "to_stop TEXT NOT NULL, " +
                    "departure_time TEXT NOT NULL)";
            stmt.execute(createTableSQL);


            processXMLAndInsertData(doc, conn,Filename);


            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

public static void processXMLAndInsertData(Document doc, Connection conn, String filePath) {
    try {
        int journeyCounter = 1;
        // process each VehicleJourney and insert data into SQLite
        NodeList vehicleJourneyList = doc.getElementsByTagName("VehicleJourney");
        for (int i = 0; i < vehicleJourneyList.getLength(); i++) {
            Element vehicleJourney = (Element) vehicleJourneyList.item(i);
            String journeyCode = getNestedTextContent(vehicleJourney, "JourneyCode");
            String routeId = GetUniqueIdentifier(filePath);
            System.out.println(routeId);
            String journeyPatternRef = getNestedTextContent(vehicleJourney, "JourneyPatternRef");
            String initialDepartureTime = getDepartureTime(filePath, journeyCode);
            LocalTime departureTime = LocalTime.parse(initialDepartureTime, DateTimeFormatter.ofPattern("HH:mm:ss"));

            //generate the VehicleJourneyRef using the counter
            String vehicleJourneyRef = "VJ_" + journeyCounter;
            journeyCounter++; // increment the counter for the next journey

            NodeList journeyPatternList = doc.getElementsByTagName("JourneyPattern");
            for (int j = 0; j < journeyPatternList.getLength(); j++) {
                Element journeyPattern = (Element) journeyPatternList.item(j);
                if (journeyPattern.getAttribute("id").equals(journeyPatternRef)) {
                    String sectionRef = getNestedTextContent(journeyPattern, "JourneyPatternSectionRefs");
                    NodeList sectionList = doc.getElementsByTagName("JourneyPatternSection");

                    String JourneyCodeSQL = "INSERT INTO journeyCode (journey_code, route_id, journey_pattern_ref,Vehicle_journey_code) VALUES (?, ?, ?,?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(JourneyCodeSQL)) {
                        pstmt.setString(1, journeyCode);
                        pstmt.setString(2, routeId);
                        pstmt.setString(3, journeyPatternRef);
                        pstmt.setString(4, vehicleJourneyRef);
                        pstmt.executeUpdate();
                    }

                    for (int k = 0; k < sectionList.getLength(); k++) {
                        Element section = (Element) sectionList.item(k);
                        if (section.getAttribute("id").equals(sectionRef)) {
                            NodeList linkList = section.getElementsByTagName("JourneyPatternTimingLink");
                            for (int l = 0; l < linkList.getLength(); l++) {
                                Element link = (Element) linkList.item(l);
                                String fromStop = getNestedTextContent((Element) link.getElementsByTagName("From").item(0), "StopPointRef");
                                String toStop = getNestedTextContent((Element) link.getElementsByTagName("To").item(0), "StopPointRef");
                                String runTime = getNestedTextContent(link, "RunTime");

                                if (fromStop != null && toStop != null && runTime != null) {
                                    // calculate the new departure time
                                    Duration duration = Duration.parse(runTime);
                                    departureTime = departureTime.plus(duration);

                                    String insertSQL = "INSERT INTO journeyRoutes (route_id, journey_pattern_ref, Vehicle_journey_code,from_stop, to_stop, departure_time) VALUES (?, ?, ?, ?, ?, ?)";
                                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                                        pstmt.setString(1, routeId);
                                        pstmt.setString(2, journeyPatternRef);
                                        pstmt.setString(3, vehicleJourneyRef);
                                        pstmt.setString(4, fromStop);
                                        pstmt.setString(5, toStop);
                                        pstmt.setString(6, departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                                        pstmt.executeUpdate();
                                    }
                                } else {
                                    // log detailed information about the skipped insertion
                                    System.out.println("Skipping insertion due to null from_stop, to_stop, or runTime:");
                                    System.out.println("JourneyCode: " + journeyCode);
                                    System.out.println("RouteId: " + routeId);
                                    System.out.println("JourneyPatternRef: " + journeyPatternRef);
                                    System.out.println("FromStop: " + fromStop);
                                    System.out.println("ToStop: " + toStop);
                                    System.out.println("RunTime: " + runTime);


                                    System.out.println("Link XML:");
                                    System.out.println(elementToString(link));
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private static String getNestedTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                return node.getTextContent();
            }
        }
        return null;
    }


    private static String elementToString(Element element) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            return "Error converting element to string: " + e.getMessage();
        }
    }

    public static String getDepartureTime(String filePath, String journeyCodeValue) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));
        boolean isVehicleJourney = false;
        String departureTime = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                if ("JourneyCode".equals(elementName)) {
                    Characters characters = (Characters) eventReader.nextEvent();
                    String currentRef = characters.getData();
                    if (journeyCodeValue.equals(currentRef)) {
                        isVehicleJourney = true;
                    }
                } else if (isVehicleJourney && "DepartureTime".equals(elementName)) {
                    Characters characters = (Characters) eventReader.nextEvent();
                    departureTime = characters.getData();
                    break;
                }
            } else if (event.isEndElement()) {
                if ("VehicleJourney".equals(event.asEndElement().getName().getLocalPart()) && isVehicleJourney) {
                    break;
                }
            }
        }

        return departureTime;
    }

}
