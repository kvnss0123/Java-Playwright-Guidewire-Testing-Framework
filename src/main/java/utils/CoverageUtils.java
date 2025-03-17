package utils;

import com.microsoft.playwright.Page;

import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for handling coverage-related interactions.
 */
public class CoverageUtils extends WebInteractionHelper {
    private static final Logger logger = Logger.getLogger(CoverageUtils.class.getName());

    /**
     * Constructor to initialize CoverageUtils with a Playwright Page instance.
     *
     * @param page The Playwright Page instance.
     */
    public CoverageUtils(Page page) {
        super(page);
    }

    // Generic coverage interaction methods

    /**
     * Add coverage if it's electable and not already present.
     *
     * @param coverageName Display name of the coverage.
     */
    public void addElectableCoverage(String coverageName) {
        String checkboxLocator = String.format("//div[contains(text(), '%s')]/ancestor::tr//input[@type='checkbox']", coverageName);
        if (!isCoveragePresent(coverageName)) {
            click(checkboxLocator);
            waitForCoverageAdded(coverageName);
        }
    }

    /**
     * Remove coverage if it's optional.
     *
     * @param coverageName Display name of the coverage.
     */
    public void removeElectableCoverage(String coverageName) {
        if (isCoverageRemovable(coverageName)) {
            String checkboxLocator = String.format("//div[contains(text(), '%s')]/ancestor::tr//input[@type='checkbox']", coverageName);
            uncheck(checkboxLocator);
        } else {
            throw new IllegalStateException(coverageName + " is mandatory and cannot be removed");
        }
    }

    /**
     * Set coverage term value based on field type.
     *
     * @param coverageName Parent coverage name.
     * @param termName     Term display name.
     * @param value        Value to set.
     */
    public void setCoverageTerm(String coverageName, String termName, String value) {
        String termLocator = getTermLocator(coverageName, termName);
        String fieldType = getFieldType(termLocator);

        switch (fieldType.toLowerCase()) {
            case "textfield":
                type(termLocator, value);
                break;
            case "dropdown":
                selectByText(termLocator, value);
                break;
            case "checkbox":
                if (value.equalsIgnoreCase("true")) check(termLocator);
                else uncheck(termLocator);
                break;
            case "currency":
                typeCurrencyField(termLocator, value);
                break;
            default:
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    // Dynamic coverage state checks

    /**
     * Check if a coverage is present on the page.
     *
     * @param coverageName Display name of the coverage.
     * @return true if the coverage is present, false otherwise.
     */
    public boolean isCoveragePresent(String coverageName) {
        return isVisible(getCoverageRowLocator(coverageName));
    }

    /**
     * Check if a coverage is editable.
     *
     * @param coverageName Display name of the coverage.
     * @return true if the coverage is editable, false otherwise.
     */
    public boolean isCoverageEditable(String coverageName) {
        return !hasAttribute(getCoverageRowLocator(coverageName), "readonly");
    }

    /**
     * Check if a coverage is removable.
     *
     * @param coverageName Display name of the coverage.
     * @return true if the coverage is removable, false otherwise.
     */
    public boolean isCoverageRemovable(String coverageName) {
        return !isMandatoryCoverage(coverageName);
    }

    // Term management utilities

    /**
     * Get available options for a coverage term.
     *
     * @param coverageName Parent coverage name.
     * @param termName     Term display name.
     * @return List of available options.
     */
    public List<String> getAvailableTermOptions(String coverageName, String termName) {
        String optionsLocator = getTermLocator(coverageName, termName) + "//option";
        return getAllTextContents(optionsLocator);
    }

    /**
     * Get the current value of a coverage term.
     *
     * @param coverageName Parent coverage name.
     * @param termName     Term display name.
     * @return The current value of the term.
     */
    public String getCurrentTermValue(String coverageName, String termName) {
        return getInputValue(getTermLocator(coverageName, termName));
    }

    // Helper methods

    /**
     * Generates the locator for a coverage term.
     *
     * @param coverageName Parent coverage name
     * @param termName     Term display name
     * @return Locator for the term
     */
    private String getTermLocator(String coverageName, String termName) {
        return String.format("%s//div[contains(text(), '%s')]/following-sibling::div//input | %s//div[contains(text(), '%s')]/following-sibling::div//select",
                getCoverageRowLocator(coverageName), termName,
                getCoverageRowLocator(coverageName), termName);
    }

    /**
     * Generates the locator for a coverage row.
     *
     * @param coverageName Display name of the coverage
     * @return Locator for the coverage row
     */
    private String getCoverageRowLocator(String coverageName) {
        return String.format("//div[@role='row' and .//div[contains(text(), '%s')]]", coverageName);
    }

    /**
     * Checks if a coverage is mandatory.
     *
     * @param coverageName Display name of the coverage
     * @return true if the coverage is mandatory, false otherwise
     */
    private boolean isMandatoryCoverage(String coverageName) {
        String locator = String.format("%s//*[local-name()='svg' and contains(@class,'required-icon')]", getCoverageRowLocator(coverageName));
        return isVisible(locator);
    }

    /**
     * Determines the coverage field type.
     *
     * @param locator of the coverage field
     * @return Field Type of the coverage
     */
    private String getFieldType(String locator) {
        if (locator.contains("select")) return "dropdown";
        if (locator.contains("checkbox")) return "checkbox";
        if (hasClass(locator, "currency-input")) return "currency";
        return "textfield";
    }

    /**
     * Waits for a coverage to be added.
     *
     * @param coverageName Display name of the coverage
     */
    private void waitForCoverageAdded(String coverageName) {
        String locator = String.format("//div[contains(text(), '%s')]/ancestor::tr[contains(@class, 'selected')]", coverageName);
        waitForElement(locator);
    }
}