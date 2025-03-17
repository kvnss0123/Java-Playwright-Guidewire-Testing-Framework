package pageobjects.BOP;

import com.microsoft.playwright.Page;
import pageobjects.HomePage;

/**
 * Page object for the Businessowners page.
 */
public class BusinessownersPage extends HomePage {

    /**
     * Creates a new BusinessownersPage instance.
     *
     * @param page Playwright Page instance
     */
    public BusinessownersPage(Page page) {
        super(page);
    }

}