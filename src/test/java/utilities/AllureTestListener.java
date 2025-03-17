package utilities;

import io.qameta.allure.*;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import stepdefinitions.TestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced Allure listener that provides comprehensive test reporting
 * with rich data visualization capabilities and automatic report generation.
 */
public class AllureTestListener implements TestLifecycleListener {
    private static final Logger LOG = LoggerFactory.getLogger(AllureTestListener.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CAPTURE_SUCCESS_SCREENSHOTS_PROPERTY = "allure.capture.success.screenshots";

    // Test execution metrics
    private static final Map<String, Long> testStartTimes = new ConcurrentHashMap<>();
    private static final AtomicInteger totalTests = new AtomicInteger(0);
    private static final AtomicInteger passedTests = new AtomicInteger(0);
    private static final AtomicInteger failedTests = new AtomicInteger(0);
    private static final AtomicInteger skippedTests = new AtomicInteger(0);
    private static final AtomicInteger brokenTests = new AtomicInteger(0);
    private static final AtomicLong totalExecutionTime = new AtomicLong(0);

    // Store browser console logs between lifecycle events
    private static final ThreadLocal<List<String>> consoleLogStorage = ThreadLocal.withInitial(ArrayList::new);

    // Custom categories for better report organization
    private static final List<String> CRITICAL_ERRORS = Arrays.asList(
            "AssertionError", "NullPointerException", "ElementNotInteractableException"
    );

    @Override
    public void beforeTestStart(TestResult result) {
        if (result == null) {
            LOG.warn("TestResult is null in beforeTestStart");
            return;
        }

        String testId = result.getUuid();
        String testName = result.getName();
        LOG.info("Starting test: {}", testName);

        // Store start time for duration calculation
        testStartTimes.put(testId, System.currentTimeMillis());
        totalTests.incrementAndGet();

        // Add essential test metadata
        Allure.addAttachment("Test Start Time", "text/plain", LocalDateTime.now().format(TIME_FORMATTER));

        // Add test categorization for better filtering in reports
        addTestCategories(result);

        // Add custom test parameters for better visualization
        if (TestBase.getTestParameters() != null) {
            Map<String, String> params = TestBase.getTestParameters();
            params.forEach(Allure::parameter);
        }

        // Add step to show test initialization
        Allure.step("Test Initialization", () -> LOG.debug("Test environment prepared for: {}", testName));
    }

    @Override
    public void afterTestStart(TestResult result) {
        if (result == null) {
            LOG.warn("TestResult is null in afterTestStart");
            return;
        }

        // Add detailed environment information
        try {
            Map<String, String> envInfo = new HashMap<>();
            envInfo.put("OS", System.getProperty("os.name"));
            envInfo.put("OS Version", System.getProperty("os.version"));
            envInfo.put("Java Version", System.getProperty("java.version"));
            envInfo.put("Browser", TestBase.getBrowserName());
            envInfo.put("Browser Version", TestBase.getBrowserVersion());
            envInfo.put("Test Environment", TestBase.getEnvironmentName());

            // Write environment properties to a file
            writeEnvironmentProperties(envInfo);

            // Add as attachment for individual test view
            StringBuilder envDetails = new StringBuilder();
            envInfo.forEach((k, v) -> envDetails.append(k).append(": ").append(v).append("\n"));

            Allure.addAttachment("Environment Details", "text/plain", envDetails.toString());
        } catch (Exception e) {
            LOG.warn("Failed to attach environment info", e);
        }

        // Capture initial page state if available
        captureInitialState(result.getName());
    }

    /**
     * Writes environment properties to a file in the allure-results directory.
     */
    private void writeEnvironmentProperties(Map<String, String> envInfo) {
        try {
            // Define the path to the environment.properties file
            String allureResultsDir = System.getProperty("allure.results.directory", "allure-results");
            File environmentFile = new File(allureResultsDir, "environment.properties");

            // Create the allure-results directory if it doesn't exist
            if (!environmentFile.getParentFile().exists()) {
                environmentFile.getParentFile().mkdirs();
            }

            // Write properties to the file
            try (PrintWriter writer = new PrintWriter(environmentFile, StandardCharsets.UTF_8)) {
                envInfo.forEach((key, value) -> writer.println(key + "=" + value));
            }

            LOG.info("Environment properties written to: {}", environmentFile.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("Failed to write environment properties", e);
        }
    }

    @Override
    public void beforeTestStop(TestResult result) {
        if (result == null) {
            LOG.warn("TestResult is null in beforeTestStop");
            return;
        }

        Status status = result.getStatus();
        String testName = result.getName();
        String testId = result.getUuid();

        LOG.info("Test finishing: {} with status: {}", testName, status);

        // Update test counters based on status
        updateTestCounters(status);

        // Calculate test duration
        long endTime = System.currentTimeMillis();
        long startTime = testStartTimes.getOrDefault(testId, endTime);
        long duration = endTime - startTime;
        totalExecutionTime.addAndGet(duration);

        // Handle different test result states
        switch (status) {
            case FAILED:
            case BROKEN:
                captureFailureEvidence(testName, result.getStatusDetails().getTrace(), duration);
                break;
            case SKIPPED:
                captureSkipEvidence(testName, result.getStatusDetails().getMessage(), duration);
                break;
            case PASSED:
                captureSuccessEvidence(testName, duration);
                break;
            default:
                LOG.warn("Unexpected test status: {}", status);
        }

        // Add browser console logs as attachment
        captureBrowserLogs(testName);

        // Add performance metrics if available
        capturePerformanceMetrics(testName);
    }

    @Override
    public void afterTestStop(TestResult result) {
        if (result == null) {
            LOG.warn("TestResult is null in afterTestStop");
            return;
        }

        String testId = result.getUuid();

        // Calculate and add test execution duration
        long startTime = testStartTimes.getOrDefault(testId, 0L);
        if (startTime > 0) {
            long endTime = System.currentTimeMillis();
            long durationMillis = endTime - startTime;

            // Remove from map to free memory
            testStartTimes.remove(testId);

            // Add duration in different formats for better visualization
            Allure.addAttachment("Test Duration", "text/plain",
                    String.format("Duration: %.2f seconds (%.2f minutes)",
                            durationMillis / 1000.0,
                            durationMillis / 60000.0));

            // Add execution time histogram data
            updateExecutionTimeHistogram(result.getName(), durationMillis);
        }

        // Add test summary
        addTestSummary(result);

        // Clear thread-local storage
        consoleLogStorage.remove();

        LOG.info("Test completed: {} with status: {}", result.getName(), result.getStatus());
    }

    @Override
    public void beforeTestWrite(TestResult result) {
        // Add test statistics to Allure report for better visualization
        addTestStatistics();
    }

    private void updateTestCounters(Status status) {
        switch (status) {
            case PASSED:
                passedTests.incrementAndGet();
                break;
            case FAILED:
                failedTests.incrementAndGet();
                break;
            case SKIPPED:
                skippedTests.incrementAndGet();
                break;
            case BROKEN:
                brokenTests.incrementAndGet();
                break;
            default:
                // No action needed
        }
    }

    private void addTestCategories(TestResult result) {
        try {
            String fullName = result.getFullName();
            if (fullName != null && fullName.contains(".")) {
                String packageName = fullName.substring(0, fullName.lastIndexOf("."));
                result.getLabels().add(new Label().setName("package").setValue(packageName));
            }

            Class<?> testClass = TestBase.getCurrentTestClass();
            if (testClass != null) {
                if (testClass.isAnnotationPresent(Epic.class)) {
                    result.getLabels().add(new Label().setName("epic").setValue(testClass.getAnnotation(Epic.class).value()));
                }
                if (testClass.isAnnotationPresent(Feature.class)) {
                    result.getLabels().add(new Label().setName("feature").setValue(testClass.getAnnotation(Feature.class).value()));
                }
            }

            Method testMethod = TestBase.getCurrentTestMethod();
            if (testMethod != null) {
                if (testMethod.isAnnotationPresent(Story.class)) {
                    result.getLabels().add(new Label().setName("story").setValue(testMethod.getAnnotation(Story.class).value()));
                }
                if (testMethod.isAnnotationPresent(Severity.class)) {
                    result.getLabels().add(new Label().setName("severity").setValue(testMethod.getAnnotation(Severity.class).value().toString()));
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to add test categories", e);
        }
    }

    private void captureInitialState(String testName) {
        if (TestBase.getPage() == null) {
            LOG.warn("Page object is null, cannot capture initial state");
            return;
        }

        try {
            byte[] screenshot = TestBase.getPage().screenshot();
            Allure.addAttachment(testName + "_Initial_State", "image/png", new ByteArrayInputStream(screenshot), ".png");
            Allure.attachment("Initial URL", TestBase.getPage().url());
        } catch (Exception e) {
            LOG.warn("Failed to capture initial state", e);
        }
    }

    private void captureFailureEvidence(String testName, String errorTrace, long duration) {
        LOG.debug("Capturing failure evidence for test: {}", testName);

        try {
            if (TestBase.getPage() != null) {
                try {
                    byte[] screenshot = TestBase.getPage().screenshot();
                    Allure.addAttachment(testName + "_Failed_Screenshot", "image/png", new ByteArrayInputStream(screenshot), ".png");

                    String pageContent = TestBase.getPage().content();
                    if (pageContent != null && !pageContent.isEmpty()) {
                        Allure.addAttachment(testName + "_Page_HTML", "text/html", pageContent);
                    }

                    Allure.addAttachment(testName + "_Current_URL", "text/plain", TestBase.getPage().url());

                    try {
                        String domStructure = (String) TestBase.getPage().evaluate("() => document.documentElement.outerHTML");
                        Allure.addAttachment(testName + "_DOM_Structure", "text/html", domStructure);
                    } catch (Exception e) {
                        LOG.debug("Could not capture DOM structure", e);
                    }

                    byte[] videoBytes = TestBase.getVideoRecording();
                    if (videoBytes != null && videoBytes.length > 0) {
                        Allure.addAttachment(testName + "_Test_Recording", "video/mp4", new ByteArrayInputStream(videoBytes), ".mp4");
                    }
                } catch (Exception e) {
                    LOG.error("Failed to capture screenshot or page content", e);
                    captureExceptionDetails("Screenshot_Capture_Error", e);
                }
            } else {
                LOG.warn("Cannot capture screenshot - Page object is null");
                Allure.addAttachment(testName + "_No_Screenshot", "text/plain", "No screenshot available: Page object was null");
            }

            if (errorTrace != null) {
                Allure.addAttachment(testName + "_Error_Trace", "text/plain", errorTrace);
                analyzeError(testName, errorTrace);
            }

            addDurationInfo(testName, duration, "Failed");
        } catch (Exception e) {
            LOG.error("Error in captureFailureEvidence", e);
            captureExceptionDetails("Evidence_Capture_Error", e);
        }
    }

    private void analyzeError(String testName, String errorTrace) {
        try {
            String errorAnalysis = "Error Analysis:\n";

            for (String criticalError : CRITICAL_ERRORS) {
                if (errorTrace.contains(criticalError)) {
                    errorAnalysis += "- Found critical error: " + criticalError + "\n";
                    errorAnalysis += "- Suggestion: " + getSuggestionForError(criticalError) + "\n";
                    break;
                }
            }

            if (errorTrace.contains("TimeoutException") || errorTrace.contains("timed out")) {
                errorAnalysis += "- Possible performance or loading issue detected\n";
                errorAnalysis += "- Check for slow-loading elements or network issues\n";
            }

            if (errorTrace.contains("NoSuchElementException") || errorTrace.contains("ElementNotFound")) {
                errorAnalysis += "- Element not found on page\n";
                errorAnalysis += "- Check if selectors are correct or if element is in a different frame\n";
            }

            Allure.addAttachment(testName + "_Error_Analysis", "text/plain", errorAnalysis);
        } catch (Exception e) {
            LOG.warn("Failed to analyze error", e);
        }
    }

    private String getSuggestionForError(String error) {
        switch (error) {
            case "NullPointerException":
                return "Check for uninitialized objects or null values.";
            case "ElementNotInteractableException":
                return "Ensure the element is visible and not covered by another element.";
            case "TimeoutException":
                return "Increase wait time or check for slow network conditions.";
            default:
                return "Review the error trace for more details.";
        }
    }

    private void captureSkipEvidence(String testName, String skipReason, long duration) {
        try {
            Allure.addAttachment(testName + "_Skip_Reason", "text/plain", skipReason != null ? skipReason : "Test was skipped with no reason provided");

            if (TestBase.getPage() != null) {
                try {
                    Allure.addAttachment(testName + "_Skipped_Screenshot", "image/png", new ByteArrayInputStream(TestBase.getPage().screenshot()), ".png");
                } catch (Exception e) {
                    LOG.warn("Failed to capture screenshot for skipped test", e);
                }
            }

            addDurationInfo(testName, duration, "Skipped");
        } catch (Exception e) {
            LOG.error("Error in captureSkipEvidence", e);
        }
    }

    private void captureSuccessEvidence(String testName, long duration) {
        try {
            boolean captureSuccessScreenshots = Boolean.parseBoolean(System.getProperty(CAPTURE_SUCCESS_SCREENSHOTS_PROPERTY, "false"));

            if (captureSuccessScreenshots && TestBase.getPage() != null) {
                Allure.addAttachment(testName + "_Success_Screenshot", "image/png", new ByteArrayInputStream(TestBase.getPage().screenshot()), ".png");
            }

            addDurationInfo(testName, duration, "Passed");
        } catch (Exception e) {
            LOG.warn("Failed to capture success evidence", e);
        }
    }

    private void captureBrowserLogs(String testName) {
        try {
            List<String> consoleLogs = TestBase.getBrowserConsoleLogs();
            if (consoleLogs != null && !consoleLogs.isEmpty()) {
                consoleLogStorage.get().addAll(consoleLogs);

                StringBuilder logContent = new StringBuilder();
                logContent.append("=== BROWSER CONSOLE LOGS ===\n");
                consoleLogs.forEach(log -> logContent.append(log).append("\n"));

                Allure.addAttachment(testName + "_Browser_Console_Logs", "text/plain", logContent.toString());

                long errorCount = consoleLogs.stream()
                        .filter(log -> log.contains("ERROR") || log.contains("SEVERE"))
                        .count();

                if (errorCount > 0) {
                    Allure.addAttachment(testName + "_Console_Errors_Found", "text/plain", String.format("Found %d errors in browser console logs", errorCount));
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to capture browser console logs", e);
        }
    }

    private void capturePerformanceMetrics(String testName) {
        try {
            Map<String, Object> performanceMetrics = TestBase.getPerformanceMetrics();
            if (performanceMetrics != null && !performanceMetrics.isEmpty()) {
                StringBuilder metrics = new StringBuilder();
                metrics.append("=== PERFORMANCE METRICS ===\n");

                performanceMetrics.forEach((key, value) -> metrics.append(key).append(": ").append(value).append("\n"));

                Allure.addAttachment(testName + "_Performance_Metrics", "text/plain", metrics.toString());
            }
        } catch (Exception e) {
            LOG.warn("Failed to capture performance metrics", e);
        }
    }

    private void addDurationInfo(String testName, long duration, String outcome) {
        try {
            String formattedDuration;
            if (duration < 1000) {
                formattedDuration = duration + " ms";
            } else if (duration < 60000) {
                formattedDuration = String.format("%.2f seconds", duration / 1000.0);
            } else {
                formattedDuration = String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(duration),
                        TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
            }

            Allure.addAttachment(testName + "_" + outcome + "_Duration", "text/plain", "Test duration: " + formattedDuration);
        } catch (Exception e) {
            LOG.warn("Failed to add duration info", e);
        }
    }

    private void updateExecutionTimeHistogram(String testName, long durationMillis) {
        try {
            LOG.debug("Execution time for {}: {} ms", testName, durationMillis);
        } catch (Exception e) {
            LOG.warn("Failed to update execution time histogram", e);
        }
    }

    private void addTestSummary(TestResult result) {
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("=== TEST SUMMARY ===\n");
            summary.append("Name: ").append(result.getName()).append("\n");
            summary.append("Status: ").append(result.getStatus()).append("\n");

            if (result.getStatusDetails() != null && result.getStatusDetails().getMessage() != null) {
                summary.append("Message: ").append(result.getStatusDetails().getMessage()).append("\n");
            }

            result.getParameters().forEach(param ->
                    summary.append("Parameter: ")
                            .append(param.getName())
                            .append(" = ")
                            .append(param.getValue())
                            .append("\n"));

            Allure.addAttachment("Test Summary", "text/plain", summary.toString());
        } catch (Exception e) {
            LOG.warn("Failed to add test summary", e);
        }
    }

    private void captureExceptionDetails(String name, Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            Allure.addAttachment(name, "text/plain", sw.toString());
        } catch (Exception ex) {
            LOG.error("Failed to capture exception details", ex);
        }
    }

    private void addTestStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== TEST STATISTICS ===\n");
        stats.append("Total Tests: ").append(totalTests.get()).append("\n");
        stats.append("Passed Tests: ").append(passedTests.get()).append("\n");
        stats.append("Failed Tests: ").append(failedTests.get()).append("\n");
        stats.append("Skipped Tests: ").append(skippedTests.get()).append("\n");
        stats.append("Broken Tests: ").append(brokenTests.get()).append("\n");
        stats.append("Total Execution Time: ").append(totalExecutionTime.get() / 1000.0).append(" seconds\n");

        Allure.addAttachment("Test Statistics", "text/plain", stats.toString());
    }

    @Override
    public void beforeTestSchedule(TestResult result) {
        LOG.debug("Test scheduled: {}", result.getName());
    }

    @Override
    public void afterTestSchedule(TestResult result) {
        // No implementation needed
    }

    @Override
    public void beforeTestUpdate(TestResult result) {
        // No implementation needed
    }

    @Override
    public void afterTestUpdate(TestResult result) {
        // No implementation needed
    }

    @Override
    public void afterTestWrite(TestResult result) {
        // No implementation needed
    }
}