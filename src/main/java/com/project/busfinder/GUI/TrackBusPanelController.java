package com.project.busfinder.GUI;

import com.project.busfinder.Mapping_util.JourneyLeg;
import com.project.busfinder.Mapping_util.LiveRouteInfo;
import com.project.busfinder.Mapping_util.vjcAndDay;
import com.project.busfinder.readDatabase.getRouteDetails;
import com.sothawo.mapjfx.MapView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import javafx.application.Platform;

import static com.project.busfinder.Mapping_util.simulateBusLocations.getJourneyLegs;
import static com.project.busfinder.Mapping_util.simulateBusLocations.getVehicleJourneyCode;
import static com.project.busfinder.helperFunctions.getStopName.StopName;
import static com.project.busfinder.readDatabase.getRoutes.getLiveRoutes;
import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;
import static com.project.busfinder.util.readLiveLocation.processXmlResponse;

/**
 *
 * testing errors:
 *
 * re-render routes and departure time combos when going between weekdays/ sat / sun - kinda fixed, combobox prompt text disappears tho
 *
 *  change cursor to indicate hover on hover
 *
 *
 * track live isnt set up to handle two routes of the same route ID
 *
 * handle UI change when swapping between non-live and live days
 *
 * add plan route func
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
    private ComboBox<String> RendertimeComboBox;

    @FXML
    private ComboBox<String> departureTimeComboBox;


    // database connection
    private Connection conn;


    private MainController mainController;
    @FXML
    private BusIconController busIconController;

    private stopsPanelController stopsPanelController;
    @FXML
    private MapView mapView;
    private Task<Void> activeBusTask = null;

    private String lastSelectedDay = null;

    private String selectedRouteName;
    private String selectedVehicleJourneyCode;
    private String selectedDay;
    private boolean useLiveRoutes = true;
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
                        RendertimeComboBox.setVisible(true);
                    }

                    System.out.println("Number of routes found: " + routeInfoList.size());

                // populate the combo box with the journey route information
                for (getRouteDetails.JourneyRouteInfo info : routeInfoList) {
                    // lookup stop names
                    String firstFromStopName = StopName(info.getFirstFromStop());
                    String lastToStopName = StopName(info.getLastToStop());

                    // format the earliest departure time
                    String formattedDepartureTime = info.getEarliestDepartureDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    // Construct the item string for the combo box
                    String item = String.format("%s -> %s at %s (%s) ",
                            firstFromStopName,
                            lastToStopName,
                            formattedDepartureTime,
                            info.getVehicleJourneyCode());

                    // Add the item to the combo box
                    departureTimeComboBox.getItems().add(item);
                }

                    // set prompt text
                    if (!routeInfoList.isEmpty()) {
                        departureTimeComboBox.setPromptText("Select Departure Time");
                        departureTimeComboBox.getSelectionModel().selectFirst();
                    }
                    departureTimeComboBox.setPromptText("Select Departure Time");

                    System.out.println("Items in Departure Time ComboBox: " + departureTimeComboBox.getItems().size());

                // set an action listener for the departure time combo box, populate
                // RenderTime combo box on action
                departureTimeComboBox.setOnAction(event -> {
                    String selectedItem = departureTimeComboBox.getValue();
                    if (selectedItem != null) {
                        String vehicleJourneyCode = handleDepartureTimeSelection(selectedItem, routeName);
                        try {
                            populateRouteTimes(routeName, vehicleJourneyCode, getDay());
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                routesComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    System.out.println("Route ComboBox changed. Old Value: " + oldValue + ", New Value: " + newValue);
                    // re-run function when choice changes
                    chooseRoute();
                });

                RendertimeComboBox.setPromptText("Choose bus stop");

                   try {
                        String vehicleJourneyCode = handleDepartureTimeSelection(departureTimeComboBox.getValue(), routeName);
                        populateRouteTimes(routeName, vehicleJourneyCode, getDay());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Failed to fetch route information.");
                }
            } else {
                showAlert("Selection Warning", "Please select a route.");
            }
        }
    }
    public void populateRouteTimes(String routeId, String vehicleJourneyCode, String day) throws SQLException {
            RendertimeComboBox.getItems().clear();
            RendertimeComboBox.setPromptText("Choose bus stop");
            // get journey legs
            List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, day);
            System.out.println("loadStopTimes journeyLegs: " + journeyLegs);

        // convert the journey legs to strings for display in the ListView
            ObservableList<String> stopTimeStrings = FXCollections.observableArrayList();
            for (JourneyLeg leg : journeyLegs) {
                String stopName = StopName(leg.getFromStop());
                String displayText = stopName + " - " + leg.getDepartureTime().toString();
                stopTimeStrings.add(displayText);
                System.out.println("Adding to ListView: " + displayText);
                if (RendertimeComboBox != null) {
                    RendertimeComboBox.getItems().add(displayText);
                }
            }

            System.out.println("Setting ListView items...");
            System.out.println("ListView should be updated.");
    }

    private String handleDepartureTimeSelection(String selectedItem, String routeName) {
        // split the selected item to extract stop and time information
        String[] parts = selectedItem.split(" at ");
        System.out.println("parts: " + Arrays.toString(parts));

        if (parts.length == 2) {
            String stopSection = parts[0].trim();
            String timeAndJourneyCode = parts[1].trim();

            // split to get the date-time and vehicle journey code
            int lastSpaceIndex = timeAndJourneyCode.lastIndexOf(' ');
            if (lastSpaceIndex != -1) {
                String departureTimeString = timeAndJourneyCode.substring(0, lastSpaceIndex).trim();
                String vehicleJourneyCode = timeAndJourneyCode.substring(lastSpaceIndex + 1).replaceAll("[()]", "").trim();  // Remove parentheses

                System.out.println("Extracted Departure Time String: " + departureTimeString);
                System.out.println("Extracted Vehicle Journey Code: " + vehicleJourneyCode);

                //  parse the departure time
                LocalDateTime departureTime = null;
                try {
                    departureTime = LocalDateTime.parse(departureTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
                return vehicleJourneyCode;
            } else {
                System.err.println("Could not split time and journey code correctly.");
            }
        } else {
            System.err.println("Unexpected format for selected item: " + selectedItem);
        }
        return null;
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

        if (selectedValue.equals("Track Live")) {

            // new task to fetch live route data in background
            Task<List<LiveRouteInfo>> fetchTask = new Task<>() {
                @Override
                protected List<LiveRouteInfo> call() throws Exception {
                    System.out.println("Fetching live route data...");
                    String xmlResponse = fetchAndProcessResponse();
                    if (xmlResponse != null) {
                        return processXmlResponse(xmlResponse);
                    }
                    return Collections.emptyList();
                }

                @Override
                protected void succeeded() {
                    System.out.println("Fetch task succeeded.");
                    List<LiveRouteInfo> liveRouteInfoList = getValue();
                    if (liveRouteInfoList != null && !liveRouteInfoList.isEmpty()) {
                        useLiveRoutes = true;
                        System.out.println("use Live routes before:");
                        selectedRouteName = null;
                        selectedVehicleJourneyCode = null;
                        selectedDay = null;

                        if (stopsPanelController != null) {
                            stopsPanelController.setUseLiveRoutes(useLiveRoutes);
                        }
                        System.out.println("use live routes after" + useLiveRoutes);
                        for (LiveRouteInfo liveRouteInfo : liveRouteInfoList) {
                            String routeId = liveRouteInfo.getLineRef();
                            String journeyRef = liveRouteInfo.getJourneyRef();

                            if (routeId.equals(extractRouteName(selectedRoute))) {
                                vjcAndDay patternData = null;
                                try {
                                    patternData = getVehicleJourneyCode(routeId, journeyRef);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                                if (patternData != null && patternData.getVehicleJourneyCode() != null && patternData.getDay() != null) {
                                    selectedVehicleJourneyCode = patternData.getVehicleJourneyCode();
                                    selectedDay = patternData.getDay();
                                    System.out.println("Matched LiveRouteInfo - Route: " + routeId + ", JourneyRef: " + journeyRef +
                                            ", VehicleJourneyRef: " + selectedVehicleJourneyCode +
                                            ", Day: " + selectedDay);
                                    break;
                                } else {
                                    System.err.println("Missing VehicleJourneyCode or Day for Route: " + routeId + ", JourneyRef: " + journeyRef);
                                }
                            }
                        }

                        if (selectedVehicleJourneyCode == null || selectedDay == null) {
                            System.err.println("No matching live route found for the selected route.");
                        } else {
                            selectedRouteName = extractRouteName(selectedRoute);
                            System.out.println("Selected Route Name: " + selectedRouteName);
                            System.out.println("Selected Vehicle Journey Code: " + selectedVehicleJourneyCode);
                            System.out.println("Selected Day: " + selectedDay);

                            // update ui once all data is processed
                            Platform.runLater(() -> {
                                busIconController.startBusMovementUpdate(selectedRouteName, liveRouteInfoList, useLiveRoutes);
                                renderNextPanel(selectedRouteName, selectedVehicleJourneyCode, selectedDay);

                            });
                        }
                    } else {
                        System.out.println("No live route data to update.");
                    }
                }

                @Override
                protected void failed() {
                    System.err.println("Fetch task failed.");
                    getException().printStackTrace();
                }
            };

            // start the fetch task in a new thread
            Thread fetchThread = new Thread(fetchTask);
            fetchThread.setDaemon(true);
            fetchThread.start();

        } else {
            // case where "track live is not chosen"
            useLiveRoutes = false;

            // end any existing tasks
            if (activeBusTask != null && activeBusTask.isRunning()) {
                activeBusTask.cancel(true);
                activeBusTask = null;
            }

            busIconController.stopBusMovementUpdate();

            // split the selected item to extract stop and time information
            String[] parts = selectedValue.split(" at ");
            if (parts.length == 2) {
                String stopSection = parts[0].trim();
                String timeAndJourneyCode = parts[1].trim();
                // split to get the date-time and vehicle journey code
                int lastSpaceIndex = timeAndJourneyCode.lastIndexOf(' ');
                if (lastSpaceIndex != -1) {
                    String departureTimeString = timeAndJourneyCode.substring(0, lastSpaceIndex).trim();
                    String vehicleJourneyCode = timeAndJourneyCode.substring(lastSpaceIndex + 1).replaceAll("[()]", "").trim();
                        //  parse the departure time
                    LocalDateTime departureTime = null;
                    try {
                        departureTime = LocalDateTime.parse(departureTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (DateTimeParseException e) {
                        System.err.println("Invalid departure time format: " + departureTimeString);
                        e.printStackTrace();
                    }

                    if (departureTime != null) {
                        // get necessary variables for function
                        final LocalDateTime[] currentDepartureTime = {departureTime};
                        selectedRouteName = extractRouteName(selectedRoute);
                        selectedVehicleJourneyCode = vehicleJourneyCode;
                        selectedDay = getDay();
                        System.out.println("Selected From Stop: " + stopSection);
                        System.out.println("Selected Departure Time: " + departureTime);
                        System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                        System.out.println("Selected Route ID: " + selectedRouteName);
                        System.out.println("Selected Day: " + selectedDay);
                        System.out.println("current departure time: " + currentDepartureTime[0]);
                        //LocalDateTime finalDepartureTime = departureTime;

                        String selectedTime = RendertimeComboBox.getValue();
                        // get and process time value from combo box into correct format ( add seconds even if the seconds are not specified)
                        if (selectedTime != null) {
                            try {
                                LocalTime newTime;
                                try {
                                    newTime = LocalTime.parse(selectedTime.split(" - ")[1], DateTimeFormatter.ofPattern("HH:mm:ss"));
                                } catch (DateTimeParseException e) {
                                    newTime = LocalTime.parse(selectedTime.split(" - ")[1], DateTimeFormatter.ofPattern("HH:mm"));
                                }

                                // update current time to keep old date, but update time
                                currentDepartureTime[0] = currentDepartureTime[0]
                                        .withHour(newTime.getHour())
                                        .withMinute(newTime.getMinute())
                                        .withSecond(newTime.getSecond());
                                System.out.println("Updated current departure time: " + currentDepartureTime[0]);
                            } catch (DateTimeParseException e) {
                                System.err.println("Invalid time format: " + selectedTime);
                                e.printStackTrace();
                            }
                        }
                        Task<Void> task = new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                busIconController.mapActiveBuses(getDay(), currentDepartureTime[0], 1, selectedRouteName, vehicleJourneyCode);
                                return null;
                            }

                            @Override
                            protected void succeeded() {
                                if (stopsPanelController != null) {
                                    stopsPanelController.setUseLiveRoutes(useLiveRoutes);
                                }
                                System.out.println("Bus mapping completed successfully.");
                                Platform.runLater(() -> {
                                    System.out.println("Calling renderNextPanel...");
                                    System.out.println("selectedRouteName = " + selectedRouteName);
                                    System.out.println("selectedVehicleJourneyCode = " + selectedVehicleJourneyCode);
                                    System.out.println("selectedDay = " + selectedDay);
                                    renderNextPanel(selectedRouteName, selectedVehicleJourneyCode, selectedDay);
                                    System.out.println("Finished calling renderNextPanel.");

                                    //handling to allow user to update page to map new buses if desired
                                    Timeline timer = new Timeline(new KeyFrame(javafx.util.Duration.minutes(5), event -> {
                                        //code runs every 10 minutes so user isnt bombarded
                                        Platform.runLater(() -> {
                                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                            alert.setTitle("Restart Bus Mapping");
                                            alert.setHeaderText("Restart Bus Mapping?");
                                            alert.setContentText("New bus routes may have started since the most previous search, would you like to update the new buses?" +
                                                    "CAUTION, you will be returned to the bus you initially searched for.");

                                            ButtonType buttonTypeYes = new ButtonType("Yes");
                                            ButtonType buttonTypeNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

                                            alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

                                            Optional<ButtonType> result = alert.showAndWait();
                                            if (result.isPresent() && result.get() == buttonTypeYes) {
                                                // continue if user chooses to restart

                                                // cancel existing task
                                                if (activeBusTask != null && activeBusTask.isRunning()) {
                                                    activeBusTask.cancel(true);
                                                    activeBusTask = null;
                                                }

                                                busIconController.stopBusMovementUpdate();
                                                currentDepartureTime[0] = currentDepartureTime[0].plusMinutes(5);
                                                System.out.println("departure time : " + currentDepartureTime[0]);
                                                // parse the selected departure time and journey code
                                                String[] parts = selectedValue.split(" at ");
                                                if (parts.length == 2) {
                                                    String stopSection = parts[0].trim();
                                                    String timeAndJourneyCode = parts[1].trim();

                                                    int lastSpaceIndex = timeAndJourneyCode.lastIndexOf(' ');
                                                    if (lastSpaceIndex != -1) {
                                                        String departureTimeString = timeAndJourneyCode.substring(0, lastSpaceIndex).trim();
                                                        String vehicleJourneyCode = timeAndJourneyCode.substring(lastSpaceIndex + 1).replaceAll("[()]", "").trim();

                                                        LocalDateTime departureTime = null;
                                                        try {
                                                            departureTime = LocalDateTime.parse(departureTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                                        } catch (DateTimeParseException e) {
                                                            System.err.println("Invalid departure time format: " + departureTimeString);
                                                            e.printStackTrace();
                                                        }

                                                        if (departureTime != null) {
                                                            selectedRouteName = extractRouteName(selectedRoute);
                                                            selectedVehicleJourneyCode = vehicleJourneyCode;
                                                            selectedDay = getDay();
                                                            System.out.println("Selected From Stop: " + stopSection);
                                                            System.out.println("Selected Departure Time: " + departureTime);
                                                            System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                                                            System.out.println("Selected Route ID: " + selectedRouteName);
                                                            System.out.println("Selected Day: " + selectedDay);


                                                            Task<Void> task = new Task<Void>() {
                                                                @Override
                                                                protected Void call() throws Exception {
                                                                    busIconController.mapActiveBuses(getDay(), currentDepartureTime[0], 1, selectedRouteName, vehicleJourneyCode);
                                                                    return null;
                                                                }

                                                                @Override
                                                                protected void succeeded() {
                                                                    System.out.println("Bus mapping completed successfully.");
                                                                    //move to route details page once ready
                                                                    Platform.runLater(() -> {
                                                                        System.out.println("Calling renderNextPanel...");
                                                                        System.out.println("selectedRouteName = " + selectedRouteName);
                                                                        System.out.println("selectedVehicleJourneyCode = " + selectedVehicleJourneyCode);
                                                                        System.out.println("selectedDay = " + selectedDay);
                                                                        renderNextPanel(selectedRouteName, selectedVehicleJourneyCode, selectedDay);
                                                                        System.out.println("Finished calling renderNextPanel.");
                                                                    });

                                                                }

                                                                @Override
                                                                protected void failed() {
                                                                    System.err.println("Bus mapping failed.");
                                                                    getException().printStackTrace();
                                                                }
                                                            };

                                                            Thread thread = new Thread(task);
                                                            thread.setDaemon(true);
                                                            thread.start();
                                                        } else {
                                                            System.err.println("Departure time parsing failed, cannot proceed with bus mapping.");
                                                        }
                                                    } else {
                                                        System.err.println("Could not split time and journey code correctly.");
                                                    }
                                                } else {
                                                    System.err.println("Unexpected format for selected item: " + selectedValue);
                                                }
                                            } else {
                                                currentDepartureTime[0] = currentDepartureTime[0].plusMinutes(5);
                                                System.out.println("departure time : " + currentDepartureTime[0]);
                                                // user chooses to not restart, so we just continue
                                                System.out.println("User chose not to restart bus mapping.");
                                            }
                                        });
                                    }));

                                    timer.setCycleCount(Timeline.INDEFINITE); // run indefinety
                                    timer.play();
                                });

                            }

                            @Override
                            protected void failed() {
                                System.err.println("Bus mapping failed.");
                                getException().printStackTrace();
                            }
                        };

                        Thread thread = new Thread(task);
                        thread.setDaemon(true);
                        thread.start();
                    } else {
                        System.err.println("Departure time parsing failed, cannot proceed with bus mapping.");
                    }
                } else {
                    System.err.println("Could not split time and journey code correctly.");
                }
            } else {
                System.err.println("Unexpected format for selected item: " + selectedValue);
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
    public void renderNextPanel(String selectedRouteName, String selectedVehicleJourneyCode, String selectedDay) {
        System.out.println("renderNextPanel started with: " + selectedRouteName + ", " + selectedVehicleJourneyCode + ", " + selectedDay);

        if (mainController != null) {
            stopsPanelController stopsController = mainController.getStopsPanelController();
            if (stopsController != null) {
                System.out.println("Loading stop times in renderNextPanel...");
                try {
                    stopsController.loadStopTimes(selectedRouteName, selectedVehicleJourneyCode, selectedDay);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Loaded stop times in renderNextPanel.");
            } else {
                System.err.println("stopsPanelController is not initialized in renderNextPanel.");
            }
        } else {
            System.err.println("MainController is null in renderNextPanel.");
        }
        System.out.println("renderNextPanel finished.");
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.busIconController = mainController.getBusIconController();
        this.stopsPanelController = mainController.getStopsPanelController();
        if (stopsPanelController != null) {
            stopsPanelController.setUseLiveRoutes(useLiveRoutes);
        }
    }
    public boolean getUseLiveRoutes() {
        return useLiveRoutes;
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

    @FXML
    public void moveToRouteDetailsPage() {
        if (mainController != null) {
            mainController.loadSidePanel("/com/project/busfinder/GUI/routeDetailsPanel.fxml");

        }
    }

    public String getMostRecentDay(){
        return this.selectedDay;
    }


}
