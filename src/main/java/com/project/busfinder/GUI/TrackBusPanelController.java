package com.project.busfinder.GUI;

import com.project.busfinder.Mapping.simulateBusLocations;
import com.project.busfinder.readDatabase.getRouteDetails;
import com.sothawo.mapjfx.Configuration;
import com.sothawo.mapjfx.MapView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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


    private MainController mainController;
    @FXML
    private BusIconController busIconController;
    @FXML
    private MapView mapView;



    @FXML
    private void initialize() {
        initializeDatabaseConnection();

        populateRoutesComboBox();


        routesComboBox.setOnAction(event -> handleSelectionChange());


        submitButton.setOnAction(event -> {
            try {
                onSwitchViewButtonClick();
            } catch (IOException | InterruptedException | SQLException e) {
                e.printStackTrace();
            }
        });


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

    // we need to lock onto the specific bus chosen, and map all buses that would be active at the same time
    //lock onto the specific bus by returning route_id and journey. return time so we can find time it would be mapped.
    private void chooseRoute() {
        String selectedRoute = routesComboBox.getValue();
        if (selectedRoute != null) {
            departureTimeComboBox.setVisible(true);


            String routeName = extractRouteName(selectedRoute);

            try {

                getRouteDetails fetcher = new getRouteDetails(conn);

                List<getRouteDetails.JourneyRouteInfo> routeInfoList = fetcher.getJourneyRouteInfo(routeName);


                departureTimeComboBox.getItems().clear();

                if (selectedRoute.contains("Live")) {
                    departureTimeComboBox.getItems().add("Track Live");
                }


                for (getRouteDetails.JourneyRouteInfo info : routeInfoList) {
                    System.out.println(info);

                    String item = String.format("%s -> %s at %s (%s)", info.getFirstFromStopName(), info.getLastToStopName(),
                            info.getEarliestDepartureTime().toString(), info.getVehicleJourneyCode());
                    departureTimeComboBox.getItems().add(item);
                }

                if (!routeInfoList.isEmpty()) {
                    departureTimeComboBox.setPromptText("Select Departure Time");
                    departureTimeComboBox.getSelectionModel().selectFirst();
                }


                departureTimeComboBox.setOnAction(event -> {
                    String selectedItem = departureTimeComboBox.getValue();
                    System.out.println(selectedItem);
                    if (selectedItem != null) {

                        String[] parts = selectedItem.split(" at ");

                        if (parts.length == 2) {
                            String stopSection = parts[0].trim();
                            String timeAndJourneyCode = parts[1].trim();


                            String[] timeAndCodeParts = timeAndJourneyCode.split(" ");
                            if (timeAndCodeParts.length >= 2) {
                                String departureTimeString = timeAndCodeParts[0].trim();
                                String vehicleJourneyCode = timeAndCodeParts[1].replaceAll("[()]", "").trim();  // Remove brackets


                                LocalTime departureTime = null;
                                try {
                                    departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm"));
                                } catch (DateTimeParseException e) {
                                    System.err.println("Invalid departure time format: " + departureTimeString);
                                    e.printStackTrace();
                                }


                                String[] stopParts = stopSection.split("->");
                                if (stopParts.length == 2) {
                                    String fromStop = stopParts[0].trim();
                                    String toStop = stopParts[1].trim();


                                    System.out.println("Selected From Stop: " + fromStop);
                                    System.out.println("Selected To Stop: " + toStop);
                                    System.out.println("Selected Departure Time: " + departureTime);
                                    System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                                    System.out.println("Selected Route ID: " + routeName); // Route ID is the routeName

                                }
                            }
                        }
                    }
                });

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

    @FXML
    private void onSwitchViewButtonClick() throws IOException, InterruptedException, SQLException {

        if (busIconController == null) {
            System.err.println("BusIconController is not initialized.");
            return;
        }


        String selectedRoute = routesComboBox.getValue();
        String selectedValue = departureTimeComboBox.getValue();

        if (selectedValue.equals("Track Live")) {

            busIconController.mapLiveRoutesWithJourneyInfos(extractRouteName(selectedRoute));
        } else {

            String[] parts = selectedValue.split(" at ");

            if (parts.length == 2) {
                String stopSection = parts[0].trim();
                String timeAndJourneyCode = parts[1].trim();


                String[] timeAndCodeParts = timeAndJourneyCode.split(" ");
                if (timeAndCodeParts.length >= 2) {
                    String departureTimeString = timeAndCodeParts[0].trim();
                    String vehicleJourneyCode = timeAndCodeParts[1].replaceAll("[()]", "").trim();  // Remove brackets


                    LocalTime departureTime = null;
                    try {
                        departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm"));
                    } catch (DateTimeParseException e) {
                        System.err.println("Invalid departure time format: " + departureTimeString);
                        e.printStackTrace();
                    }


                    String routeName = extractRouteName(selectedRoute);
                    String[] stopParts = stopSection.split("->");
                    if (stopParts.length == 2) {
                        String fromStop = stopParts[0].trim();
                        String toStop = stopParts[1].trim();


                        System.out.println("Selected From Stop: " + fromStop);
                        System.out.println("Selected To Stop: " + toStop);
                        System.out.println("Selected Departure Time: " + departureTime);
                        System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                        System.out.println("Selected Route ID: " + routeName); // Route ID is the routeName


                        busIconController.mapActiveBuses(departureTime, 5,extractRouteName(selectedRoute));  // Adjust the parameters as needed
                    }
                }
            }
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
        this.busIconController = mainController.getBusIconController(); // Obtain the BusIconController from MainController
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
