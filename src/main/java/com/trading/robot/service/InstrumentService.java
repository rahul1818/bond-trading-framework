package com.trading.robot.service;

import com.trading.robot.model.Instrument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock instrument service returning randomized yield and tenor.
 */
public class InstrumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentService.class);

    private final Map<String, Instrument> cache = new ConcurrentHashMap<>();

    public InstrumentService() {
        seedInstruments();
    }

    public Optional<Instrument> findInstrument(String isin) {
        Instrument instrument = cache.computeIfAbsent(isin.toUpperCase(), this::generateInstrument);
        return Optional.ofNullable(instrument);
    }

    public List<Instrument> findByCurrency(String currency) {
        return cache.values().stream()
            .filter(instrument -> instrument.getCurrency().equalsIgnoreCase(currency))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Instrument generateInstrument(String isin) {
        if (!isin.matches("[A-Z0-9]{12}")) {
            LOGGER.warn("Invalid ISIN format requested: {}", isin);
            return null;
        }
        BigDecimal yield = randomYield();
        int tenor = ThreadLocalRandom.current().nextInt(2, 30);
        String currency = ThreadLocalRandom.current().nextBoolean() ? "USD" : "EUR";
        Instrument instrument = new Instrument(isin, "Bond-" + isin.substring(0, 4), currency, yield, tenor);
        LOGGER.debug("Generated instrument {} at {}", instrument, Instant.now());
        return instrument;
    }

    private void seedInstruments() {
        List<String> knownIsins = List.of(
            "US1234567890",
            "GB0987654321",
            "DE4455667788",
            "FR5566778899",
            "JP6677889900",
            "IN7788990011"
        );
        knownIsins.forEach(isin -> cache.put(isin, generateInstrument(isin)));
    }

    private static BigDecimal randomYield() {
        double value = ThreadLocalRandom.current().nextDouble(0.5, 8.0);
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }
}

