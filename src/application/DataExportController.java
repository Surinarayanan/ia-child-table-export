package application;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.controlsfx.control.MaskerPane;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static application.Alerts.ErrorAlert;
import static application.Alerts.WarningAlert;

public class DataExportController implements Initializable {

    @FXML
    private ImageView applicationLogo;
    @FXML
    private JFXTextField dataPathTextField;
    @FXML
    private JFXTextField outputPathTextField;
    @FXML
    private JFXButton dataPathBrowseBtn;
    @FXML
    private JFXButton outputPathBrowseBtn;
    @FXML
    private JFXCheckBox refactorFile;
    @FXML
    private Label noteId;
    @FXML
    private JFXButton scheduleBtn;
    @FXML
    private JFXTextField mainTableTo;
    @FXML
    private JFXRadioButton excelFile;
    @FXML
    private JFXRadioButton multipleCsvFIle;
    @FXML
    private MaskerPane maskpane;

    private List<String> headerList = new ArrayList<>();
    private List<String> tableList = new ArrayList<>();
    private String lastColumnName = "";
    private boolean scheduleAction = true;

    private static Logger LOGGER = Logger.getLogger(DataExportController.class);

    @PostConstruct
    public void init() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        multipleCsvFIle.setSelectedColor(Color.valueOf("#1565c0"));
        excelFile.setSelectedColor(Color.valueOf("#1565c0"));
        excelFile.setSelected(true);
        refactorFile.setOnAction(refractorAction());
        Platform.runLater(() -> {
            bindSetup();
        });
    }

    @FXML
    void getDataPath(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV(*.csv)", "*.csv")
        );
        File file = fileChooser.showOpenDialog(dataPathBrowseBtn.getScene().getWindow());
        if (file != null) {
            dataPathTextField.setText(file.getAbsolutePath());
            updateParentTableColumns(file);
            if (outputPathTextField.getText().isEmpty()) {
                outputPathTextField.setText(dataPathTextField.getText().substring(0, dataPathTextField.getText().lastIndexOf(File.separator)));
            }
            checkOutputFolderExists();
        } else {
            dataPathTextField.setText("");
        }
        bindSetup();
    }

    @FXML
    void getOutputPath(ActionEvent event) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dir = directoryChooser.showDialog(outputPathBrowseBtn.getScene().getWindow());
        if (dir != null) {
            outputPathTextField.setText(dir.getAbsolutePath());
        } else {
            if (!dataPathTextField.getText().isEmpty()) {
                outputPathTextField.setText(dataPathTextField.getText().substring(0, dataPathTextField.getText().lastIndexOf(File.separator)));
            } else {
                outputPathTextField.setText("");
            }
        }
        checkOutputFolderExists();
        bindSetup();
    }

    private void checkOutputFolderExists() {
        String outputLocation = outputPathTextField.getText();
        if (!outputLocation.isEmpty() && !dataPathTextField.getText().isEmpty()) {
            String existedOutputFolder=outputLocation + File.separator + new File(dataPathTextField.getText()).getName().split("\\.")[0] + "-Child Tables";
            File file = new File(existedOutputFolder);
            if (file.isDirectory()) {
                Alerts.WarningAlert("Warning !!", "Output Folder Exists", "Please choose some other folder or delete existing folder");
                outputPathTextField.setText("");
            }
        }
    }

    @FXML
    void onClickMultiCsvFile(ActionEvent event) {
        multipleCsvFIle.setSelected(true);
        excelFile.setSelected(false);
        checkOutputFolderExists();
        bindSetup();
    }

    @FXML
    void onClickExcel(ActionEvent event) {
        excelFile.setSelected(true);
        multipleCsvFIle.setSelected(false);
        checkOutputFolderExists();
        bindSetup();
    }

    @FXML
    void getScheduleAction(ActionEvent event) {

        checkOutputFolderExists();
        if (tableList.isEmpty()) {
            Alerts.WarningAlert("Warning !!", "No child table to process", "Please choose csv file with child table");
        } else if (!mainTableTo.getText().equalsIgnoreCase(lastColumnName) && scheduleAction) {
            maskpane.setVisible(true);
            mainTableLastColumnNameAlert(event);
            maskpane.setVisible(false);
        } else if (!headerList.contains(mainTableTo.getText())) {
            Alerts.ErrorAlert("Error !!", "Column name Invalid", "Column Name not present in parent table");
        } else {
            maskpane.setVisible(true);
            setValuesIntoBean();
            createOutputFolder();
            startTriggerProgressScreen();
        }
        scheduleBtn.getScene().getWindow().setOnCloseRequest(Alerts::ConfirmationAlert);
    }

    private void mainTableLastColumnNameAlert(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning !!");
        alert.setHeaderText("Parent Table Last Column was changed");
        alert.setWidth(500);
        alert.setHeight(500);
        alert.setContentText("Are you sure you want to proceed with the changes ?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                mainTableTo.setText(lastColumnName);
            }
            scheduleAction = false;
            event.consume();
        });
    }

    private void startTriggerProgressScreen() {
        Stage progressPage = new Stage();
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("Progress.fxml"));
        } catch (IOException e) {
            LOGGER.error("Fxml Loader Exception", e.fillInStackTrace());
            ErrorAlert("ERROR !!", "Fxml Loader Exception", e.getMessage());
        }
        Scene scene = new Scene(root, 500, 270);
        progressPage.setScene(scene);
        progressPage.show();
        progressPage.setOnCloseRequest(getProgressControllerCloseEvent());
        progressPage.setOnHidden(event -> maskpane.setVisible(false));
    }
    private EventHandler<WindowEvent> getProgressControllerCloseEvent() {
        return event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning !!");
            alert.setHeaderText("Confirm Cancel");
            alert.setWidth(500);
            alert.setHeight(500);
            alert.setContentText("Are you sure you want to cancel add creation?");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.CANCEL) {
                    event.consume();
                } else if (response == ButtonType.OK) {
                    if (DataExportBean.currentThread.isAlive()) {
                        DataExportBean.currentThread.stop();
                    }
                    /*File file = new File(DataExportBean.jobOutputPath);
                    if (file != null && file.isDirectory()) {
                        deleteDirectory(file);
                    }*/
                    bindSetup();
                    maskpane.setVisible(false);
                }
                alert.close();
            });
        };
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private void setValuesIntoBean() {
        DataExportBean.dataFilePath = dataPathTextField.getText();
        DataExportBean.isRefactor = refactorFile.isSelected();
        DataExportBean.isMultipleCsvFile = multipleCsvFIle.isSelected();
        DataExportBean.headerLastPosition = headerList.indexOf(mainTableTo.getText()) + 1;
    }

    private void createOutputFolder() {
        if (!new File(outputPathTextField.getText()).isDirectory()) {
            new File(outputPathTextField.getText()).mkdir();
            if (!new File(outputPathTextField.getText()).isDirectory()) {
                LOGGER.error("Can't able to create output folder.");
                ErrorAlert("Error !!", "Can't able to create Output Folder", "Please check the permission");
            }
        }
        DataExportBean.jobOutputPath = outputPathTextField.getText() + File.separator + new File(DataExportBean.dataFilePath).getName().split("\\.")[0] + "-Child Tables";
        if (!new File(DataExportBean.jobOutputPath).isDirectory()) {
            new File(DataExportBean.jobOutputPath).mkdir();
            if (!new File(DataExportBean.jobOutputPath).isDirectory()) {
                LOGGER.error("Can't able to create output folder.");
                ErrorAlert("Error !!", "Can't able to create Output Folder", "Please check the permission");
            }
        }
    }

    private EventHandler<ActionEvent> refractorAction() {
        return event -> {
            checkOutputFolderExists();
        };
    }

    private void updateParentTableColumns(File file) {
        List<String> tempHeaderList = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            boolean headerReadFlag = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (headerReadFlag) {
                    tempHeaderList = Arrays.asList(line.split(","));
                    headerReadFlag = false;
                } else {
                    break;
                }
            }
            bufferedReader.close();
            for (String header : tempHeaderList) {
                if (!header.contains("_ROW")) {
                    headerList.add(header.replace("\"", ""));
                } else {
                    tableList.add(header.replace("\"", ""));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Input Csv File error", e.fillInStackTrace());
            ErrorAlert("Error !!", "Can't open the file", e.getMessage());
        }
        if (headerList.size() == 0) {
            WarningAlert("Warning !!", "Parent Table Columns was empty", "Please choose valid csv file");
        } else {
            lastColumnName = headerList.get(headerList.size() - 1);
            mainTableTo.setText(headerList.get(headerList.size() - 1));
        }
    }

    private void bindSetup() {
        BooleanBinding dataPathValid = Bindings.createBooleanBinding(() -> {
            if (dataPathTextField.getText() != null && !dataPathTextField.getText().isEmpty()) {
                return true;
            }
            return false;
        }, dataPathTextField.textProperty());

        BooleanBinding outputPathValid = Bindings.createBooleanBinding(() -> {
            if (outputPathTextField.getText() != null && !outputPathTextField.getText().isEmpty()) {
                return true;
            }
            return false;
        }, outputPathTextField.textProperty());

        BooleanBinding outputType = Bindings.createBooleanBinding(() -> {
                    if (mainTableTo.getText() != null && !mainTableTo.getText().isEmpty()) {
                        return true;
                    }
                    return false;
                }
        );
        BooleanBinding lastColumnName = Bindings.createBooleanBinding(() -> {
            if (outputPathTextField.getText() != null && !outputPathTextField.getText().isEmpty()) {
                return true;
            }
            return false;
        }, outputPathTextField.textProperty());
        scheduleBtn.disableProperty().bind(dataPathValid.not().or(outputPathValid.not().or(outputType.not()).or(lastColumnName.not())));
    }
}
