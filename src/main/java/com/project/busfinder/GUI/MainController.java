package com.project.busfinder.GUI;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainController {
    @FXML
    private VBox sidePanel;

    @FXML
    private Button toggleButton;

    @FXML
    private ImageView buttonImage;

    private boolean isPanelOpen = false;
    private boolean isFirstClick = true;

    @FXML
    private void initialize() {
        // Initial setup
        sidePanel.setTranslateX(-200);
        toggleButton.setTranslateX(-200);
        sidePanel.setVisible(false);
    }

    @FXML
    private void toggleSidePanel(ActionEvent event) {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.3), sidePanel);
        TranslateTransition buttonTransition = new TranslateTransition(Duration.seconds(0.3), toggleButton);

        if (isPanelOpen) {
            transition.setToX(-200);
            buttonTransition.setToX(-200);
            transition.setOnFinished(e -> sidePanel.setVisible(false));
        } else {
            sidePanel.setVisible(true);
            transition.setToX(0);
            buttonTransition.setToX(0);
        }

        transition.play();
        buttonTransition.play();
        isPanelOpen = !isPanelOpen;
    }
}
