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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
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
    private JFXButton cancelButton;
    @FXML
    public Label progressStatusLabel;

    @FXML
    public Label statusLabel;
    @FXML
    public Label progressLabel;

    String dataLocation;
    String outputLocation;
    boolean isRefactorFile;
    boolean isMultipleCsvFile;

    private Map<String, ICSVWriter> tableICSVWriterMap = new LinkedHashMap<>();
    private Map<String, List<String>> tableWithHeaderList = new LinkedHashMap<>();
    private Map<String, Boolean> isHeaderAddedInFile = new LinkedHashMap<>();
    private Map<String, Long> tableRowCount = new LinkedHashMap<>();

    private final static String CSV = ".csv";
    private final static String ROOT_TABLE = "main_table";
    private final static String NULL_VALUE = "";
    private static double progress_status = 0.0;
    private static double childTableCount = 0;
    private static String EXCEL = ".xlsx";
    private static String splitRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    private long headerLastPosition;
    long CHUNK_SIZE = 500;

    private CSVParser parser = new CSVParserBuilder().withSeparator(',')
            .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).withIgnoreLeadingWhiteSpace(true)
            .withIgnoreQuotations(false).withQuoteChar('"').withStrictQuotes(false).build();

    private static Logger LOGGER = Logger.getLogger(DataExportController.class);
    private static boolean wholeProcessCompleted;
    int THREAD_CAPACITY = Runtime.getRuntime().availableProcessors() * 2; // sufficent usgae of threads ...
    ForkJoinPool customThreadPool = new ForkJoinPool(THREAD_CAPACITY);

    @PostConstruct
    public void init() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ForkJoinPool customThreadPool = new ForkJoinPool(THREAD_CAPACITY);
        wholeProcessCompleted = false;
        progressBar.setStyle("-fx-accent:  #1565c0;");
        progressStatusLabel.setText("0 %");
        dataLocation = DataExportBean.dataFilePath;
        outputLocation = DataExportBean.jobOutputPath;
        isRefactorFile = DataExportBean.isRefactor;
        isMultipleCsvFile = DataExportBean.isMultipleCsvFile;
        headerLastPosition = DataExportBean.headerLastPosition;
        progress_status = 0.0;
        cancelButton.setText("Cancel");
        DataExportBean.currentThread = new Thread(this::startProcess);
        Platform.runLater(
                () -> {
                    DataExportBean.currentThread.start();
                }
        );
    }


    private void startProcess() {

        Platform.runLater(() -> {
            if (isMultipleCsvFile && isRefactorFile) {
                progressLabel.setText("Please wait , Step 1 of Step 2");
            } else if (isMultipleCsvFile) {
                progressLabel.setText("Please wait , Step 1 of Step 1");
            } else if (isRefactorFile) {
                progressLabel.setText("Please wait , Step 1 of Step 3");
            } else {
                progressLabel.setText("Please wait , Step 1 of Step 2");
            }
        });
        String refactorDataPath = dataLocation;
        double noOfLines = 0;
        if (isRefactorFile) {
            RefactorProcess refactorProcess = new RefactorProcess(refactorDataPath, outputLocation, progress_status, progressStatusLabel, progressBar);
            refactorDataPath = refactorProcess.startRefactoring();
            noOfLines = refactorProcess.getTotalNumberOfLines();
        } else {
            noOfLines = getNoOfLines(refactorDataPath);
        }

        progress_status = 0.0;
        updateProgress();
        Platform.runLater(() -> {
            if (isMultipleCsvFile && isRefactorFile) {
                progressLabel.setText("Please wait , Step 2 of Step 2");
            } else if (isMultipleCsvFile) {
                progressLabel.setText("Please wait , Step 1 of Step 1");
            } else if (isRefactorFile) {
                progressLabel.setText("Please wait , Step 2 of Step 3");
            } else {
                progressLabel.setText("Please wait , Step 1 of Step 2");
            }

            /*if (isMultipleCsvFile) {
                progressLabel.setText("Please wait , Step 2 of Step 2");
            } else {
                progressLabel.setText("Please wait , Step 2 of Step 3");
            }*/
        });
        if (noOfLines != 0) {
            double progress_status_increment_row = 1.0 / noOfLines;
            Map<Integer, String> tableListWithPosition = null;
            try {
                CSVReader csvReader = new CSVReaderBuilder(new FileReader(refactorDataPath)).withCSVParser(parser).build();
                long count = 0;
                String[] line = null;

                while ((line = csvReader.readNext()) != null) {
                    long finalCount = count;
                    Platform.runLater(() -> {
                        statusLabel.setText("Processing Line No :" + finalCount);
                    });
                    if (count == 0) {
                        tableListWithPosition = determineAllTables(Arrays.asList(line));
                        initializePrintWriterForAllTables(tableListWithPosition);
                    }
                    if (count != 0) {
                        List<String> values = Arrays.asList(line);
                       /* if (values.size() < tableWithHeaderList.get(ROOT_TABLE).size()) {
                            LOGGER.debug("Values Size :" + values.size());
                            LOGGER.debug("Values Size :" + tableWithHeaderList.get(ROOT_TABLE).size());
                            LOGGER.debug("Main Table Values Missing at line no :" + count);
                        }*/
                        if (!values.isEmpty() && values.size() > tableWithHeaderList.get(ROOT_TABLE).size()) {
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
                                        headerLineWriterList.addAll(Arrays.asList(tableHeader.split(splitRegex)).stream().map(header -> header.replace("\"", ""))
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
                                        for (String rowValue : tableRowContent[i].split(splitRegex)) {
                                            valueLineWriterList.add(rowValue.replace("\"", ""));
                                        }
                                        tableICSVWriterMap.get(childTable.getValue()).writeNext(valueLineWriterList.toArray(new String[valueLineWriterList.size()]));
                                        tableRowCount.put(childTable.getValue(), tableRowCount.get(childTable.getValue()) + 1);
                                    }
                                }
                            }
                        }
                    }
                    count++;
                    progress_status += progress_status_increment_row;
                    progress_status = progress_status >= 1.0 ? 1.0 : progress_status;
                    updateProgress();
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
                progress_status = progress_status >= 1.0 ? 1.0 : progress_status;
                wholeProcessCompleted = true;
                updateProgress();
            } else {
                Platform.runLater(() -> {
                    progressLabel.setText("Please wait , Step 3 of Step 3");
                });
                progress_status = 0.0;
                updateProgress();
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
    private void cancelAction(ActionEvent event) {
        if (progress_status >= 1.0 && wholeProcessCompleted) {
            Platform.runLater(() -> {
                try {
                    clearBeans();
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

    private void clearBeans() {
        DataExportBean.clear();
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
            progressBar.setProgress(progress_status);
            progressValue = String.valueOf(progress_status * 100).split("\\.")[0] + " %";
            progressStatusLabel.setText(progressValue);
            if (progress_status >= 1.0 && wholeProcessCompleted) {
                statusLabel.setText(" ");
                cancelButton.setText("OK");
                progressLabel.setText("Job Completed");
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
    }

    private void initializePrintWriterForAllTables(Map<Integer, String> tableList) {

        for (String table : tableList.values()) {
            try {
                tableICSVWriterMap.put(table, new CSVWriterBuilder(new FileWriter(outputLocation + File.separator + "TABLE_" + table + CSV)).withParser(parser).build());
                tableRowCount.put(table, (long) 0);
            } catch (IOException e) {
                LOGGER.error("Create File Writer for this table : " + table, e.fillInStackTrace());
                ErrorAlert("ERROR !!", "Create File Writer for this table : " + table, e.getMessage());
            }
            isHeaderAddedInFile.put(table, false);
        }
        childTableCount = tableICSVWriterMap.size() - 1;
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
        LOGGER.debug("Table Rwo Count :" + tableRowCount);
        double fileProgressStatus = 1.0 / childTableCount;
        double tableFileProcessStatus;
        File folder = new File(outputLocation);

        XSSFWorkbook workbook = new XSSFWorkbook();
        File processingFile = null;
        Sheet sheet = null;
        try {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    LOGGER.debug("Current Csv File :" + file.getName() + " start processing");
                    String tableName = file.getName().substring(6).split("\\.")[0];
                    tableFileProcessStatus = fileProgressStatus / tableRowCount.get(tableName);
                    processingFile = file;
                    sheet = workbook.createSheet(file.getName().split("\\.")[0]);
                    CSVReader csvReader = new CSVReaderBuilder(new FileReader(file.getAbsoluteFile()))
                            .withCSVParser(parser).build();
                    String[] line = null;
                    long rownum = 0;
                    while ((line = csvReader.readNext()) != null) {
                        Long finalRownum = rownum;
                        Platform.runLater(() -> {
                            statusLabel.setText("File Name :" + file.getName().split("\\.")[0] + "  line :" + finalRownum);
                        });
                        Row row = sheet.createRow(Math.toIntExact(rownum++));
                        int cellnum = 0;
                        for (String obj : line) {
                            Cell cell = row.createCell(cellnum++);
                            cell.setCellValue(obj);
                        }
                        //re assign the sheet exceeds chunk size
                        if (rownum % CHUNK_SIZE == 0) {
                            FileOutputStream fileOut = new FileOutputStream(outputLocation + File.separator + new File(dataLocation).getName().split("\\.")[0] + EXCEL);
                            workbook.write(fileOut);
                            fileOut.flush();
                            fileOut.close();
                            sheet = workbook.getSheet(file.getName().split("\\.")[0]);
                        }
                        progress_status += tableFileProcessStatus;
                        progress_status = progress_status >= 1.0 ? 1.0 : progress_status;
                        updateProgress();
                    }
                    csvReader.close();
                    LOGGER.debug("Current Csv File  :" + file.getName() + " processed");
                    updateDataIntoWorkBook(fileProgressStatus, workbook, file);
                }
            }
            wholeProcessCompleted = true;
        } catch (FileNotFoundException e) {
            LOGGER.error("Error occurred While data write into an excel file ," + "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
            ErrorAlert("ERROR !!", "Error occurred While data write into an excel file", "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Error occurred While data write into an excel file ," + "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
            ErrorAlert("ERROR !!", "Error occurred While data write into an excel file", "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
        } catch (CsvValidationException e) {
            LOGGER.error("Error occurred While data write into an excel file ," + "Processing File Name :" + processingFile.getName() +
                    "\n" + e.getMessage());
            ErrorAlert("ERROR !!", "Error occurred While data write into an excel file", "Processing File Name :" + processingFile.getName() +
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
            progress_status = progress_status >= 1.0 ? 1.0 : progress_status;
            if (progress_status >= 1.0) {
                wholeProcessCompleted = true;
            }
            updateProgress();
        }
    }


}
