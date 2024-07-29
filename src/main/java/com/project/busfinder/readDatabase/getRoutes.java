package com.project.busfinder.readDatabase;


import com.project.busfinder.util.readLiveLocation;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;


import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;

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
        getLiveRoutes();
    }

    public static void getLiveRoutes() throws IOException, InterruptedException {

        ArrayList<AbstractMap.SimpleEntry<String, String>> routes = readLiveLocation.processXmlResponse(fetchAndProcessResponse());
        //System.out.println(routes.get(0));

        String route = routes.get(0).getKey();
        String pattern = routes.get(0).getValue();

        //findVehicleJourneyRef(pattern);
        //System.out.println(route_1);




    }
}
