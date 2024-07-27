package com.project.busfinder.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    double x,y = 0;
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/busfinder/GUI/main.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("JavaFX Test");
        primaryStage.setScene(new Scene(root, 800, 500));
        primaryStage.show();

        
    }


    public static void main(String[] args) {
        launch(args);
    }
}
