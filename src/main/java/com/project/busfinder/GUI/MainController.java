package com.project.busfinder.GUI;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.layout.*;
import java.io.IOException;


public class MainController {
    @FXML
    private VBox sidePanel;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private VBox buttonPanel;

    @FXML
    private GridPane buttonPanelGridPane;

    private boolean isPanelOpen = false;



    @FXML
    private void initialize() {

        sidePanel.setTranslateX(-200);
        sidePanel.setVisible(false);
        buttonPanel.setTranslateX(sidePanel.getTranslateX() + 20);

        loadSidePanel("/com/project/busfinder/GUI/startingSidePanel.fxml");
        sidePanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanelGridPane.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
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
