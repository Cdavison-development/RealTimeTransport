package com.project.busfinder.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

    static String API_KEY = "19f45ab4075ee6ba01144659bd9c987468a00212";
    private static final String Posts_API_URL = "https://data.bus-data.dft.gov.uk/api/v1/datafeed/";


    public static void main(String[] args) {
        try {
            fetchAndProcessResponse();
        } catch (Exception e) {
            System.err.println("Error during API fetch and process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String fetchAndProcessResponse() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String parameters = "api_key=" + API_KEY + "&operatorRef=AMSY"; //AMSY being the operator ref for Arriva merseyside
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Posts_API_URL + "?" + parameters))
                .header("accept", "*/*")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            //processXmlResponse(response.body());
            return response.body();
        } else {
            System.out.println("Failed to get a valid response. Status Code: " + response.statusCode());
            return null;
        }
    }

    public static ArrayList<AbstractMap.SimpleEntry<String, String>> processXmlResponse(String xml) throws IOException, InterruptedException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> liveRoutes = new ArrayList<>();
        ArrayList<String> dbRoutes = ReadFromDatabase.readRoutes();
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
            for (int i = 0; i < vehicleActivities.getLength(); i++) {
                Element vehicleActivity = (Element) vehicleActivities.item(i);
                String recordedAtTime = xpath.evaluate(".//siri:RecordedAtTime", vehicleActivity);
                String operatorRef = xpath.evaluate(".//siri:OperatorRef", vehicleActivity);
                String latitude = xpath.evaluate(".//siri:Latitude", vehicleActivity);
                String longitude = xpath.evaluate(".//siri:Longitude", vehicleActivity);
                String line_ref = xpath.evaluate(".//siri:LineRef", vehicleActivity);
                String journeyRef = xpath.evaluate(".//siri:DatedVehicleJourneyRef", vehicleActivity);

                // some journeyRefs are returning as a 5000 instead of 1000, ex: 62=5044. This is erroneus and should be 1044
                // Adjust journeyRef if it does not start with '1'
                if (!journeyRef.startsWith("1")) {
                    journeyRef = "1" + journeyRef.substring(1);
                }

                if (dbRoutes.contains(line_ref)) {
                    liveRoutes.add(new AbstractMap.SimpleEntry<>(line_ref, journeyRef));
                }
                System.out.printf("Route %s, latitude: %s, longitude: %s, Journey ref : %s\n", line_ref, latitude, longitude, journeyRef);
                count++;
            }
        } catch (Exception e) {
            System.out.println("Error parsing XML: " + e.getMessage());
        }
        // some journeyRefs are returning as a 5000 instead of 1000, ex: 62=5044. This is erroneus and should be 1044
        System.out.println(liveRoutes);
        return liveRoutes;
    }


}
