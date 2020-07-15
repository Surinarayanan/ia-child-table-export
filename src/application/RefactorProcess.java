package application;

import com.jfoenix.controls.JFXProgressBar;
import com.opencsv.*;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static application.Alerts.ErrorAlert;

/**
 * Created by Suriyanarayanan K
 * on 02/06/20 3:25 PM.
 */
public class RefactorProcess {

    private String dataFilePath;
    private String outputLocation;
    private double totalNumberOfLines = 0;

    private static final String CSV = ".csv";
    private static Logger LOGGER = Logger.getLogger(RefactorProcess.class);
    private double progress_status = 0.0;
    Label progressStatusLabel;
    JFXProgressBar progressBar;

    public RefactorProcess(String refactorDataPath, String outputLocation, double progress_status, Label progressStatusLabel, JFXProgressBar progressBar) {
        this.dataFilePath = refactorDataPath;
        this.outputLocation = outputLocation;
        this.progress_status = progress_status;
        this.progressStatusLabel = progressStatusLabel;
        this.progressBar = progressBar;
    }

    public String startRefactoring() {

        String refactorFileName = outputLocation + File.separator + UUID.randomUUID().toString().substring(0, 8) + CSV;
        CSVParser parser = new CSVParserBuilder().withSeparator(',')
                .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).withIgnoreLeadingWhiteSpace(true)
                .withIgnoreQuotations(false).withQuoteChar('"').withStrictQuotes(false).build();
        CSVReader countReader = null;
        CSVReader csvReader = null;
        ICSVWriter csvWriter = null;
        FileWriter fileWriter = null;
        long countNoOfLines = 0;
        try {
            progress_status += 0.2;
            progress_status = progress_status >= 1.0 ? 1.0 : progress_status;
            updateProgress();
            countReader = new CSVReaderBuilder(new FileReader(dataFilePath)).withCSVParser(parser).build();
            String[] line = null;
            while ((line = countReader.readNext()) != null) {
                countNoOfLines++;
            }

            double progress_status_increment_row = 0.8 / countNoOfLines;
            line = null;
            fileWriter = new FileWriter(refactorFileName);
            csvWriter = new CSVWriterBuilder(fileWriter).withParser(parser).build();
            csvReader = new CSVReaderBuilder(new FileReader(dataFilePath)).withCSVParser(parser).build();

            while ((line = csvReader.readNext()) != null) {
                List<String> lineValuesList = Arrays.asList(line);
                List<String> modifiedList = new ArrayList<>();
                for (String lineList : lineValuesList) {
                    if (lineList == null) {
                        modifiedList.add("");
                    } else {
                        modifiedList.add(lineList.replace("\\" + "\"", "\""));
                    }
                }
                csvWriter.writeNext(modifiedList.toArray(new String[modifiedList.size()]));
                csvWriter.flush();
                progress_status += progress_status_increment_row;
                progress_status = progress_status >= 1.0 ? 1.0 : progress_status;
                updateProgress();
                totalNumberOfLines++;
            }
            csvWriter.flush();
            csvWriter.close();
            fileWriter.close();
            csvReader.close();
            csvWriter = null;
        } catch (IOException | CsvValidationException e) {
            LOGGER.error("Error occurred while refactor", e.fillInStackTrace());
            ErrorAlert("Error occurred when refactor", e.getMessage(), e.getMessage());
        }
        LOGGER.debug("Refactor File Created");
        return refactorFileName;
    }

    public double getTotalNumberOfLines() {
        return totalNumberOfLines;
    }


    public void updateProgress() {
        Platform.runLater(() -> {
            progressBar.setProgress(progress_status);
            String progressValue = "";
            progressValue = String.valueOf(progress_status * 100).split("\\.")[0] + " %";
            progressStatusLabel.setText(progressValue);
        });
    }
}
