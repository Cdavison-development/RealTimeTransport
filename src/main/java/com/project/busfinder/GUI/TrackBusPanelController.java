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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.util.Pair;

import static com.project.busfinder.Mapping_util.simulateBusLocations.getJourneyLegs;
import static com.project.busfinder.Mapping_util.simulateBusLocations.getVehicleJourneyCode;
import static com.project.busfinder.helperFunctions.getStopName.StopName;
import static com.project.busfinder.readDatabase.getRoutes.getLiveRoutes;
import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;
import static com.project.busfinder.util.readLiveLocation.processXmlResponse;

/**
 *
 * testing errors:
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
    private Timeline activeTimeline;
    private String lastSelectedDay = null;

    private String selectedRouteName;
    private String selectedVehicleJourneyCode;
    private String selectedDay;
    private boolean useLiveRoutes = true;
    private Task<Void> activeTask;
    private int timeWindow = 3;

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

            // repopulate if new day chosen
            if (selectedDay != null ) {
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

    /**
     *
     * queries getLiveRoutes, and returns all live routes and VJCs at a certain moment,
     * specifying if live tracking is available at that point
     *
     *
     */
    private void populateRoutesComboBox() {
        List<String> routeLabels = new ArrayList<>();
        Set<String> addedRoutes = new HashSet<>();
        try {
            // get day
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            String currentDayString = currentDay.toString();

            // get list of active routes
            Map<String, List<Pair<Boolean, String>>> liveRoutes = getLiveRoutes(conn);

            for (Map.Entry<String, List<Pair<Boolean, String>>> entry : liveRoutes.entrySet()) {
                String route = entry.getKey();
                List<Pair<Boolean, String>> liveInfoList = entry.getValue();
                boolean routeAdded = false;
                for (Pair<Boolean, String> liveInfo : liveInfoList) {
                    Boolean isLive = liveInfo.getKey();
                    String vjc = liveInfo.getValue();
                    // mark each route with whether it's got live tracking and/or just simulated
                    String label;

                    if (isLive && getDay().equalsIgnoreCase(currentDayString)) {
                        label = route + " - Live tracking available";
                    } else {
                        label = route + " - simulated tracking available";
                    }

                    if (!addedRoutes.contains(label)) {
                        System.out.println("Route: " + route + " | Live: " + isLive + " | VJC: " + vjc);
                        System.out.println(label);
                        routeLabels.add(label);
                        addedRoutes.add(label);
                        routeAdded = true;
                    }
                }
                if (!routeAdded && !addedRoutes.contains(route + " - simulated tracking available")) {
                    String simulatedLabel = route + " - simulated tracking available";
                    System.out.println(simulatedLabel);
                    routeLabels.add(simulatedLabel);
                    addedRoutes.add(simulatedLabel);
                }
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

        if (dayComboBox.getValue().contains("Live")) {
            RendertimeComboBox.setVisible(false);
        }
        if (selectedDay != null) {
            Platform.runLater(() -> {
                System.out.println("Setting routesComboBox to visible");
                routesComboBox.setVisible(true);
            });
        }
        RendertimeComboBox.getItems().clear();
        RendertimeComboBox.setVisible(true);
    }

    /**
     * handles the selection of a route and prepares the departure time options accordingly
     * populates departure time cobo box based on selected route and day
     * if live tracking available for the selected route, the option to track live is added
     * always populates simulated routes, despite whether track live is true or not
     *
     *
     */
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
                    // get the live routes with VJCs
                    Map<String, List<Pair<Boolean, String>>> liveRoutes = getLiveRoutes(conn);

                    List<Pair<Boolean, String>> liveInfoList = liveRoutes.get(routeName);

                    boolean hasLiveTracking = false;

                    // if live routes are avilable, add track live option.
                    if (liveInfoList != null && !liveInfoList.isEmpty()) {
                        for (Pair<Boolean, String> liveInfo : liveInfoList) {
                            Boolean isLive = liveInfo.getKey();
                            if (isLive && getDay().equalsIgnoreCase(LocalDate.now().getDayOfWeek().toString())) {
                                departureTimeComboBox.getItems().add("Track Live");
                                System.out.println("Track live available for route: " + routeName);
                                hasLiveTracking = true;
                                break;
                            }
                        }
                    }

                    // add simulated routes in all cases, user may want to check a simulated route, even on a live tracking day
                    populateSimulatedRoutes(routeName);

                    // set prompt text
                    if (!departureTimeComboBox.getItems().isEmpty()) {
                        departureTimeComboBox.setPromptText("Select Departure Time");
                    } else {
                        departureTimeComboBox.setPromptText("No available departure times");
                    }

                    // set up action listener for the departure time combo box
                    departureTimeComboBox.setOnAction(event -> {
                        String selectedItem = departureTimeComboBox.getValue();
                        if (selectedItem != null) {
                            if (selectedItem.equals("Track Live")) {
                                // if "Track Live" is selected, populate VJCs for the live route
                                try {
                                    populateVJCForLiveRoute(routeName, liveInfoList);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                RendertimeComboBox.getItems().clear();
                                String vehicleJourneyCode = handleDepartureTimeSelection(selectedItem, routeName);
                                try {
                                    populateRouteTimes(routeName, vehicleJourneyCode, getDay());
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });

                } catch (SQLException | IOException | InterruptedException e) {
                    e.printStackTrace();
                    System.err.println("Failed to fetch route information.");
                }
            } else {
            }
        }
    }

    /**
     *
     * populates RenderTimeCombo box with all bus stops the user can start simulation from
     *
     *
     * @param routeName
     * @throws SQLException
     */
    private void populateSimulatedRoutes(String routeName) throws SQLException {
        RendertimeComboBox.getItems().clear();
        getRouteDetails fetcher = new getRouteDetails(conn);
        List<getRouteDetails.JourneyRouteInfo> routeInfoList = fetcher.getJourneyRouteInfo(routeName, getDay());

        for (getRouteDetails.JourneyRouteInfo info : routeInfoList) {
            // get stop names
            String firstFromStopName = StopName(info.getFirstFromStop());
            String lastToStopName = StopName(info.getLastToStop());

            // format earliest departure time
            String formattedDepartureTime = info.getEarliestDepartureDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // construct the item string for the combo box
            String item = String.format("%s -> %s at %s (%s) ",
                    firstFromStopName,
                    lastToStopName,
                    formattedDepartureTime,
                    info.getVehicleJourneyCode());

            // add item to combo box
            departureTimeComboBox.getItems().add(item);
        }
    }

    /**
     *
     * populates RenderTime combo box with the different current live routes.
     *
     *
     * @param routeName
     * @param liveInfoList
     * @throws SQLException
     */
    private void populateVJCForLiveRoute(String routeName, List<Pair<Boolean, String>> liveInfoList) throws SQLException {
        RendertimeComboBox.getItems().clear();
        RendertimeComboBox.setVisible(true);
        if (liveInfoList != null) {
            getRouteDetails fetcher = new getRouteDetails(conn);

            for (Pair<Boolean, String> liveInfo : liveInfoList) {

                Boolean isLive = liveInfo.getKey();
                String vjc = liveInfo.getValue();

                vjcAndDay patternData = null;
                try {
                    patternData = getVehicleJourneyCode(routeName, vjc);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("isLive: " + isLive);
                System.out.println("journeyCode: " + vjc);
                vjc = patternData.getVehicleJourneyCode();
                System.out.println("vjc: " + vjc);
                if (isLive && vjc != null) {
                    // get data for ensuring that  RenderTimesCombo box text is user friendly
                    List<getRouteDetails.JourneyRouteInfo> routeInfoList = fetcher.getJourneyRouteInfo(routeName, getDay());

                    for (getRouteDetails.JourneyRouteInfo info : routeInfoList) {
                        if (info.getVehicleJourneyCode().equals(vjc)) {
                            // get stop names
                            String firstFromStopName = StopName(info.getFirstFromStop());
                            String lastToStopName = StopName(info.getLastToStop());

                            // format  earliest departure time
                            String formattedDepartureTime = info.getEarliestDepartureDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                            // construct the item string for the combo box
                            String displayText = String.format("From %s to %s (%s)",
                                    firstFromStopName,
                                    lastToStopName,
                                    //formattedDepartureTime,
                                    vjc);

                            // add item to combo box
                            RendertimeComboBox.getItems().add(displayText);
                            break; // dont need to continue loop since we found the matching VJC
                        }
                    }
                }
            }

            if (RendertimeComboBox.getItems().isEmpty()) {
                RendertimeComboBox.setPromptText("No Live VJCs available");
            } else {
                RendertimeComboBox.setVisible(true);
                RendertimeComboBox.setPromptText("Select Vehicle Journey Code");
            }
        } else {
            RendertimeComboBox.setPromptText("No Live VJCs available");
        }
    }
    // selecting stops within the range of timeWindow from 00:00 break the system, so we just make these unavailable. if the user
    //wants to see these stops they can render a stop slightly outside the range and wait.
    // also make the final stop unavailable as by this time the bus is removed and nothing is plotted anyway.
    public void populateRouteTimes(String routeId, String vehicleJourneyCode, String day) throws SQLException {
            RendertimeComboBox.getItems().clear();
            RendertimeComboBox.setPromptText("Choose bus stop");
            // get journey legs
            List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, day);
            System.out.println("loadStopTimes journeyLegs: " + journeyLegs);

            LocalTime windowStart = LocalTime.of(23, 59).minusMinutes(timeWindow);
            LocalTime windowEnd = LocalTime.of(0, 0).plusMinutes(timeWindow);


        // convert the journey legs to strings for display in the ListView
            ObservableList<String> stopTimeStrings = FXCollections.observableArrayList();
            for (int i = 0; i < journeyLegs.size(); i++) {
                JourneyLeg leg = journeyLegs.get(i);
                LocalTime departureTime = leg.getDepartureTime();

                String stopName = StopName(leg.getFromStop());
                String displayText = stopName + " - " + leg.getDepartureTime().toString();

                if (departureTime.isAfter(windowStart) || departureTime.isBefore(windowEnd)) {
                    displayText += " (Unavailable)";
                }

                if (i == journeyLegs.size() - 1) {
                    displayText += " (Unavailable)";  // Mark the last item as unavailable
                }

                stopTimeStrings.add(displayText);
                System.out.println("Adding to ListView: " + displayText);
                if (RendertimeComboBox != null) {
                    RendertimeComboBox.getItems().add(displayText);
                }
            }

            System.out.println("Setting ListView items...");
            System.out.println("ListView should be updated.");

        RendertimeComboBox.setOnAction(event -> {
            String selectedItem = RendertimeComboBox.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.contains(" (Unavailable)")) {
                RenderRoutesAlert("Invalid choice");
                RendertimeComboBox.getSelectionModel().clearSelection();
                System.out.println("This stop is unavailable and cannot be selected.");
            }
        });
    }

    /**
     *
     * gets the VJC code from the string
     *
     * @param selectedItem
     * @param routeName
     * @return
     */
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

// taken from https://www.w3schools.com/java/java_regex.asp
    public String returnVJCForLiveRouteString(String liveRouteString){
        String regex = "\\(([^)]+)\\)$";

        // compile the regex pattern
        Pattern pattern = Pattern.compile(regex);

        // use matcher to match the pattern against the input string
        Matcher matcher = pattern.matcher(liveRouteString);
        String vjc = null;
        // extract the VJC
        if (matcher.find()) {
            vjc = matcher.group(1);
            System.out.println("Extracted VJC: " + vjc);
        } else {
            System.out.println("No VJC found.");
        }
        return vjc;
    }


    private void RenderRoutesAlert(String title) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText("This stop is unavailable and cannot be selected.");
        alert.showAndWait();

        routesComboBox.setPromptText("Select a route");
        departureTimeComboBox.setPromptText("Select a departure time");
    }

    private void LiveTimesAlert(String title) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText("Please Note: These travel times are predicted by the Tom Tom API and may not be accurate");
        alert.showAndWait();

    }


    /**
     *  handles the event when the switch view button is clicked, either to track live buses or simulated buses depending on the selection in the
     *  departure time combo box, the method fetches and processes live route data or parses the
     *   selected route and departure time to initiate bus tracking and mapping.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws SQLException
     */
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
                        System.out.println("use Live routes before:");
                        selectedRouteName = null;
                        selectedVehicleJourneyCode = null;
                        selectedDay = null;

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
                            selectedVehicleJourneyCode = returnVJCForLiveRouteString(RendertimeComboBox.getValue());
                            System.out.println(selectedVehicleJourneyCode);
                            // update ui once all data is processed
                            Platform.runLater(() -> {
                                useLiveRoutes = true;
                                if (stopsPanelController != null) {
                                    busIconController.setUseLiveRoutes(useLiveRoutes);
                                }else{
                                    System.out.println("stops panel controller null at Live onclick ");
                                }
                                LiveTimesAlert("Live Routes Alert");
                                busIconController.startBusMovementUpdate(selectedRouteName, liveRouteInfoList, useLiveRoutes,selectedVehicleJourneyCode);
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
            //case where "Track live is not chosen"

            // end any existing tasks
            if (activeBusTask != null && activeBusTask.isRunning()) {
                activeBusTask.cancel(true);
                activeBusTask = null;
                System.out.println("Cancelling active task");
            }

            busIconController.stopBusMovementUpdate();
            if (activeTimeline != null) {
                activeTimeline.stop();
                activeTimeline = null;
                System.out.println("Cancelling previous timeline");
            } else {
                System.out.println("No active timeline to cancel");
            }
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
                                busIconController.mapActiveBuses(getDay(), currentDepartureTime[0], timeWindow, selectedRouteName, vehicleJourneyCode);
                                return null;
                            }

                            @Override
                            protected void succeeded() {
                                Platform.runLater(() -> {
                                    if (activeBusTask != null && activeBusTask != this && activeBusTask.isRunning()) {
                                        activeBusTask.cancel(true);
                                        //activeBusTask = null;
                                        System.out.println("Cancelling active task");
                                    }

                                    stopsPanelController stopsController = mainController.getStopsPanelController();
                                    useLiveRoutes = false;
                                    if (stopsController != null) {
                                        busIconController.setUseLiveRoutes(useLiveRoutes);
                                        System.out.println("At set on 648 we see = " + useLiveRoutes);
                                    } else {
                                        System.err.println("StopsPanelController is null, cannot set useLiveRoutes.");
                                    }


                                System.out.println("Bus mapping completed successfully.");

                                    System.out.println("Calling renderNextPanel...");
                                    System.out.println("selectedRouteName = " + selectedRouteName);
                                    System.out.println("selectedVehicleJourneyCode = " + selectedVehicleJourneyCode);
                                    System.out.println("selectedDay = " + selectedDay);
                                    renderNextPanel(selectedRouteName, selectedVehicleJourneyCode, selectedDay);
                                    System.out.println("Finished calling renderNextPanel.");
                                    System.out.println("previous selected route name : " + selectedRoute);


                                    //handling to allow user to update page to map new buses if desired
                                    activeTimeline = new Timeline(new KeyFrame(javafx.util.Duration.minutes(2), event -> {
                                        System.out.println("Timeline triggered");
                                        //code runs every 10 minutes so user isnt bombarded
                                        Platform.runLater(() -> {
                                            System.out.println(" at set on 648 we see = " + useLiveRoutes);
                                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                            alert.setTitle("Restart Bus Mapping");
                                            alert.setHeaderText("Restart Bus Mapping?");
                                            alert.setContentText("New bus routes may have started since the most previous search, would you like to update the new buses?" +
                                                    "CAUTION, you will be returned to the bus you initially searched for." + selectedRouteName);

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
                                                            System.out.println("proposed selected route name" + selectedRouteName);
                                                            selectedVehicleJourneyCode = vehicleJourneyCode;
                                                            selectedDay = getDay();
                                                            System.out.println("Selected From Stop: " + stopSection);
                                                            System.out.println("Selected Departure Time: " + departureTime);
                                                            System.out.println("Selected Vehicle Journey Code: " + vehicleJourneyCode);
                                                            System.out.println("Selected Route ID: " + selectedRouteName);
                                                            System.out.println("Selected Day: " + selectedDay);


                                                            Task<Void> newTask = new Task<Void>() {
                                                                @Override
                                                                protected Void call() throws Exception {
                                                                    busIconController.mapActiveBuses(getDay(), currentDepartureTime[0], timeWindow, selectedRouteName, vehicleJourneyCode);
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
                                                            activeBusTask = newTask;
                                                            Thread thread = new Thread(newTask);
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
                                    System.out.println("activeTimeline set: " + (activeTimeline != null));
                                    activeTimeline.setCycleCount(Timeline.INDEFINITE); // run indefinety
                                    activeTimeline.play();
                                    System.out.println("New activeTimeline started");
                                });

                            }

                            @Override
                            protected void failed() {
                                System.err.println("Bus mapping failed.");
                                getException().printStackTrace();
                            }
                        };
                        if (activeBusTask != null && activeBusTask.isRunning()) {
                            activeBusTask.cancel(true);
                            System.out.println("Cancelling previous active task before starting new one.");
                        }
                        activeBusTask = task;
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
            mainController.loadSidePanel("/com/project/busfinder/GUI/routeDetailsPanel.fxml");
            stopsPanelController stopsController = mainController.getStopsPanelController();

            if (stopsController != null) {
                System.out.println("Loading stop times in renderNextPanel...");
                try {
                    stopsController.loadStopTimes(selectedRouteName, selectedVehicleJourneyCode, selectedDay,useLiveRoutes);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Loaded stop times in renderNextPanel.");
            } else {
                System.err.println("StopsPanelController is not initialized in renderNextPanel.");
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
    }
    public boolean getUseLiveRoutes() {
        System.out.println("use live routes current state : " + useLiveRoutes);
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



}
