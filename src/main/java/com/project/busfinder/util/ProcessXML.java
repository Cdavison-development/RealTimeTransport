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
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.*;

import static com.project.busfinder.helperFunctions.getUniqueIdentifer.GetUniqueIdentifier;

// need to account for the different days outlines in XML.

/**
 *
 *  what may need to change
 *
 *  Routes
 *  JourneyCode
 *  JourneyRoutes
 *
 * considerations: need to consider day of the week
 *
 *  do buses take different routes on different days of the week
 *
 */
public class ProcessXML {

    public static void main(String[] args) {
        String Filename = "data/routes/AMSY_10A_AMSYPC00011414710A_20240721_-_1894508.xml";
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

            // extract the list of VehicleJourney elements from the XML
            NodeList vehicleJourneyList = doc.getElementsByTagName("VehicleJourney");

            // extract the days of the week associated with the journeys
            List<String> daysOfWeek = extractDaysOfWeek(doc);

            // iterate over each VehicleJourney in the list
            for (int i = 0; i < vehicleJourneyList.getLength(); i++) {
                Element vehicleJourney = (Element) vehicleJourneyList.item(i);
                String journeyCode = getNestedTextContent(vehicleJourney, "JourneyCode");
                String routeId = GetUniqueIdentifier(filePath);
                String journeyPatternRef = getNestedTextContent(vehicleJourney, "JourneyPatternRef");
                String initialDepartureTime = getDepartureTime(filePath, journeyCode);
                LocalTime departureTime = LocalTime.parse(initialDepartureTime, DateTimeFormatter.ofPattern("HH:mm:ss"));

                // generate a unique reference for each VehicleJourney
                String vehicleJourneyRef = "VJ_" + journeyCounter;
                journeyCounter++;

                NodeList journeyPatternList = doc.getElementsByTagName("JourneyPattern");

                // find and process the corresponding JourneyPattern
                for (int j = 0; j < journeyPatternList.getLength(); j++) {
                    Element journeyPattern = (Element) journeyPatternList.item(j);
                    if (journeyPattern.getAttribute("id").equals(journeyPatternRef)) {
                        String sectionRef = getNestedTextContent(journeyPattern, "JourneyPatternSectionRefs");
                        NodeList sectionList = doc.getElementsByTagName("JourneyPatternSection");

                        // insert the journey code and other relevant information into the database
                        String JourneyCodeSQL = "INSERT INTO journeyCode (journey_code, route_id, journey_pattern_ref, Vehicle_journey_code, days_of_week) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(JourneyCodeSQL)) {
                            pstmt.setString(1, journeyCode);
                            pstmt.setString(2, routeId);
                            pstmt.setString(3, journeyPatternRef);
                            pstmt.setString(4, vehicleJourneyRef);
                            pstmt.setString(5, String.join(",", daysOfWeek));
                            pstmt.executeUpdate();
                        }

                        // prepare to distribute and insert journey legs
                        List<JourneyLegDeparture> sameMinuteLegs = new ArrayList<>();
                        for (int k = 0; k < sectionList.getLength(); k++) {
                            Element section = (Element) sectionList.item(k);
                            if (section.getAttribute("id").equals(sectionRef)) {
                                NodeList linkList = section.getElementsByTagName("JourneyPatternTimingLink");
                                for (int l = 0; l < linkList.getLength(); l++) {
                                    Element link = (Element) linkList.item(l);
                                    String fromStop = getNestedTextContent((Element) link.getElementsByTagName("From").item(0), "StopPointRef");
                                    String toStop = getNestedTextContent((Element) link.getElementsByTagName("To").item(0), "StopPointRef");
                                    String runTime = getNestedTextContent(link, "RunTime");

                                    // process and adjust the departure times for legs within the same minute
                                    if (fromStop != null && toStop != null && runTime != null) {
                                        java.time.Duration duration = java.time.Duration.parse(runTime);
                                        LocalTime newDepartureTime = departureTime.plus(duration);

                                        // determine the date based on the day of the week
                                        for (String day : daysOfWeek) {
                                            LocalDate date = determineDateForDay(day,newDepartureTime);
                                            JourneyLegDeparture journeyLeg = new JourneyLegDeparture(fromStop, toStop, javafx.util.Duration.millis(duration.toMillis()), newDepartureTime, date);

                                            if (!sameMinuteLegs.isEmpty() && !newDepartureTime.truncatedTo(ChronoUnit.MINUTES).equals(sameMinuteLegs.get(0).getDepartureTime().truncatedTo(ChronoUnit.MINUTES))) {
                                                distributeTimeWithinSameMinute(sameMinuteLegs);
                                                insertJourneyLegsIntoDB(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn);
                                                sameMinuteLegs.clear();
                                            }

                                            sameMinuteLegs.add(journeyLeg);
                                            departureTime = newDepartureTime;
                                        }
                                    } else {
                                        System.out.println("JourneyCode: " + journeyCode);
                                        System.out.println("RouteId: " + routeId);
                                    }
                                }
                            }
                        }

                        // process any remaining legs after the loop
                        if (!sameMinuteLegs.isEmpty()) {
                            distributeTimeWithinSameMinute(sameMinuteLegs);
                            insertJourneyLegsIntoDB(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static LocalDate determineDateForDay(String day, LocalTime time) {
        LocalDate baseDate;
        switch (day) {
            case "Monday":
                baseDate = LocalDate.of(2024, 8, 12);
                break;
            case "Tuesday":
                baseDate = LocalDate.of(2024, 8, 13);
                break;
            case "Wednesday":
                baseDate = LocalDate.of(2024, 8, 14);
                break;
            case "Thursday":
                baseDate = LocalDate.of(2024, 8, 15);
                break;
            case "Friday":
                baseDate = LocalDate.of(2024, 8, 16);
                break;
            case "Saturday":
                baseDate = LocalDate.of(2024, 8, 17);
                break;
            case "Sunday":
                baseDate = LocalDate.of(2024, 8, 18);
                break;
            default:
                throw new IllegalArgumentException("Invalid day of the week: " + day);
        }

        // adjust date if the time is before 3AM, consider this time part of the previous day
        if (time.isBefore(LocalTime.of(3, 0))) {
            baseDate = baseDate.minusDays(1);
        }

        return baseDate;
    }
    private static String getNestedTextContent(Element parent, String tagName) {
        // retrieve the list of nodes with the specified tag name
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            // get the first node in the list
            Node node = nodeList.item(0);
            // ensure the node is an element node and return its text content
            if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                return node.getTextContent();
            }
        }
        // return null if the tag is not found or the node is not an element
        return null;
    }

    private static List<String> extractDaysOfWeek(Document doc) {
        // create a list to store the days of the week
        List<String> daysOfWeek = new ArrayList<>();

        // get the list of RegularDayType elements from the document
        NodeList regularDayTypeList = doc.getElementsByTagName("RegularDayType");

        // check if there are any RegularDayType elements
        if (regularDayTypeList.getLength() > 0) {
            // get the first RegularDayType element
            Element regularDayTypeElement = (Element) regularDayTypeList.item(0);

            // get the list of DaysOfWeek elements within the RegularDayType
            NodeList daysOfWeekList = regularDayTypeElement.getElementsByTagName("DaysOfWeek");

            // check if there are any DaysOfWeek elements
            if (daysOfWeekList.getLength() > 0) {
                // get the first DaysOfWeek element
                Element daysOfWeekElement = (Element) daysOfWeekList.item(0);

                // check for each day of the week and add it to the list if present
                if (daysOfWeekElement.getElementsByTagName("Monday").getLength() > 0) {
                    daysOfWeek.add("Monday");
                }
                if (daysOfWeekElement.getElementsByTagName("Tuesday").getLength() > 0) {
                    daysOfWeek.add("Tuesday");
                }
                if (daysOfWeekElement.getElementsByTagName("Wednesday").getLength() > 0) {
                    daysOfWeek.add("Wednesday");
                }
                if (daysOfWeekElement.getElementsByTagName("Thursday").getLength() > 0) {
                    daysOfWeek.add("Thursday");
                }
                if (daysOfWeekElement.getElementsByTagName("Friday").getLength() > 0) {
                    daysOfWeek.add("Friday");
                }
                if (daysOfWeekElement.getElementsByTagName("Saturday").getLength() > 0) {
                    daysOfWeek.add("Saturday");
                }
                if (daysOfWeekElement.getElementsByTagName("Sunday").getLength() > 0) {
                    daysOfWeek.add("Sunday");
                }
            }
        }

        // return the list of days of the week
        return daysOfWeek;
    }

    private static String elementToString(Element element) {
        try {
            // convert the XML element to a string representation
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            // handle and return any errors during the conversion process
            return "error converting element to string: " + e.getMessage();
        }
    }

    public static String getDepartureTime(String filePath, String journeyCodeValue) throws Exception {
        // create an XML event reader to parse the XML file
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));
        boolean isVehicleJourney = false;
        String departureTime = null;

        // iterate through the XML events to find the matching journey code and retrieve its departure time
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                // check if the current element is JourneyCode and matches the given journey code value
                if ("JourneyCode".equals(elementName)) {
                    Characters characters = (Characters) eventReader.nextEvent();
                    String currentRef = characters.getData();
                    if (journeyCodeValue.equals(currentRef)) {
                        isVehicleJourney = true;
                    }
                    // if within the correct vehicle journey, find and store the departure time
                } else if (isVehicleJourney && "DepartureTime".equals(elementName)) {
                    Characters characters = (Characters) eventReader.nextEvent();
                    departureTime = characters.getData();
                    break;
                }
                // exit the loop once the end of the relevant VehicleJourney element is reached
            } else if (event.isEndElement()) {
                if ("VehicleJourney".equals(event.asEndElement().getName().getLocalPart()) && isVehicleJourney) {
                    break;
                }
            }
        }

