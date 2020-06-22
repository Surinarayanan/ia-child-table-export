package application;

import com.opencsv.*;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;
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
    private double totalNumberOfLines;

    private static final String CSV = ".csv";
    private static Logger LOGGER = Logger.getLogger(RefactorProcess.class);

    public RefactorProcess(String refactorDataPath, String outputLocation) {
        this.dataFilePath = refactorDataPath;
        this.outputLocation = outputLocation;
    }

    public String startRefactoring() {

        String refactorFileName = outputLocation + File.separator + UUID.randomUUID().toString() + CSV;
        CSVParser parser = new CSVParserBuilder().withSeparator(',')
                .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).withIgnoreLeadingWhiteSpace(true)
                .withIgnoreQuotations(false).withQuoteChar('"').withStrictQuotes(false).build();
        CSVReader csvReader = null;
        ICSVWriter csvWriter = null;
        try {
            csvWriter = new CSVWriterBuilder(new FileWriter(refactorFileName)).withParser(parser).build();
            csvReader = new CSVReaderBuilder(new FileReader(dataFilePath))
                    .withCSVParser(parser).build();
            String[] line = null;
            List<String[]> modifyLinesList = new ArrayList<>();
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
                modifyLinesList.add(modifiedList.toArray(new String[modifiedList.size()]));
            }
            totalNumberOfLines = modifyLinesList.size();
            csvWriter.writeAll(modifyLinesList);
            csvWriter.close();
            LOGGER.debug("Refactor File Created");
        } catch (IOException | CsvValidationException e) {
            LOGGER.error("Error occurred while refactor", e.fillInStackTrace());
            ErrorAlert("Error occurred when refactor", e.getMessage(), e.getMessage());
        }
        return refactorFileName;
    }

    public double getTotalNumberOfLines() {
        return totalNumberOfLines;
    }
}
