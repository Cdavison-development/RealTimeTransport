package com.project.busfinder.GUI;

import com.project.busfinder.readDatabase.getRouteDetails;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.project.busfinder.readDatabase.getRoutes.getLiveRoutes;

/**
 *
 * Next step is to get data based on chosen route.
 *
 *
 */
public class TrackBusPanelController {



    @FXML
    private Button returnButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button submitButton;

    @FXML
    private ComboBox<String> routesComboBox;

    @FXML
    private ComboBox<String> departureTimeComboBox;


    // database connection
    private Connection conn;

    //reference to the main controller, needed to change sidepanel content
    private MainController mainController;


    public void initialize() {
        //Connect to database
        initializeDatabaseConnection();
        //fill up the routes dropdown
        populateRoutesComboBox();

        routesComboBox.setOnAction(event -> handleSelectionChange());
    }

    private void initializeDatabaseConnection() {
        try {

            conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void populateRoutesComboBox() {
        List<String> routeLabels = new ArrayList<>();
        try {
            //get list of active routes
            Map<String, Boolean> liveRoutes = getLiveRoutes(conn);

            for (Map.Entry<String, Boolean> entry : liveRoutes.entrySet()) {
                String route = entry.getKey();
                Boolean isLive = entry.getValue();
                // mark each route with whether it's got live tracking or just simulated.
                String label = isLive ? route + " - Live tracking available" : route + " - simulated tracking available";
                System.out.println(label);
                // add to dropdown
                routeLabels.add(label);
            }

            // Sort the list of route labels
            routeLabels.sort(String::compareToIgnoreCase);

            // clear any existing items in the combo box
            routesComboBox.getItems().clear();

            // Add the sorted labels to the combo box
            routesComboBox.getItems().addAll(routeLabels);

            // set prompt text
            routesComboBox.setPromptText("Select a route");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Failed to populate routes combo box.");
        }
    }

    private String extractRouteName(String fullRoute) {
        if (fullRoute == null || fullRoute.isEmpty()) {
            return "";
        }

        // split the strings at delimiter "-"
        String[] parts = fullRoute.split(" - ");

        // return first part of the string which should be the route name
        return parts.length > 0 ? parts[0] : "";
    }

    private void handleSelectionChange() {
        chooseRoute();
    }

    private void chooseRoute() {
        String selectedRoute = routesComboBox.getValue();
        if (selectedRoute != null) {
            departureTimeComboBox.setVisible(true);

            // extract the route name
            String routeName = extractRouteName(selectedRoute);

            try {
                // create new instance of GetRouteDetails
                getRouteDetails fetcher = new getRouteDetails(conn);

                //call the getJourneyRouteInfo method
                List<getRouteDetails.JourneyRouteInfo> routeInfoList = fetcher.getJourneyRouteInfo(routeName);

                //clear the existing items in the combo box
                departureTimeComboBox.getItems().clear();

                if (selectedRoute.contains("Live")){
                    departureTimeComboBox.getItems().add("Track Live");
                }

                //populate the combobox with the formatted items
                for (getRouteDetails.JourneyRouteInfo info : routeInfoList) {
                    System.out.println(info);

                    String item = String.format("%s -> %s at %s", info.getFirstFromStopName(), info.getLastToStopName(), info.getEarliestDepartureTime().toString());
                    departureTimeComboBox.getItems().add(item);
                }

                if (!routeInfoList.isEmpty()) {
                    departureTimeComboBox.setPromptText("Select Departure Time");
                    departureTimeComboBox.getSelectionModel().selectFirst();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Failed to fetch route information.");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selection Warning");
            alert.setHeaderText(null);
            alert.setContentText("Please select a route.");
            alert.showAndWait();
        }
    }


    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void moveToStartingPage(ActionEvent event) {
        if (mainController != null) {
            mainController.loadSidePanel("/com/project/busfinder/GUI/startingSidePanel.fxml");
        }
    }

    @FXML
    public void reloadRoutesPage(ActionEvent event){
        if (mainController != null) {
            mainController.loadSidePanel("/com/project/busfinder/GUI/trackBusPanel.fxml");
        }
    }
}
