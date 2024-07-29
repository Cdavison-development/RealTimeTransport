package com.project.busfinder.util;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Characters;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.time.Duration;


/**
 *
 * we did not realise that different Journeycodes/services may use the same route pattern. for each journey code
 *
 *
 * create function to find all vehicle codes
 *
 * use vehicle Journey code to identify Journey Pattern ref and Journey code
 *
 * for this, we may not need to
 *
 */
public class XMLtoTimetableFunctions{


    public static void main(String[] args) throws Exception {
        String filePath = "data/routes/AMSY_G10S_AMSYPC000114157610B_20240123_-_1808719.xml";
        //String JourneyCode = findAllJourneyCodes(filePath);
        //String journeyRefs = findAllVehicleJourneyRefs(filePath, "1003");
        String departureTime = getDepartureTime(filePath, "1003");
        System.out.println(departureTime);
    }
    /**
     *
     *
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public static List<String> findAllJourneyCodes(String filePath) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));

        List<String> journeyCodes = new ArrayList<>();
        boolean inTicketMachine = false; // Flag to check if inside a TicketMachine element

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                switch (elementName) {
                    case "TicketMachine":
                        inTicketMachine = true; // Parsing inside <TicketMachine>
                        break;
                    case "JourneyCode":
                        if (inTicketMachine) {
                            event = eventReader.nextEvent();
                            if (event.isCharacters()) {
                                journeyCodes.add(event.asCharacters().getData()); // Add JourneyCode to list
                            }
                        }
                        break;
                }
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals("TicketMachine")) {
                    inTicketMachine = false; // No longer parsing inside <TicketMachine>
                }
            }
        }
        System.out.println(journeyCodes);
        return journeyCodes;
    }


    /**
     *
     *
     *
     * @param filePath
     * @param targetJourneyCode
     * @return
     * @throws Exception
     */
    public static String findAllVehicleJourneyRefs(String filePath, String targetJourneyCode) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));

        boolean inTicketMachine = false;
        String currentJourneyCode = null;
        String JourneyPatternRef = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                switch (elementName) {
                    case "TicketMachine":
                        inTicketMachine = true;
                        break;
                    case "JourneyCode":
                        if (inTicketMachine) {
                            event = eventReader.nextEvent();
                            if (event.isCharacters()) {
                                currentJourneyCode = event.asCharacters().getData();
                            }
                        }
                        break;
                    case "VehicleJourneyCode":
                        event = eventReader.nextEvent();
                        if (event.isCharacters()) {
                            JourneyPatternRef = event.asCharacters().getData();
                        }
                        break;
                }
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String elementName = endElement.getName().getLocalPart();

                if (elementName.equals("TicketMachine")) {
                    inTicketMachine = false;
                } else if (elementName.equals("VehicleJourney") && currentJourneyCode != null && currentJourneyCode.equals(targetJourneyCode)) {
                    System.out.println(JourneyPatternRef);
                    return JourneyPatternRef;
                }
            }
        }

        return null;
    }

    /**
     *
     * get vehicleJourney Code and journey code. Use that to identify
     *
     *
     * @param filePath
     * @param targetJourneyCode
     * @return
     * @throws Exception
     */
    public static String findAllVehicleJourneyCodes(String filePath, String targetJourneyCode) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));

        boolean inTicketMachine = false;
        String currentJourneyCode = null;
        String VehicleJourneyCode = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                switch (elementName) {
                    case "TicketMachine":
                        inTicketMachine = true;
                        break;
                    case "JourneyCode":
                        if (inTicketMachine) {
                            event = eventReader.nextEvent();
                            if (event.isCharacters()) {
                                currentJourneyCode = event.asCharacters().getData();
                            }
                        }
                        break;
                    case "VehicleJourneyCode":
                        event = eventReader.nextEvent();
                        if (event.isCharacters()) {
                            VehicleJourneyCode = event.asCharacters().getData();
                        }
                        break;
                }
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String elementName = endElement.getName().getLocalPart();

                if (elementName.equals("TicketMachine")) {
                    inTicketMachine = false;
                } else if (elementName.equals("VehicleJourney") && currentJourneyCode != null && currentJourneyCode.equals(targetJourneyCode)) {
                    System.out.println(VehicleJourneyCode);
                    return VehicleJourneyCode;
                }
            }
        }

        return null;
    }

    public static String findJourneyCodes(String filePath, String targetJourneyCode) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));

        boolean inTicketMachine = false;
        String currentJourneyCode = null;
        String JourneyPatternRef = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                switch (elementName) {
                    case "TicketMachine":
                        inTicketMachine = true;
                        break;
                    case "JourneyCode":
                        if (inTicketMachine) {
                            event = eventReader.nextEvent();
                            if (event.isCharacters()) {
                                currentJourneyCode = event.asCharacters().getData();
                            }
                        }
                        break;
                    case "JourneyPatternRef":
                        event = eventReader.nextEvent();
                        if (event.isCharacters()) {
                            JourneyPatternRef = event.asCharacters().getData();
                        }
                        break;
                }
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String elementName = endElement.getName().getLocalPart();

                if (elementName.equals("TicketMachine")) {
                    inTicketMachine = false;
                } else if (elementName.equals("VehicleJourney") && currentJourneyCode != null && currentJourneyCode.equals(targetJourneyCode)) {
                    System.out.println(currentJourneyCode);
                    return currentJourneyCode;
                }
            }
        }
        return null;
    }
