package utilities;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import java.util.logging.Logger;

public class BrowserUtil {

    private static final Logger logger = Logger.getLogger(BrowserUtil.class.getName());

    // Enum to define valid browser types
    public enum BrowserName {
        CHROMIUM,
        CHROMIUM_HEADLESS,
        FIREFOX,
        FIREFOX_HEADLESS,
        WEBKIT,
        WEBKIT_HEADLESS,
        CHROME,
        CHROME_HEADLESS,
        MSEDGE,
        MSEDGE_HEADLESS
    }

    /**
     * Creates and launches a browser instance based on the provided browser type.
     *
     * @param browserType The type of browser to launch (e.g., "CHROMIUM", "FIREFOX_HEADLESS").
     * @param playwright  The Playwright instance.
     * @return A Browser instance.
     * @throws IllegalArgumentException If the browser type is invalid.
     */
    public static Browser createBrowser(String browserType, Playwright playwright) {
        try {
            BrowserName browserName = BrowserName.valueOf(browserType.toUpperCase());
            return launchBrowser(browserName, playwright);
        } catch (IllegalArgumentException e) {
            logger.severe("Invalid browser type: " + browserType);
            throw new IllegalArgumentException("Invalid browser type: " + browserType, e);
        }
    }

    /**
     * Launches the browser based on the provided BrowserName enum.
     *
     * @param browserName The BrowserName enum value.
     * @param playwright  The Playwright instance.
     * @return A Browser instance.
     */
    private static Browser launchBrowser(BrowserName browserName, Playwright playwright) {
        BrowserType browserType;
        boolean headless = false;
        String channel = null;

        switch (browserName) {
            case CHROMIUM:
                browserType = playwright.chromium();
                break;
            case CHROMIUM_HEADLESS:
                browserType = playwright.chromium();
                headless = true;
                break;
            case FIREFOX:
                browserType = playwright.firefox();
                break;
            case FIREFOX_HEADLESS:
                browserType = playwright.firefox();
                headless = true;
                break;
            case WEBKIT:
                browserType = playwright.webkit();
                break;
            case WEBKIT_HEADLESS:
                browserType = playwright.webkit();
                headless = true;
                break;
            case CHROME:
                browserType = playwright.chromium();
                channel = "chrome";
                break;
            case CHROME_HEADLESS:
                browserType = playwright.chromium();
                channel = "chrome";
                headless = true;
                break;
            case MSEDGE:
                browserType = playwright.chromium();
                channel = "msedge";
                break;
            case MSEDGE_HEADLESS:
                browserType = playwright.chromium();
                channel = "msedge";
                headless = true;
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser type: " + browserName);
        }

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(headless);
        if (channel != null) {
            options.setChannel(channel);
        }

        logger.info("Launching browser: " + browserName + " (headless: " + headless + ")");
        return browserType.launch(options);
    }
}