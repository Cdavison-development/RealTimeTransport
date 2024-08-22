package com.project.busfinder.GUI;


import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
//import com.gluonhq.maps.MapView;
import com.project.busfinder.Mapping.*;
import com.project.busfinder.util.CoordinateConverter;
import com.project.busfinder.util.PolylineDecoder;
import com.project.busfinder.util.ResourceMonitor;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.sothawo.mapjfx.event.MarkerEvent;

import static com.project.busfinder.Mapping.simulateBusLocations.*;


//TODO: allow user to click on bus for live tracking and route details. same for searching from the side bar.
// remove completed bus routes/ add new bus routes
//
// if the user wants to simulate a bus route, it may be better, and easier, to just re-render all objects
// and simulate as if the time the simulated bus route begins is this current time.

/**
 *  Things to remember since most recent push (14/08/24)
 *
 *  classes to be handled with new datasets
 *
 *
 * getLive routes function in getRoutes Class and its getter class
 *
 *
 * all functions in simulate BusLocation
 *
 * add a day component to track bus panel.
 * return the chosen item
 *
 * This will be used to specify what routes table we use
 *
 * following this, any function that calls any of the above
 *
 * mapActive buses needs to take a day parameter
 *
 * if buses are showing signs of being late, switch to live tracking.
 *
 * if the slected day allows live tracking, switch to live tracking on day selection
 *
 * rather than resetting the marker list on re-rendering of buses, marker list does not completely reset.
 * performing multiple renders does not return render list to 0
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

    private BusIconController busIconController;
    private RouteService routeService;

    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private CoordinateLine coordinateLine;



    public MainController() {

    }

    @FXML
    public void initialize() throws IOException, InterruptedException {
        // initialise the route service
        routeService = new RouteService();

        // configure the map view with zoom controls enabled
        Configuration configuration = Configuration.builder()
                .showZoomControls(true)
                .build();
        mapView.initialize(configuration);

        // add a listener to execute code once the map view is fully initialised
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // initialise the bus icon controller with the map view
                busIconController = new BusIconController(mapView);
                busIconController.initializeMap();

                // map active buses after the map is ready
                try {
                    LocalDate date = LocalDate.of(2024, 8, 18); // Example date (for "Sunday")
                    LocalTime testTime = LocalTime.of(8, 4, 0); // Example time


                    LocalDateTime testDateTime = LocalDateTime.of(date, testTime);


                    busIconController.mapActiveBuses("Sunday", testDateTime, 5, null,null); // initialise after the map is ready
                } catch (IOException | InterruptedException | SQLException e) {
                    throw new RuntimeException(e); // handle exceptions by throwing a runtime exception
                }
            }
        });

        // set up any additional panels or UI components
        setupPanels();
    }


    private void setupPanels() {
        // initially hide the side panel by translating it off-screen and setting it to invisible
        sidePanel.setTranslateX(-200);
        sidePanel.setVisible(false);

        // position the button panel slightly offset from the hidden side panel
        buttonPanel.setTranslateX(sidePanel.getTranslateX() + 20);

        // load the initial content for the side panel
        loadSidePanel("/com/project/busfinder/GUI/startingSidePanel.fxml");

        // bind the height of the panels to a percentage of the anchor pane's height
        sidePanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanelGridPane.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
    }

    public BusIconController getBusIconController() {
        return busIconController;
    }

    public void loadSidePanel(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            VBox newSidePanel = loader.load();

            // set the main controller for the loaded side panel based on the FXML file
            if ("/com/project/busfinder/GUI/startingSidePanel.fxml".equals(fxmlFile)) {
                SidePanelController sidePanelController = loader.getController();
                sidePanelController.setMainController(this);
            } else if ("/com/project/busfinder/GUI/trackBusPanel.fxml".equals(fxmlFile)) {
                TrackBusPanelController trackBusPanelController = loader.getController();
                trackBusPanelController.setMainController(this);
            }

            // replace the existing content of the side panel with the new content
            sidePanel.getChildren().clear();
            sidePanel.getChildren().add(newSidePanel);
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile);
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSidePanel(ActionEvent event) {
        final double gap = 10;  // defines the gap between the side panel and the button panel
        double endX = gap;  // target position when the panel is opened
        double startX = -200;  // initial off-screen position when the panel is closed

        // create transitions for both the side panel and the button panel
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.3), sidePanel);
        TranslateTransition buttonTransition = new TranslateTransition(Duration.seconds(0.3), buttonPanel);

        // check if the panel is currently open or closed
        if (isPanelOpen) {
            // move the side panel off-screen and adjust the button panel accordingly
            transition.setToX(startX);
            buttonTransition.setToX(startX + 20);
        } else {
            // move the side panel on-screen and adjust the button panel accordingly
            sidePanel.setVisible(true);
            transition.setToX(endX);
            buttonTransition.setToX(endX + gap);
        }

        // play the transitions
        transition.play();
        buttonTransition.play();

        // toggle the panel's open/closed state
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
