package com.project.busfinder.GUI;
import com.project.busfinder.Mapping.*;
import com.project.busfinder.util.PolylineDecoder;
import com.project.busfinder.util.ResourceMonitor;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import com.sothawo.mapjfx.event.MarkerEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.project.busfinder.Mapping.polylineHelpers.findClosestSegmentIndices;
import static com.project.busfinder.Mapping.simulateBusLocations.findActiveBusesInTimeFrame;
import static com.project.busfinder.Mapping.simulateBusLocations.getJourneyLegs;
import static com.project.busfinder.util.PolylineDecoder.decodePolylinesIndividually;


// next step, return data on click
public class BusIconController {

    private MapView mapView;


    private final StopService stopService;
    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private final List<Marker> busMarkers = new ArrayList<>();
    private Map<String, Marker> MarkersWithVJC;
    private RouteService routeService;
    private List<Marker> renderedBusMarkers = new ArrayList<>();
    private List<CoordinateLine> renderedPolylines = new ArrayList<>();
    private List<CoordinateLine> allPolylines = new ArrayList<>();
    private boolean isFirstCall = true;
    private Set<Coordinate> uniqueCoordinates = new HashSet<>();


    public BusIconController(MapView mapView) {
        //initialise variables
        this.mapView = mapView;
        this.stopService = new StopService();
        MarkersWithVJC = new HashMap<>();
    }



