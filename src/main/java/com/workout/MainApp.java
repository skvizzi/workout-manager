package com.workout;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Головний клас ініціалізації графічного інтерфейсу JavaFX
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 680);
        stage.setTitle("Менеджер спортивних тренувань");
        stage.setScene(scene);
        stage.show();
    }
}
