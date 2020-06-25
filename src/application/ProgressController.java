package application;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.opencsv.*;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static application.Alerts.ErrorAlert;

/**
 * Created by Suriyanarayanan K
 * on 09/06/20 6:09 PM.
 */


public class ProgressController implements Initializable {

    @FXML
    private JFXProgressBar progressBar;
    @FXML
    private JFXButton refreshButton;
    @FXML
    public Label progressStatusLabel;
    @FXML
    public Label progressLabel;

    String dataLocation;
    String outputLocation;
    boolean isRefactorFile;
    boolean isMultipleCsvFile;

    private Map<String, ICSVWriter> tableICSVWriterMap = new LinkedHashMap<>();
    private Map<String, List<String>> tableWithHeaderList = new LinkedHashMap<>();
    private Map<String, Boolean> isHeaderAddedInFile = new LinkedHashMap<>();

    private final static String CSV = ".csv";
    private final static String ROOT_TABLE = "main_table";
    private final static String NULL_VALUE = "";
    private static double progress_status = 0.0;
    private static double childTableCount = 0;
    private static String EXCEL = ".xlsx";
    private long headerLastPosition;

    private CSVParser parser = new CSVParserBuilder().withSeparator(',')
            .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).withIgnoreLeadingWhiteSpace(true)
            .withIgnoreQuotations(false).withQuoteChar('"').withStrictQuotes(false).build();

    private static Logger LOGGER = Logger.getLogger(DataExportController.class);

    @PostConstruct
    public void init() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        progressBar.setStyle("-fx-accent:  #1565c0;");
        progressStatusLabel.setText("0 %");
        dataLocation = DataExportBean.dataFilePath;
        outputLocation = DataExportBean.jobOutputPath;
        isRefactorFile = DataExportBean.isRefactor;
        isMultipleCsvFile = DataExportBean.isMultipleCsvFile;
        headerLastPosition = DataExportBean.headerLastPosition;
        progress_status = 0.0;
        refreshButton.setText("Cancel");
        progressLabel.setText("Please wait , Job is in Progress");
        DataExportBean.currentThread = new Thread(this::startProcess);
        Platform.runLater(
                () -> {
                    DataExportBean.currentThread.start();
                }
        );
    }

    private void startProcess() {

        String refactorDataPath = dataLocation;
        double noOfLines = 0;
        if (isRefactorFile) {
            //    LOGGER.debug("Refactor process started");
            RefactorProcess refactorProcess = new RefactorProcess(refactorDataPath, outputLocation);
            refactorDataPath = refactorProcess.startRefactoring();
            noOfLines = refactorProcess.getTotalNumberOfLines();
            //    LOGGER.debug("Refactor process completed");
        } else {
            noOfLines = getNoOfLines(refactorDataPath);
        }
        if (noOfLines != 0) {
            double progress_status_increment_row = 0.5 / noOfLines;
            Map<Integer, String> tableListWithPosition = null;
            try {
                CSVReader csvReader = new CSVReaderBuilder(new FileReader(refactorDataPath)).withCSVParser(parser).build();
                int count = 0;
                String[] line = null;
                while ((line = csvReader.readNext()) != null) {
                    if (count == 0) {
                        tableListWithPosition = determineAllTables(Arrays.asList(line));
                        initializePrintWriterForAllTables(tableListWithPosition);
                    }
                    if (count != 0) {
                        List<String> values = Arrays.asList(line);
                        if (!values.isEmpty()) {
                            List<String> rootTableValues = Arrays.asList(line).subList(0, tableWithHeaderList.get(ROOT_TABLE).size());
                            for (Map.Entry<Integer, String> childTable : tableListWithPosition.entrySet()) {
                                if (values.get(childTable.getKey()) == null) {
                                    continue;
                                }
                                String tableRowValuesWithHeader = values.get(childTable.getKey());
                                if (tableRowValuesWithHeader != null || !tableRowValuesWithHeader.isEmpty()) {
                                    String tableRowContent[] = tableRowValuesWithHeader.split("\\n");
                                    String tableHeader = tableRowContent[0];
                                    if (!isHeaderAddedInFile.get(childTable.getValue()).booleanValue()) {
                                        List<String> headerLineWriterList = new ArrayList();
                                        headerLineWriterList.addAll(tableWithHeaderList.get(ROOT_TABLE));
                                        headerLineWriterList.add("");
                                        headerLineWriterList.addAll(Arrays.asList(tableHeader.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")).stream().map(header -> header.replace("\"", ""))
                                                .collect(Collectors.toList()));
                                        tableICSVWriterMap.get(childTable.getValue()).writeNext(headerLineWriterList.toArray(new String[headerLineWriterList.size()]));
                                        isHeaderAddedInFile.put(childTable.getValue(), true);
                                    }
                                    for (int i = 1; i < tableRowContent.length; i++) {
                                        List<String> rootTableValuesList = new ArrayList<>();
                                        for (String rootTableValue : rootTableValues.subList(0, (int) headerLastPosition)) {
                                            if (rootTableValue == null) {
                                                rootTableValuesList.add(NULL_VALUE);
                                            } else {
                                                rootTableValuesList.add(rootTableValue.replace("\"", ""));
                                            }
                                        }
                                        List<String> valueLineWriterList = new ArrayList();
                                        valueLineWriterList.addAll(rootTableValuesList);
                                        valueLineWriterList.add("");
                                        for (String rowValue : tableRowContent[i].split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")) {
                                            valueLineWriterList.add(rowValue.replace("\"", ""));
                                        }
                                        tableICSVWriterMap.get(childTable.getValue())
                                                .writeNext(valueLineWriterList.toArray(new String[valueLineWriterList.size()]));
                                    }
                                }
                            }
                        }
                    }
                    count++;
                    progress_status += progress_status_increment_row;
                    progressStatusInfo(progress_status);
                }
            } catch (IOException | CsvValidationException e) {
                LOGGER.error("Write Data into Separate CSV file", e.fillInStackTrace());
                System.exit(0);
            }
            closeAllPrintWriterForAllTable();

            if (isRefactorFile) {
                String refactorFileName = "";
                File refactorFile = new File(refactorDataPath);
                refactorFileName = refactorFile.getName();
                String finalRefactorFileName = refactorFileName;
                FilenameFilter filter = (dir, name) -> name.equalsIgnoreCase(finalRefactorFileName);
                System.gc();
                boolean isDeleted = refactorFile.delete();
                if (!isDeleted) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (new File(refactorDataPath).exists()) {
                        new File(refactorDataPath).delete();
                    }
                }
            }
            if (isMultipleCsvFile) {
                progress_status += 0.5;
                progressStatusInfo(progress_status);
            } else {
                convertCsvToExcel();
            }
        }
    }

    private double getNoOfLines(String refactorDataPath) {
        double noOfLines = 0.0;
        try {
            CSVReader csvReader = new CSVReaderBuilder(new FileReader(refactorDataPath))
                    .withCSVParser(parser).build();
            String[] line = null;
            while ((line = csvReader.readNext()) != null) {
                if (!(line.length == 0)) {
                    noOfLines += 1;
                }
            }
        } catch (IOException | CsvValidationException e) {
            LOGGER.error("when Calculating number of Lines", e.fillInStackTrace());
            ErrorAlert("ERROR !!", "when Calculating number of Lines", e.getMessage());
        }
        return noOfLines;
    }

    @FXML
    private void refreshStatus(ActionEvent event) {
        if (progress_status >= 1.0) {
            Platform.runLater(() -> {
                try {
                    Desktop.getDesktop().open(new File(outputLocation));
                } catch (IOException e) {
                    LOGGER.error("Failed to open the output file", e.fillInStackTrace());
                    ErrorAlert("ERROR !!", "Failed to open ", e.getMessage());
                }
            });
            Stage progressStage = (Stage) progressLabel.getScene().getWindow();
            progressStage.close();
        } else {
            if (DataExportBean.currentThread.isAlive()) {
                DataExportBean.currentThread.stop();
            }
            File file = new File(outputLocation);
            if (file != null && file.isDirectory()) {
                deleteDirectory(file);
            }
            Stage progressStage = (Stage) progressLabel.getScene().getWindow();
            progressStage.close();
        }
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

    private void updateProgress() {
        Platform.runLater(() -> {
            String progressValue = "";
            if (progress_status >= 1.0) {
                FilenameFilter filter = (dir, name) -> name.endsWith(".csv");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               /* while (true && !isMultipleCsvFile) {
                    File outputFolder = new File(outputLocation);
                    String[] actualFiles = outputFolder.list(filter);
                    if (actualFiles.length == 0) {
                        break;
                    }
                }*/
                progressBar.setProgress(progress_status);
                progressValue = String.valueOf(progress_status * 100).split("\\.")[0] + " %";
                progressStatusLabel.setText(progressValue);
                refreshButton.setText("OK");
                progressLabel.setText("Job Completed");
            } else {
                progressBar.setProgress(progress_status);
                progressValue = String.valueOf(progress_status * 100).split("\\.")[0] + " %";
                progressStatusLabel.setText(progressValue);
            }
        });
    }

    private Predicate<String> getTablePredicate() {
        return header ->
        {
            if (header.contains("TABLE_") && header.contains("_ROW")) {
                return false;
            } else if (header.contains("_ROW")) {
                return false;
            } else {
                return true;
            }
        };
    }

    private void closeAllPrintWriterForAllTable() {
        for (ICSVWriter tableWriter : tableICSVWriterMap.values()) {
            try {
                tableWriter.flush();
                tableWriter.close();
                tableWriter = null;
            } catch (IOException e) {
                LOGGER.error("Close File Writer for this table : " + tableWriter, e.fillInStackTrace());
                ErrorAlert("ERROR !!", "Close File Writer for this table : " + tableWriter, e.getMessage());
            }
        }
        //    LOGGER.debug("Closed all the table csv file");
    }

    private void initializePrintWriterForAllTables(Map<Integer, String> tableList) {

        for (String table : tableList.values()) {
            try {
                tableICSVWriterMap.put(table, new CSVWriterBuilder(new FileWriter(outputLocation + File.separator + "TABLE_" + table + CSV)).withParser(parser).build());
            } catch (IOException e) {
                LOGGER.error("Create File Writer for this table : " + table, e.fillInStackTrace());
                ErrorAlert("ERROR !!", "Create File Writer for this table : " + table, e.getMessage());
            }
            isHeaderAddedInFile.put(table, false);
        }
        childTableCount = tableICSVWriterMap.size() - 1;
        //   LOGGER.debug("Initialized all the table csv file");
    }

    private Map<Integer, String> determineAllTables(List<String> headerList) {
        List<String> userHeaderList = headerList.stream()
                .filter(getTablePredicate())
                .collect(Collectors.toList());
        tableWithHeaderList.put(ROOT_TABLE, userHeaderList.subList(0, (int) headerLastPosition));
        Map<Integer, String> tablePosition = new LinkedHashMap<>();
        for (String header : headerList) {
            int position = headerList.indexOf(header);
            if (header.endsWith("_ROW")) {
                tablePosition.put(position, getTableName(header).trim());
            }
        }
        //   LOGGER.debug("Determine all the child table with position");
        return tablePosition;
    }

    private String getTableName(String fullPathFLow) {
        if (!fullPathFLow.contains("-")) {
            return fullPathFLow.substring(0, fullPathFLow.lastIndexOf("_"));
        } else {
            return fullPathFLow.substring(fullPathFLow.lastIndexOf("-") + 1, fullPathFLow.lastIndexOf("_"));
        }
    }

    public void convertCsvToExcel() {
        //   LOGGER.debug("Start Merging Csv file to Excel");
        double fileProgressStatus = 0.5 / childTableCount;
        File folder = new File(outputLocation);
        XSSFWorkbook workbook = new XSSFWorkbook();
        File processingFile = null;
        try {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    //    LOGGER.debug("Current Csv File :" + file.getName());
                    processingFile = file;
                    ArrayList<String> al = null;
                    ArrayList<ArrayList<String>> arlist = new ArrayList<>();
                    Sheet sheet = workbook.createSheet(file.getName().split("\\.")[0]);
                    CSVReader csvReader = new CSVReaderBuilder(new FileReader(file.getAbsoluteFile()))
                            .withCSVParser(parser).build();
                    String[] line = null;
                    while ((line = csvReader.readNext()) != null) {
                        al = new ArrayList<>();
                        for (int j = 0; j < line.length; j++) {
                            for (int k = 0; k < arlist.size(); k++) {
                                ArrayList<String> ardata = arlist.get(k);
                                Row row = sheet.createRow((short) k);
                                for (int p = 0; p < ardata.size(); p++) {
                                    Cell cell = row.createCell((short) p);
                                    setCellTypeAndValue(ardata.get(p), p, cell);
                                }
                            }
                            al.add(line[j]);
                        }
                        arlist.add(al);
                    }
                    csvReader.close();
                    updateDataIntoWorkBook(fileProgressStatus, workbook, file);
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Error Occured While data write into an excel file ," + "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
            ErrorAlert("ERROR !!", "Error Occured While data write into an excel file", "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Error Occured While data write into an excel file ," + "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
            ErrorAlert("ERROR !!", "Error Occured While data write into an excel file", "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
        } catch (CsvValidationException e) {
            LOGGER.error("Error Occured While data write into an excel file ," + "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
            ErrorAlert("ERROR !!", "Error Occured While data write into an excel file", "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
        }
    }

    private synchronized void updateDataIntoWorkBook(double fileProgressStatus, org.apache.poi.ss.usermodel.Workbook workbook, File file) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(outputLocation + File.separator + new File(dataLocation).getName().split("\\.")[0] + EXCEL);
        workbook.write(fileOut);
        fileOut.flush();
        fileOut.close();

        if (file.delete()) {
            progress_status += fileProgressStatus;
            progressStatusInfo(progress_status);
        }
    }

    private void setCellTypeAndValue(String ardata, int p, Cell cell) {
        cell.setCellType(isInteger(ardata) ? CellType.NUMERIC : CellType.STRING);
        cell.setCellValue(ardata);
    }

    public void progressStatusInfo(double time) {
        progress_status = time >= 1.0 ? 1.0 : time;
        updateProgress();
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }


}
