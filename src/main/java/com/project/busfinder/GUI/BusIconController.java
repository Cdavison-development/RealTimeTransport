package com.project.busfinder.GUI;
import com.project.busfinder.Mapping_util.*;
import com.project.busfinder.helperFunctions.getStopName;
import com.project.busfinder.util.PolylineDecoder;
import com.project.busfinder.util.ResourceMonitor;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MarkerEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
//import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.io.IOException;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.project.busfinder.Mapping_util.polylineHelpers.findClosestSegmentIndices;
import static com.project.busfinder.Mapping_util.simulateBusLocations.*;
import static com.project.busfinder.helperFunctions.getStopName.StopName;
import static com.project.busfinder.util.PolylineDecoder.decodePolylinesIndividually;
import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;
import static com.project.busfinder.util.readLiveLocation.processXmlResponse;



public class BusIconController {

    private String ActiveBusesDay;

    private ProgressIndicator progressIndicator;

    private MapView mapView;
    private MainController mainController;
    private stopsPanelController stopsPanelController;
    private boolean isMapLocked = true;
    private Marker currentLockedMarker;
    private Timeline lockTimeline = null;
    private Map<String, JourneyInfo> closestBuses = new HashMap<>();
    private final StopService stopService;
    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private final List<Marker> busMarkers = new ArrayList<>();
    private Map<String, Marker> MarkersWithVJC;
    private RouteService routeService;
    private List<Marker> renderedBusMarkers = new ArrayList<>();
    private List<CoordinateLine> renderedPolylines = new ArrayList<>();
    private List<CoordinateLine> allPolylines = new ArrayList<>();
    private boolean isFirstCall = true;
    private boolean useFalse = true;
    private Set<Coordinate> uniqueCoordinates = new HashSet<>();
    private List<Marker> stopMarkers = new ArrayList<>();
    private List<JourneyInfo> activeBuses = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;
    private Map<String, Timeline> activeTimelines = new HashMap<>();
    private volatile boolean stopBusMovementUpdate = false;
    private String CurrentDay = null;
    private boolean useLiveRoutes = true;


    public BusIconController(MapView mapView) {
        //initialise variables
        this.mapView = mapView;
        this.stopService = new StopService();
        MarkersWithVJC = new HashMap<>();

    }


