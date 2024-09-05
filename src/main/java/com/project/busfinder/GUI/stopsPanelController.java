package com.project.busfinder.GUI;

import com.project.busfinder.Mapping_util.BusRoutePrediction;
import com.project.busfinder.Mapping_util.JourneyLeg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

import java.sql.SQLException;
import java.util.List;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;

import static com.project.busfinder.Mapping_util.simulateBusLocations.getJourneyLegs;
import static com.project.busfinder.helperFunctions.getStopName.StopName;


public class stopsPanelController {

    private MainController mainController;
    @FXML
    private BusIconController busIconController;

    @FXML
    private ListView<String> stopListView;
    private ComboBox<String> rendertimeComboBox;
    @FXML
    private ProgressIndicator loadingIndicator;

    private boolean useLiveRoutes = true;

    public void initialise(){

    }

    /**
     *
     * gets the populates the stop list view with the stops and expected time stop is met  for a given routeID and VJC
     *
     *
     * @param routeId
     * @param vehicleJourneyCode
     * @param day
     * @param useLiveRoutes
     * @throws SQLException
     */
    public void loadStopTimes(String routeId, String vehicleJourneyCode, String day,boolean useLiveRoutes) throws SQLException {
        //setUseLiveRoutes(useLiveRoutes);
        System.out.println("use live routes Print" + useLiveRoutes);
        System.out.println("loadStopTimes started...");
        System.out.println("Clearing previous data...");
        stopListView.getItems().clear();
        //setUseLiveRoutes(useLiveRoutes);
        Task<List<String>> predictionTask = null;
        if (useLiveRoutes) {
            loadingIndicator.setVisible(true);
            // query the API for departure times on separate thread to ensure user can use program during search
            predictionTask = new Task<>() {
                @Override
                protected List<String> call() throws SQLException {
                    BusRoutePrediction busRoutePrediction = new BusRoutePrediction();
                    return busRoutePrediction.predictBusRouteTimes(routeId, vehicleJourneyCode, day);
                }

                @Override
                protected void succeeded() {

                    loadingIndicator.setVisible(false);
                    List<String> predictedTimes = getValue();

                    // populate the ListView with the predicted times
                    stopListView.getItems().addAll(predictedTimes);
                    stopListView.refresh();
                    System.out.println("ListView should be updated with predicted times.");
                }

                @Override
                protected void failed() {
                    Throwable exception = getException();
                    exception.printStackTrace();
                    System.err.println("Prediction task failed.");
                }
            };

            // start task on a new thread
            Thread predictionThread = new Thread(predictionTask);
            predictionThread.setDaemon(true); // end thread when application exits
            predictionThread.start();
        } else {


            List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode, day);
            System.out.println("loadStopTimes journeyLegs: " + journeyLegs);

            // convert the journey legs to strings for display in the ListView
            ObservableList<String> stopTimeStrings = FXCollections.observableArrayList();
            for (JourneyLeg leg : journeyLegs) {
                String stopName = StopName(leg.getFromStop());
                String displayText = stopName + " - " + leg.getDepartureTime().toString();
                stopTimeStrings.add(displayText);
                System.out.println("Adding to ListView: " + displayText);
                if (rendertimeComboBox != null) {
                    rendertimeComboBox.getItems().add(displayText);
                }
            }

            // set the items in the ListView and refresh
            System.out.println("Setting ListView items...");
            stopListView.setItems(FXCollections.observableArrayList());
            stopListView.setItems(stopTimeStrings);
            stopListView.refresh();
            System.out.println("ListView should be updated.");
        }
    }
    @FXML
    public void moveToPreviousPage(ActionEvent event) {
        if (mainController != null) {
            mainController.loadSidePanel("/com/project/busfinder/GUI/trackBusPanel.fxml");
        }
    }

    @FXML
    public void reloadRouteDetailsPage(ActionEvent event){
        if (mainController != null) {
            mainController.loadSidePanel("/com/project/busfinder/GUI/routeDetailsPanel.fxml");
        }
    }
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.busIconController = mainController.getBusIconController();
    }
    public void setRendertimeComboBox(ComboBox<String> rendertimeComboBox) {
        this.rendertimeComboBox = rendertimeComboBox;
    }

}
