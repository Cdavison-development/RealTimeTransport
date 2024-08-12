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

    private Marker busMarker;
    private List<Coordinate> routeCoordinates;
    private int currentIndex = 0;
    private boolean forwardDirection = true;
    private final StopService stopService;
    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private final List<Marker> busMarkers = new ArrayList<>();
    private Map<String, Marker> MarkersWithVJC;
    private CoordinateLine routeLine;
    private Timeline timeline;
    private RouteService routeService;

    private List<Marker> renderedBusMarkers = new ArrayList<>();
    private List<CoordinateLine> renderedPolylines = new ArrayList<>();

    private List<CoordinateLine> allPolylines = new ArrayList<>();
    private boolean isFirstCall = true;

    public BusIconController(MapView mapView) {
        this.mapView = mapView;
        this.stopService = new StopService();
        MarkersWithVJC = new HashMap<>();
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
    }
    public void initializeMap() {
        mapView.initialize(Configuration.builder().build());
        routeService = new RouteService();
        mapView.setCenter(new Coordinate(53.4013, -3.057244)); // Set initial map center
        mapView.setZoom(12);

        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            Coordinate clickedCoord = event.getCoordinate();
            System.out.println("Map clicked at: " + clickedCoord.getLatitude() + ", " + clickedCoord.getLongitude());
        });

        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                System.out.println("MapView initialized.");
                //addMarkersToMap();
                //setupMapExtentHandler();
            }
        });
    }

    public void createBusMarkers(List<JourneyInfo> busInfos) {
        for (JourneyInfo busInfo : busInfos) {
            System.out.println(busInfo);
            Coordinate busCoordinate = new Coordinate(busInfo.getLatitude(), busInfo.getLongitude());
            Marker busMarker = new Marker(getClass().getResource(markerImagePath))
                    .setPosition(busCoordinate)
                    .setVisible(true);
            busMarkers.add(busMarker);
            Platform.runLater(() -> mapView.addMarker(busMarker));
        }
    }


