package com.project.busfinder.helperFunctions;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class dbHelper {

    /**
     * Helper function for parsing xmlData for ReadLiveLocation Testing
     */

    public ArrayList<String> processXmlResponse(String xmlData) {
        ArrayList<String> routes = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes()));

            NodeList lineRefNodes = doc.getElementsByTagName("LineRef");
            for (int i = 0; i < lineRefNodes.getLength(); i++) {
                Element lineRefElement = (Element) lineRefNodes.item(i);
                routes.add(lineRefElement.getTextContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return routes;
    }
}