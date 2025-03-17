package utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to automatically generate and open Allure reports
 * after test execution completes.
 */
public class AllureReportLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(AllureReportLauncher.class);
    private static final String DEFAULT_ALLURE_RESULTS_DIR = "allure-results";
    private static final String DEFAULT_ALLURE_REPORT_DIR = "allure-report";
    private static final boolean AUTO_OPEN_REPORT = Boolean.parseBoolean(
            System.getProperty("allure.auto.open", "true"));

    /**
     * Generate and optionally open Allure report
     */
    public static void generateAndOpenReport() {
        try {
            // Create trends directory if it doesn't exist
            createTrendsDirectory();

            // Generate execution environment properties
            generateEnvironmentProperties();

            // Generate categories.json for better report organization
            generateCategoriesConfig();

            // Generate executor.json for better reporting
            generateExecutorInfo();

            // Generate the report
            if (generateReport()) {
                // Open the report if configured to do so
                if (AUTO_OPEN_REPORT) {
                    openReport();
                }
            } else {
                LOG.error("Failed to generate Allure report. Skipping report opening.");
            }
        } catch (Exception e) {
            LOG.error("Failed to generate and open Allure report", e);
        }
    }

    /**
     * Creates trends directory to enable trend charts
     */
    private static void createTrendsDirectory() {
        try {
            Path trendsDir = Paths.get(DEFAULT_ALLURE_RESULTS_DIR, "history");
            if (!Files.exists(trendsDir)) {
                Files.createDirectories(trendsDir);
                LOG.info("Created trends directory: {}", trendsDir);
            }
        } catch (IOException e) {
            LOG.error("Failed to create trends directory", e);
        }
    }

    /**
     * Generates environment.properties file with test environment details
     */
    private static void generateEnvironmentProperties() {
        try {
            StringBuilder props = new StringBuilder();
            props.append("Browser=").append(System.getProperty("browser", "chrome")).append("\n");
            props.append("Browser.Version=").append(System.getProperty("browser.version", "latest")).append("\n");
            props.append("Environment=").append(System.getProperty("env", "test")).append("\n");
            props.append("OS=").append(System.getProperty("os.name")).append("\n");
            props.append("Java.Version=").append(System.getProperty("java.version")).append("\n");
            props.append("Build=").append(System.getProperty("build.number", "local")).append("\n");

            Path envFile = Paths.get(DEFAULT_ALLURE_RESULTS_DIR, "environment.properties");
            Files.write(envFile, props.toString().getBytes());
            LOG.info("Generated environment properties file");
        } catch (IOException e) {
            LOG.error("Failed to generate environment properties", e);
        }
    }

    /**
     * Generates categories.json file for better report organization
     */
    private static void generateCategoriesConfig() {
        try {
            String categoriesJson = "[\n" +
                    "  {\n" +
                    "    \"name\": \"Critical Errors\",\n" +
                    "    \"matchedStatuses\": [\"failed\"],\n" +
                    "    \"messageRegex\": \".*AssertionError.*|.*NullPointerException.*\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Element Interaction Problems\",\n" +
                    "    \"matchedStatuses\": [\"broken\"],\n" +
                    "    \"messageRegex\": \".*ElementNotInteractableException.*|.*NoSuchElementException.*\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Timeouts\",\n" +
                    "    \"matchedStatuses\": [\"broken\"],\n" +
                    "    \"messageRegex\": \".*TimeoutException.*|.*timed out.*\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Skipped Tests\",\n" +
                    "    \"matchedStatuses\": [\"skipped\"]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Product Defects\",\n" +
                    "    \"matchedStatuses\": [\"failed\"]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Test Defects\",\n" +
                    "    \"matchedStatuses\": [\"broken\"]\n" +
                    "  }\n" +
                    "]";

            Path categoriesFile = Paths.get(DEFAULT_ALLURE_RESULTS_DIR, "categories.json");
            Files.write(categoriesFile, categoriesJson.getBytes());
            LOG.info("Generated categories configuration file");
        } catch (IOException e) {
            LOG.error("Failed to generate categories config", e);
        }
    }

    /**
     * Generates executor.json file with build information
     */
    private static void generateExecutorInfo() {
        try {
            String buildName = System.getProperty("build.name", "Local Build");
            String buildUrl = System.getProperty("build.url", "");
            String reportUrl = System.getProperty("report.url", "");

            String executorJson = "{\n" +
                    "  \"name\": \"" + buildName + "\",\n" +
                    "  \"type\": \"jenkins\",\n" +
                    "  \"buildName\": \"" + buildName + "\",\n" +
                    (buildUrl.isEmpty() ? "" : "  \"buildUrl\": \"" + buildUrl + "\",\n") +
                    (reportUrl.isEmpty() ? "" : "  \"reportUrl\": \"" + reportUrl + "\",\n") +
                    "  \"reportName\": \"Allure Report " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\"\n" +
                    "}";

            Path executorFile = Paths.get(DEFAULT_ALLURE_RESULTS_DIR, "executor.json");
            Files.write(executorFile, executorJson.getBytes());
            LOG.info("Generated executor information file");
        } catch (IOException e) {
            LOG.error("Failed to generate executor info", e);
        }
    }

    /**
     * Generates the Allure report using command-line tools
     *
     * @return true if the report was generated successfully, false otherwise
     */
    private static boolean generateReport() {
        try {
            // Delete existing report directory
            Path reportDir = Paths.get(DEFAULT_ALLURE_REPORT_DIR);
            if (Files.exists(reportDir)) {
                deleteDirectory(reportDir.toFile());
            }

            // Check if Allure is installed
            if (!isAllureInstalled()) {
                LOG.warn("Allure command-line not found in PATH. Report will not be generated.");
                return false;
            }

            // Generate report
            List<String> command = Arrays.asList(
                    "allure.bat", "serve", DEFAULT_ALLURE_RESULTS_DIR,
                    "--clean"
            );

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int result = process.waitFor();
            if (result == 0) {
                LOG.info("Allure report generated successfully");
                return true;
            } else {
                LOG.error("Failed to generate Allure report. Exit code: {}", result);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Failed to generate Allure report", e);
            return false;
        }
    }

    /**
     * Checks if Allure is installed and available in the system PATH
     *
     * @return true if Allure is installed, false otherwise
     */
    private static boolean isAllureInstalled() {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder checkProcess = new ProcessBuilder(
                    isWindows ? "where" : "which",
                    "allure"
            );
            Process check = checkProcess.start();
            int exitCode = check.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            LOG.error("Failed to check if Allure is installed", e);
            return false;
        }
    }

    /**
     * Opens the generated Allure report in the default web browser
     */
    private static void openReport() {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

            ProcessBuilder processBuilder;
            if (isWindows) {
                processBuilder = new ProcessBuilder("allure.bat", "serve", Paths.get(DEFAULT_ALLURE_RESULTS_DIR).toString(), "--clean");
            } else if (isMac) {
                processBuilder = new ProcessBuilder("allure.sh", "serve", Paths.get(DEFAULT_ALLURE_RESULTS_DIR).toString(), "--clean");
            } else {
                // Linux or other Unix-like systems
                processBuilder = new ProcessBuilder("allure.sh", "serve", Paths.get(DEFAULT_ALLURE_RESULTS_DIR).toString(), "--clean");
            }

            processBuilder.start();
            LOG.info("Opened Allure report in default browser");
        } catch (IOException e) {
            LOG.error("Failed to open Allure report", e);
        }
    }

    /**
     * Recursively deletes a directory and all its contents
     */
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}