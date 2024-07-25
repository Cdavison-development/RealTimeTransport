package com.project.busfinder.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.io.StringReader;
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

    public static void fetchAndProcessResponse() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String parameters = "api_key=" + API_KEY + "&operatorRef=AMSY"; //AMSY being the operator ref for Arriva merseyside
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Posts_API_URL + "?" + parameters))
                .header("accept", "*/*")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            processXmlResponse(response.body());
            //System.out.println(response.body());
        } else {
            System.out.println("Failed to get a valid response. Status Code: " + response.statusCode());
        }
    }

    public static ArrayList<String> processXmlResponse(String xml) throws IOException, InterruptedException {
        ArrayList<String> liveRoutes = new ArrayList<>();
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
            liveRoutes = new ArrayList<String>();

            for (int i = 0; i < vehicleActivities.getLength(); i++) {
                Element vehicleActivity = (Element) vehicleActivities.item(i);
                String recordedAtTime = xpath.evaluate(".//siri:RecordedAtTime", vehicleActivity);
                String operatorRef = xpath.evaluate(".//siri:OperatorRef", vehicleActivity);
                String latitude = xpath.evaluate(".//siri:Latitude", vehicleActivity);
                String longitude = xpath.evaluate(".//siri:Longitude", vehicleActivity);
                String line_ref = xpath.evaluate(".//siri:LineRef", vehicleActivity);
                String JourneyCode = xpath.evaluate(".//siri:JourneyCode", vehicleActivity);
                liveRoutes.add(line_ref);
                System.out.printf("Route %s, latitude: %s, longitude: %s, Journey code : %s", line_ref, latitude, longitude, JourneyCode);

            }
            //System.out.println("\nliveroutes size before retainALL" + liveRoutes.size());

            liveRoutes.retainAll(dbRoutes);
            //System.out.println("liveroutes size after retainALL" + liveRoutes.size());
            //remove duplicates
            Set<String> set = new HashSet<>(liveRoutes);
            liveRoutes.clear();
            liveRoutes.addAll(set);
            //System.out.println("liveroutes after remove duplications retainALL" + liveRoutes.size());
        } catch (Exception e) {
            System.out.println("Error parsing XML: " + e.getMessage());
        }


        return liveRoutes;
    }


}