/**
    public static String findSingleVehicleJourneyRefs(String targetJourneyCode) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));

        boolean inTicketMachine = false;
        String currentJourneyCode = null;
        String JourneyPatternRef = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                switch (elementName) {
                    case "TicketMachine":
                        inTicketMachine = true;
                        break;
                    case "JourneyCode":
                        if (inTicketMachine) {
                            event = eventReader.nextEvent();
                            if (event.isCharacters()) {
                                currentJourneyCode = event.asCharacters().getData();
                            }
                        }
                        break;
                    case "JourneyPatternRef":
                        event = eventReader.nextEvent();
                        if (event.isCharacters()) {
                            JourneyPatternRef = event.asCharacters().getData();
                        }
                        break;
                }
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String elementName = endElement.getName().getLocalPart();

                if (elementName.equals("TicketMachine")) {
                    inTicketMachine = false;
                } else if (elementName.equals("VehicleJourney") && currentJourneyCode != null && currentJourneyCode.equals(targetJourneyCode)) {
                    System.out.println(JourneyPatternRef);
                    return JourneyPatternRef;
                }
            }
        }

        return null;
    }
 **/
    /**
     *
     *
     * @param filePath
     * @param initialDepartureTime
     * @return
     * @throws Exception
     */
    public static List<Map<String, String>> parseJourneyPatternSections(String filePath,String initialDepartureTime) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));
        List<Map<String, String>> links = new ArrayList<>();
        Map<String, String> timingLink = null;
        String sectionPrefix = "";  // To differentiate between "From" and "To"
        //LocalTime departureTime = LocalTime.parse(initialDepartureTime);
        Map<String, LocalTime> departureTimes = new HashMap<>();
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();
                switch (elementName) {
                    case "JourneyPatternTimingLink":
                        timingLink = new HashMap<>();
                        break;
                    case "From":
                    case "To":
                        sectionPrefix = elementName;  // Set prefix to "From" or "To"
                        break;
                    case "Activity":
                    case "StopPointRef":
                    case "TimingStatus":
                        if (timingLink == null) {
                            timingLink = new HashMap<>();
                        }
                        event = eventReader.nextEvent();
                        if (event.isCharacters()) {
                            Characters characters = (Characters) event;
                            timingLink.put(sectionPrefix + "_" + elementName, characters.getData());
                        }
                        break;
                    case "RouteLinkRef":
                        if (timingLink == null) {
                            timingLink = new HashMap<>();
                        }
                        Characters characters = (Characters) eventReader.nextEvent();
                        String routeLinkRef = characters.getData();
                        timingLink.put(elementName, routeLinkRef);
                        int routeSection = Integer.parseInt(routeLinkRef.split("_")[1]);
                        String journeyPatternRef = "jp_" + (routeSection + 1);
                        timingLink.put("JourneyPatternRef", journeyPatternRef);
                        // Initialise or get the current departure time for this journey pattern
                        if (!departureTimes.containsKey(journeyPatternRef)) {
                            LocalTime newDepartureTime = LocalTime.parse(getDepartureTime(filePath, journeyPatternRef));
                            departureTimes.put(journeyPatternRef, newDepartureTime);
                        }
                        break;
                    case "RunTime":
                        if (timingLink == null) {
                            timingLink = new HashMap<>();
                        }
                        characters = (Characters) eventReader.nextEvent();
                        timingLink.put(elementName, characters.getData());
                        Duration runDuration = Duration.parse(characters.getData());
                        journeyPatternRef = timingLink.get("JourneyPatternRef");
                        LocalTime updatedTime = departureTimes.get(journeyPatternRef).plus(runDuration);
                        departureTimes.put(journeyPatternRef, updatedTime);
                        timingLink.put("UpdatedDepartureTime", updatedTime.toString());
                        break;
                }
            } else if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("JourneyPatternTimingLink")) {
                if (timingLink != null) {
                    links.add(timingLink);  // Add completed link to list
                }
                timingLink = null;  // Reset the timingLink to prepare for the next one
                sectionPrefix = "";  // Reset section prefix
            }
        }
        return links;
    }

    /**
     *
     *
     *  notes for change: departure time needs to be based off journey code not journey pattern. this is because two busses starting
     *  at different times can have the same journey pattern. Currently, the code will find the first instance of the specifed journeypattern,
     *  and return the corresponding departure time, meaning any repeating journey patterns will have the same start time.
     *
     * @param filePath
     * @param
     * @return
     * @throws Exception
     */

    public static String getDepartureTime(String filePath, String journeyCodevalue) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(filePath));
        boolean isVehicleJourney = false;  // To track when we're inside the relevant vehicle journey
        String departureTime = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                if ("JourneyCode".equals(elementName)) {
                    Characters characters = (Characters) eventReader.nextEvent();
                    String currentRef = characters.getData();
                    if (journeyCodevalue.equals(currentRef)) {
                        isVehicleJourney = true;  // We're inside the relevant vehicle journey
                    }
                } else if (isVehicleJourney && "DepartureTime".equals(elementName)) {
                    Characters characters = (Characters) eventReader.nextEvent();
                    departureTime = characters.getData();
                    break;
                }
            } else if (event.isEndElement()) {
                if ("VehicleJourney".equals(event.asEndElement().getName().getLocalPart()) && isVehicleJourney) {
                    break;  // End of the relevant vehicle journey
                }
            }
        }

        return departureTime;
    }
}

