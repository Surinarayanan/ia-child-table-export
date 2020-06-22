package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import static application.Alerts.ErrorAlert;


public class Main extends Application {


    private static Logger LOGGER = Logger.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {

        PropertyConfigurator.configure("log4j.properties");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("DataExport.fxml"));
            Scene scene = new Scene(root, 870, 630);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            LOGGER.error("Fxml Loader Exception", e.fillInStackTrace());
            ErrorAlert("Fxml Loader Exception", e.getMessage(), e.getMessage());
        }
    }


    public static void main(String[] args) {

        launch(args);
    }
}