    public void initializeMap() {
        mapView.initialize(Configuration.builder().build());
        routeService = new RouteService();
        mapView.setCenter(new Coordinate(53.4013, -3.057244));  // set the initial map centre
        mapView.setZoom(12);  // set the initial zoom level

        // handle marker click events
        mapView.addEventHandler(MarkerEvent.MARKER_CLICKED, event -> {
            event.consume();
            Marker clickedMarker = event.getMarker();
            System.out.println("Marker clicked with ID: " + clickedMarker.getId());

            System.out.println("MarkersWithVJC contains the following markers:");
            for (Map.Entry<String, Marker> entry : MarkersWithVJC.entrySet()) {
                String vehicleJourneyCode = entry.getKey();
                Marker marker = entry.getValue();
                System.out.println("Vehicle Journey Code: " + vehicleJourneyCode
                        + ", Marker ID: " + marker.getId()
                        + ", Position: " + marker.getPosition());
            }

            System.out.println("busMarkers contains the following markers:");
            for (Marker marker : busMarkers) {
                System.out.println("Marker ID: " + marker.getId()
                        + ", Position: " + marker.getPosition());
            }
        });

        // check when the map view is fully initialised
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                System.out.println("MapView initialised.");
                // additional setup can be done here
            }
        });
    }

    private String getVehicleJourneyCodeForMarker(Marker marker) {
        for (Map.Entry<String, Marker> entry : MarkersWithVJC.entrySet()) {
            if (entry.getValue().equals(marker)) {
                return entry.getKey();
            }
        }
        return null;
    }


    public void clearMapView() {

        System.out.println("Clearing map view...");
        System.out.println("Number of markers in MarkersWithVJC: " + MarkersWithVJC.size());
        System.out.println("Number of markers in busMarkers: " + busMarkers.size());
        System.out.println("Number of rendered bus markers: " + renderedBusMarkers.size());
        System.out.println("Number of rendered polylines: " + renderedPolylines.size());

        // remove all markers from MarkersWithVJC
        if (MarkersWithVJC.isEmpty()) {
            System.out.println("No bus markers in MarkersWithVJC to remove.");
        } else {
            System.out.println("Removing " + MarkersWithVJC.size() + " bus markers from MarkersWithVJC.");
            for (Marker marker : MarkersWithVJC.values()) {
                Platform.runLater(() -> mapView.removeMarker(marker));
            }
            MarkersWithVJC.clear();
        }

        // remove all markers from busMarkers
        if (busMarkers.isEmpty()) {
            System.out.println("No bus markers in busMarkers list to remove.");
        } else {
            System.out.println("Removing " + busMarkers.size() + " bus markers from busMarkers list.");
            for (Marker marker : busMarkers) {
                Platform.runLater(() -> mapView.removeMarker(marker));
            }
            busMarkers.clear();
        }

        System.out.println("Map view cleared.");
    }

    public static CoordinateLine createCoordinateLine(List<Coordinate> coordinates) {
        // create coordiante line for a given set of coordinates
        CoordinateLine coordinateLine = new CoordinateLine(coordinates)
                .setVisible(true)
                .setColor(Color.DODGERBLUE)
                .setWidth(5);
        return coordinateLine;
    }

    public void plotIndividualPolylines(String polylineData, String selectedRoute) {
        // Decode the polyline data into individual lists of coordinates
        List<List<Coordinate>> allPolylinesCoordinates = decodePolylinesIndividually(polylineData);

        // Iterate through each list of coordinates to create and plot polylines
        for (List<Coordinate> polyline : allPolylinesCoordinates) {
            CoordinateLine coordinateLine = createCoordinateLine(polyline);

            // Check if the selected route matches, then plot the polyline on the map
            if (selectedRoute.equals(selectedRoute)) {
                allPolylines.add(coordinateLine);
                Platform.runLater(() -> mapView.addCoordinateLine(coordinateLine));
            }
        }
    }


    public void mapLiveRoutesWithJourneyInfos(String selectedRoute) throws IOException, InterruptedException, SQLException {
        // clear the map view of any existing markers and polylines
        clearMapView();

        // check if this is the first time the method is being called
        if (isFirstCall) {
            System.out.println("First call - not using selected polyline.");
            isFirstCall = false;

            // ensure the mapView is initialised before proceeding
            if (mapView == null) {
                System.err.println("MapView is not initialised. Cannot clear map view or map active buses.");
                return;
            }

            // retrieve route data for the selected route
            RouteData selectedRouteData = routeService.getRouteData(selectedRoute);
            if (selectedRouteData == null) {
                System.err.println("No RouteData found for Route ID: " + selectedRoute);
                return;
            }

            // check if polyline data is available for the selected route
            String selectedPolylineData = selectedRouteData.getPolylineData();
            if (selectedPolylineData == null) {
                System.out.println("Polyline data is null for Route ID: " + selectedRoute);
                return;
            }

            // plot the polylines on the map
            Platform.runLater(() -> plotIndividualPolylines(selectedPolylineData, selectedRoute));
        }

        // find the closest departure times for journeys
        List<JourneyInfo> journeyInfos = simulateBusLocations.findClosestDepartureTime();

        // process each journey info to map the live routes
        for (JourneyInfo journeyInfo : journeyInfos) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();

            try {
                // retrieve journey legs for the given route and vehicle journey code
                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode);
                if (!journeyLegs.isEmpty()) {
                    // retrieve route data for the given route ID
                    RouteData routeData = routeService.getRouteData(routeId);
                    System.out.println(routeService);
                    if (routeData == null) {
                        System.err.println("No RouteData found for Route ID: " + routeId);
                        continue;
                    }

                    // check if polyline data is available for the route
                    String polylineData = routeData.getPolylineData();
                    if (polylineData == null) {
                        System.out.println("Route is null: " + routeId);
                        continue;
                    }

                    // decode the polyline data into coordinates and start bus movement on the map
                    List<Coordinate> routeCoordinates = PolylineDecoder.decodeAndConcatenatePolylinesFromString(polylineData);
                    startBusMovement(journeyInfo, journeyLegs, routeCoordinates, LocalTime.now());

                    System.out.println("Resource usage after starting bus movement:");
                    ResourceMonitor.printSystemMetrics();
                } else {
                    // if no journey legs are found, log a message
                    Platform.runLater(() -> System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode));
                }
            } catch (SQLException e) {
                // handle SQL exceptions
                Platform.runLater(() -> e.printStackTrace());
            }
        }
    }

    public void mapActiveBuses(LocalTime testTime, int timeWindowMinutes, String selectedRoute) throws SQLException, IOException, InterruptedException {

        // clear the map view of any existing markers and polylines
        clearMapView();

        List<Coordinate> coordinatesForRoute = new ArrayList<>();

        // find active buses within the specified time window
        List<JourneyInfo> activeBuses = findActiveBusesInTimeFrame(testTime, timeWindowMinutes);
        for (JourneyInfo journeyInfo : activeBuses) {
            if (selectedRoute == null || journeyInfo.getRoute().equals(selectedRoute)) {
                double longitude = journeyInfo.getLongitude();
                double latitude = journeyInfo.getLatitude();
                coordinatesForRoute.add(new Coordinate(latitude, longitude));
            }
        }

        // check if the map view is initialised before proceeding
        if (mapView == null) {
            System.err.println("MapView is not initialised. Cannot clear map view or map active buses.");
            return;
        }

        // handle the first call to this method
        if (isFirstCall) {
            System.out.println("First call - not using selected polyline.");
            isFirstCall = false;

            // if coordinates are found, centre the map on the first coordinate
            if (!coordinatesForRoute.isEmpty()) {
                Coordinate centerCoordinate = coordinatesForRoute.get(0);
                System.out.println("centre coordinates" + centerCoordinate);
                Platform.runLater(() -> mapView.setCenter(centerCoordinate));
            } else {
                System.out.println("No coordinates found for the selected route.");
            }

        } else {
            // fetch and plot the selected route's polyline data
            RouteData selectedRouteData = routeService.getRouteData(selectedRoute);
            if (selectedRouteData == null) {
                System.err.println("No RouteData found for Route ID: " + selectedRoute);
                return;
            }

            String selectedPolylineData = selectedRouteData.getPolylineData();
            if (selectedPolylineData == null) {
                System.out.println("Polyline data is null for Route ID: " + selectedRoute);
                return;
            }

            // plot the polyline and centre the map if coordinates are found
            if (!coordinatesForRoute.isEmpty()) {
                Coordinate centerCoordinate = coordinatesForRoute.get(0);

                Platform.runLater(() -> {
                    plotIndividualPolylines(selectedPolylineData, selectedRoute);
                    mapView.setCenter(centerCoordinate);
                    mapView.setZoom(15);
                });
            } else {
                System.out.println("No coordinates found for the selected route.");
            }
        }

        // monitor initial resource usage
        System.out.println("Initial resource usage:");
        ResourceMonitor.printSystemMetrics();
        long startTime = System.nanoTime();

        // process each active bus journey
        for (JourneyInfo journeyInfo : activeBuses) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
            System.out.println(journeyInfo);

            try {
                // fetch journey legs for the current route and vehicle journey code
                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode);
                if (!journeyLegs.isEmpty()) {
                    RouteData routeData = routeService.getRouteData(routeId);
                    if (routeData == null) {
                        System.err.println("No RouteData found for Route ID: " + routeId);
                        return;
                    }

                    String polylineData = routeData.getPolylineData();
                    if (polylineData == null) {
                        System.out.println("Route is null: " + routeId);
                        return;
                    }

                    // decode the polyline data into coordinates
                    List<Coordinate> routeCoordinates = PolylineDecoder.decodeAndConcatenatePolylinesFromString(polylineData);


                    // start bus movement along the decoded route
                    startBusMovement(journeyInfo, journeyLegs, routeCoordinates, testTime);

                    // monitor resource usage after starting bus movement
                    System.out.println("Resource usage after starting bus movement:");
                    ResourceMonitor.printSystemMetrics();

                } else {
                    Platform.runLater(() -> System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode));
                }
            } catch (SQLException e) {
                Platform.runLater(() -> e.printStackTrace());
            }

            // log information about the current state of markers and polylines
            System.out.println(routeId);
            System.out.println("Number of markers in MarkersWithVJC: " + MarkersWithVJC.size());
            System.out.println("Number of markers in busMarkers: " + busMarkers.size());
            System.out.println("Number of rendered bus markers: " + renderedBusMarkers.size());
            System.out.println("Number of rendered polylines: " + renderedPolylines.size());
        }

        // calculate and log execution time
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println("Execution time: " + duration + " nanoseconds");

        // monitor final resource usage
        System.out.println("Final resource usage:");
        ResourceMonitor.printSystemMetrics();
    }

    public void startBusMovement(JourneyInfo journeyInfo, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, LocalTime startTime) {
        String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
        String routeId = journeyInfo.getRoute();

        // check if the map view is initialised
        if (mapView == null) {
            System.err.println("MapView is not initialised. Cannot start bus movement.");
            return;
        }

        // check if route coordinates are available
        if (routeCoordinates == null || routeCoordinates.isEmpty()) {
            System.out.println("Route coordinates are null or empty. Cannot start bus movement.");
            return;
        }

        // get stop IDs from the journey legs
        List<String> stopIds = journeyLegs.stream()
                .flatMap(leg -> List.of(leg.getFromStop(), leg.getToStop()).stream())
                .distinct()
                .toList();

        // fetch the coordinates for the stops
        Map<String, Coordinate> stopCoordinates = stopService.getStopCoordinates(stopIds);

        // check if stop coordinates are available
        if (stopCoordinates == null || stopCoordinates.isEmpty()) {
            System.out.println("Stop coordinates are null or empty. Cannot start bus movement.");
            return;
        }

        // initialise the bus marker at the starting coordinate
        Coordinate busCoordinate = new Coordinate(journeyInfo.getLatitude(), journeyInfo.getLongitude());
        final Marker[] busMarker = {null};

        // add the bus marker if its coordinate is unique
        if (uniqueCoordinates.add(busCoordinate)) {
            busMarker[0] = new Marker(getClass().getResource(markerImagePath))
                    .setPosition(busCoordinate)
                    .setVisible(true);

            String uniqueKey = routeId + "_" + vehicleJourneyCode;
            MarkersWithVJC.put(uniqueKey, busMarker[0]);
            busMarkers.add(busMarker[0]);

            Platform.runLater(() -> mapView.addMarker(busMarker[0]));
        } else {
            System.out.println("Duplicate coordinate found: " + busCoordinate + ". Marker not added.");
        }

        // remove any duplicate markers from the map
        removeDuplicateMarkers();

        // start bus movement if the marker was successfully added
        if (busMarker[0] != null) {
            final LocalTime[] currentTime = {startTime};

            // create a timeline to move the bus along the route
            Timeline busTimeline = new Timeline(new KeyFrame(Duration.millis(200), event -> {
                // increment currentTime by 200ms per tick
                currentTime[0] = currentTime[0].plusNanos(200000000);
                moveBus(busMarker[0], journeyLegs, routeCoordinates, stopCoordinates, currentTime[0]);
            }));
            busTimeline.setCycleCount(Timeline.INDEFINITE);
            busTimeline.play();
        } else {
            System.out.println("No bus movement started due to duplicate marker.");
        }
    }
