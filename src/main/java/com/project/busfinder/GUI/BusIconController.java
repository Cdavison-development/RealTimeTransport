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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private CoordinateLine routeLine;
    private Timeline timeline;

    private List<CoordinateLine> allPolylines = new ArrayList<>();
    public BusIconController(MapView mapView) {
        this.mapView = mapView;
        this.stopService = new StopService();
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
    public void startBusMovement(List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates,Coordinate initialCoordinate) {
        List<String> stopIds = journeyLegs.stream()
                .flatMap(leg -> List.of(leg.getFromStop(), leg.getToStop()).stream())
                .distinct()
                .toList();

        Map<String, Coordinate> stopCoordinates = stopService.getStopCoordinates(stopIds);


        placeMarkersAtStops(stopCoordinates);


        busMarker = new Marker(getClass().getResource(markerImagePath), -10, -10)
                .setPosition(initialCoordinate)
                .setVisible(true);

        Platform.runLater(() -> mapView.addMarker(busMarker));

        timeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            moveBus(journeyLegs, routeCoordinates,stopCoordinates);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
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
    private void moveBus(List<JourneyLeg> journeyLegs, List<Coordinate> routeCoordinates, Map<String, Coordinate> stopCoordinates) {
        LocalTime currentTime = LocalTime.now();
        //System.out.println("Current Time: " + currentTime);

        if (routeCoordinates.size() < 2) {
            System.out.println("Not enough points in polyline to determine route");
            return;
        }

        for (int i = 0; i < journeyLegs.size(); i++) {
            JourneyLeg leg = journeyLegs.get(i);
            LocalTime departureTime = leg.getDepartureTime();
            LocalTime nextDepartureTime = (i + 1 < journeyLegs.size()) ? journeyLegs.get(i + 1).getDepartureTime() : departureTime.plusMinutes(1);

            if (!currentTime.isBefore(departureTime) && !currentTime.isAfter(nextDepartureTime)) {
                long totalSeconds = departureTime.until(nextDepartureTime, ChronoUnit.SECONDS);
                long elapsedSeconds = departureTime.until(currentTime, ChronoUnit.SECONDS);
                double progress = (double) elapsedSeconds / totalSeconds;


                if (progress < 0) {
                    progress = 0;
                } else if (progress > 1) {
                    progress = 1;
                }

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
                    System.out.println("FromStop: " + fromStop + ", ToStop: " + toStop);
                    System.out.println("FromSegmentIndices: " + Arrays.toString(fromSegmentIndices) + ", ToSegmentIndices: " + Arrays.toString(toSegmentIndices));
                    System.out.println("Coordinates around FromSegmentIndices:");
                    for (int j = Math.max(0, fromSegmentIndices[0] - 5); j < Math.min(routeCoordinates.size(), fromSegmentIndices[0] + 5); j++) {
                       // System.out.println("Index " + j + ": " + routeCoordinates.get(j));
                    }
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

                //System.out.printf("Current Bus Position: Lat: %.6f, Lon: %.6f%n", newLatitude, newLongitude);

                //Platform.runLater(() -> mapView.setCenter(newPosition));
                break;
            } else {
                System.out.println("Current time is not within this leg's interval.");
            }
        }
    }

}


