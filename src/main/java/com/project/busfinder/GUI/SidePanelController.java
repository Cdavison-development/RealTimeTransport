package com.project.busfinder.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class SidePanelController {
    @FXML
    private VBox rootPanel;
    @FXML
    private GridPane sidePanelGridPane;
    @FXML
    private Button trackBusButton;

    private MainController mainController;

    @FXML
    public void initialize() {

    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void moveToRoutesPage(ActionEvent event){
        // loads side panel content
        if (mainController != null) {
            mainController.loadSidePanel("/com/project/busfinder/GUI/trackBusPanel.fxml");
        }
    }


}
