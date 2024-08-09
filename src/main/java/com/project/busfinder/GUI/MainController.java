package com.project.busfinder.GUI;


import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
//import com.gluonhq.maps.MapView;
import com.project.busfinder.Mapping.*;
import com.project.busfinder.util.CoordinateConverter;
import com.project.busfinder.util.PolylineDecoder;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import javafx.util.Duration;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.sothawo.mapjfx.event.MarkerEvent;

import static com.project.busfinder.Mapping.simulateBusLocations.findClosestDepartureTime;
import static com.project.busfinder.Mapping.simulateBusLocations.getJourneyLegs;



//TODO: allow user to click on bus for live tracking and route details. same for searching from the side bar.
// remove completed bus routes/ add new bus routes
//
public class MainController {
    @FXML
    private VBox sidePanel;
    @FXML
    private Pane background;

    @FXML
    private AnchorPane anchorPane;
    @FXML
    private VBox buttonPanel;

    @FXML
    private GridPane buttonPanelGridPane;

    @FXML
    private MapView mapView;


    private boolean isPanelOpen = false;

    private BusIconController BusIconController;
    private RouteService routeService;

    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private CoordinateLine coordinateLine;



    public MainController() {

    }

    @FXML
    public void initialize() throws IOException, InterruptedException {
        // initialise the RouteService and BusSimulator
        routeService = new RouteService();
        BusIconController = new BusIconController(mapView);
        BusIconController.initializeMap();


        Configuration configuration = Configuration.builder()
                .showZoomControls(true)
                .build();

        mapView.initialize(configuration);


        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                try {
                    Platform.runLater(() -> {
                        try {
                            onMapInitialized();
                        } catch (IOException | InterruptedException | SQLException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        setupPanels();
    }



    private void createBusMarkers() throws IOException, InterruptedException {
        List<JourneyInfo> busInfos = findClosestDepartureTime();
        BusIconController.createBusMarkers(busInfos);
    }


    private void onMapInitialized() throws IOException, InterruptedException, SQLException {

        List<JourneyInfo> journeyInfos = simulateBusLocations.findClosestDepartureTime();

        for (JourneyInfo journeyInfo : journeyInfos) {
            String routeId = journeyInfo.getRoute();
            String vehicleJourneyCode = journeyInfo.getVehicleJourneyCode();

            try {

                List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode);
                if (!journeyLegs.isEmpty()) {

                    RouteData routeData = routeService.getRouteData(routeId);
                    String polylineData = routeData.getPolylineData();

                    System.out.println("Polyline Data Length: " + polylineData.length());
                    System.out.println("Polyline Data: " + polylineData);

                    List<Coordinate> routeCoordinates = PolylineDecoder.decodeAndConcatenatePolylinesFromString(polylineData);

                    Platform.runLater(() -> {
                        //BusIconController.plotIndividualPolylines(polylineData);
                        //System.out.println("Polyline added to map view for route: " + routeId);
                    });


                    BusIconController.startBusMovement(journeyInfo, journeyLegs, routeCoordinates);
                } else {
                    Platform.runLater(() -> System.out.println("No journey legs found for Vehicle Journey Code: " + vehicleJourneyCode));
                }
            } catch (SQLException e) {
                Platform.runLater(() -> e.printStackTrace());
            }
        }
    }

    /**
     *
     * if the user changes the width of the panel , they run the risk of hiding the button and sidepanel
     *
     */
    private void setupPanels() {


        sidePanel.setTranslateX(-200);
        sidePanel.setVisible(false);
        buttonPanel.setTranslateX(sidePanel.getTranslateX() + 20);


        loadSidePanel("/com/project/busfinder/GUI/startingSidePanel.fxml");

        // bind panel sizes
        sidePanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanelGridPane.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));


    }


    public void loadSidePanel(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            VBox newSidePanel = loader.load();

            if ("/com/project/busfinder/GUI/startingSidePanel.fxml".equals(fxmlFile)) {
                SidePanelController sidePanelController = loader.getController();
                sidePanelController.setMainController(this);
            } else if ("/com/project/busfinder/GUI/trackBusPanel.fxml".equals(fxmlFile)) {
                TrackBusPanelController trackBusPanelController = loader.getController();
                trackBusPanelController.setMainController(this);
            }

            sidePanel.getChildren().clear();
            sidePanel.getChildren().add(newSidePanel);
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile);
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSidePanel(ActionEvent event) {
        final double gap = 10;
        double endX = gap;
        double startX = -200;

        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.3), sidePanel);
        TranslateTransition buttonTransition = new TranslateTransition(Duration.seconds(0.3), buttonPanel);

        if (isPanelOpen) {

            transition.setToX(startX);
            buttonTransition.setToX(startX + 20);
        } else {
            sidePanel.setVisible(true);
            transition.setToX(endX);
            buttonTransition.setToX(endX + gap);
        }

        transition.play();
        buttonTransition.play();
        isPanelOpen = !isPanelOpen;
    }

    @FXML
    private void loadStartingSidePanel(ActionEvent event) {
        loadSidePanel("/com/project/busfinder/GUI/startingSidePanel.fxml");
    }

    @FXML
    private void loadTrackBusPanel(ActionEvent event) {
        loadSidePanel("/com/project/busfinder/GUI/trackBusPanel.fxml");
    }

}
