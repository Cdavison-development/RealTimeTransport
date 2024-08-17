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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

import javafx.application.Platform;
import static com.project.busfinder.readDatabase.getRoutes.getLiveRoutes;

/**
 *
 * testing errors:
 *
 * zoom function when choosing bus needs fixing.
 *
 * if a new day is selected we should either hold what was previously in the combo boxes or
 * remove content from both
 *
 * re-render routes and departure time combos when going between weekdays/ sat / sun - kinda fixed, combobox prompt text disappears tho
 *
 * consider how we handle routes that go over two days - if bus is labelled as saturday, but crosses over 00:00:00, dynamically set date to sunday for these routes.
 *
 * figure out why some routes, such as saturday 10 bus (23:11) renders to many more busmarkers than markers with VJC
 *
 * are seconds handled correctly?
 *
 *  if buses are showing signs of being late, switch to live tracking.
 *
 *  if the slected day allows live tracking, switch to live tracking on day selection
 *
 *  certain circular routes dont work
 *
 *  we see repeating routes and journey codes, just with different times.
 */
public class TrackBusPanelController {



    @FXML
    private Button returnButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button submitButton;
    @FXML

    private ComboBox<String> dayComboBox;

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

    private String lastSelectedDay = null;

    @FXML
    private void initialize() {
        // initialise the database connection
        initializeDatabaseConnection();

        // populate the day combo box initially
        populateChooseDay();

        // set an action listener for the day combo box
        dayComboBox.setOnAction(event -> {
            String selectedDay = getDay();
            System.out.println("Selected Day: " + selectedDay);

            // only repopulate if the selected day is different from the last selected day
            if (selectedDay != null && (!isWeekday(selectedDay) || lastSelectedDay == null || !isWeekday(lastSelectedDay))) {
                System.out.println("Switching between weekday and weekend");
                populateRoutesComboBox();
                routesComboBox.setVisible(true);
            }

            // cache the current selected day
            lastSelectedDay = selectedDay;
        });

        // set an action listener for the routes combo box
        routesComboBox.setOnAction(event -> {
            handleSelectionChange();
            // make the departure time combo box visible after selecting a route
            departureTimeComboBox.setVisible(true);
            routesComboBox.setPromptText("Select a route");
            departureTimeComboBox.setPromptText("Select a departure time");
        });

        // set an action listener for the submit button
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
            // establish a connection to the SQLite database
            conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getDay() {
        String selectedDay = dayComboBox.getValue();
        if (selectedDay == null) {
            // return default day
            return LocalDate.now().getDayOfWeek().toString(); // default to the current day
        }
        return selectedDay.split(" \\(")[0];
    }
    private boolean isWeekday(String day) {
        if (day == null) {
            return false;
        }
        return day.equals("Monday") || day.equals("Tuesday") || day.equals("Wednesday") ||
                day.equals("Thursday") || day.equals("Friday");
    }
    private void populateRoutesComboBox() {
        List<String> routeLabels = new ArrayList<>();
        try {
            // get day
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            String currentDayString = currentDay.toString();

            // get list of active routes
            Map<String, Boolean> liveRoutes = getLiveRoutes(conn);

            for (Map.Entry<String, Boolean> entry : liveRoutes.entrySet()) {
                String route = entry.getKey();
                Boolean isLive = entry.getValue();

                // mark each route with whether it's got live tracking or just simulated.
                String label;
                if (isLive && getDay().equalsIgnoreCase(currentDayString)) {
                    label = route + " - Live tracking available";
                } else {
                    label = route + " - simulated tracking available";
                }

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

    public void populateChooseDay() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        List<String> daysOfWeek = List.of(
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
                "Sunday"
        );

        // clear existing items
        dayComboBox.getItems().clear();

        // add items to the ComboBox, appending "(Live tracking available)" to today's day
        for (String day : daysOfWeek) {
            if (DayOfWeek.valueOf(day.toUpperCase()) == today) {
                dayComboBox.getItems().add(day + " (Live tracking available)");
            } else {
                dayComboBox.getItems().add(day + " (simulated tracking available)");
            }
        }
    }
    private void chooseDay() {
        // Get the selected day
        String selectedDay = dayComboBox.getValue();
        System.out.println("chooseDay() called. Selected Day: " + selectedDay); // Debugging

        if (selectedDay != null) {
            Platform.runLater(() -> {
                System.out.println("Setting routesComboBox to visible"); // Debugging
                routesComboBox.setVisible(true);
            });
        }
    }

    // we need to lock onto the specific bus chosen, and map all buses that would be active at the same time
    //lock onto the specific bus by returning route_id and journey. return time so we can find time it would be mapped.
    private void chooseRoute() {

        chooseDay();
        departureTimeComboBox.getItems().clear();

        departureTimeComboBox.setPromptText("Select a departure time");

        if (routesComboBox.isVisible() && dayComboBox.getValue() != null) {

            String selectedRoute = routesComboBox.getValue();
            System.out.println("Selected Route: " + selectedRoute);
            System.out.println("Selected Day: " + getDay());

            if (selectedRoute != null) {
                departureTimeComboBox.setVisible(true);

            // extract the route name from the selected route
            String routeName = extractRouteName(selectedRoute);

            try {
                    // create an instance of getRouteDetails to fetch route information
                    getRouteDetails fetcher = new getRouteDetails(conn);

                    // clear the previous items in the departure time combo box
                    departureTimeComboBox.getItems().clear();

                    List<getRouteDetails.JourneyRouteInfo> routeInfoList;


                    if (selectedRoute.contains("Live") && dayComboBox.getValue().contains("Live")) {
                        departureTimeComboBox.getItems().add("Track Live");
                        System.out.println("track live true");
                        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
                        routeInfoList = fetcher.getJourneyRouteInfo(routeName, String.valueOf(dayOfWeek));
                    } else {
                        routeInfoList = fetcher.getJourneyRouteInfo(routeName, getDay());
                    }

                    System.out.println("Number of routes found: " + routeInfoList.size());

                // populate the combo box with the journey route information
                    for (getRouteDetails.JourneyRouteInfo info : routeInfoList) {

                        System.out.println("Raw departure time object: " + info.getEarliestDepartureTime());


                        String departureTimeString = info.getEarliestDepartureTime().toString();


                        System.out.println("Raw departure time string: " + departureTimeString);

                        // if seconds are left out, append
                        if (departureTimeString.length() == 5) {
                            departureTimeString += ":00";
                            System.out.println("Adjusted departure time string (added seconds): " + departureTimeString);
                        } else {
                            System.out.println("Departure time string already includes seconds: " + departureTimeString);
                        }


                        String item = String.format("%s -> %s at %s (%s)",
                                info.getFirstFromStopName(),
                                info.getLastToStopName(),
                                departureTimeString,
                                info.getVehicleJourneyCode());


                        System.out.println("Final item to add to combo box: " + item);


                        departureTimeComboBox.getItems().add(item);
                    }

                    // if the route information list is not empty, set the prompt text and select the first item
                    if (!routeInfoList.isEmpty()) {
                        departureTimeComboBox.setPromptText("Select Departure Time");
                        departureTimeComboBox.getSelectionModel().selectFirst();
                    }


                    System.out.println("Items in Departure Time ComboBox: " + departureTimeComboBox.getItems().size());

                // set an action listener for the departure time combo box
                    departureTimeComboBox.setOnAction(event -> {
                        String selectedItem = departureTimeComboBox.getValue();
                        if (selectedItem != null) {

                            handleDepartureTimeSelection(selectedItem, routeName);
                        }
                    });

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Failed to fetch route information.");
                }
            } else {
                showAlert("Selection Warning", "Please select a route.");
            }
        }
    }

    private void handleDepartureTimeSelection(String selectedItem, String routeName) {
        // split the selected item to extract stop and time information
        String[] parts = selectedItem.split(" at ");
        System.out.println("parts" + Arrays.toString(parts));
        if (parts.length == 2) {
            String stopSection = parts[0].trim();
            String timeAndJourneyCode = parts[1].trim();

            String[] timeAndCodeParts = timeAndJourneyCode.split(" ");
            if (timeAndCodeParts.length >= 2) {
                String departureTimeString = timeAndCodeParts[0].trim();
                String vehicleJourneyCode = timeAndCodeParts[1].replaceAll("[()]", "").trim();  // Remove brackets


                if (departureTimeString.length() == 5) {
                    departureTimeString += ":00";
                }

                //  parse the departure time
                LocalTime departureTime = null;
                try {
                    departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid departure time format: " + departureTimeString);
                    e.printStackTrace();
                }
                // split to get from and to stop information
                String[] stopParts = stopSection.split("->");
                System.out.println("stop parts: " + Arrays.toString(stopParts));
                if (stopParts.length == 2) {
                    String fromStop = stopParts[0].trim();
                    String toStop = stopParts[1].trim();

                    // output selected details
                    System.out.println("Selected From Stop: " + fromStop);
                    System.out.println("Selected To Stop: " + toStop);
                    System.out.println("Selected Departure Time: " + departureTime);
                    System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                    System.out.println("Selected Route ID: " + routeName); // Route ID is the routeName
                }
            }
        }
    }

    // show a warning alert if no route is selected
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();

        routesComboBox.setPromptText("Select a route");
        departureTimeComboBox.setPromptText("Select a departure time");
    }


    //unsure if addition of days of week table means this needs to be changed.
    @FXML
    private void onSwitchViewButtonClick() throws IOException, InterruptedException, SQLException {
        // check if the bus icon controller is initialised
        if (busIconController == null) {
            System.err.println("BusIconController is not initialized.");
            return;
        }

        // retrieve the selected route and departure time from the combo boxes
        String selectedRoute = routesComboBox.getValue();
        String selectedValue = departureTimeComboBox.getValue();

        // handle the case where "Track Live" is selected
        if (selectedValue.equals("Track Live")) {
            busIconController.mapLiveRoutesWithJourneyInfos(extractRouteName(selectedRoute));
        } else {

            // parse the selected departure time and journey code
            String[] parts = selectedValue.split(" at ");

            if (parts.length == 2) {
                String stopSection = parts[0].trim();
                String timeAndJourneyCode = parts[1].trim();

                // further split to extract departure time and vehicle journey code
                String[] timeAndCodeParts = timeAndJourneyCode.split(" ");
                if (timeAndCodeParts.length >= 2) {
                    String departureTimeString = timeAndCodeParts[0].trim();
                    String vehicleJourneyCode = timeAndCodeParts[1].replaceAll("[()]", "").trim();  // remove brackets

                    // parse the departure time
                    LocalTime departureTime = null;
                    try {
                        departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                    } catch (DateTimeParseException e) {
                        System.err.println("Invalid departure time format: " + departureTimeString);
                        e.printStackTrace();
                    }

                    // extract the route name and stops
                    String routeName = extractRouteName(selectedRoute);
                    String[] stopParts = stopSection.split("->");
                    if (stopParts.length == 2) {
                        String fromStop = stopParts[0].trim();
                        String toStop = stopParts[1].trim();

                        System.out.println("Selected From Stop: " + fromStop);
                        System.out.println("Selected To Stop: " + toStop);
                        System.out.println("Selected Departure Time: " + departureTime);
                        System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                        System.out.println("Selected Route ID: " + routeName);

                        // map active buses for the selected route and departure time
                        busIconController.mapActiveBuses(getDay(),departureTime, 5, extractRouteName(selectedRoute));
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
        this.busIconController = mainController.getBusIconController();
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
