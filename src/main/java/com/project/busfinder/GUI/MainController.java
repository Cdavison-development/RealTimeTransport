package com.project.busfinder.GUI;


//import com.gluonhq.maps.MapView;
import com.project.busfinder.Mapping_util.*;
        import com.sothawo.mapjfx.*;
        import javafx.animation.TranslateTransition;
        import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import javafx.util.Duration;
import javafx.scene.layout.*;
import java.io.IOException;
        import java.util.List;

        import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;
import static com.project.busfinder.util.readLiveLocation.processXmlResponse;


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
    private stopsPanelController stopsPanelController;
    private TrackBusPanelController trackBusPanelController;
    private RouteService routeService;

    private final String markerImagePath = "/com/project/busfinder/GUI/images/bus3.png";
    private CoordinateLine coordinateLine;

    private ProgressIndicator progressIndicator;

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

        // loading icon used when searching for bus
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(200, 200);
        progressIndicator.setVisible(false);  // initially hidden
        AnchorPane.setTopAnchor(progressIndicator, 10.0);
        AnchorPane.setRightAnchor(progressIndicator, 10.0);
        anchorPane.getChildren().add(progressIndicator);

        setupPanels();
        // add a listener to execute code once the map view is fully initialised
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // initialise the bus icon controller with the map view
                busIconController = new BusIconController(mapView);
                busIconController.setMainController(this);
                busIconController.setProgressIndicator(progressIndicator);
                busIconController.initializeMap();
                if (trackBusPanelController != null) {
                    busIconController.setTrackBusPanelController(trackBusPanelController);
                    stopsPanelController.setUseLiveRoutes(trackBusPanelController.getUseLiveRoutes());
                }
                // map active buses after the map is ready
                try {
                    // get live route information
                    String xmlResponse = fetchAndProcessResponse();
                    if (xmlResponse != null) {
                        List<LiveRouteInfo> liveRouteInfoList = processXmlResponse(xmlResponse);

                        // call method with the updated list
                        busIconController.startBusMovementUpdate(null, liveRouteInfoList,true);
                    } else {
                        System.out.println("Failed to fetch live route data.");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

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
                this.trackBusPanelController = trackBusPanelController;
                busIconController.setTrackBusPanelController(trackBusPanelController);
            } else if ("/com/project/busfinder/GUI/routeDetailsPanel.fxml".equals(fxmlFile)) {
                stopsPanelController stopsPanelController = loader.getController();
                stopsPanelController.setMainController(this);
                this.stopsPanelController = stopsPanelController;

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
    public stopsPanelController getStopsPanelController() {
        return stopsPanelController;
    }
}
