package com.trading.robot.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple converter turning Robot Framework output.xml into Allure results.
 * Produces minimal result and container JSON files so that Allure CLI/Maven can render reports.
 */
public final class AllureReportGenerator {

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss[.SSS]").withLocale(Locale.ENGLISH);

    private AllureReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: AllureReportGenerator <robot-output-xml> <allure-results-dir>");
            return;
        }
        Path robotOutput = Paths.get(args[0]);
        Path allureResults = Paths.get(args[1]);

        if (!Files.exists(robotOutput)) {
            System.err.println("Robot output file not found: " + robotOutput);
            return;
        }

        prepareDirectory(allureResults);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(robotOutput.toFile());
        document.getDocumentElement().normalize();

        List<SuiteResult> suites = extractSuites(document);
        writeAllureResults(allureResults, suites);
        System.out.println("Allure result files generated at " + allureResults.toAbsolutePath());
    }

    private static void prepareDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.collect(Collectors.toList())) {
                    Files.deleteIfExists(file);
                }
            }
        } else {
            Files.createDirectories(directory);
        }
    }

    private static List<SuiteResult> extractSuites(Document document) {
        NodeList suiteNodes = document.getElementsByTagName("suite");
        List<SuiteResult> suites = new ArrayList<>();
        for (int i = 0; i < suiteNodes.getLength(); i++) {
            Node node = suiteNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element suiteElement = (Element) node;
            List<TestResult> tests = extractDirectTests(suiteElement);
            if (!tests.isEmpty()) {
                SuiteResult suiteResult = new SuiteResult();
                suiteResult.name = suiteElement.getAttribute("name");
                suiteResult.uuid = UUID.randomUUID().toString();
                suiteResult.tests.addAll(tests);
                suiteResult.start = tests.stream().map(t -> t.start).filter(Objects::nonNull).min(Long::compareTo).orElse(null);
                suiteResult.stop = tests.stream().map(t -> t.stop).filter(Objects::nonNull).max(Long::compareTo).orElse(null);
                suites.add(suiteResult);
            }
        }
        return suites;
    }

    private static List<TestResult> extractDirectTests(Element suiteElement) {
        NodeList testNodes = suiteElement.getElementsByTagName("test");
        List<TestResult> tests = new ArrayList<>();
        for (int j = 0; j < testNodes.getLength(); j++) {
            Node node = testNodes.item(j);
            if (node.getParentNode() != suiteElement || node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element testElement = (Element) node;
            TestResult result = new TestResult();
            result.uuid = UUID.randomUUID().toString();
            result.name = testElement.getAttribute("name");
            result.fullName = suiteElement.getAttribute("name") + "." + result.name;
            result.testElement = testElement;
            result.suiteName = suiteElement.getAttribute("name");

            NodeList statusNodes = testElement.getElementsByTagName("status");
            Element statusElement = null;
            if (statusNodes.getLength() > 0) {
                // Get the LAST status element (final test status)
                statusElement = (Element) statusNodes.item(statusNodes.getLength() - 1);
            }
            if (statusElement != null) {
                String robotStatus = statusElement.getAttribute("status");
                result.status = mapStatus(robotStatus);
                result.start = parseTimestamp(statusElement.getAttribute("starttime"));
                result.stop = parseTimestamp(statusElement.getAttribute("endtime"));
                result.message = statusElement.getTextContent().trim();
            } else {
                result.status = "broken";
            }
            tests.add(result);
        }
        return tests;
    }

    private static Long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, TIME_FORMATTER);
            return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ex) {
            try {
                return Instant.parse(timestamp).toEpochMilli();
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private static String mapStatus(String robotStatus) {
        if (robotStatus == null) {
            return "broken";
        }
        switch (robotStatus.toUpperCase(Locale.ENGLISH)) {
            case "PASS":
                return "passed";
            case "FAIL":
                return "failed";
            case "SKIP":
            case "NOT RUN":
                return "skipped";
            default:
                return "broken";
        }
    }

    private static void writeAllureResults(Path resultsDir, List<SuiteResult> suites) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        for (SuiteResult suite : suites) {
            List<String> children = new ArrayList<>();
            for (TestResult test : suite.tests) {
                children.add(test.uuid);
                ObjectNode resultNode = mapper.createObjectNode();
                resultNode.put("uuid", test.uuid);
                resultNode.put("name", test.name);
                resultNode.put("fullName", test.fullName);
                resultNode.put("status", test.status);
                resultNode.put("stage", "finished");
                if (test.start != null) {
                    resultNode.put("start", test.start);
                }
                if (test.stop != null) {
                    resultNode.put("stop", test.stop);
                }
                
                // Calculate duration
                if (test.start != null && test.stop != null) {
                    resultNode.put("duration", test.stop - test.start);
                }
                
                ObjectNode statusDetails = mapper.createObjectNode();
                statusDetails.put("message", test.message == null ? "" : test.message);
                statusDetails.put("trace", "");
                resultNode.set("statusDetails", statusDetails);
                
                // Extract and add test steps
                ArrayNode stepsArray = extractTestSteps(mapper, test.testElement, test.start);
                resultNode.set("steps", stepsArray);
                
                resultNode.set("attachments", mapper.createArrayNode());
                resultNode.set("parameters", mapper.createArrayNode());
                
                // Add labels
                ArrayNode labelsArray = mapper.createArrayNode();
                addLabel(labelsArray, "suite", test.suiteName);
                addLabel(labelsArray, "testClass", test.suiteName);
                addLabel(labelsArray, "package", "com.trading.robot.tests");
                addLabel(labelsArray, "framework", "robotframework");
                resultNode.set("labels", labelsArray);

                Path resultFile = resultsDir.resolve(test.uuid + "-result.json");
                mapper.writeValue(resultFile.toFile(), resultNode);
            }

            ObjectNode container = mapper.createObjectNode();
            container.put("uuid", suite.uuid);
            container.put("name", suite.name);
            if (suite.start != null) {
                container.put("start", suite.start);
            }
            if (suite.stop != null) {
                container.put("stop", suite.stop);
            }
            ArrayNode childrenArray = mapper.createArrayNode();
            children.forEach(childrenArray::add);
            container.set("children", childrenArray);
            container.set("befores", mapper.createArrayNode());
            container.set("afters", mapper.createArrayNode());

            Path containerFile = resultsDir.resolve(suite.uuid + "-container.json");
            mapper.writeValue(containerFile.toFile(), container);
        }

        ObjectNode environment = mapper.createObjectNode();
        environment.put("robot.version", readRobotVersion(resultsDir.getParent()));
        environment.put("generated.at", Instant.now().toString());
        Path envFile = resultsDir.resolve("environment.properties");
        Files.writeString(envFile, "robot.version=" + environment.get("robot.version").asText() + System.lineSeparator());
    }

    private static String readRobotVersion(Path robotOutputDir) {
        Path metadata = robotOutputDir.resolve("output.xml");
        if (!Files.exists(metadata)) {
            return "unknown";
        }
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(metadata.toFile());
            doc.getDocumentElement().normalize();
            NodeList statistics = doc.getElementsByTagName("robot");
            if (statistics.getLength() > 0) {
                Element robot = (Element) statistics.item(0);
                return robot.getAttribute("generator");
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "unknown";
    }

    private static final class SuiteResult {
        String uuid;
        String name;
        Long start;
        Long stop;
        final List<TestResult> tests = new ArrayList<>();
    }

    private static ArrayNode extractTestSteps(ObjectMapper mapper, Element testElement, Long testStart) {
        ArrayNode stepsArray = mapper.createArrayNode();
        if (testElement == null) {
            return stepsArray;
        }
        
        NodeList kwNodes = testElement.getElementsByTagName("kw");
        long stepStart = testStart != null ? testStart : System.currentTimeMillis();
        
        for (int i = 0; i < kwNodes.getLength(); i++) {
            Node kwNode = kwNodes.item(i);
            if (kwNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element kwElement = (Element) kwNode;
            
            // Only process direct children (top-level keywords)
            if (kwElement.getParentNode() != testElement && 
                !kwElement.getParentNode().getNodeName().equals("kw")) {
                continue;
            }
            
            String kwName = kwElement.getAttribute("name");
            String library = kwElement.getAttribute("library");
            String fullName = library != null && !library.isEmpty() ? library + "." + kwName : kwName;
            
            // Get keyword status
            NodeList kwStatusNodes = kwElement.getElementsByTagName("status");
            String stepStatus = "passed";
            Long stepStartTime = stepStart;
            Long stepStopTime = stepStart;
            String stepMessage = "";
            
            if (kwStatusNodes.getLength() > 0) {
                Element kwStatus = (Element) kwStatusNodes.item(kwStatusNodes.getLength() - 1);
                String robotStatus = kwStatus.getAttribute("status");
                stepStatus = mapStatus(robotStatus);
                stepStartTime = parseTimestamp(kwStatus.getAttribute("starttime"));
                stepStopTime = parseTimestamp(kwStatus.getAttribute("endtime"));
                stepMessage = kwStatus.getTextContent().trim();
                
                if (stepStartTime == null) {
                    stepStartTime = stepStart;
                }
                if (stepStopTime == null) {
                    stepStopTime = stepStartTime;
                }
            }
            
            ObjectNode stepNode = mapper.createObjectNode();
            stepNode.put("name", fullName);
            stepNode.put("status", stepStatus);
            stepNode.put("stage", "finished");
            if (stepStartTime != null) {
                stepNode.put("start", stepStartTime);
            }
            if (stepStopTime != null) {
                stepNode.put("stop", stepStopTime);
                if (stepStartTime != null) {
                    stepNode.put("duration", stepStopTime - stepStartTime);
                }
            }
            
            ObjectNode stepStatusDetails = mapper.createObjectNode();
            stepStatusDetails.put("message", stepMessage);
            stepStatusDetails.put("trace", "");
            stepNode.set("statusDetails", stepStatusDetails);
            stepNode.set("attachments", mapper.createArrayNode());
            stepNode.set("parameters", mapper.createArrayNode());
            
            stepsArray.add(stepNode);
            
            // Update step start for next iteration
            if (stepStopTime != null) {
                stepStart = stepStopTime;
            }
        }
        
        return stepsArray;
    }
    
    private static void addLabel(ArrayNode labelsArray, String name, String value) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode label = mapper.createObjectNode();
        label.put("name", name);
        label.put("value", value);
        labelsArray.add(label);
    }

    private static final class TestResult {
        String uuid;
        String name;
        String fullName;
        String status;
        Long start;
        Long stop;
        String message;
        Element testElement;
        String suiteName;
    }
}

