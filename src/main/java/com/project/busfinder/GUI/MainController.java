package com.project.busfinder.GUI;


import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
//import com.gluonhq.maps.MapView;
import com.project.busfinder.Mapping.JourneyInfo;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import javafx.util.Duration;
import javafx.scene.layout.*;
import java.io.IOException;
import com.sothawo.mapjfx.event.MarkerEvent;


/**
 *
 * Next: fork from gluon or find way to change content of com.gluonhq.impl.maps.BaseMap zoom so that what is considered too small fits our boundaries
 *
 *
 */
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
    private static final Coordinate coordKarlsruheHarbour = new Coordinate(49.015511, 8.323497);

    private static final Coordinate coordKarlsruheCastle = new Coordinate(49.013517, 8.404435);
    private static final Coordinate coordKarlsruheStation = new Coordinate(48.993284, 8.402186);
    private static final Coordinate coordKarlsruheSoccer = new Coordinate(49.020035, 8.412975);
    private static final Coordinate coordKarlsruheUniversity = new Coordinate(49.011809, 8.413639);
    private static final Extent extentAllLocations = Extent.forCoordinates(coordKarlsruheCastle, coordKarlsruheHarbour, coordKarlsruheStation, coordKarlsruheSoccer);

    private final Marker markerKaHarbour;
    private final Marker markerKaCastle;
    private final Marker markerKaStation;
    private final Marker markerKaSoccer;
    private final Marker markerClick;

    public MainController() {
        markerKaHarbour = Marker.createProvided(Marker.Provided.BLUE).setPosition(coordKarlsruheHarbour).setVisible(
                false);
        markerKaCastle = Marker.createProvided(Marker.Provided.GREEN).setPosition(coordKarlsruheCastle).setVisible(
                false);
        markerKaStation =
                Marker.createProvided(Marker.Provided.RED).setPosition(coordKarlsruheStation).setVisible(false);

        markerClick = Marker.createProvided(Marker.Provided.ORANGE).setVisible(false);

        // a marker with a custom icon
        markerKaSoccer = new Marker(getClass().getResource("/com/project/busfinder/GUI/images/arrow4.png"), -20, -20).setPosition(coordKarlsruheSoccer)
                .setVisible(false);
    }

    @FXML
    public void initialize() {
        configureMapView();

        setupPanels();
    }


    private void configureMapView() {
        mapView.initialize(Configuration.builder().build());

        mapView.setCenter(new Coordinate(48.2081743, 16.3738189)); //example coordinates
        mapView.setZoom(12);

        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            Coordinate clickedCoord = event.getCoordinate();
            System.out.println("Map clicked at: " + clickedCoord.getLatitude() + ", " + clickedCoord.getLongitude());
        });

        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                addMarkersToMap();
            }
        });

    }

    private void addMarkersToMap() {

        mapView.addMarker(markerKaHarbour);
        mapView.addMarker(markerKaCastle);
        mapView.addMarker(markerKaStation);
        mapView.addMarker(markerClick);
        mapView.addMarker(markerKaSoccer);


        markerKaHarbour.setVisible(true);
        markerKaCastle.setVisible(true);
        markerKaSoccer.setVisible(true);




        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            Coordinate clickLocation = event.getCoordinate();
            markerClick.setPosition(clickLocation);
            markerClick.setVisible(true);

        });

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
