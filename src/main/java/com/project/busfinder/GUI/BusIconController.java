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
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
//import javafx.util.Duration;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.project.busfinder.Mapping.polylineHelpers.findClosestSegmentIndices;
import static com.project.busfinder.Mapping.simulateBusLocations.findActiveBusesInTimeFrame;
import static com.project.busfinder.Mapping.simulateBusLocations.getJourneyLegs;
import static com.project.busfinder.util.PolylineDecoder.decodePolylinesIndividually;


// next step, return data on click
public class BusIconController {


    @FXML
    private ProgressIndicator loadingIndicator;
    private MapView mapView;

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

    private List<JourneyInfo> activeBuses = new ArrayList<>();
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
            handleMarkerClick(clickedMarker);
            lockOntoMarker(clickedMarker);
            System.out.println(MarkersWithVJC);
        });

        // check when the map view is fully initialised
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                System.out.println("MapView initialised.");

            }
        });


    }
    private void handleMarkerClick(Marker clickedMarker) {
        // log the ID of the clicked marker
        System.out.println("Marker clicked with ID: " + clickedMarker.getId());

        // center the map on the clicked marker and zoom in
        Platform.runLater(() -> {
            mapView.setCenter(clickedMarker.getPosition());
            mapView.setZoom(16);
        });

        // find the corresponding entry in MarkersWithVJC
        for (Map.Entry<String, Marker> entry : MarkersWithVJC.entrySet()) {
            String key = entry.getKey();
            Marker marker = entry.getValue();

            if (marker.equals(clickedMarker)) {
                // log the matched entry
                System.out.println("Matched entry in MarkersWithVJC: " + key);

                // split the key to separate Route ID and Vehicle Journey Code
                int underscoreIndex = key.indexOf("_");
                if (underscoreIndex != -1) {
                    String routeId = key.substring(0, underscoreIndex); // before the first underscore
                    String vehicleJourneyCode = key.substring(underscoreIndex + 1);

                    System.out.println("Route ID: " + routeId);
                    System.out.println("Vehicle Journey Code: " + vehicleJourneyCode);

                    // clear existing polylines
                    clearRenderedPolylines();

                    // get and plot the polyline for the clicked marker's journey
                    RouteData selectedRouteData = routeService.getRouteData(routeId);
                    String selectedPolylineData = selectedRouteData.getPolylineData();
                    String polylineData = getPolylineDataForJourney(routeId);
                    if (polylineData != null) {
                        System.out.println("polyline data: " + polylineData);
                        plotIndividualPolylines(selectedPolylineData, routeId);
                    } else {
                        System.out.println("polyline data null");
                    }

                } else {
                    System.out.println("Invalid format in MarkersWithVJC key: " + key);
                }
            } else {
                System.out.println("No matching entry found in MarkersWithVJC for clicked marker.");
            }
        }
    }

    private String getPolylineDataForJourney(String routeId) {
        RouteData routeData = routeService.getRouteData(routeId);
        return routeData != null ? routeData.getPolylineData() : null;
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


    public void mapLiveRoutesWithJourneyInfos(String selectedRoute) throws IOException, InterruptedException, SQLException {
        // clear the map view of any existing markers and polylines
        clearMapView();
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
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
                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, String.valueOf(currentDay));
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
                    clearRenderingState();
                    //startBusMovement(journeyInfo, journeyLegs, routeCoordinates, LocalTime.now(),null,null);

                    System.out.println("Resource usage after starting bus movement:");
                    ResourceMonitor.printSystemMetrics();
                } else {
                    Platform.runLater(() -> System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode));
                }
            } catch (SQLException e) {
                // handle SQL exceptions
                Platform.runLater(() -> e.printStackTrace());
            }
        }
    }
    public void mapActiveBuses(String Day, LocalDateTime testDateTime, int timeWindowMinutes, String selectedRoute, String SelectedVJC) throws SQLException, IOException, InterruptedException {
        // display the loading indicator while the map is being updated
        Platform.runLater(() -> loadingIndicator.setVisible(true));

        // clear the closest buses map to store new entries
        closestBuses.clear();
        Map<String, Marker> newMarkersWithVJC = new HashMap<>();
        List<Marker> newBusMarkers = new ArrayList<>();
        boolean matchFound = false;
        List<Coordinate> coordinatesForRoute = new ArrayList<>();

        // extract the time part for the findActiveBusesInTimeFrame method
        LocalTime testTime = testDateTime.toLocalTime();
        activeBuses.clear();

        // find active buses within the specified time window
        activeBuses = findActiveBusesInTimeFrame(testTime, timeWindowMinutes, Day);
        System.out.println("Active buses found: " + activeBuses.size());

        // log each active bus
        for (JourneyInfo journeyInfo : activeBuses) {
            System.out.println("Active Bus - Route: " + journeyInfo.getRoute() + ", VJC: " + journeyInfo.getVehicleJourneyCode() + ", Departure Time: " + journeyInfo.getClosestDepartureTime());
        }

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


            String uniqueKey = routeId + "_" + vehicleJourneyCode;

            // check if there is an existing entry with the same key (routeId and vehicleJourneyCode)
            if (!closestBuses.containsKey(uniqueKey) || difference.compareTo(java.time.Duration.between(testTime, closestBuses.get(uniqueKey).getClosestDepartureTime())) < 0) {
                // add the closest bus to the map
                System.out.println("added to closestBuses: Route " + routeId + ", VJC: " + vehicleJourneyCode);
                closestBuses.put(uniqueKey, journeyInfo);
            } else {
                // print information about the closer bus already in closestBuses
                JourneyInfo existingJourney = closestBuses.get(uniqueKey);
                System.out.println("not added to closestBuses: Route " + routeId + ", VJC: " + vehicleJourneyCode + " - Closer bus already in list");
                System.out.println("closer bus details - Route: " + existingJourney.getRoute() + ", VJC: " + existingJourney.getVehicleJourneyCode() + ", Departure Time: " + existingJourney.getClosestDepartureTime());
            }
        }


        System.out.println("Final closest buses: " + closestBuses);

        // add the coordinates of the closest buses to the route coordinates list
        for (JourneyInfo journeyInfo : activeBuses) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
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

        // handle the first call to this method
        if (isFirstCall) {
            System.out.println("First call - not using selected polyline.");
            isFirstCall = false;
        } else {
            // fetch and plot the selected route's polyline data
            if (selectedRoute == null) {
                System.out.println("Error: selectedRoute is null! This should not happen.");
            } else {
                System.out.println("Selected Route is not null.");
            }
            RouteData selectedRouteData = selectedRoute != null ? routeService.getRouteData(selectedRoute) : null;

            if (selectedRouteData != null) {
                String selectedPolylineData = selectedRouteData.getPolylineData();
                if (selectedPolylineData != null) {
                    // plot the polyline for the route
                    Platform.runLater(() -> plotIndividualPolylines(selectedPolylineData, selectedRoute));
                } else {
                    System.out.println("Polyline data is null for Route ID: " + selectedRoute);
                }
            } else {
                System.out.println("Route data is null for selected route: " + selectedRoute);
            }

            // if no selected route or no polyline data found, center of coordinates
            if (coordinatesForRoute.isEmpty()) {
                System.out.println("No coordinates found for the selected route.");
            }
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

                if (markerToLock != null) {
                    System.out.println("Locking onto marker with key: " + uniqueKey);
                    lockOntoMarker(markerToLock);
                    handleMarkerClick(markerToLock);
                    markerToLock.setVisible(true);
                    mapView.setCenter(markerToLock.getPosition());
                    mapView.setZoom(18);
                } else {
                    System.out.println("Marker not found for key: " + uniqueKey);
                }
            }


            Platform.runLater(() -> loadingIndicator.setVisible(true));
        });

    }

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

            // add the marker to the map view
            Platform.runLater(() -> mapView.addMarker(busMarker));
        } else {
            busMarker = null;
            System.out.println("Duplicate coordinate found: " + busCoordinate + ". Marker not added.");
        }

        // start bus movement if the marker was successfully added
        if (busMarker != null) {
            // use AtomicReference to ensure thread-safe updates to currentTime during animaton
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

    private void lockOntoMarker(Marker marker) {
        // stop the previous lock if there is one
        if (currentLockedMarker != null && lockTimeline != null) {
            lockTimeline.stop();  // stop the previous lock timeline
            currentLockedMarker = null;
            isMapLocked = false;
        }

        isMapLocked = true;
        currentLockedMarker = marker;

        // centre the map on the marker and zoom in
        Platform.runLater(() -> {
            mapView.setCenter(marker.getPosition());
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
    }

}