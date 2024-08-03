package com.project.busfinder.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;


public class Main extends Application {

    double x,y = 0;
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {

            URL fxmlLocation = getClass().getResource("/com/project/busfinder/GUI/main.fxml");
            if (fxmlLocation == null) {
                System.err.println("FXML file not found: /com/project/busfinder/GUI/main.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            primaryStage.setTitle("JavaFX Test");
            primaryStage.setScene(new Scene(root, 1200, 800));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load main.fxml");
        }

    }
    public static void main(String[] args) {
        launch(args);
    }
}
