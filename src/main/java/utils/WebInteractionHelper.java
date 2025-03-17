package utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

/**
 * Helper class for interacting with page elements.
 */
public class WebInteractionHelper extends LocatorPageManager {
    private static final Logger logger = Logger.getLogger(WebInteractionHelper.class.getName());
    private static final int DEFAULT_TIMEOUT = 30000;
    protected static Page page;

    /**
     * Creates a new WebInteractionHelper instance and initializes all pages and locators.
     *
     * @param page Playwright Page instance
     */
    public WebInteractionHelper(Page page) {
        this.page = page;
        LocatorPageManager.initializeAllPages(page); // Initialize all pages and locators
    }


    /**
     * Gets the Playwright Locator object for this element after waiting for it to be in a specific state.
     *
     * @param element Element name to retrieve locator for.
     * @param state   State to wait for (VISIBLE, HIDDEN, ATTACHED, DETACHED).
     * @param timeout Wait timeout in milliseconds.
     * @return Playwright Locator object.
     */
    protected Locator getLocator(String element, WaitForSelectorState state, int timeout) {
        ElementInfo elementInfo = new ElementInfo(element);
        logger.fine("Getting locator for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());

        try {
            Locator locator = elementInfo.getLocator();
            locator.waitFor(new Locator.WaitForOptions().setState(state).setTimeout(timeout));
            locator.scrollIntoViewIfNeeded();
            return locator;
        } catch (Exception e) {
            logger.severe("Timeout waiting for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " to be in state: " + state);
            throw new RuntimeException("Timeout waiting for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " to be in state: " + state, e);
        }
    }

    /**
     * Gets the Playwright Locator object for this element with default wait and scroll into view.
     *
     * @param element Element name to retrieve locator for.
     * @return Playwright Locator object.
     */
    protected Locator getElementLocator(String element) {
        return getLocator(element, WaitForSelectorState.VISIBLE, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for an element to be visible on the page.
     *
     * @param element Element to wait for
     * @param timeout Wait timeout in milliseconds
     */
    public void waitForElement(String element, int timeout) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Waiting for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getLocator(element, WaitForSelectorState.VISIBLE, timeout);
        } catch (Exception e) {
            logger.severe("Failed to wait for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to wait for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Waits for an element to be visible on the page with default timeout.
     *
     * @param element Element to wait for
     */
    public void waitForElement(String element) {
        waitForElement(element, DEFAULT_TIMEOUT);
    }

    /**
     * Clicks on the specified element with default wait.
     *
     * @param element Element to click on
     */
    public void click(String element) {
        click(element, DEFAULT_TIMEOUT);
    }

    /**
     * Clicks on the specified element with custom timeout.
     *
     * @param element Element to click on
     * @param timeout Wait timeout in milliseconds
     */
    public void click(String element, int timeout) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Clicking on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getLocator(element, WaitForSelectorState.VISIBLE, timeout).click();
        } catch (Exception e) {
            logger.severe("Failed to click on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to click on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Clears the text in an input field.
     *
     * @param element Element to clear text from
     */
    public void clear(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Clearing text from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).clear();
        } catch (Exception e) {
            logger.severe("Failed to clear text from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to clear text from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Focuses on the specified element.
     *
     * @param element Element to focus on
     */
    public void focus(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Focusing on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).focus();
        } catch (Exception e) {
            logger.severe("Failed to focus on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to focus on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Hovers over the specified element.
     *
     * @param element Element to hover over
     */
    public void hover(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Hovering over element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).hover();
        } catch (Exception e) {
            logger.severe("Failed to hover over element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to hover over element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Verifies if an element is enabled.
     *
     * @param element Element to check
     * @return true if the element is enabled, false otherwise
     */
    public boolean isEnabled(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Checking if element is enabled: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).isEnabled();
        } catch (Exception e) {
            logger.severe("Failed to check if element is enabled: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies if an element is checked (for checkboxes and radio buttons).
     *
     * @param element Element to check
     * @return true if the element is checked, false otherwise
     */
    public boolean isChecked(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Checking if element is checked: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).isChecked();
        } catch (Exception e) {
            logger.severe("Failed to check if element is checked: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Scrolls to the specified element.
     *
     * @param element Element to scroll to
     */
    public void scrollToElement(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Scrolling to element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).scrollIntoViewIfNeeded();
        } catch (Exception e) {
            logger.severe("Failed to scroll to element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to scroll to element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Checks a checkbox or radio button.
     *
     * @param element Element to check
     */
    public void check(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Checking element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).check();
        } catch (Exception e) {
            logger.severe("Failed to check element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to check element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Unchecks a checkbox or radio button.
     *
     * @param element Element to uncheck
     */
    public void uncheck(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Unchecking element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).uncheck();
        } catch (Exception e) {
            logger.severe("Failed to uncheck element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to uncheck element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Toggles a checkbox or radio button.
     *
     * @param element Element to toggle
     */
    public void toggle(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Toggling element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            Locator locator = getElementLocator(element);
            if (locator.isChecked()) {
                locator.uncheck();
            } else {
                locator.check();
            }
        } catch (Exception e) {
            logger.severe("Failed to toggle element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to toggle element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Verifies if an element is visible on the page.
     *
     * @param element Element to verify visibility for
     * @return true if visible, false otherwise
     */
    public boolean isVisible(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Checking visibility of element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).isVisible();
        } catch (Exception e) {
            logger.severe("Failed to check visibility of element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Selects an option from a dropdown by visible text.
     *
     * @param element Element to select option from
     * @param option  Option to select (text)
     */
    public void selectByText(String element, String option) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Selecting option: " + option + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).selectOption(new SelectOption().setLabel(option));
        } catch (Exception e) {
            logger.severe("Failed to select option: " + option + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to select option: " + option + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Selects an option from a dropdown by value.
     *
     * @param element Element to select option from
     * @param value   Value to select
     */
    public void selectByValue(String element, String value) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Selecting value: " + value + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).selectOption(new SelectOption().setValue(value));
        } catch (Exception e) {
            logger.severe("Failed to select value: " + value + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to select value: " + value + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Selects an option from a dropdown by index.
     *
     * @param element Element to select option from
     * @param index   Index to select
     */
    public void selectByIndex(String element, int index) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Selecting index: " + index + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).selectOption(new SelectOption().setIndex(index));
        } catch (Exception e) {
            logger.severe("Failed to select index: " + index + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to select index: " + index + " from dropdown: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Double clicks on the specified element.
     *
     * @param element Element to double click on
     */
    public void doubleClick(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Double clicking on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).dblclick();
        } catch (Exception e) {
            logger.severe("Failed to double click on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to double click on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Right clicks on the specified element.
     *
     * @param element Element to right click on
     */
    public void rightClick(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Right clicking on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        } catch (Exception e) {
            logger.severe("Failed to right click on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to right click on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Types text into the specified element.
     *
     * @param element Element to type text into
     * @param text    Text to type
     */
    public void type(String element, String text) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Typing text: " + text + " into element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).type(text);
        } catch (Exception e) {
            logger.severe("Failed to type text into element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to type text into element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Gets the text content of the specified element.
     *
     * @param element Element to get text from
     * @return Text content of the element
     */
    public String getText(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Getting text from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).textContent();
        } catch (Exception e) {
            logger.severe("Failed to get text from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to get text from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Gets the value of the specified attribute of the element.
     *
     * @param element   Element to get attribute from
     * @param attribute Attribute name
     * @return Attribute value
     */
    public String getAttribute(String element, String attribute) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Getting attribute: " + attribute + " from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).getAttribute(attribute);
        } catch (Exception e) {
            logger.severe("Failed to get attribute: " + attribute + " from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to get attribute: " + attribute + " from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Gets the value of the specified CSS property of the element.
     *
     * @param element     Element to get CSS value from
     * @param cssProperty CSS property name
     * @return CSS property value
     */
    public String getCssValue(String element, String cssProperty) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Getting CSS property: " + cssProperty + " from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).evaluate("element => window.getComputedStyle(element).getPropertyValue('" + cssProperty + "')").toString();
        } catch (Exception e) {
            logger.severe("Failed to get CSS property: " + cssProperty + " from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to get CSS property: " + cssProperty + " from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Drags an element and drops it onto another element.
     *
     * @param sourceElement Element to drag
     * @param targetElement Element to drop onto
     */
    public void dragAndDrop(String sourceElement, String targetElement) {
        ElementInfo sourceElementInfo = new ElementInfo(sourceElement);
        ElementInfo targetElementInfo = new ElementInfo(targetElement);
        try {
            logger.fine("Dragging element: " + sourceElementInfo.getElementName() + " and dropping onto element: " + targetElementInfo.getElementName());
            getElementLocator(sourceElement).dragTo(getElementLocator(targetElement));
        } catch (Exception e) {
            logger.severe("Failed to drag and drop element: " + sourceElementInfo.getElementName() + " onto element: " + targetElementInfo.getElementName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to drag and drop element: " + sourceElementInfo.getElementName() + " onto element: " + targetElementInfo.getElementName(), e);
        }
    }

    /**
     * Uploads a file to the specified file input element.
     *
     * @param element  Element to upload file to
     * @param filePath Path to the file to upload
     */
    public void uploadFile(String element, String filePath) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Uploading file: " + filePath + " to element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).setInputFiles(Paths.get(filePath));
        } catch (Exception e) {
            logger.severe("Failed to upload file: " + filePath + " to element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to upload file: " + filePath + " to element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Clears the file input element.
     *
     * @param element Element to clear file input from
     */
    public void clearFileInput(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Clearing file input for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).setInputFiles(new Path[0]);
        } catch (Exception e) {
            logger.severe("Failed to clear file input for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to clear file input for element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Gets the count of elements matching the locator.
     *
     * @param element Element to count
     * @return Number of elements matching the locator
     */
    public int getElementCount(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Getting count of elements: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).count();
        } catch (Exception e) {
            logger.severe("Failed to get count of elements: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to get count of elements: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Simulates the press of a key or combination of keys.
     *
     * @param element Element to focus on before pressing the key (can be null to press on the page)
     * @param keys    Key or combination of keys to press (e.g., "Control+A", "Shift+Tab", "Enter")
     */
    public void pressKey(String element, String keys) {
        try {
            if (element != null) {
                ElementInfo elementInfo = new ElementInfo(element);
                logger.fine("Pressing key(s): " + keys + " on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
                getElementLocator(element).focus();
            } else {
                logger.fine("Pressing key(s): " + keys + " on the page");
            }
            page.keyboard().press(keys);
        } catch (Exception e) {
            logger.severe("Failed to press key(s): " + keys + " - " + e.getMessage());
            throw new RuntimeException("Failed to press key(s): " + keys, e);
        }
    }

    /**
     * Switches to a frame using its locator.
     *
     * @param frameLocator Locator of the frame to switch to
     */
    public FrameLocator switchToFrame(String frameLocator) {
        try {
            logger.fine("Switching to frame: " + frameLocator);
            FrameLocator frame = page.frameLocator(frameLocator);
            if (frame == null) {
                throw new IllegalArgumentException("Frame not found: " + frameLocator);
            }
            return frame;
        } catch (Exception e) {
            logger.severe("Failed to switch to frame: " + frameLocator + " - " + e.getMessage());
            throw new RuntimeException("Failed to switch to frame: " + frameLocator, e);
        }
    }

    /**
     * Switches to a frame by its name or URL and returns it.
     *
     * @param frameNameOrUrl Name or URL of the frame to switch to
     */
    public Frame switchToFrameByNameOrUrl(String frameNameOrUrl) {
        try {
            logger.fine("Switching to frame by name or URL: " + frameNameOrUrl);
            Frame frame = page.frame(frameNameOrUrl);
            if (frame == null) {
                throw new IllegalArgumentException("Frame not found: " + frameNameOrUrl);
            }
            return frame;
        } catch (Exception e) {
            logger.severe("Failed to switch to frame by name or URL: " + frameNameOrUrl + " - " + e.getMessage());
            throw new RuntimeException("Failed to switch to frame by name or URL: " + frameNameOrUrl, e);
        }
    }

    /**
     * Switches back to the main page from a frame.
     */
    public void switchToMainPage() {
        try {
            logger.fine("Switching back to the main page");
            page.mainFrame();
        } catch (Exception e) {
            logger.severe("Failed to switch back to the main page - " + e.getMessage());
            throw new RuntimeException("Failed to switch back to the main page", e);
        }
    }

    /**
     * Returns a list of all open tabs (pages).
     *
     * @return List of Page objects representing open tabs
     */
    public List<Page> getAllTabs() {
        try {
            logger.fine("Getting all open tabs");
            return page.context().pages();
        } catch (Exception e) {
            logger.severe("Failed to get all open tabs - " + e.getMessage());
            throw new RuntimeException("Failed to get all open tabs", e);
        }
    }

    /**
     * Switches to a specific tab by its index.
     *
     * @param index Index of the tab to switch to (starting from 0)
     */
    public void switchToTab(int index) {
        try {
            logger.fine("Switching to tab at index: " + index);
            List<Page> tabs = page.context().pages();
            if (index >= tabs.size()) {
                throw new IllegalArgumentException("Tab index out of bounds: " + index);
            }
            page = tabs.get(index);
        } catch (Exception e) {
            logger.severe("Failed to switch to tab at index: " + index + " - " + e.getMessage());
            throw new RuntimeException("Failed to switch to tab at index: " + index, e);
        }
    }

    /**
     * Switches to a tab by its title.
     *
     * @param title Title of the tab to switch to
     */
    public void switchToTabByTitle(String title) {
        try {
            logger.fine("Switching to tab with title: " + title);
            List<Page> tabs = page.context().pages();
            for (Page tab : tabs) {
                if (title.equals(tab.title())) {
                    page = tab;
                    return;
                }
            }
            throw new IllegalArgumentException("No tab found with title: " + title);
        } catch (Exception e) {
            logger.severe("Failed to switch to tab with title: " + title + " - " + e.getMessage());
            throw new RuntimeException("Failed to switch to tab with title: " + title, e);
        }
    }

    /**
     * Returns a list of all open windows (pages).
     *
     * @return List of Page objects representing open windows
     */
    public List<Page> getAllWindows() {
        try {
            logger.fine("Getting all open windows");
            return page.context().pages();
        } catch (Exception e) {
            logger.severe("Failed to get all open windows - " + e.getMessage());
            throw new RuntimeException("Failed to get all open windows", e);
        }
    }

    /**
     * Switches to a specific window by its index.
     *
     * @param index Index of the window to switch to (starting from 0)
     */
    public void switchToWindow(int index) {
        try {
            logger.fine("Switching to window at index: " + index);
            List<Page> windows = page.context().pages();
            if (index >= windows.size()) {
                throw new IllegalArgumentException("Window index out of bounds: " + index);
            }
            page = windows.get(index);
        } catch (Exception e) {
            logger.severe("Failed to switch to window at index: " + index + " - " + e.getMessage());
            throw new RuntimeException("Failed to switch to window at index: " + index, e);
        }
    }

    /**
     * Switches to a window by its URL.
     *
     * @param url URL of the window to switch to
     */
    public void switchToWindowByUrl(String url) {
        try {
            logger.fine("Switching to window with URL: " + url);
            List<Page> windows = page.context().pages();
            for (Page window : windows) {
                if (url.equals(window.url())) {
                    page = window;
                    return;
                }
            }
            throw new IllegalArgumentException("No window found with URL: " + url);
        } catch (Exception e) {
            logger.severe("Failed to switch to window with URL: " + url + " - " + e.getMessage());
            throw new RuntimeException("Failed to switch to window with URL: " + url, e);
        }
    }

    /**
     * Checks if an element has a specific attribute.
     *
     * @param element   Element to check
     * @param attribute Attribute name
     * @return true if the attribute exists, false otherwise
     */
    public boolean hasAttribute(String element, String attribute) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Checking if element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " has attribute: " + attribute);
            return getElementLocator(element).getAttribute(attribute) != null;
        } catch (Exception e) {
            logger.severe("Failed to check attribute: " + attribute + " on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an element has a specific class.
     *
     * @param element Element to check
     * @param className Class name to check
     * @return true if the class exists, false otherwise
     */
    public boolean hasClass(String element, String className) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Checking if element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " has class: " + className);
            return getElementLocator(element).getAttribute("class").contains(className);
        } catch (Exception e) {
            logger.severe("Failed to check class: " + className + " on element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Types a value into a currency field.
     *
     * @param element Element to type into
     * @param value   Value to type
     */
    public void typeCurrencyField(String element, String value) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Typing currency value: " + value + " into element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            getElementLocator(element).type(value);
        } catch (Exception e) {
            logger.severe("Failed to type currency value into element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to type currency value into element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }

    /**
     * Retrieves all text contents of elements matching a locator.
     *
     * @param locator Locator of the elements
     * @return List of text contents
     */
    public List<String> getAllTextContents(String locator) {
        try {
            logger.fine("Getting all text contents for locator: " + locator);
            return page.locator(locator).allTextContents();
        } catch (Exception e) {
            logger.severe("Failed to get all text contents for locator: " + locator + " - " + e.getMessage());
            throw new RuntimeException("Failed to get all text contents for locator: " + locator, e);
        }
    }

    /**
     * Retrieves the value of an input field.
     *
     * @param element Element to get value from
     * @return Value of the input field
     */
    public String getInputValue(String element) {
        ElementInfo elementInfo = new ElementInfo(element);
        try {
            logger.fine("Getting value from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName());
            return getElementLocator(element).inputValue();
        } catch (Exception e) {
            logger.severe("Failed to get value from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to get value from element: " + elementInfo.getElementName() + " in page: " + elementInfo.getPageName(), e);
        }
    }
}