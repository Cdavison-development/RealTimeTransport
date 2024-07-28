package com.project.busfinder.GUI;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {
    @FXML
    private VBox sidePanel;
    @FXML
    private GridPane sidePanelGridPane;
    @FXML
    private VBox buttonPanel;
    @FXML
    private GridPane buttonPanelGridPane;

    @FXML
    private Button toggleButton;

    @FXML
    private ImageView buttonImage;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private TextField CompanyTextField;

    private boolean isPanelOpen = false;
    private boolean isFirstClick = true;

    @FXML
    private void initialize() {

        sidePanel.setTranslateX(-200);
        sidePanel.setVisible(false);
        buttonPanel.setTranslateX(sidePanel.getTranslateX() + 20);

        loadGridPane();

        sidePanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanel.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        buttonPanelGridPane.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
    }

    public void loadGridPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/busfinder/GUI/trackBusPanel.fxml"));
            sidePanelGridPane = loader.load();
            sidePanel.getChildren().clear();
            sidePanel.getChildren().add(sidePanelGridPane);
            sidePanelGridPane.prefHeightProperty().bind(anchorPane.heightProperty().multiply(0.8));
        } catch (IOException e) {
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

}
