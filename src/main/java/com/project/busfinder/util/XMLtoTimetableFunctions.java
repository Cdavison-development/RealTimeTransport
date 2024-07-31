package com.project.busfinder.util;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 *
 * we did not realise that different Journeycodes/services may use the same route pattern. for each journey code
 *
 *
 * create function to find all vehicle codes
 *
 * use vehicle Journey code to identify Journey Pattern ref and Journey code
 *
 *  this function may be obsolete due to processXML function, leave in for now
 *
 */
public class XMLtoTimetableFunctions{


    public static void main(String[] args) throws Exception {
        String filePath = "data/routes/AMSY_G10S_AMSYPC000114157610B_20240123_-_1808719.xml";
        //String JourneyCode = findAllJourneyCodes(filePath);
        //String journeyRefs = findAllVehicleJourneyRefs(filePath, "1003");
        String departureTime = getDepartureTime(filePath, "1003");
        System.out.println(departureTime);

        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            String journeyCode = "1001";
            Element section = findJourneyPatternSection(doc, journeyCode);
            if (section != null) {
                printElement(section);
            } else {
                System.out.println("JourneyPatternSection not found for JourneyCode: " + journeyCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Element findJourneyPatternSection(Document doc, String journeyCode) {
        NodeList vehicleJourneyList = doc.getElementsByTagName("VehicleJourney");
        for (int i = 0; i < vehicleJourneyList.getLength(); i++) {
            Element vehicleJourney = (Element) vehicleJourneyList.item(i);
            String code = getTextContent(vehicleJourney, "JourneyCode");
            if (journeyCode.equals(code)) {
                String journeyPatternRef = getTextContent(vehicleJourney, "JourneyPatternRef");
                NodeList journeyPatternList = doc.getElementsByTagName("JourneyPattern");
                for (int j = 0; j < journeyPatternList.getLength(); j++) {
                    Element journeyPattern = (Element) journeyPatternList.item(j);
                    if (journeyPattern.getAttribute("id").equals(journeyPatternRef)) {
                        String sectionRef = getTextContent(journeyPattern, "JourneyPatternSectionRefs");
                        NodeList sectionList = doc.getElementsByTagName("JourneyPatternSection");
                        for (int k = 0; k < sectionList.getLength(); k++) {
                            Element section = (Element) sectionList.item(k);
                            if (section.getAttribute("id").equals(sectionRef)) {
                                return section;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private static void printElement(Element element) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(element);
            StreamResult result = new StreamResult(System.out);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
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
        boolean inTicketMachine = false;

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
                                journeyCodes.add(event.asCharacters().getData());
                            }
                        }
                        break;
                }
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals("TicketMachine")) {
                    inTicketMachine = false;
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
        String sectionPrefix = "";
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
                        sectionPrefix = elementName;
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
                    links.add(timingLink);
                }
                timingLink = null;
                sectionPrefix = "";
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
                    if (journeyCodevalue.equals(currentRef)) {
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

