package com.project.busfinder.Mapping_util;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sothawo.mapjfx.Coordinate;

import static com.project.busfinder.Mapping_util.simulateBusLocations.*;
import static com.project.busfinder.helperFunctions.getStopName.StopName;

public class BusRoutePrediction {
    private static final String API_KEY = "DMANLGTWEFKcXIN9XsEUfc89vLjfOKY1";
    private static final long REQUEST_DELAY_MS = 400;

    public static void main(String[] args) {
        String routeID = "33";
        String vehicleJourneyCode = "VJ_1";
        String day = "Thursday";
        initializeDatabaseConnection();

        // create a new prediction object
        BusRoutePrediction prediction = new BusRoutePrediction();

        try {
            // call the method to get predicted bus times
            prediction.predictBusRouteTimes(routeID, vehicleJourneyCode, day);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> predictBusRouteTimes(String routeId, String vehicleJourneyCode, String day) throws SQLException {
        List<String> predictedTimes = new ArrayList<>();  // list to hold predicted times

        // get journey legs based on route and day
        List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, day);
        StopService stopService = new StopService();

        LocalDateTime currentTime = journeyLegs.get(0).getDepartureDate().atTime(journeyLegs.get(0).getDepartureTime());

        for (JourneyLeg currentLeg : journeyLegs) {
            Coordinate toStopCoordinates = stopService.getCoordinates(currentLeg.getToStop());

            // build the url for the tomtom api request
            String urlStr = buildTomTomApiUrl(currentLeg, toStopCoordinates);

            try {
                // send request to the tomtom api
                JsonObject response = sendTomTomApiRequest(urlStr);

                if (response != null) {
                    long travelTimeInSeconds = parseTravelTimeInSeconds(response);

                    // calculate expected arrival time
                    currentTime = currentTime.plusSeconds(travelTimeInSeconds);

                    String expectedArrivalTime = ((LocalDateTime) currentTime).format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    String stopName = StopName(currentLeg.getFromStop());
                    String displayText = stopName + " - " + expectedArrivalTime;
                    predictedTimes.add(displayText);  // add to the list
                }

                // delay to avoid api rate limit
                Thread.sleep(REQUEST_DELAY_MS);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return predictedTimes;  // return the list of predicted times
    }

    private long parseTravelTimeInSeconds(JsonObject response) {
        // get travel time from the api response
        JsonArray routes = response.getAsJsonArray("routes");
        if (routes.size() > 0) {
            JsonObject route = routes.get(0).getAsJsonObject();
            JsonObject summary = route.getAsJsonObject("summary");

            return summary.get("travelTimeInSeconds").getAsLong();
        }
        return 0;
    }

    private String buildTomTomApiUrl(JourneyLeg leg, Coordinate toStopCoordinates) {
        String baseUrl = "https://api.tomtom.com/routing/1/calculateRoute";

        // build coordinates for the request
        String coordinates = String.format("%f,%f:%f,%f",
                leg.getLatitude(), leg.getLongitude(),
                toStopCoordinates.getLatitude(), toStopCoordinates.getLongitude());

        return String.format("%s/%s/json?computeBestOrder=false&traffic=true&travelMode=bus&vehicleCommercial=true&key=%s",
                baseUrl, coordinates, API_KEY);
    }

    private JsonObject sendTomTomApiRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 429) {
            System.out.println("Rate limit exceeded. Retrying after delay...");
            Thread.sleep(5000); // wait before retrying
            return sendTomTomApiRequest(urlStr);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return JsonParser.parseString(content.toString()).getAsJsonObject();
    }
}
