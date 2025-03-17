package stepdefinitions;

import com.microsoft.playwright.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;
import pageobjects.BOP.BusinessownersPage;
import pageobjects.HomePage;
import utilities.BrowserUtil;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base class for all test scripts with Playwright and Allure reporting functionality.
 * This class handles browser initialization, test setup, and reporting.
 */
public class TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    private static Playwright playwright;
    private static Browser browser;
    private static final ThreadLocal<BrowserContext> context = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> TEST_PARAMETERS = new ThreadLocal<>();
    private static final ThreadLocal<Class<?>> CURRENT_TEST_CLASS = new ThreadLocal<>();
    private static final ThreadLocal<Method> CURRENT_TEST_METHOD = new ThreadLocal<>();
    private static final ThreadLocal<byte[]> VIDEO_RECORDING = new ThreadLocal<>();
    private static Properties config;

    //Page Objects
    protected HomePage homePage;
    protected BusinessownersPage businessownersPage;

    static {
        // Load configuration from config.properties
        config = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/properties/config.properties")) {
            config.load(fis);
        } catch (IOException e) {
            LOG.error("Failed to load config.properties: " + e.getMessage());
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    /**
     * Initialize test suite and setup Allure reporting.
     */
    @BeforeSuite
    public void initTestSuite() {
        // Initialize Allure result directory
        try {
            Path allureResultsDir = Paths.get("allure-results");
            if (!Files.exists(allureResultsDir)) {
                Files.createDirectories(allureResultsDir);
            }

            // Add history directory for trend charts
            Path historyDir = allureResultsDir.resolve("history");
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }
        } catch (IOException e) {
            LOG.error("Failed to initialize Allure directories", e);
        }

        LOG.info("Test suite started");
    }

    /**
     * Launches the browser and
     * Initializes page objects before a class.
     */
    @BeforeClass
    void launchBrowser() {
        String browserType = config.getProperty("browser", "CHROMIUM_HEADLESS");
        playwright = Playwright.create();
        browser = BrowserUtil.createBrowser(browserType, playwright);
        LOG.info("Browser launched: " + browserType);

        // Create a new browser context and page for the entire test class
        BrowserContext browserContext = browser.newContext();
        context.set(browserContext);
        PAGE.set(browserContext.newPage());
        LOG.info("New browser context and page created for the test class.");

        // Initialize page objects
        homePage = new HomePage(getPage());
        businessownersPage = new BusinessownersPage(getPage());
    }

    /**
     * Also initializes test parameters and Allure reporting.
     */
    @BeforeMethod
    public void setupTest(Method method, Object[] params) {
        // Store test method for later use
        CURRENT_TEST_METHOD.set(method);
        CURRENT_TEST_CLASS.set(method.getDeclaringClass());

        // Initialize test parameters
        TEST_PARAMETERS.set(new HashMap<>());

        // Log test start
        LOG.info("Starting test: {}", method.getName());

        // Add test description from annotations if available
        if (method.isAnnotationPresent(io.qameta.allure.Description.class)) {
            String description = method.getAnnotation(io.qameta.allure.Description.class).value();
            Allure.description(description);
        }

        // Add test parameters
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    String paramName = "param" + (i + 1);
                    String paramValue = params[i].toString();
                    Allure.parameter(paramName, paramValue);
                    TEST_PARAMETERS.get().put(paramName, paramValue);
                }
            }
        }
    }


    /**
     * Closes the browser context after each test method.
     * Also handles Allure reporting and cleanup.
     */
    @AfterMethod
    public void teardownTest(ITestResult result) {
        // Log test completions
        LOG.info("Test completed: {} - {}",
                result.getName(),
                result.getStatus() == ITestResult.SUCCESS ? "PASSED" : "FAILED");

        // Start video recording if configured
        if (getBooleanProperty("allure.video.record", false)) {
            stopVideoRecording(result);
        }

        // Collect performance metrics if available
        collectPerformanceMetrics();

        // Clear thread-local variables
        CURRENT_TEST_METHOD.remove();
        CURRENT_TEST_CLASS.remove();
        TEST_PARAMETERS.remove();
    }

    /**
     * Closes the browser after all tests in the class.
     */
    @AfterClass
    void closeBrowser() {
        // Close the browser context and page after all tests in the class
        if (context.get() != null) {
            context.get().close();
            LOG.info("Browser context closed.");
        }

        if (browser != null) {
            browser.close();
            LOG.info("Browser closed.");
        }

        if (playwright != null) {
            playwright.close();
            LOG.info("Playwright closed.");
        }
    }

    /**
     * Final cleanup after all tests.
     * Generates and opens the Allure report if configured.
     */
    @AfterSuite
    public void teardownTestSuite() {
        // Generate and open Allure report
        if (getBooleanProperty("allure.auto.generate", true)) {
            utilities.AllureReportLauncher.generateAndOpenReport();
        }

        // Log test suite completion
        LOG.info("Test suite completed");
    }

    /**
     * Capture screenshot and add to Allure report.
     *
     * @param name The name of the screenshot.
     */
    @Step("Capture screenshot: {name}")
    public static void captureScreenshot(String name) {
        if (PAGE.get() != null) {
            try {
                byte[] screenshot = PAGE.get().screenshot();
                Allure.addAttachment(
                        name,
                        "image/png",
                        new ByteArrayInputStream(screenshot),
                        ".png");
            } catch (Exception e) {
                LOG.error("Failed to capture screenshot: {}", name, e);
            }
        }
    }

    /**
     * Add a step to the Allure report.
     *
     * @param stepDescription The description of the step.
     */
    @Step("{stepDescription}")
    public static void addStep(String stepDescription) {
        LOG.info("Step: {}", stepDescription);
    }

    /**
     * Add a step with status to the Allure report.
     *
     * @param stepDescription The description of the step.
     * @param isSuccessful    Whether the step was successful.
     */
    public static void addStep(String stepDescription, boolean isSuccessful) {
        if (isSuccessful) {
            addSuccessStep(stepDescription);
        } else {
            addFailureStep(stepDescription);
        }
    }

    /**
     * Add a successful step to the Allure report.
     *
     * @param stepDescription The description of the step.
     */
    @Step("{stepDescription}")
    public static void addSuccessStep(String stepDescription) {
        LOG.info("Step succeeded: {}", stepDescription);
    }

    /**
     * Add a failure step to the Allure report.
     *
     * @param stepDescription The description of the step.
     */
    @Step("{stepDescription}")
    public static void addFailureStep(String stepDescription) {
        LOG.error("Step failed: {}", stepDescription);
        if (PAGE.get() != null) {
            captureScreenshot("Failure_" + stepDescription.replaceAll("\\s+", "_"));
        }
    }

    /**
     * Get browser console logs.
     *
     * @return A list of console logs.
     */
    public static List<String> getBrowserConsoleLogs() {
        // Implementation depends on your test framework
        // This is a placeholder - implement as needed
        return null;
    }

    /**
     * Get performance metrics.
     *
     * @return A map of performance metrics.
     */
    public static Map<String, Object> getPerformanceMetrics() {
        // Implementation depends on your test framework
        // This is a placeholder - implement as needed
        return null;
    }

    /**
     * Start video recording.
     */
    protected void startVideoRecording() {
        // Implementation depends on your test framework
        // This is a placeholder - implement as needed
    }

    /**
     * Stop video recording.
     *
     * @param result The test result.
     */
    protected void stopVideoRecording(ITestResult result) {
        // Implementation depends on your test framework
        // This is a placeholder - implement as needed
    }

    /**
     * Collect performance metrics.
     */
    protected void collectPerformanceMetrics() {
        // Implementation depends on your test framework
        // This is a placeholder - implement as needed
    }

    /**
     * Get a boolean property value.
     *
     * @param name         The name of the property.
     * @param defaultValue The default value if the property is not set.
     * @return The boolean value of the property.
     */
    private boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = System.getProperty(name);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Returns the current Page instance for the thread.
     *
     * @return The Page instance.
     */
    public static Page getPage() {
        return PAGE.get();
    }

    /**
     * Sets the current Page instance for the thread.
     *
     * @param page The Page instance.
     */
    public static void setPage(Page page) {
        PAGE.set(page);
    }

    /**
     * Returns the test parameters for the current test.
     *
     * @return A map of test parameters.
     */
    public static Map<String, String> getTestParameters() {
        return TEST_PARAMETERS.get();
    }

    /**
     * Returns the current test class.
     *
     * @return The current test class.
     */
    public static Class<?> getCurrentTestClass() {
        return CURRENT_TEST_CLASS.get();
    }

    /**
     * Returns the current test method.
     *
     * @return The current test method.
     */
    public static Method getCurrentTestMethod() {
        return CURRENT_TEST_METHOD.get();
    }

    /**
     * Returns the video recording for the current test.
     *
     * @return The video recording as a byte array.
     */
    public static byte[] getVideoRecording() {
        return VIDEO_RECORDING.get();
    }

    /**
     * Sets the video recording for the current test.
     *
     * @param video The video recording as a byte array.
     */
    public static void setVideoRecording(byte[] video) {
        VIDEO_RECORDING.set(video);
    }

    /**
     * Returns the browser name.
     *
     * @return The browser name.
     */
    public static String getBrowserName() {
        return System.getProperty("browser", "chrome");
    }

    /**
     * Returns the browser version.
     *
     * @return The browser version.
     */
    public static String getBrowserVersion() {
        return System.getProperty("browser.version", "latest");
    }

    /**
     * Returns the environment name.
     *
     * @return The environment name.
     */
    public static String getEnvironmentName() {
        return System.getProperty("env", "test");
    }
}