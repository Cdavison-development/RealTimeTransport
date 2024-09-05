package com.project.busfinder.util;

import java.net.URI;

import java.net.URLEncoder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.io.StringReader;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.project.busfinder.Mapping_util.LiveRouteInfo;

import com.project.busfinder.helperFunctions.ReadFromDatabase;
import com.project.busfinder.helperFunctions.SiriNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import org.xml.sax.InputSource;

public class readLiveLocation {

    // my api key for OpenBusData

    static String API_KEY = "19f45ab4075ee6ba01144659bd9c987468a00212";
    private static final String Posts_API_URL = "https://data.bus-data.dft.gov.uk/api/v1/datafeed/";


    public static void main(String[] args) {
        try {


            List<LiveRouteInfo> lineAndJourneyRefs = processXmlResponse(fetchAndProcessResponse());
            System.out.println(lineAndJourneyRefs);


        } catch (IOException | InterruptedException e) {

            e.printStackTrace();
        }
    }


    /**
     *
     * fetches XML data for a given request, we request all vehicles under the operator Ref AMSY, future work includes handling more
     * operator Refs, This would be easy to implement, however time constraints mean it cannot be done at the moment
     *

     *
     *
     * @return returns the XML data from the API if the request is successful, else return null
     * @throws IOException
     * @throws InterruptedException
     */
    public static String fetchAndProcessResponse() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String parameters = "api_key=" + API_KEY + "&operatorRef=AMSY"; // AMSY being the operator ref for Arriva Merseyside

        // retry 3 times
        int maxRetries = 3;
        int attempt = 0;
        String responseBody = null;

        while (attempt < maxRetries) {
            attempt++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Posts_API_URL + "?" + parameters))
                    .header("accept", "*/*")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                responseBody = response.body();
                //System.out.println(responseBody);
                List<LiveRouteInfo> liveRoutes = processXmlResponse(responseBody);

                if (!liveRoutes.isEmpty()) {

                    return responseBody;
                } else {
                    System.out.println("No live routes found, retrying... (Attempt " + attempt + ")");
                }
            } else {
                System.out.println("Failed to get a valid response. Status Code: " + response.statusCode());
            }
            Thread.sleep(1000);
        }

        System.out.println("Failed to fetch valid data after " + maxRetries + " attempts.");
        return null;
    }

    /**
     *
     * parses the XML data returned by the api and iterates through each item in Vehicleactivities tree/block,
     * storing the data we need in relevant variables. Currently, the openBusData live location API is rather broken,
     * one of the primary problems is that data does not update. so routes that ended days ago are still live according to the
     * api, therefore we need to filter by current time, to ensure we are mapping buses that are currently live.
     *
     * @param xml takes xml returned by fetchAndProcessResponse as input
     * @return infoList of storing line_red,journeyRef, long and lat data for each live bus
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<LiveRouteInfo> processXmlResponse(String xml) throws IOException, InterruptedException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> liveRoutes = new ArrayList<>();
        ArrayList<String> dbRoutes = ReadFromDatabase.readRoutes();
        List<LiveRouteInfo> routeInfoList = new ArrayList<>();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new SiriNamespaceContext());

            NodeList vehicleActivities = (NodeList) xpath.evaluate("//siri:VehicleActivity", doc, XPathConstants.NODESET);

            int count = 0;

            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < vehicleActivities.getLength(); i++) {
                Element vehicleActivity = (Element) vehicleActivities.item(i);
                String line_ref = xpath.evaluate(".//siri:LineRef", vehicleActivity);
                String journeyRef = xpath.evaluate(".//siri:DatedVehicleJourneyRef", vehicleActivity);
                String originAimedDepartureTime = xpath.evaluate(".//siri:OriginAimedDepartureTime", vehicleActivity);
                String destinationAimedArrivalTime = xpath.evaluate(".//siri:DestinationAimedArrivalTime", vehicleActivity);
                String latitudeStr = xpath.evaluate(".//siri:Latitude", vehicleActivity);
                String longitudeStr = xpath.evaluate(".//siri:Longitude", vehicleActivity);

                if (originAimedDepartureTime == null || originAimedDepartureTime.isEmpty() ||
                        destinationAimedArrivalTime == null || destinationAimedArrivalTime.isEmpty()) {
                    System.out.println("Skipping entry due to missing time fields.");
                    continue;  // skip if times are missing
                }

                double latitude = Double.parseDouble(latitudeStr);
                double longitude = Double.parseDouble(longitudeStr);

                try {

                    LocalDateTime departureTime = OffsetDateTime.parse(originAimedDepartureTime).toLocalDateTime();
                    LocalDateTime arrivalTime = OffsetDateTime.parse(destinationAimedArrivalTime).toLocalDateTime();

                    // check if the journey times are within the current time
                    if (departureTime.isBefore(now) && arrivalTime.isAfter(now)) {
                        if (dbRoutes.contains(line_ref)) {
                            liveRoutes.add(new AbstractMap.SimpleEntry<>(line_ref, journeyRef));
                            routeInfoList.add(new LiveRouteInfo(line_ref, journeyRef, latitude, longitude));
                        }
                        count++;
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("Error parsing date-time fields: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Error parsing XML: " + e.getMessage());
        }


        System.out.println("Filtered live routes: " + liveRoutes);
        return routeInfoList;
    }

}
