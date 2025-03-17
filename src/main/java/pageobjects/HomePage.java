package pageobjects;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

import java.util.Locale;
import java.util.Random;

import configurations.EnvironmentConfig;
import utils.WebInteractionHelper;

public class HomePage extends WebInteractionHelper {

    Random random = new Random();
    Faker fakedData = new Faker(new Locale("en-US"));

    // Constants for repeated values
    private static final String COUNTRY_US = "United States";
    private static final String ADDRESS_TYPE_HOME = "Home";
    private static final String ADDRESS_TYPE_OFFICE = "Office";
    private static final String PHONE_TYPE_MOBILE = "Mobile";

    /**
     * Constructor to initialize HomePage with a Playwright Page instance.
     *
     * @param page The Playwright Page instance.
     */
    public HomePage(Page page) {
        super(page);
    }

    /**
     * Navigate to PolicyCenter.
     */
    @Step("Open PolicyCenter")
    public void navigate() {
        page.navigate(EnvironmentConfig.getUrl());
    }

    /**
     * Login to PolicyCenter
     */
    @Step("Login to GW PC")
    public boolean login() {

        type("common.usernameField", EnvironmentConfig.getUsername());
        type("common.passwordField", EnvironmentConfig.getPassword());
        click("common.loginButton");

        return isVisible("common.actionsButton");
    }

    @Step("Create an account for a person")
    public boolean createAccount_Person() {
        Name name = fakedData.name();
        Address fakedAddress = fakedData.address();
        Name fakedName = fakedData.name();
        String addressState = fakedAddress.stateAbbr();
        String zipCode = fakedAddress.zipCodeByState(addressState);

        // Navigate to account creation
        click("common.actionsButton");
        click("common.newAccountButton");

        // Fill in person details
        String firstName = name.firstName();
        String lastName = name.lastName();
//        page.locator("input[name*=InputSet-FirstName]").waitFor();
        type("account.firstNameField", firstName);
        type("account.lastNameField", lastName);

        click("common.searchButton");
        click("account.createNewAccountButton");
        click("account.personButton");

        // Fill in personal information
        type("account.dateOfBirthField", fakedData.date().birthday(18, 65).toString());
        selectByText("account.genderDropdown", getRandomGender());
        selectByText("account.maritalStatusDropdown", getRandomMaritalStatus());

        // Fill in contact information
        selectByText("account.primaryPhoneDropdown", PHONE_TYPE_MOBILE);
        type("account.primaryEmailField", fakedData.internet().emailAddress());
        type("account.mobilePhoneField", fakedData.phoneNumber().cellPhone());

        // Fill in US address information
        selectByText("account.countryDropdown", COUNTRY_US);
        type("account.zipCodeField", zipCode);
        type("account.addressLine1Field", fakedAddress.streetAddress());
        type("account.cityField", fakedAddress.city());
        selectByValue("account.stateDropdown", addressState);

        selectByText("account.addressTypeDropdown", ADDRESS_TYPE_HOME);

        // Fill in organization information
        type("account.organizationField", "Aon Org");
        click("account.searchOrganizationIcon");
        selectByIndex("account.producerCodeDropdown", 2);

        // Save the account
        click("common.updateButton");

        return true;
    }

    @Step("Create an account for a company")
    public boolean createAccount_Company() {
        // Navigate to account creation
        click("common.actionsButton");
        click("common.newAccountButton");

        // Fill in company details
        type("account.companyNameField", fakedData.company().name());

        click("common.searchButton");
        click("account.createNewAccountButton");
        click("account.companyButton");

        // Fill in contact information
        selectByText("account.primaryPhoneDropdown", PHONE_TYPE_MOBILE);
        type("account.primaryEmailField", fakedData.internet().emailAddress());
        type("account.mobilePhoneField", fakedData.phoneNumber().cellPhone());

        // Fill in US address information
        selectByText("account.countryDropdown", COUNTRY_US);
        type("account.zipCodeField", fakedData.address().zipCode());
        type("account.addressLine1Field", fakedData.address().streetAddress());
        type("account.cityField", fakedData.address().city());
        selectByText("account.stateDropdown", fakedData.address().state());

        selectByText("account.addressTypeDropdown", ADDRESS_TYPE_OFFICE);

        // Fill in organization information
        type("account.organizationField", "Aon Org");
        click("account.searchOrganizationIcon");
        selectByIndex("account.producerCodeDropdown", 2);

        // Save the account
        click("common.updateButton");

        return true;
    }

    /**
     * Generates a random gender (Male or Female).
     *
     * @return A randomly selected gender.
     */
    private String getRandomGender() {
        return random.nextBoolean() ? "Male" : "Female";
    }

    /**
     * Generates a random marital status (Single, Married, Divorced, Widowed).
     *
     * @return A randomly selected marital status.
     */
    private String getRandomMaritalStatus() {
        String[] maritalStatuses = {"Single", "Married", "Divorced", "Widowed"};
        return maritalStatuses[random.nextInt(maritalStatuses.length)];
    }
}