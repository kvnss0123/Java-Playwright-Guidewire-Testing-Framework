package stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountPerson extends TestBase {

    @Test(priority = 1)
    @When("I enter valid credentials and login")
    public void verifyLogin() {
        homePage.navigate();
        assertThat(homePage.login()).isTrue();
    }

    @Test(priority = 2)
    @Then("I create an account for a person")
    public void createAccount_Person() {
        assertThat(homePage.createAccount_Person()).isTrue();
    }

}