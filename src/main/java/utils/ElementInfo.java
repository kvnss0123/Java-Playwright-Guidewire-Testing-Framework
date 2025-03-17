package utils;

import com.microsoft.playwright.Locator;

/**
 * Parses and manages element information in the format "pageName.elementName".
 */
public class ElementInfo {
    private final String pageName;
    private final String elementName;

    /**
     * Constructor to parse the element string into page name and element name.
     *
     * @param element The element string in the format "pageName.elementName".
     * @throws IllegalArgumentException If the element string is not in the expected format.
     */
    public ElementInfo(String element) {
        if (element == null || element.isEmpty()) {
            throw new IllegalArgumentException("Element string cannot be null or empty.");
        }

        String[] parts = element.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid element format. Expected 'pageName.elementName', but got: " + element);
        }

        this.pageName = parts[0];
        this.elementName = parts[1];
    }

    public String getPageName() {
        return pageName;
    }

    public String getElementName() {
        return elementName;
    }

    /**
     * Retrieves the locator for the element using the LocatorPageManager.
     *
     * @return The locator as a Playwright Locator object.
     * @throws IllegalStateException If the locator cannot be retrieved.
     */
    public Locator getLocator() {
        Locator locator = LocatorPageManager.getLocator(elementName);
        if (locator == null) {
            throw new IllegalStateException("Locator not found for element: " + elementName + " in page: " + pageName);
        }
        return locator;
    }
}