// at current, this isnt moving anything
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
        CoordinateLine coordinateLine = new CoordinateLine(coordinates)
                .setVisible(true)
                .setColor(Color.DODGERBLUE)
                .setWidth(5);
        return coordinateLine;
    }

    public void plotIndividualPolylines(String polylineData, String selectedRoute) {
        List<List<Coordinate>> allPolylinesCoordinates = decodePolylinesIndividually(polylineData);

        for (List<Coordinate> polyline : allPolylinesCoordinates) {
            CoordinateLine coordinateLine = createCoordinateLine(polyline);


            if (selectedRoute.equals(selectedRoute)) {
                allPolylines.add(coordinateLine);
                Platform.runLater(() -> mapView.addCoordinateLine(coordinateLine));
            }
        }
    }
    public void mapLiveRoutesWithJourneyInfos(String selectedRoute) throws IOException, InterruptedException, SQLException, IOException {
        clearMapView();
        if(isFirstCall) {
            System.out.println("First call - not using selected polyline.");
            isFirstCall = false;
            if (mapView == null) {
                System.err.println("MapView is not initialized. Cannot clear map view or map active buses. mapLiveRoutesWithJourneyInfos");
                return;
            }
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

            Platform.runLater(() -> {
                plotIndividualPolylines(selectedPolylineData, selectedRoute);
            });
        }
        List<JourneyInfo> journeyInfos = simulateBusLocations.findClosestDepartureTime();

        for (JourneyInfo journeyInfo : journeyInfos) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();

            try {
                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode);
                if (!journeyLegs.isEmpty()) {
                    RouteData routeData = routeService.getRouteData(routeId);
                    System.out.println(routeService);
                    if (routeData == null) {
                        System.err.println("No RouteData found for Route ID: " + routeId);
                        continue;
                    }
                    String polylineData = routeData.getPolylineData();
                    if (polylineData == null) {
                        System.out.println("Route is null: " + routeId);
                        continue;
                    }

                    List<Coordinate> routeCoordinates = PolylineDecoder.decodeAndConcatenatePolylinesFromString(polylineData);

                    startBusMovement(journeyInfo, journeyLegs, routeCoordinates, LocalTime.now());

                    System.out.println("Resource usage after starting bus movement:");
                    ResourceMonitor.printSystemMetrics();
                } else {
                    Platform.runLater(() -> System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode));
                }
            } catch (SQLException e) {
                Platform.runLater(() -> e.printStackTrace());
            }
        }
    }

    public void mapActiveBuses(LocalTime testTime, int timeWindowMinutes,String selectedRoute) throws SQLException, IOException, InterruptedException {

        clearMapView();

        List<Coordinate> coordinatesForRoute = new ArrayList<>();

        List<JourneyInfo> activeBuses = findActiveBusesInTimeFrame(testTime, timeWindowMinutes);
        for (JourneyInfo journeyInfo : activeBuses) {
            if (selectedRoute == null || journeyInfo.getRoute().equals(selectedRoute)) {
                double longitude = journeyInfo.getLongitude();
                double latitude = journeyInfo.getLatitude();
                coordinatesForRoute.add(new Coordinate(latitude, longitude));
            }
        }

        if (mapView == null) {
            System.err.println("MapView is not initialized. Cannot clear map view or map active buses.");
            return;
        }

        if (isFirstCall) {
            // do not plot on first call
            System.out.println("First call - not using selected polyline.");
            isFirstCall = false;

            if (!coordinatesForRoute.isEmpty()) {
                Coordinate centerCoordinate = coordinatesForRoute.get(0);
                System.out.println("center coordinates" + centerCoordinate);
                Platform.runLater(() -> mapView.setCenter(centerCoordinate));
            } else {
                System.out.println("No coordinates found for the selected route.");
            }

        } else {
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

        System.out.println("Initial resource usage:");
        ResourceMonitor.printSystemMetrics();
        long startTime = System.nanoTime();

        System.out.println(activeBuses);
        for (JourneyInfo journeyInfo : activeBuses) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();
            System.out.println(journeyInfo);
            try {

                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode);
                if (!journeyLegs.isEmpty()) {
                    System.out.println("routeservice" + routeService.getRouteData(routeId));
                    RouteData routeData = routeService.getRouteData(routeId);
                    if (routeData == null) {
                        System.err.println("No RouteData found for Route ID: " + routeId);
                        // handle the case where routeData is null
                        return;
                    }
                    String polylineData = routeData.getPolylineData();
                    if(polylineData == null){
                        System.out.println("route is null : " + routeId);
                        return;
                    }

                    List<Coordinate> routeCoordinates = PolylineDecoder.decodeAndConcatenatePolylinesFromString(polylineData);

                    System.out.println("Resource usage after polyline decoding:");
                    ResourceMonitor.printSystemMetrics();



                    startBusMovement(journeyInfo, journeyLegs, routeCoordinates,testTime);

                    System.out.println("Resource usage after starting bus movement:");
                    ResourceMonitor.printSystemMetrics();

                } else {
                    Platform.runLater(() -> System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode));
                }
            } catch (SQLException e) {
                Platform.runLater(() -> e.printStackTrace());
            }
            System.out.println(routeId);
            System.out.println("Number of markers in MarkersWithVJC: " + MarkersWithVJC.size());
            System.out.println("Number of markers in busMarkers: " + busMarkers.size());
            System.out.println("Number of rendered2 bus markers: " + renderedBusMarkers.size());
            System.out.println("Number of rendered polylines: " + renderedPolylines.size());
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println("Execution time: " + duration + " nanoseconds");
        System.out.println("Final resource usage:");
        ResourceMonitor.printSystemMetrics();

    }

    public void startBusMovement(JourneyInfo journeyInfo, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, LocalTime startTime) {
        String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();

        if (mapView == null) {
            System.err.println("MapView is not initialized. Cannot start bus movement. startBusMovement");
            return;
        }

        if (routeCoordinates == null || routeCoordinates.isEmpty()) {
            System.out.println("Route coordinates are null or empty. Cannot start bus movement.");
            return;
        }

        List<String> stopIds = journeyLegs.stream()
                .flatMap(leg -> List.of(leg.getFromStop(), leg.getToStop()).stream())
                .distinct()
                .toList();

        Map<String, Coordinate> stopCoordinates = stopService.getStopCoordinates(stopIds);

        if (stopCoordinates == null || stopCoordinates.isEmpty()) {
            System.out.println("Stop coordinates are null or empty. Cannot start bus movement.");
            return;
        }

        Marker busMarker = new Marker(getClass().getResource(markerImagePath))
                .setPosition(new Coordinate(journeyInfo.getLatitude(), journeyInfo.getLongitude()))
                .setVisible(true);

        MarkersWithVJC.put(vehicleJourneyCode, busMarker);
        busMarkers.add(busMarker);

        Platform.runLater(() -> mapView.addMarker(busMarker));

        final LocalTime[] currentTime = {startTime};

        Timeline busTimeline = new Timeline(new KeyFrame(Duration.millis(200), event -> {
            // increment currentTime by 200ms per tiuck
            currentTime[0] = currentTime[0].plusNanos(200000000);
            moveBus(busMarker, journeyLegs, routeCoordinates, stopCoordinates, currentTime[0]);
        }));
        busTimeline.setCycleCount(Timeline.INDEFINITE);
        busTimeline.play();
    }

    private void moveBus(Marker initialBusMarker, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, Map<String, Coordinate> stopCoordinates, LocalTime currentTime) {
        final Marker[] busMarker = {initialBusMarker};

        for (int i = 0; i < journeyLegs.size(); i++) {
            JourneyLeg leg = journeyLegs.get(i);
            LocalTime departureTime = leg.getDepartureTime();
            LocalTime nextDepartureTime = (i + 1 < journeyLegs.size()) ? journeyLegs.get(i + 1).getDepartureTime() : departureTime.plusMinutes(1);

            if (!currentTime.isBefore(departureTime) && !currentTime.isAfter(nextDepartureTime)) {
                long totalSeconds = departureTime.until(nextDepartureTime, ChronoUnit.SECONDS);
                long elapsedSeconds = departureTime.until(currentTime, ChronoUnit.SECONDS);
                double progress = (double) elapsedSeconds / totalSeconds;

                if (progress < 0) progress = 0;
                if (progress > 1) progress = 1;

                Coordinate fromStop = stopCoordinates.get(leg.getFromStop());
                Coordinate toStop = stopCoordinates.get(leg.getToStop());

                if (fromStop == null || toStop == null) {
                    System.out.println("Missing coordinates for stops");
                    return;
                }

                int[] fromSegmentIndices = findClosestSegmentIndices(routeCoordinates, fromStop);
                int[] toSegmentIndices = findClosestSegmentIndices(routeCoordinates, toStop);

                if (fromSegmentIndices[0] == -1 || toSegmentIndices[0] == -1 || fromSegmentIndices[0] >= toSegmentIndices[0]) {
                    return;
                }

                int segmentLength = toSegmentIndices[1] - fromSegmentIndices[0];
                double segmentProgress = progress * segmentLength;
                int currentSegmentIndex = fromSegmentIndices[0] + (int) segmentProgress;
                double segmentFraction = segmentProgress - (int) segmentProgress;

                if (currentSegmentIndex >= routeCoordinates.size() - 1) {
                    currentSegmentIndex = routeCoordinates.size() - 2;
                }

                Coordinate fromCoord = routeCoordinates.get(currentSegmentIndex);
                Coordinate toCoord = routeCoordinates.get(currentSegmentIndex + 1);

                double newLatitude = fromCoord.getLatitude() + segmentFraction * (toCoord.getLatitude() - fromCoord.getLatitude());
                double newLongitude = fromCoord.getLongitude() + segmentFraction * (toCoord.getLongitude() - fromCoord.getLongitude());

                Coordinate newPosition = new Coordinate(newLatitude, newLongitude);
                busMarker[0].setPosition(newPosition);

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