/**
    public void compareMarkers() {

        Set<Coordinate> vjcCoordinates = new HashSet<>();
        Set<Coordinate> busCoordinates = new HashSet<>();


        for (Map.Entry<String, Marker> entry : MarkersWithVJC.entrySet()) {
            Marker marker = entry.getValue();
            vjcCoordinates.add(marker.getPosition());
        }


        for (Marker marker : busMarkers) {
            busCoordinates.add(marker.getPosition());
        }


        Set<Coordinate> uniqueToVJC = new HashSet<>(vjcCoordinates);
        uniqueToVJC.removeAll(busCoordinates);


        Set<Coordinate> uniqueToBusMarkers = new HashSet<>(busCoordinates);
        uniqueToBusMarkers.removeAll(vjcCoordinates);


        if (!uniqueToVJC.isEmpty()) {
            System.out.println("Markers in MarkersWithVJC but not in busMarkers:");
            for (Coordinate coordinate : uniqueToVJC) {
                //System.out.println("Coordinate: " + coordinate);
            }
        } else {
            System.out.println("No unique markers found in MarkersWithVJC.");
        }

        if (!uniqueToBusMarkers.isEmpty()) {
            System.out.println("Markers in busMarkers but not in MarkersWithVJC:");
            for (Coordinate coordinate : uniqueToBusMarkers) {
                //System.out.println("Coordinate: " + coordinate);
            }
        } else {
            System.out.println("No unique markers found in busMarkers.");
        }
    }
**/

