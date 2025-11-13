package com.trading.robot.service;

import com.trading.robot.model.TradeRequest;
import com.trading.robot.model.TradeSide;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Loads trade requests from CSV files to drive data-driven tests.
 */
public class TradeDataLoader {

    public List<TradeRequest> loadCsv(Path csvPath) {
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim()
                 .parse(reader)) {
            List<TradeRequest> requests = new ArrayList<>();
            for (CSVRecord record : parser) {
                TradeRequest request = new TradeRequest(
                    record.get("ISIN"),
                    record.get("Trader"),
                    Integer.parseInt(record.get("Quantity")),
                    TradeSide.valueOf(record.get("Side").toUpperCase()),
                    new BigDecimal(record.get("LimitPrice"))
                );
                requests.add(request);
            }
            return requests;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load CSV data from " + csvPath, ex);
        }
    }
}

