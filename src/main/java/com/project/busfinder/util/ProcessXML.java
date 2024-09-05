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



public class ProcessXML {
    // main function used for testing individual files
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


    /**
     *
     * Used to iterate over an XML file and grab all relevant data, and insert into DB
     *
     *
     *
     * @param doc given XML document
     * @param conn database connection
     * @param filePath specified filepath, as shown in the main, but more accurately in addTimes, as we iterate over this function
     *                 for files in a folder.
     */
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
                            for (String dayOfWeek : daysOfWeek) {
                                String tableName = determineTableForDays(dayOfWeek);
                                // insert the journey code and other relevant information into the database
                                String JourneyCodeSQL = "INSERT INTO journeyCode (journey_code, route_id, journey_pattern_ref, Vehicle_journey_code, days_of_week) VALUES (?, ?, ?, ?, ?)";
                                try (PreparedStatement pstmt = conn.prepareStatement(JourneyCodeSQL)) {
                                    pstmt.setString(1, journeyCode);
                                    pstmt.setString(2, routeId);
                                    pstmt.setString(3, journeyPatternRef);
                                    pstmt.setString(4, vehicleJourneyRef);
                                    pstmt.setString(5, dayOfWeek);
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
                                                LocalDate date = determineDateForDay(dayOfWeek, newDepartureTime);
                                                JourneyLegDeparture journeyLeg = new JourneyLegDeparture(fromStop, toStop, javafx.util.Duration.millis(duration.toMillis()), newDepartureTime, date);
                                                // if the time between stops is <1 minutes, the RunTime tag = 0. To combat this, split the minute between however many stops
                                                //have a runtime of 0. Example: rather than Stop A (08:05:00) and Stop B (08:05:00) and Stop C (08:06:00),
                                                // we get Stop A (08:05:00) and Stop B (08:05:30) and Stop C (08:06:00). thus spreading the time out evenly
                                                if (!sameMinuteLegs.isEmpty() && !newDepartureTime.truncatedTo(ChronoUnit.MINUTES).equals(sameMinuteLegs.get(0).getDepartureTime().truncatedTo(ChronoUnit.MINUTES))) {
                                                    distributeTimeWithinSameMinute(sameMinuteLegs);
                                                    groupAndInsertJourneyLegsByMinute(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, dayOfWeek, conn, tableName);
                                                    sameMinuteLegs.clear();
                                                }

                                                sameMinuteLegs.add(journeyLeg);
                                                departureTime = newDepartureTime;
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
                                    groupAndInsertJourneyLegsByMinute(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, dayOfWeek, conn,tableName);
                                }
                            }
                        }
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * We had a problem with plotting buses that started before midnight (00:00), as while they may have started on
     * sunday, they may move over to a monday. To solve this, we use example dates to emulate the crossing over of a day.
     *
     * rather than seeing monday,tuesday etc.. we now use Timestrings to emulate a specific date being moved to.
     *
     *
     * @param day, used to get correct basedate using the switch statement
     * @param time need to use this as time value can change if we need to distribute time over columns
     * @return returns basedate value represented as a timestring
     */
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
            baseDate = baseDate.plusDays(1);
        }

        return baseDate;
    }

    /**
     * gets text of child element with specified tag name from a parent element
     *
     * @param parent the parent element to search through
     * @param tagName the tag name of the child element whose content is going to be retrieved
     * @return the text of the first matching child element, or null if not found
     */
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

    /**
     * extracts and returns a list of days of week the bus is active.
     *
     *
     *
     * @param doc the XML document
     * @return a list of the days of week the bus is active.
     */
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

    /**
     *
     * get the departure time value for a given journey code. It does this by searching for a "VehicleJourney"
     * element that contains the journeyCodeValue and from this gets the corresponding departure time and returns it.
     *
     * @param filePath path to the XML file
     * @param journeyCodeValue journey code to match
     * @return departure time for a journey code.
     * @throws Exception
     */

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

    /**
     *  inserts journey leg departures into the db. It groups legs that occur within the same minute. This is important for
     *  distributing time over trips that happen within the same minute.
     *
     * @param journeyLegs
     * @param routeId current route ID
     * @param journeyPatternRef current journey pattern ref
     * @param vehicleJourneyRef current vehicle journey code
     * @param daysOfWeek days of week in current leg
     * @param conn database connection
     * @param tableName name of the table
     */
    private static void groupAndInsertJourneyLegsByMinute(List<JourneyLegDeparture> journeyLegs, String routeId, String journeyPatternRef, String vehicleJourneyRef, String daysOfWeek, Connection conn, String tableName) {
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
                processLegs(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn,tableName);

                // start a new group for the current leg
                sameMinuteLegs.clear();
                sameMinuteLegs.add(leg);
                currentMinute = legMinute;
            }
        }

        // process the final group of legs after the loop
        if (!sameMinuteLegs.isEmpty()) {
            processLegs(sameMinuteLegs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn,tableName);
        }
    }

    /**
     *
     * helper function for distributing time within the same minute by ensuring that there is more than one leg, and then continuing with the insertion
     */
    private static void processLegs(List<JourneyLegDeparture> legs, String routeId, String journeyPatternRef, String vehicleJourneyRef, String daysOfWeek, Connection conn,String tableName) {
        // distribute times within the same minute if there is more than one leg
        if (legs.size() > 1) {
            distributeTimeWithinSameMinute(legs);
        }

        // insert the processed legs into the selected database table
        insertLegsIntoDatabase(legs, routeId, journeyPatternRef, vehicleJourneyRef, daysOfWeek, conn, tableName);
    }

    /**
     *
     * as previously explained , equally distributes time over columns with same time value.
     *
     *
     * @param sameMinuteLegs
     */
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

    /**
     * used to identify the table that the data is going to be inserted into.
     *
     *
     * @param dayOfWeek String storing the day of week value used to identify table
     * @return
     */
    private static String determineTableForDays(String dayOfWeek) {
    switch (dayOfWeek) {
        case "Monday":
            return "monday_routes";
        case "Tuesday":
            return "tuesday_routes";
        case "Wednesday":
            return "wednesday_routes";
        case "Thursday":
            return "thursday_routes";
        case "Friday":
            return "friday_routes";
        case "Saturday":
            return "saturday_routes";
        case "Sunday":
            return "sunday_routes";
        default:
            throw new IllegalArgumentException("Invalid day of the week: " + dayOfWeek);
    }
}

    /**
     *
     *
     * Uses an sql query to insert data into table for each leg in journey legs.
     *
     *
     *
     *
     * @param journeyLegs iterated over for how ever many journey legs are found in the XML
     * @param routeId  used to insert routeId data into DB
     * @param journeyPatternRef used to insert journeyPatternRef data into DB
     * @param vehicleJourneyRef used to insert vehicleJourneyRef into DB
     * @param dayOfWeek used to insert day of week data into DB
     * @param conn database connection
     * @param tableName used to identify table to be used for insertion
     */
    private static void insertLegsIntoDatabase(List<JourneyLegDeparture> journeyLegs, String routeId, String journeyPatternRef, String vehicleJourneyRef, String dayOfWeek, Connection conn,String tableName) {

        // iterate through each leg and insert its data into the specified table
        for (JourneyLegDeparture leg : journeyLegs) {
            String insertSQL = "INSERT INTO " + tableName + " (route_id, journey_pattern_ref, Vehicle_journey_code, from_stop, to_stop, day_of_week, departure_time, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, routeId);
                pstmt.setString(2, journeyPatternRef);
                pstmt.setString(3, vehicleJourneyRef);
                pstmt.setString(4, leg.getFromStop());
                pstmt.setString(5, leg.getToStop());
                pstmt.setString(6, dayOfWeek);
                pstmt.setString(7, leg.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                pstmt.setDate(8, Date.valueOf(leg.getDate()));
                pstmt.executeUpdate();
                System.out.println("Inserted successfully for " + dayOfWeek + ".");
            } catch (SQLException e) {
                System.err.println("Error inserting leg into the database for " + dayOfWeek + ":");
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * get and set mathods to access private variables.
     *
     */
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