public void removeDuplicateMarkers() {

    // create a set to track unique coordinates
    Set<Coordinate> uniqueCoordinates = new HashSet<>();

    // create an iterator to traverse through the busMarkers list
    Iterator<Marker> iterator = busMarkers.iterator();

    // iterate over the bus markers
    while (iterator.hasNext()) {
        Marker marker = iterator.next();
        Coordinate coordinate = marker.getPosition();

        // check if the coordinate is already in the set (indicating a duplicate marker)
        if (!uniqueCoordinates.add(coordinate)) {
            System.out.println("Duplicate marker found and removed: " + marker.getId() +
                    ", Position: " + marker.getPosition());

            // remove the marker from the map and from the list
            Platform.runLater(() -> mapView.removeMarker(marker));
            iterator.remove();
        }
    }

    System.out.println("Remaining unique markers in busMarkers:");
    for (Marker remainingMarker : busMarkers) {
        // System.out.println("Marker ID: " + remainingMarker.getId() +
        // ", Position: " + remainingMarker.getPosition());
    }
}
    private void moveBus(Marker initialBusMarker, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, Map<String, Coordinate> stopCoordinates, LocalTime currentTime) {
        final Marker[] busMarker = {initialBusMarker};

        // iterate through each journey leg to update the bus position
        for (int i = 0; i < journeyLegs.size(); i++) {
            JourneyLeg leg = journeyLegs.get(i);
            LocalTime departureTime = leg.getDepartureTime();
            LocalTime nextDepartureTime = (i + 1 < journeyLegs.size()) ? journeyLegs.get(i + 1).getDepartureTime() : departureTime.plusMinutes(1);

            // check if the current time is within the timeframe of this leg
            if (!currentTime.isBefore(departureTime) && !currentTime.isAfter(nextDepartureTime)) {
                long totalSeconds = departureTime.until(nextDepartureTime, ChronoUnit.SECONDS);
                long elapsedSeconds = departureTime.until(currentTime, ChronoUnit.SECONDS);
                double progress = (double) elapsedSeconds / totalSeconds;

                if (progress < 0) progress = 0;
                if (progress > 1) progress = 1;

                // retrieve coordinates for the from and to stops
                Coordinate fromStop = stopCoordinates.get(leg.getFromStop());
                Coordinate toStop = stopCoordinates.get(leg.getToStop());

                // ensure both stop coordinates are available
                if (fromStop == null || toStop == null) {
                    System.out.println("Missing coordinates for stops");
                    return;
                }

                // find the closest segments on the route for the from and to stops
                int[] fromSegmentIndices = findClosestSegmentIndices(routeCoordinates, fromStop);
                int[] toSegmentIndices = findClosestSegmentIndices(routeCoordinates, toStop);

                // validate the segment indices
                if (fromSegmentIndices[0] == -1 || toSegmentIndices[0] == -1 || fromSegmentIndices[0] >= toSegmentIndices[0]) {
                    return;
                }

                // calculate the current segment and position within the segment
                int segmentLength = toSegmentIndices[1] - fromSegmentIndices[0];
                double segmentProgress = progress * segmentLength;
                int currentSegmentIndex = fromSegmentIndices[0] + (int) segmentProgress;
                double segmentFraction = segmentProgress - (int) segmentProgress;

                // ensure the segment index is within bounds
                if (currentSegmentIndex >= routeCoordinates.size() - 1) {
                    currentSegmentIndex = routeCoordinates.size() - 2;
                }

                // interpolate the bus's new position between the two segment coordinates
                Coordinate fromCoord = routeCoordinates.get(currentSegmentIndex);
                Coordinate toCoord = routeCoordinates.get(currentSegmentIndex + 1);

                double newLatitude = fromCoord.getLatitude() + segmentFraction * (toCoord.getLatitude() - fromCoord.getLatitude());
                double newLongitude = fromCoord.getLongitude() + segmentFraction * (toCoord.getLongitude() - fromCoord.getLongitude());

                Coordinate newPosition = new Coordinate(newLatitude, newLongitude);
                busMarker[0].setPosition(newPosition);

                // check if the bus has nearly reached the final stop
                if (i == journeyLegs.size() - 1 && progress >= 0.98) {
                    System.out.println("Bus has reached the final stop. Removing marker.");
                    Platform.runLater(() -> {
                        mapView.removeMarker(busMarker[0]);
                        busMarker[0] = null;
                    });
                }

                break;
            }
        }
    }

}