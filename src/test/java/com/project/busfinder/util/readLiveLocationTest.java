package com.project.busfinder.util;


import com.project.busfinder.helperFunctions.ReadFromDatabase;
import com.project.busfinder.helperFunctions.dbHelper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class readLiveLocationTest {

    @Test
    public void testProcessXmlResponse() throws Exception {

        dbHelper readLiveLocation = new dbHelper();

        String xmlData = """
<Siri xmlns="http://www.siri.org.uk/siri" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.siri.org.uk/siri http://www.siri.org.uk/schema/2.0/xsd/siri.xsd" version="2.0">
    <ServiceDelivery>
        <ResponseTimestamp>2024-07-17T12:00:14.383970+00:00</ResponseTimestamp>
        <ProducerRef>ItoWorld</ProducerRef>
        <VehicleMonitoringDelivery>
            <ResponseTimestamp>2024-07-17T12:00:14.383970+00:00</ResponseTimestamp>
            <RequestMessageRef>4bccea6b-ab45-4f9b-ab1c-83fcd1f395a3</RequestMessageRef>
            <ValidUntil>2024-07-17T12:05:14.383970+00:00</ValidUntil>
            <ShortestPossibleCycle>PT5S</ShortestPossibleCycle>
            <VehicleActivity>
                <RecordedAtTime>2024-07-17T12:00:01+00:00</RecordedAtTime>
                <ItemIdentifier>b1569f63-a92b-44ad-b716-7046ae0458f9</ItemIdentifier>
                <ValidUntilTime>2024-07-17T12:05:14.384179</ValidUntilTime>
                <MonitoredVehicleJourney>
                    <LineRef>418</LineRef>
                    <DirectionRef>outbound</DirectionRef>
                    <FramedVehicleJourneyRef>
                        <DataFrameRef>2024-07-17</DataFrameRef>
                        <DatedVehicleJourneyRef>1015</DatedVehicleJourneyRef>
                    </FramedVehicleJourneyRef>
                    <PublishedLineName>418</PublishedLineName>
                    <OperatorRef>AMSY</OperatorRef>
                    <OriginRef>2800S27026A</OriginRef>
                    <OriginName>Pollitt_Square</OriginName>
                    <DestinationRef>2800S27026A</DestinationRef>
                    <DestinationName>Pollitt_Square</DestinationName>
                    <OriginAimedDepartureTime>2024-07-17T11:31:00+00:00</OriginAimedDepartureTime>
                    <DestinationAimedArrivalTime>2024-07-17T13:26:00+00:00</DestinationAimedArrivalTime>
                    <VehicleLocation>
                        <Longitude>-3.094135</Longitude>
                        <Latitude>53.370001</Latitude>
                    </VehicleLocation>
                    <Bearing>292.0</Bearing>
                    <BlockRef>1030</BlockRef>
                    <VehicleRef>2957</VehicleRef>
                </MonitoredVehicleJourney>
                <Extensions>
                    <VehicleJourney>
                        <Operational>
                            <TicketMachine>
                                <TicketMachineServiceCode>418</TicketMachineServiceCode>
                                <JourneyCode>1231</JourneyCode>
                            </TicketMachine>
                        </Operational>
                        <VehicleUniqueId>2957</VehicleUniqueId>
                    </VehicleJourney>
                </Extensions>
            </VehicleActivity>
        </VehicleMonitoringDelivery>
    </ServiceDelivery>
</Siri>
""";
        ArrayList<String> liveRoutes = readLiveLocation.processXmlResponse(xmlData);
        ArrayList<String> dbRoutes = ReadFromDatabase.readRoutes();

        boolean containsAll = dbRoutes.containsAll(liveRoutes);
        System.out.println("Database routes contain all live routes: " + containsAll);
    }


}