    public void setMainController(MainController mainController) {
        this.mainController = mainController;

    }
    public void initializeMap() {
        //mainController.loadSidePanel("/com/project/busfinder/GUI/routeDetailsPanel.fxml");
        mapView.initialize(Configuration.builder().build());
        routeService = new RouteService();
        mapView.setCenter(new Coordinate(53.4013, -3.057244));  // set the initial map centre
        mapView.setZoom(12);  // set the initial zoom level

        // handle marker click events
        mapView.addEventHandler(MarkerEvent.MARKER_CLICKED, event -> {
            event.consume();
            Marker clickedMarker = event.getMarker();
            lockOntoMarker(clickedMarker,CurrentDay);
            handleMarkerClick(clickedMarker,CurrentDay);
            //lockOntoMarker(clickedMarker,CurrentDay);
        });

        // check when the map view is fully initialised
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                System.out.println("MapView initialised.");

            }
        });


    }

    /**
     *
     * handles the initializaton and rendering of the bus stop times in the stopsPanelController  on marker click
     *
     * @param clickedMarker
     * @param day
     */
    private void handleMarkerClick(Marker clickedMarker, String day) {
        // log the ID of the clicked marker
        System.out.println("Marker clicked with ID: " + clickedMarker.getId());


        System.out.println("use live routes is " + useLiveRoutes);
        if (mainController.getStopsPanelController() == null) {
            System.err.println("StopsPanelController was not initialized as expected.");
            mainController.loadAndInitializeStopsPanel();  // Fallback initialization if needed
        }
        for (Map.Entry<String, Marker> entry : MarkersWithVJC.entrySet()) {
            String key = entry.getKey();
            Marker marker = entry.getValue();

            if (marker.equals(clickedMarker)) {
                System.out.println("Matched entry in MarkersWithVJC: " + key);

                int underscoreIndex = key.indexOf("_");
                if (underscoreIndex != -1) {
                    String routeId = key.substring(0, underscoreIndex);
                    String vehicleJourneyCode = key.substring(underscoreIndex + 1);
                    System.out.println("Route ID: " + routeId);
                    System.out.println("Vehicle Journey Code: " + vehicleJourneyCode);
                    Platform.runLater(() -> {
                        try {
                            mainController.getStopsPanelController().loadStopTimes(routeId, vehicleJourneyCode, day,useLiveRoutes);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });

                } else {
                    System.out.println("Invalid format in MarkersWithVJC key: " + key);
                }
                break;
            }

        }
    }

    /**
     *
     * plots the markers for each stop and converts the name from the unique ID for the stop to the common name for the stop
     *
     * @param routeId
     * @param vehicleJourneyCode
     * @param Day
     */
    private void plotStopsForRoute(String routeId, String vehicleJourneyCode,String Day) {

        clearRenderedMarkers();
        try {
            // get the journey legs
            List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode,Day);

            // convert journeyLeg objects to JourneyInfo objects
            List<JourneyInfo> journeyInfos = new ArrayList<>();
            for (JourneyLeg leg : journeyLegs) {
                String stopName = StopName(leg.getFromStop());
                double longitude = leg.getLongitude();
                double latitude = leg.getLatitude();
                journeyInfos.add(new JourneyInfo(vehicleJourneyCode, routeId, leg.getDepartureTime(), leg.getFromStop(), leg.getToStop(), longitude, latitude));
            }

            // plot the stops
            for (JourneyInfo info : journeyInfos) {
                Coordinate coord = new Coordinate(info.getLatitude(), info.getLongitude());


                Marker stopMarker = Marker.createProvided(Marker.Provided.BLUE)
                        .setPosition(coord)
                        .setVisible(true);


                MapLabel stopLabel = new MapLabel(getStopName.StopName(info.getFromStop()), 10, -10).setVisible(true);

                stopMarker.attachLabel(stopLabel);

                mapView.addMarker(stopMarker);

                stopMarkers.add(stopMarker);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching journey legs: " + e.getMessage());
        }
    }

    private void clearRenderedMarkers() {
        // remove all currently rendered polylines from the map
        if (stopMarkers.isEmpty()) {
            System.out.println("No polylines to remove.");
        } else {
            System.out.println("Removing " + stopMarkers.size() + " polylines.");
            for (Marker markers : stopMarkers) {
                Platform.runLater(() -> mapView.removeMarker(markers));
            }
            stopMarkers.clear();
        }

        System.out.println("Map view cleared.");
    }

    private String getPolylineDataForJourney(String routeId) {
        RouteData routeData = routeService.getRouteData(routeId);
        return routeData != null ? routeData.getPolylineData() : null;
    }

    /**
     * clears JFX components from the mapview
     *
     *
     */
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

        clearRenderedPolylines();
    }
    private void clearRenderedPolylines() {
        // remove all currently rendered polylines from the map
        if (allPolylines.isEmpty()) {
            System.out.println("No polylines to remove.");
        } else {
            System.out.println("Removing " + allPolylines.size() + " polylines.");
            for (CoordinateLine polyline : allPolylines) {
                Platform.runLater(() -> mapView.removeCoordinateLine(polyline));
            }
            allPolylines.clear();
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
        // decode polyline data
        List<List<Coordinate>> allPolylinesCoordinates = decodePolylinesIndividually(polylineData);

        // iterate through each list of coordinates
        for (List<Coordinate> polyline : allPolylinesCoordinates) {
            CoordinateLine coordinateLine = createCoordinateLine(polyline);

            //check if the selected route matches, plot
            if (selectedRoute.equals(selectedRoute)) {
                allPolylines.add(coordinateLine);
                Platform.runLater(() -> mapView.addCoordinateLine(coordinateLine));
            }
        }
    }

    /**
     *
     *
     * Used for running simulated bus routes
     *
     * @param Day
     * @param testDateTime
     * @param timeWindowMinutes
     * @param selectedRoute
     * @param SelectedVJC
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    public void mapActiveBuses(String Day, LocalDateTime testDateTime, int timeWindowMinutes, String selectedRoute, String SelectedVJC) throws SQLException, IOException, InterruptedException {
        // display the loading indicator while the map is being updated
        Platform.runLater(() -> progressIndicator.setVisible(true));

        //initialise new ArrayList and Hashmap to store new markers to
        //store new markers during processing
        Map<String, Marker> newMarkersWithVJC = new HashMap<>();
        List<Marker> newBusMarkers = new ArrayList<>();
        boolean matchFound = false;
        List<Coordinate> coordinatesForRoute = new ArrayList<>();


        activeBuses.clear();
        closestBuses.clear();
        // extract the time part for the findActiveBusesInTimeFrame method
        LocalTime testTime = testDateTime.toLocalTime();

        List<JourneyLeg> allJourneyLegsForRoute = getJourneyLegs(selectedRoute, SelectedVJC, Day);
        if (allJourneyLegsForRoute.isEmpty()) {
            System.out.println("No journey legs found for the selected route: " + selectedRoute);
            return;
        }else{
            System.out.println("all journey legs " + allJourneyLegsForRoute);
        }
        // find active buses within the specified time window
        activeBuses = findActiveBusesInTimeFrame(testTime, timeWindowMinutes, Day);
        System.out.println("Active buses found: " + activeBuses.size());


        // determine the closest bus for each vehicle journey code across all routes
        for (JourneyInfo journeyInfo : activeBuses) {
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
            LocalTime closestDepartureTime = journeyInfo.getClosestDepartureTime();
            String routeId = journeyInfo.getRoute();

            // calculate the time difference
            java.time.Duration difference = java.time.Duration.between(testTime, closestDepartureTime);

            // adjust the difference if it wraps around midnight
            if (difference.isNegative()) {
                difference = difference.plusDays(1);
            }

            //System.out.println("active bus : " + routeId);
            String uniqueKey = routeId + "_" + vehicleJourneyCode;

            // check if there is an existing entry with the same key (routeId and vehicleJourneyCode)
            if (!closestBuses.containsKey(uniqueKey) || difference.compareTo(java.time.Duration.between(testTime, closestBuses.get(uniqueKey).getClosestDepartureTime())) < 0) {
                closestBuses.put(uniqueKey, journeyInfo);
            } else {
                JourneyInfo existingJourney = closestBuses.get(uniqueKey);
            }
        }


        System.out.println("Final closest buses: " + closestBuses);

        // add the coordinates of the closest buses to the route coordinates list
        for (JourneyInfo journeyInfo : activeBuses) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
            System.out.println("Processing JourneyInfo - Route: " + routeId + ", VJC: " + vehicleJourneyCode);
            // check if the selected route matches or if no route is selected
            if (selectedRoute == null || routeId.equals(selectedRoute)) {
                double longitude = journeyInfo.getLongitude();
                double latitude = journeyInfo.getLatitude();
                coordinatesForRoute.add(new Coordinate(latitude, longitude));

                // check if the selected VJC matches the current journey
                if (vehicleJourneyCode.equals(SelectedVJC) && routeId.equals(selectedRoute)) {
                    System.out.println("Match found: Route " + routeId + ", VJC: " + vehicleJourneyCode);
                    matchFound = true;
                    System.out.println("markers with vjcoid: " + MarkersWithVJC);
                } else {
                    System.out.println("Match not found for Route " + routeId + ", VJC: " + vehicleJourneyCode);
                }
            } else {
                System.out.println("Route does not match: " + routeId + " (Expected: " + selectedRoute + ")");
            }
        }

        // check if the map view is initialised before proceeding
        if (mapView == null) {
            System.err.println("MapView is not initialised. Cannot clear map view or map active buses.");
            return;
        }

        // monitor initial resource usage
        System.out.println("Initial resource usage:");
        ResourceMonitor.printSystemMetrics();
        long startTime = System.nanoTime();

        // process each active bus journey
        for (JourneyInfo journeyInfo : closestBuses.values()) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
            System.out.println("Processing Journey - Route: " + routeId + ", VJC: " + vehicleJourneyCode);

            try {
                // fetch journey legs for the current route and vehicle journey code
                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, Day);
                System.out.println( "journey legs  : " + journeyLegs);
                if (!journeyLegs.isEmpty()) {
                    RouteData routeData = routeService.getRouteData(routeId);
                    if (routeData == null) {
                        System.err.println("No RouteData found for Route ID: " + routeId);
                        continue;
                    }

                    String polylineData = routeData.getPolylineData();
                    if (polylineData == null) {
                        System.out.println("Polyline data is null for Route ID: " + routeId);
                        continue;
                    }

                    // decode the polyline data into coordinates

                    List<Coordinate> routeCoordinates = PolylineDecoder.decodeAndConcatenatePolylinesFromString(polylineData);
                    clearRenderingState();
                    // start bus movement along the decoded route
                    startBusMovement(journeyInfo, journeyLegs, routeCoordinates, testTime, selectedRoute, SelectedVJC, newMarkersWithVJC, newBusMarkers);

                } else {
                    System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode);
                }
            } catch (SQLException e) {
                System.err.println("SQLException occurred while processing Journey - Route: " + routeId + ", VJC: " + vehicleJourneyCode);
                e.printStackTrace();
            }

            // log information about the current state of markers and polylines
            System.out.println("Current Route ID: " + routeId);
            System.out.println("Number of markers in MarkersWithVJC: " + MarkersWithVJC.size());
            System.out.println("Number of markers in busMarkers: " + busMarkers.size());
            System.out.println("Number of rendered bus markers: " + renderedBusMarkers.size());
            System.out.println("Number of rendered polylines: " + renderedPolylines.size());
        }

        Platform.runLater(() -> {

            CurrentDay = Day;
            // clear the map view of any existing markers and polylines
            clearMapView();

            // update the global markers and bus markers with the newly prepared ones
            MarkersWithVJC.clear();
            MarkersWithVJC.putAll(newMarkersWithVJC);

            busMarkers.clear();
            busMarkers.addAll(newBusMarkers);

            // add the new markers to the map view
            for (Marker marker : busMarkers) {
                mapView.addMarker(marker);
            }

            // lock onto the selected marker
            if (SelectedVJC != null) {
                String uniqueKey = selectedRoute + "_" + SelectedVJC;
                Marker markerToLock = MarkersWithVJC.get(uniqueKey);
                System.out.println("marker to lock is : " + markerToLock);
                if (markerToLock != null) {
                    System.out.println("Locking onto marker with key: " + uniqueKey);
                    lockOntoMarker(markerToLock,Day);
                    markerToLock.setVisible(true);
                } else {
                    System.out.println("Marker not found for key: " + uniqueKey);
                }
            }


            Platform.runLater(() -> progressIndicator.setVisible(false));
        });

    }

    /**
     *
     * starts the bus movement animation for each bus
     * @param journeyInfo
     * @param journeyLegs
     * @param routeCoordinates
     * @param startTime
     * @param selectedRoute
     * @param selectedVJC
     * @param newMarkersWithVJC
     * @param newBusMarkers
     */
    public void startBusMovement(JourneyInfo journeyInfo, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, LocalTime startTime, String selectedRoute, String selectedVJC, Map<String, Marker> newMarkersWithVJC, List<Marker> newBusMarkers) {

        String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
        String routeId = journeyInfo.getRoute();
        System.out.println("vehicle journey code : " + vehicleJourneyCode);
        System.out.println("route :" + routeId);


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
        Marker busMarker;

        // add the bus marker if its coordinate is unique
        if (uniqueCoordinates.add(busCoordinate)) {
            busMarker = new Marker(getClass().getResource(markerImagePath))
                    .setPosition(busCoordinate)
                    .setVisible(true);

            String uniqueKey = routeId + "_" + vehicleJourneyCode;
            newMarkersWithVJC.put(uniqueKey, busMarker);
            newBusMarkers.add(busMarker);

        } else {
            busMarker = null;
        }

        // start bus movement if the marker was successfully added
        if (busMarker != null) {
            // use AtomicReference for thread-safe updates to currentTime during animaton
            AtomicReference<LocalTime> currentTime = new AtomicReference<>(startTime);

            // create a timeline to move the bus along the route
            Timeline busTimeline = new Timeline(new KeyFrame(javafx.util.Duration.millis(100), event -> {
                // increment currentTime by 200ms per tick
                currentTime.set(currentTime.get().plusNanos(200000000));
                moveBus(busMarker, journeyLegs, routeCoordinates, stopCoordinates, currentTime.get());
            }));
            busTimeline.setCycleCount(Timeline.INDEFINITE);
            busTimeline.play();

        } else {
            System.out.println("No bus movement started due to duplicate marker.");
        }
    }

    public void clearRenderingState() {
        uniqueCoordinates.clear();
    }

    /**
     *
     * moves the bus marker along the journey route based on current time and progress through the journey legs
     * if the bus reaches the final stop, or deviates sginificantly from the markers position, the marker is removed.
     *
     * This fixes the large problem where buses appeared to be "flying" around the map.
     *
     *
     * @param initialBusMarker
     * @param journeyLegs
     * @param routeCoordinates
     * @param stopCoordinates
     * @param currentTime
     */
    private void moveBus(Marker initialBusMarker, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, Map<String, Coordinate> stopCoordinates, LocalTime currentTime) {
        final Marker[] busMarker = {initialBusMarker};
        //ideal threshold found through trial and error
        double maxDistanceThreshold = 0.25;
        //System.out.println("Starting moveBus with currentTime: " + currentTime);
        double maxSpeedKmPerHour = 100.0;
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
                //fix for bug where buses were  going all the way around the bus route to get to the next stop
                double distanceFromExpectedPosition = calculateDistanceInKm(busMarker[0].getPosition(), newPosition);

                if (distanceFromExpectedPosition > maxDistanceThreshold) {
                    //System.err.println("Warning: Bus " + busMarker[0].getId() + " is far from the expected route! Distance: " + distanceFromExpectedPosition + " km.");
                    return;
                }
               // System.out.println("New Bus Position: " + newPosition);
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
    private double calculateDistanceInKm(Coordinate coord1, Coordinate coord2) {
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(coord2.getLatitude() - coord1.getLatitude());
        double dLon = Math.toRadians(coord2.getLongitude() - coord1.getLongitude());

        double lat1 = Math.toRadians(coord1.getLatitude());
        double lat2 = Math.toRadians(coord2.getLatitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    /**
     *
     * initialises the live bus process, and locks onto the selected bus passed by the selected route and VJC parameters
     *
     * the movements are then updated by the startBusMovementUpdate function
     *
     * @param selectedRoute
     * @param liveRouteInfoList
     * @param firstRun
     * @param VJC
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    public void mapLiveRoutesWithJourneyInfos(String selectedRoute, List<LiveRouteInfo> liveRouteInfoList, boolean firstRun,String VJC) throws SQLException, IOException, InterruptedException {
        Platform.runLater(() -> {
            try {
                // show loading indicator while rendering new items
                progressIndicator.setVisible(true);

                // clear the closest buses map to store new entries
                Map<String, Marker> newMarkersWithVJC = new HashMap<>();
                List<Marker> newBusMarkers = new ArrayList<>();
                List<Coordinate> coordinatesForRoute = new ArrayList<>();
                Marker markerToLockOnto = null;
                CurrentDay = null;

                // handle each live route entry
                for (LiveRouteInfo liveRouteInfo : liveRouteInfoList) {
                    String routeId = liveRouteInfo.getLineRef();
                    String journeyRef = liveRouteInfo.getJourneyRef();
                    double latitude = liveRouteInfo.getLatitude();
                    double longitude = liveRouteInfo.getLongitude();
                    Coordinate coordinate = new Coordinate(latitude, longitude);

                    // get pattern fata
                    vjcAndDay patternData = getVehicleJourneyCode(routeId, journeyRef);

                    if (patternData != null && patternData.getVehicleJourneyCode() != null && patternData.getDay() != null) {
                        CurrentDay = patternData.getDay();

                        String markerKey = routeId + "_" + patternData.getVehicleJourneyCode();

                            coordinatesForRoute.add(coordinate);


                            Marker busMarker = new Marker(getClass().getResource(markerImagePath))
                                    .setPosition(coordinate)
                                    .setVisible(true);

                            // add marker to the new maps
                            newMarkersWithVJC.put(markerKey, busMarker);
                            newBusMarkers.add(busMarker);

                            // lock onto marker that matches requested marker
                            if (firstRun && selectedRoute != null && routeId.equals(selectedRoute) && patternData.getVehicleJourneyCode().equals(VJC)) {
                                markerToLockOnto = busMarker;
                                System.out.println("vehicle journey code: " + patternData.getVehicleJourneyCode());
                            }

                    } else {
                        if (patternData == null) {
                            System.out.println("Pattern data is null for Route: " + routeId + ", JourneyRef: " + journeyRef);
                        } else {
                            System.out.println("VehicleJourneyCode is null for Route: " + routeId + ", JourneyRef: " + journeyRef);
                        }
                    }
                }
                System.out.println("current day : " + CurrentDay);
                // clear current markers when population of new marker lists are done
                for (Marker marker : MarkersWithVJC.values()) {
                    mapView.removeMarker(marker);
                }

                // update the global markers and bus markers with the newly prepared ones
                MarkersWithVJC.clear();
                MarkersWithVJC.putAll(newMarkersWithVJC);

                busMarkers.clear();
                busMarkers.addAll(newBusMarkers);

                // add the new markers to the map view
                for (Marker marker : busMarkers) {
                    mapView.addMarker(marker);
                }

                // lock onto the selected marker
                if (firstRun && markerToLockOnto != null) {
                    System.out.println("Locking onto marker for Route: " + selectedRoute);
                    lockOntoMarker(markerToLockOnto,CurrentDay);
                }
                // hide the progress indicator
                progressIndicator.setVisible(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private List<LiveRouteInfo> fetchUpdatedLiveRouteInfo() throws IOException, InterruptedException {
        // fetch new data from api
        String response = fetchAndProcessResponse();
        return processXmlResponse(response);
    }

    /**
     *
     * handles the movement for live buses
     *
     *
     * @param selectedRoute
     * @param liveRouteInfoList
     * @param isLive
     * @param VJC
     */
    public void startBusMovementUpdate(String selectedRoute, List<LiveRouteInfo> liveRouteInfoList, boolean isLive,String VJC) {
        stopBusMovementUpdate = false;
        // cancel any previous tasks
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }

        if (!isLive) {
            // if the route isnt live clear mapview and rendered markers
            clearMapView();
            clearRenderedMarkers();
            return;
        }

        AtomicBoolean firstRun = new AtomicBoolean(true);

        Runnable updateTask = () -> {
            if (stopBusMovementUpdate) {
                return; // exit if the stop flag is set
            }
            try {
                // get new coordinates in the background
                List<LiveRouteInfo> updatedLiveRouteInfoList = fetchUpdatedLiveRouteInfo();

                // update UI on JFX thread
                Platform.runLater(() -> {
                    try {
                        mapLiveRoutesWithJourneyInfos(selectedRoute, updatedLiveRouteInfoList, firstRun.get(),VJC);
                        firstRun.set(false);
                    } catch (SQLException | IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };

        scheduledTask = scheduler.scheduleAtFixedRate(updateTask, 0, 10, TimeUnit.SECONDS);
        System.out.println("Started live tracking updates.");
    }
    public void stopBusMovementUpdate() {
        stopBusMovementUpdate = true;
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }

    }

    /**
     * handles the mapping of JFX components on marker click
     *
     *
     * @param targetMarker
     * @param day
     */
    private void lockOntoMarker(Marker targetMarker, String day) {
        // stop the previous lock if there is one
        if (currentLockedMarker != null && lockTimeline != null) {
            lockTimeline.stop();  // stop the previous lock timeline
            currentLockedMarker = null;
            isMapLocked = false;
        }

        isMapLocked = true;
        currentLockedMarker = targetMarker;

        // centre the map on the marker and zoom in
        Platform.runLater(() -> {
            mapView.setCenter(targetMarker.getPosition());
            mapView.setZoom(18);
        });

        // unlock the map when the user drags
        mapView.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            isMapLocked = false;
        });

        // keep centering the map on the marker while it is locked
        lockTimeline = new Timeline(new KeyFrame(javafx.util.Duration.millis(100), e -> {
            if (isMapLocked && currentLockedMarker != null) {
                Platform.runLater(() -> {
                    mapView.setCenter(currentLockedMarker.getPosition());
                });
            }
        }));
        lockTimeline.setCycleCount(Timeline.INDEFINITE);
        lockTimeline.play();
        // add map components
        for (Map.Entry<String, Marker> entry : MarkersWithVJC.entrySet()) {
            String key = entry.getKey();
            Marker marker = entry.getValue();

            if (marker.equals(targetMarker)) {
                System.out.println("Matched entry in MarkersWithVJC: " + key);

                int underscoreIndex = key.indexOf("_");
                if (underscoreIndex != -1) {
                    String routeId = key.substring(0, underscoreIndex);
                    String vehicleJourneyCode = key.substring(underscoreIndex + 1);

                    System.out.println("Route ID: " + routeId);
                    System.out.println("Vehicle Journey Code: " + vehicleJourneyCode);


                    clearRenderedPolylines();
                    RouteData selectedRouteData = routeService.getRouteData(routeId);
                    String selectedPolylineData = selectedRouteData.getPolylineData();
                    String polylineData = getPolylineDataForJourney(routeId);
                    if (polylineData != null) {
                        plotIndividualPolylines(selectedPolylineData, routeId);
                    } else {
                        System.out.println("Polyline data is null.");
                    }
                    plotStopsForRoute(routeId, vehicleJourneyCode, day);

                    try {
                        List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, day);
                        for (JourneyLeg leg : journeyLegs) {
                            String fromStop = StopName(leg.getFromStop());
                            String toStop = StopName(leg.getToStop());
                            System.out.println("From: " + fromStop + ", To: " + toStop + ", Departure Time: " + leg.getDepartureTime());
                        }
                    } catch (SQLException e) {
                        System.err.println("Error retrieving journey legs: " + e.getMessage());
                    }
                } else {
                    System.out.println("Invalid format in MarkersWithVJC key: " + key);
                }
                break;
            }
        }
    }
    public void setProgressIndicator(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public void setTrackBusPanelController(TrackBusPanelController controller) {
       // this.trackBusPanelController = controller;
    }

    public void setUseLiveRoutes(boolean useLiveRoutes) {
        System.out.println("Setting useLiveRoutes in BusPanelController to: " + useLiveRoutes);
        this.useLiveRoutes = useLiveRoutes;
    }

}