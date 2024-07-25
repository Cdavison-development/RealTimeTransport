package com.project.busfinder.util;

import org.junit.jupiter.api.Test;

import static com.project.busfinder.util.XMLtoTimetableFunctions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XMLtoTimeTableFunctionsTest {


    @Test
    public void findAllJourneyCodesInDirectoryTest() throws Exception {
        String directoryPath = "data/routetest";

        // Iterate through each file in the directory
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());

            for (Path file : files) {
                String filepath = file.toString();
                List<String> expectedJourneyCodes = getExpectedJourneyCodes(filepath);

                if (expectedJourneyCodes != null) {
                    List<String> actualJourneyCodes = findAllJourneyCodes(filepath);
                    assertEquals(expectedJourneyCodes, actualJourneyCodes, "Failed for file: " + filepath);
                }
            }
        }
    }

    private List<String> getExpectedJourneyCodes(String filepath) {

        if (filepath.contains("AMSY_144")) {
            return Arrays.asList("1001", "1003", "1005", "1007", "1009", "1011", "1013", "1015", "1017", "1019");
        } else if (filepath.contains("AMSY_6")) {
            return Arrays.asList("1001", "1002");
        } else if (filepath.contains("AMSY_9")) {
            return Arrays.asList("1001", "1003", "1005", "1009", "1007", "1011", "1013", "1015", "1017", "1019", "1021", "1023", "1025", "1027", "1029", "1031", "1033", "1035", "1037", "1039", "1041", "1043", "1045", "1047", "1049", "1051", "1053");
        }
        return null;
    }

    @Test
    public void findVehicleJourneyRefTest() throws Exception {
        // Sample XML content
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <VehicleJourneys>
                <VehicleJourney>
                    <TicketMachine>
                        <JourneyCode>1001</JourneyCode>
                    </TicketMachine>
                    <JourneyPatternRef>PatternA</JourneyPatternRef>
                </VehicleJourney>
                <VehicleJourney>
                    <TicketMachine>
                        <JourneyCode>1002</JourneyCode>
                    </TicketMachine>
                    <JourneyPatternRef>PatternB</JourneyPatternRef>
                </VehicleJourney>
            </VehicleJourneys>
            """;


        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), xmlContent.getBytes());


        String targetJourneyCode = "1001";
        String expectedJourneyPatternRef = "PatternA";

        String actualJourneyPatternRef = findVehicleJourneyRef(tempFile.getAbsolutePath(), targetJourneyCode);
        assertEquals(expectedJourneyPatternRef, actualJourneyPatternRef);


        targetJourneyCode = "1002";
        expectedJourneyPatternRef = "PatternB";

        actualJourneyPatternRef = findVehicleJourneyRef(tempFile.getAbsolutePath(), targetJourneyCode);
        assertEquals(expectedJourneyPatternRef, actualJourneyPatternRef);


        targetJourneyCode = "9999";
        expectedJourneyPatternRef = null;

        actualJourneyPatternRef = findVehicleJourneyRef(tempFile.getAbsolutePath(), targetJourneyCode);
        assertEquals(expectedJourneyPatternRef, actualJourneyPatternRef);
    }

    @Test
    public void parseJourneyPatternSectionsTest() throws Exception {

        String filePath = "data/routetest/AMSY_786A.xml";


        String initialDepartureTime = "08:10";


        List<Map<String, String>> expectedLinks = new ArrayList<>();

        Map<String, String> link1 = new HashMap<>();
        link1.put("From_Activity", "pickUp");
        link1.put("From_StopPointRef", "2800S48054B");
        link1.put("From_TimingStatus", "principalTimingPoint");
        link1.put("To_StopPointRef", "2800S48090A");
        link1.put("To_TimingStatus", "otherPoint");
        link1.put("RouteLinkRef", "rl_0000_1");
        link1.put("JourneyPatternRef", "jp_1");
        link1.put("RunTime", "PT1M");
        link1.put("UpdatedDepartureTime", "08:11");
        expectedLinks.add(link1);

        Map<String, String> link2 = new HashMap<>();
        link2.put("From_StopPointRef", "2800S48090A");
        link2.put("From_TimingStatus", "otherPoint");
        link2.put("To_StopPointRef", "2800S48176B");
        link2.put("To_TimingStatus", "otherPoint");
        link2.put("RouteLinkRef", "rl_0000_2");
        link2.put("JourneyPatternRef", "jp_1");
        link2.put("RunTime", "PT1M");
        link2.put("UpdatedDepartureTime", "08:12");
        expectedLinks.add(link2);

        Map<String, String> link3 = new HashMap<>();
        link3.put("From_StopPointRef", "2800S48176B");
        link3.put("From_TimingStatus", "otherPoint");
        link3.put("To_Activity", "setDown");
        link3.put("To_StopPointRef", "2800S48119A");
        link3.put("To_TimingStatus", "principalTimingPoint");
        link3.put("RouteLinkRef", "rl_0000_3");
        link3.put("JourneyPatternRef", "jp_1");
        link3.put("RunTime", "PT11M");
        link3.put("UpdatedDepartureTime", "08:23");
        expectedLinks.add(link3);


        List<Map<String, String>> actualLinks = XMLtoTimetableFunctions.parseJourneyPatternSections(filePath, initialDepartureTime);
        assertEquals(expectedLinks, actualLinks);
    }

    @Test
    public void departureTimesTest() throws Exception {
        // Sample XML content
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <VehicleJourneys>
                <VehicleJourney>
                    <JourneyPatternRef>jp_2</JourneyPatternRef>
                    <DepartureTime>08:00:00</DepartureTime>
                </VehicleJourney>
                <VehicleJourney>
                    <JourneyPatternRef>jp_3</JourneyPatternRef>
                    <DepartureTime>09:00:00</DepartureTime>
                </VehicleJourney>
            </VehicleJourneys>
            """;


        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), xmlContent.getBytes());


        String journeyPatternRef = "jp_2";
        String expectedDepartureTime = "08:00:00";
        String actualDepartureTime = XMLtoTimetableFunctions.getDepartureTime(tempFile.getAbsolutePath(), journeyPatternRef);
        assertEquals(expectedDepartureTime, actualDepartureTime);


        journeyPatternRef = "jp_3";
        expectedDepartureTime = "09:00:00";
        actualDepartureTime = XMLtoTimetableFunctions.getDepartureTime(tempFile.getAbsolutePath(), journeyPatternRef);
        assertEquals(expectedDepartureTime, actualDepartureTime);


        journeyPatternRef = "jp_4";
        expectedDepartureTime = null;
        actualDepartureTime = XMLtoTimetableFunctions.getDepartureTime(tempFile.getAbsolutePath(), journeyPatternRef);
        assertEquals(expectedDepartureTime, actualDepartureTime);
    }
}
