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

        routeService = new RouteService();




        Configuration configuration = Configuration.builder()
                .showZoomControls(true)
                .build();
        mapView.initialize(configuration);


        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                busIconController = new BusIconController(mapView);
                busIconController.initializeMap();
                try {
                    LocalTime TestTime = LocalTime.of(8, 04);
                    busIconController.mapActiveBuses(TestTime,5,null); // Initialize after the map is ready
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

            }
        });


        setupPanels();
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

    public BusIconController getBusIconController() {
        return busIconController;
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
