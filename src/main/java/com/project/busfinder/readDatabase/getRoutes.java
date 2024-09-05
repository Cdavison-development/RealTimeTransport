package com.project.busfinder.readDatabase;


import com.project.busfinder.Mapping_util.LiveRouteInfo;
import javafx.util.Pair;

import java.io.IOException;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;
import static com.project.busfinder.util.readLiveLocation.processXmlResponse;

/**
 *
 * will return a list of all trackable buses that will be used in the combobox
 *
 * This will be done by cross referencing bus routes returned by the API, with the bus
 * route data in the database
 *
 *
 *
 *
 *  find matching Route Names between API and database. From there, match API journey ref with journey pattern
 *
 *
 *  The combobox will have all available routes live or not. some kind of indicator will be added to live routes and
 *  non-live routes so the user knows which routes are active.
 *
 *  once an option has been selected, another combo box will appear, allowing the user to select departure time, with
 *  the top option being Track Live, all other options being the vehicle location at x time.
 */
public class getRoutes {

    public static void main(String[] args) throws IOException, InterruptedException {
        String dbPath = "jdbc:sqlite:data/databases/routes.db";

        try {

            Connection conn = DriverManager.getConnection(dbPath);


            getLiveRoutes(conn);


            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * retrieves live route information and checks it against existing route and journey data in the database.
     * returns a map where each route ID is associated with a list of pairs. each pair contains a boolean indicating
     * whether the journey is live (true) or not (false), and the journey code.
     *
     *
     * @param conn database connection object
     * @return map linking route ids with lists of pairs indicating live status and journeycode
     * @throws IOException
     * @throws InterruptedException
     */
    public static Map<String, List<Pair<Boolean, String>>> getLiveRoutes(Connection conn) throws IOException, InterruptedException {
        //get live route information
        List<LiveRouteInfo> routeInfoList = processXmlResponse(fetchAndProcessResponse());
        Map<String, List<Pair<Boolean, String>>> routeExistsMap = new HashMap<>();
        System.out.println("routes :  " + routeInfoList);
    // query database to get all route ids and initialize the map
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT route_id, journey_code FROM journeyCode")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String route_id = rs.getString("route_id");
                routeExistsMap.put(route_id, new ArrayList<>());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // check each live route against the database and update map
        for (LiveRouteInfo routeInfo : routeInfoList) {
            String route = routeInfo.getLineRef();
            String journeyRef = routeInfo.getJourneyRef();

            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM journeyCode WHERE route_id = ? AND journey_code = ?")) {
                pstmt.setString(1, route);
                pstmt.setString(2, journeyRef);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String journeyCode = rs.getString("journey_code");
                    String route_id = rs.getString("route_id");

                    // add live status and journey code to map
                    List<Pair<Boolean, String>> liveInfoList = routeExistsMap.get(route_id);
                    if (liveInfoList != null) {
                        liveInfoList.add(new Pair<>(true, journeyCode));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // mark routes that do not have live journeys with a false status
        for (Map.Entry<String, List<Pair<Boolean, String>>> entry : routeExistsMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                entry.getValue().add(new Pair<>(false, null));
            }
        }

        return routeExistsMap;
    }
}
