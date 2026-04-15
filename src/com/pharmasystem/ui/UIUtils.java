package com.pharmasystem.ui;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.image.Image;


public final class UIUtils {

    private UIUtils() {
    }

    public static Parent loadView(String resourcePath) throws IOException {
        URL resource = UIUtils.class.getResource(resourcePath);
        if (resource == null) {
            throw new IOException("Missing UI resource: " + resourcePath);
        }
        return FXMLLoader.load(resource);
    }

    public static FXMLLoader createLoader(String resourcePath) throws IOException {
        URL resource = UIUtils.class.getResource(resourcePath);
        if (resource == null) {
            throw new IOException("Missing UI resource: " + resourcePath);
        }
        return new FXMLLoader(resource);
    }

    public static void switchScene(Node source,
                                   String resourcePath,
                                   String title,
                                   boolean maximized) throws IOException {

        Stage stage = (Stage) source.getScene().getWindow();
        Parent root = loadView(resourcePath);

        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.setResizable(true);

        if (maximized) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }

        stage.show();
    }

    public static Stage openPopup(Window owner, String title, Parent root, boolean modal) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.getIcons().setAll(
                new Image(UIUtils.class.getResourceAsStream(
                    "/com/pharmasystem/assets/pharx-icon.png"
                ))
            );

        if (owner != null) {
            stage.initOwner(owner);
        }
        if (modal) {
            stage.initModality(Modality.WINDOW_MODAL);
        }
        return stage;
    }

    public static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    public static void applyAppIcon(Stage stage) {
    stage.getIcons().setAll(
        new Image(UIUtils.class.getResourceAsStream(
            "/com/pharmasystem/assets/pharx-icon.png"
        ))
    );
}
}
