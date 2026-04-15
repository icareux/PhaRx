package com.pharmasystem.main;

import com.pharmasystem.ui.UIUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import com.pharmasystem.database.DatabaseSetup;
import javafx.scene.image.Image;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        
        stage.getIcons().add(
            new Image(getClass().getResourceAsStream(
                "/com/pharmasystem/assets/pharx-icon.png"
            ))
        );

        try {
            DatabaseSetup.initialize();
            Scene scene = new Scene(
                    UIUtils.loadView("/com/pharmasystem/ui/Login.fxml")
            );

            stage.setTitle("PhaRx");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Startup Error", "The application could not start correctly.");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
