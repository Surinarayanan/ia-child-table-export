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
    private double totalNumberOfLines = 0;

    private static final String CSV = ".csv";
    private static Logger LOGGER = Logger.getLogger(RefactorProcess.class);

    public RefactorProcess(String refactorDataPath, String outputLocation) {
        this.dataFilePath = refactorDataPath;
        this.outputLocation = outputLocation;
    }

    public String startRefactoring() {

        String refactorFileName = outputLocation + File.separator + UUID.randomUUID().toString().substring(0, 8) + CSV;
        CSVParser parser = new CSVParserBuilder().withSeparator(',')
                .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).withIgnoreLeadingWhiteSpace(true)
                .withIgnoreQuotations(false).withQuoteChar('"').withStrictQuotes(false).build();
        CSVReader csvReader = null;
        ICSVWriter csvWriter = null;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(refactorFileName);
            csvWriter = new CSVWriterBuilder(fileWriter).withParser(parser).build();
            csvReader = new CSVReaderBuilder(new FileReader(dataFilePath)).withCSVParser(parser).build();
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
                if (modifyLinesList.size() == 100) {
                    totalNumberOfLines += modifyLinesList.size();
                    csvWriter.writeAll(modifyLinesList);
                    modifyLinesList.clear();
                }
            }
            totalNumberOfLines += modifyLinesList.size();
            csvWriter.writeAll(modifyLinesList);
            csvWriter.close();
            fileWriter.close();
            csvReader.close();
            csvWriter = null;
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
