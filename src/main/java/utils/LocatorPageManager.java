package utils;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages locators initialization and element retrieval.
 */
public class LocatorPageManager {
    private static final Map<String, Locator> locators = new ConcurrentHashMap<>();
    private static Page page;

    /**
     * Initializes all page objects from YAML files in the resources/locators directory.
     */
    public static void initializeAllPages(Page page) {
        try {
            LocatorPageManager.page = page;
            // Get all YAML files from the resources/locators directory
            File locatorsDir = Paths.get(Objects.requireNonNull(LocatorPageManager.class.getClassLoader().getResource("locatorpages")).toURI()).toFile();

            if (locatorsDir.exists() && locatorsDir.isDirectory()) {
                File[] yamlFiles = locatorsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yaml"));

                if (yamlFiles != null) {
                    for (File yamlFile : yamlFiles) {
                        String pageName = yamlFile.getName().replace(".yaml", "");
                        initializePage(pageName);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize page objects", e);
        }
    }

    /**
     * Initializes a specific page by loading its locator data from a YAML file.
     *
     * @param pageName Name of the page to initialize.
     */
    public static void initializePage(String pageName) {
        Map<String, String> locatorStrings = YamlParser.parseYamlFile(pageName);

        for (Map.Entry<String, String> entry : locatorStrings.entrySet()) {
            String elementName = entry.getKey();
            String locatorValue = entry.getValue();
            locators.put(elementName, page.locator(locatorValue));
        }
    }

    /**
     * Retrieves the pre-configured Playwright Locator object for the specified element.
     *
     * @param element The name of the element whose locator is to be fetched.
     * @return Playwright Locator object.
     * @throws IllegalArgumentException If the element is not found.
     */
    public static Locator getLocator(String element) {
        Locator locator = locators.get(element);
        if (locator == null) {
            throw new IllegalArgumentException("Locator not found for element: " + element);
        }
        return locator;
    }
}