        return departureTime;
    }

    private static void insertJourneyLegsIntoDB(List<JourneyLegDeparture> journeyLegs, String routeId, String journeyPatternRef, String vehicleJourneyRef, List<String> daysOfWeek, Connection conn) {
        // initialise variables to track legs within the same minute
        List<JourneyLegDeparture> sameMinuteLegs = new ArrayList<>();
        LocalTime currentMinute = null;

        // iterate through the journey legs to group and process them by minute
        for (JourneyLegDeparture leg : journeyLegs) {
            LocalTime legMinute = leg.getDepartureTime().truncatedTo(ChronoUnit.MINUTES);

            // group legs that occur within the same minute
            if (currentMinute == null || legMinute.equals(currentMinute)) {
                sameMinuteLegs.add(leg);
                currentMinute = legMinute;
            } else {
                // process the group of legs that occurred within the previous minute
                processLegs(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn);

                // start a new group for the current leg
                sameMinuteLegs.clear();
                sameMinuteLegs.add(leg);
                currentMinute = legMinute;
            }
        }

        // process the final group of legs after the loop
        if (!sameMinuteLegs.isEmpty()) {
            processLegs(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn);
        }
    }

    private static void processLegs(List<JourneyLegDeparture> legs, String routeId, String journeyPatternRef, String vehicleJourneyRef, List<String> daysOfWeek, Connection conn) {
        // distribute times within the same minute if there is more than one leg
        if (legs.size() > 1) {
            distributeTimeWithinSameMinute(legs);
        }

        // determine the appropriate database table based on the days of the week
        String tableName = determineTableForDays(daysOfWeek);

        // insert the processed legs into the selected database table
        insertLegsIntoDatabase(legs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn, tableName);
    }

    private static void distributeTimeWithinSameMinute(List<JourneyLegDeparture> sameMinuteLegs) {
        int legCount = sameMinuteLegs.size();
        long secondsPerLeg = 60 / legCount; // distribute 60 seconds equally among the legs

        // adjust the departure time for each leg within the same minute
        for (int i = 1; i < legCount; i++) {
            LocalTime previousDepartureTime = sameMinuteLegs.get(i - 1).getDepartureTime();
            LocalTime newDepartureTime = previousDepartureTime.plusSeconds(secondsPerLeg);

            sameMinuteLegs.get(i).setDepartureTime(newDepartureTime);
        }
    }

    private static String determineTableForDays(List<String> daysOfWeek) {

        if (daysOfWeek.contains("Saturday") && daysOfWeek.size() == 1) {
            return "saturday_routes";
        } else if (daysOfWeek.contains("Sunday") && daysOfWeek.size() == 1) {
            return "sunday_routes";
        } else {
            return "weekday_routes";
        }
    }

    private static void insertLegsIntoDatabase(List<JourneyLegDeparture> legs, String routeId, String journeyPatternRef, String vehicleJourneyRef, List<String> daysOfWeek, Connection conn, String tableName) {
        // iterate through each leg and insert its data into the specified table
        for (JourneyLegDeparture leg : legs) {
            String insertSQL = "INSERT INTO " + tableName +"(route_id, journey_pattern_ref, Vehicle_journey_code, from_stop, to_stop, days_of_week, departure_time,date) VALUES (?, ?, ?, ?, ?, ?, ?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, routeId);
                pstmt.setString(2, journeyPatternRef);
                pstmt.setString(3, vehicleJourneyRef);
                pstmt.setString(4, leg.getFromStop());
                pstmt.setString(5, leg.getToStop());
                pstmt.setString(6, String.join(",", daysOfWeek));
                pstmt.setString(7, leg.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                pstmt.setDate(8, Date.valueOf(leg.getDate()));
                pstmt.executeUpdate();
                System.out.println("Inserted successfully.");
            } catch (SQLException e) {
                System.err.println("error inserting leg into the database:");
                e.printStackTrace();
            }
        }
    }

    private static class JourneyLegDeparture {

        private final String fromStop;
        private final String toStop;
        private final javafx.util.Duration runTime;
        private LocalTime departureTime;
        private final LocalDate date;

        public JourneyLegDeparture(String fromStop, String toStop, javafx.util.Duration runTime, LocalTime departureTime, LocalDate date) {
            this.fromStop = fromStop;
            this.toStop = toStop;
            this.runTime = runTime;
            this.departureTime = departureTime;
            this.date = date;
        }

        public String getFromStop() {
            return fromStop;
        }

        public String getToStop() {
            return toStop;
        }

        public LocalTime getDepartureTime() {
            return departureTime;
        }

        public void setDepartureTime(LocalTime departureTime) {
            this.departureTime = departureTime;
        }

        public LocalDate getDate() {
            return date;
        }
    }
}
