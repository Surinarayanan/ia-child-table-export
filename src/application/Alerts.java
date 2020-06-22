package application;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.WindowEvent;

import java.util.Optional;

/**
 * Created by Suriyanarayanan K
 * on 12/06/20 6:59 PM.
 */
public class Alerts {

    public static void ErrorAlert(String title, String headerText, String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(headerText);
        errorAlert.setWidth(500);
        errorAlert.setHeight(500);
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }

    public static void WarningAlert(String title, String headerText, String message) {
        Alert errorAlert = new Alert(Alert.AlertType.WARNING);
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(headerText);
        errorAlert.setWidth(500);
        errorAlert.setHeight(500);
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }

    public static void ConfirmationAlert(WindowEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation Dialog");
        confirmation.setHeaderText("Confirm Cancel");
        confirmation.setContentText("Are you sure you want to cancel add creation?");
        Optional<ButtonType> result = confirmation.showAndWait();
        result.ifPresent(res -> {
            if (res == ButtonType.CANCEL) {
                event.consume();
            } else if (res == ButtonType.OK) {

            }
        });

    }
}
