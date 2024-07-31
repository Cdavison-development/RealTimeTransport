package com.project.busfinder.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Map;

import static com.project.busfinder.readDatabase.getRoutes.getLiveRoutes;

/**
 *
 * Next step is to get data based on chosen route.
 *
 *
 */
public class TrackBusPanelController {

    /**
     * defining FXML components for the Track bus panel
     */

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
    }

    private void initializeDatabaseConnection() {
        try {
            //attempt to connect to database at the specified path
            conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void populateRoutesComboBox() {
        try {
            //get list of active routes
            Map<String, Boolean> liveRoutes = getLiveRoutes(conn);

            for (Map.Entry<String, Boolean> entry : liveRoutes.entrySet()) {
                String route = entry.getKey();
                Boolean isLive = entry.getValue();
                // mark each route with whether it's got live tracking or just simulated.
                String label = isLive ? route + " - Live tracking available" : route + " - simulated tracking available";
                System.out.println(label); // logging
                // add to dropdown
                routesComboBox.getItems().add(label);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Failed to populate routes combo box.");
        }
    }

    @FXML
    private void handleGetSelectedRoute() {

        String selectedRoute = routesComboBox.getValue();

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
}
