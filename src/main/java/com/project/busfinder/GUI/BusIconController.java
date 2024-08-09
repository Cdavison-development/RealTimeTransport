package com.project.busfinder.GUI;
import com.project.busfinder.Mapping.JourneyLeg;
import com.project.busfinder.Mapping.StopService;
import com.sothawo.mapjfx.Coordinate;
import com.project.busfinder.Mapping.JourneyInfo;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.project.busfinder.Mapping.polylineHelpers.findClosestSegmentIndices;
import static com.project.busfinder.util.PolylineDecoder.decodePolylinesIndividually;

public class BusIconController {

    private MapView mapView;

    private Marker busMarker;
    private List<Coordinate> routeCoordinates;
    private int currentIndex = 0;
    private boolean forwardDirection = true;
    private final StopService stopService;
    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private final List<Marker> busMarkers = new ArrayList<>();
    private Map<String, Marker> busMarkerMAPs;
    private CoordinateLine routeLine;
    private Timeline timeline;

    private List<CoordinateLine> allPolylines = new ArrayList<>();
    public BusIconController(MapView mapView) {
        this.mapView = mapView;
        this.stopService = new StopService();
        busMarkerMAPs = new HashMap<>();
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
    }

    public void initializeMap() {
        mapView.initialize(Configuration.builder().build());

        mapView.setCenter(new Coordinate(53.4013, -3.057244)); // Set initial map center
        mapView.setZoom(12);

        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            Coordinate clickedCoord = event.getCoordinate();
            System.out.println("Map clicked at: " + clickedCoord.getLatitude() + ", " + clickedCoord.getLongitude());
        });

        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                addMarkersToMap();
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

    private void addMarkersToMap() {
        for (Marker busMarker : busMarkers) {
            mapView.addMarker(busMarker);
        }
    }
    public static CoordinateLine createCoordinateLine(List<Coordinate> coordinates) {
        CoordinateLine coordinateLine = new CoordinateLine(coordinates)
                .setVisible(true)
                .setColor(Color.DODGERBLUE)
                .setWidth(5);
        return coordinateLine;
    }

    public void startBusMovement(JourneyInfo journeyInfo, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates) {
        String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();


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


        placeMarkersAtStops(stopCoordinates);


        Marker busMarker = new Marker(getClass().getResource(markerImagePath), -10, -10)
                .setPosition(new Coordinate(journeyInfo.getLatitude(), journeyInfo.getLongitude()))
                .setVisible(true);

        busMarkerMAPs.put(vehicleJourneyCode, busMarker);

        Platform.runLater(() -> mapView.addMarker(busMarker));


        Timeline busTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            moveBus(busMarker, journeyLegs, routeCoordinates, stopCoordinates);
        }));
        busTimeline.setCycleCount(Timeline.INDEFINITE);
        busTimeline.play();
    }



    private void placeMarkersAtStops(Map<String, Coordinate> stopCoordinates) {
        Platform.runLater(() -> {
            stopCoordinates.forEach((stopId, coordinate) -> {

                mapView.addMarker(busMarker);
            });
        });
    }

    public void plotIndividualPolylines(String json) {
        List<List<Coordinate>> allPolylinesCoordinates = decodePolylinesIndividually(json);

        for (List<Coordinate> polyline : allPolylinesCoordinates) {
            CoordinateLine coordinateLine = new CoordinateLine(polyline)
                    .setVisible(true)
                    .setColor(Color.DODGERBLUE)
                    .setWidth(7);

            allPolylines.add(coordinateLine);

            Platform.runLater(() -> mapView.addCoordinateLine(coordinateLine));
        }
    }

    private void setupMapExtentHandler() {
        mapView.addEventHandler(MapViewEvent.MAP_EXTENT, event -> {
            // force re-render
            for (CoordinateLine line : allPolylines) {
                line.setVisible(false);  // hide temporarily
                line.setVisible(true);   // re-render
            }
            System.out.println("Polylines re-rendered after extent change.");
        });
    }
    private void moveBus(Marker busMarker, List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, Map<String, Coordinate> stopCoordinates) {
        LocalTime currentTime = LocalTime.now();

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
                    System.out.println("Invalid indices for from/to stops in polyline");
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
                busMarker.setPosition(newPosition);


                // Platform.runLater(() -> mapView.setCenter(newPosition));
                break;
            }
        }
    }

}