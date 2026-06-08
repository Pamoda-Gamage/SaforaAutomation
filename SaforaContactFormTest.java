package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;

/**
 * Part 2: Basic UI Automation – Safora Contact Us Form
 * Target URL : https://safora.se/en/contact.html
 * Framework  : Selenium WebDriver 4.18.1 + TestNG 7.9.0
 * Language   : Java
 * IDE        : Eclipse
 *
 * Test Scenarios Covered:
 *   TC-CA-01  Fill all fields with valid data and verify form is ready to submit
 *   TC-CA-02  Submit empty form and verify browser HTML5 validation fires
 *   TC-CA-03  Fill name + phone + message but leave email blank – verify email validation
 *
 * NOTE ON RECAPTCHA:
 *   The Safora contact form includes a Google reCAPTCHA checkbox.
 *   Automated scripts cannot solve reCAPTCHA (it is intentionally bot-proof).
 *   TC-CA-01 therefore verifies correct field filling and that the Submit button
 *   is clickable — which is the testable scope without manual CAPTCHA solving.
 *   TC-CA-02 and TC-CA-03 test HTML5 required-field validation BEFORE reCAPTCHA
 *   is ever reached, so they work fully automatically.
 *
 * Author  : Pamoda (QA Intern)
 * Version : 2.0  (locators updated after DOM inspection)
 */
public class SaforaContactFormTest {

    private WebDriver driver;
    private WebDriverWait wait;

    // ---------------------------------------------------------------
    // Configuration
    // ---------------------------------------------------------------
    private static final String CONTACT_URL   = "https://safora.se/en/contact.html";
    private static final int    WAIT_SECONDS  = 10;

    // ---------------------------------------------------------------
    // Locators – confirmed from live DOM inspection
    // ---------------------------------------------------------------
    private static final By FIELD_NAME    = By.id("name");
    private static final By FIELD_EMAIL   = By.id("email");
    private static final By FIELD_PHONE   = By.id("phone");
    private static final By FIELD_MESSAGE = By.id("message");
    private static final By BTN_SUBMIT    = By.cssSelector("button[type='submit'].rs-btn");

    // ---------------------------------------------------------------
    // Setup & Teardown
    // ---------------------------------------------------------------
    @BeforeClass
    public void setUp() {
        // WebDriverManager auto-downloads the correct ChromeDriver version
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new"); // uncomment to run headlessly

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait   = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------------------------------------------------------------
    // Helper: navigate and scroll form into view
    // ---------------------------------------------------------------
    private void goToContactPage() {
        driver.get(CONTACT_URL);
        // Wait until the Name field is visible before doing anything
        wait.until(ExpectedConditions.visibilityOfElementLocated(FIELD_NAME));
        // Scroll the name field into view (form is lower on the page)
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement nameField = driver.findElement(FIELD_NAME);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", nameField);
        sleep(600); // brief settle after scroll
    }

    // ---------------------------------------------------------------
    // Helper: fill a field
    // ---------------------------------------------------------------
    private void fillField(By locator, String value) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(value);
    }

    // ---------------------------------------------------------------
    // TC-CA-01: Fill all fields with valid data
    // Verifies: all fields accept input correctly + Submit button is clickable
    // NOTE: We do NOT click Submit here because reCAPTCHA blocks bots.
    //       Filling + asserting field values is the automatable scope.
    // ---------------------------------------------------------------
    @Test(priority = 1, description = "TC-CA-01: All fields filled correctly with valid data")
    public void testFillFormWithValidData() {
        goToContactPage();

        fillField(FIELD_NAME,    "Pamoda Test");
        fillField(FIELD_EMAIL,   "pamoda.test@example.com");
        fillField(FIELD_PHONE,   "0771234567");
        fillField(FIELD_MESSAGE, "This is an automated test message sent by Selenium WebDriver for QA assignment.");

        // Assert each field holds the entered value
        Assert.assertEquals(driver.findElement(FIELD_NAME).getAttribute("value"),
                "Pamoda Test",              "TC-CA-01 FAIL: Name field value mismatch.");
        Assert.assertEquals(driver.findElement(FIELD_EMAIL).getAttribute("value"),
                "pamoda.test@example.com",  "TC-CA-01 FAIL: Email field value mismatch.");
        Assert.assertEquals(driver.findElement(FIELD_PHONE).getAttribute("value"),
                "0771234567",               "TC-CA-01 FAIL: Phone field value mismatch.");

        // Assert Submit button is present and clickable (reCAPTCHA will block the actual send)
        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(BTN_SUBMIT));
        Assert.assertTrue(submitBtn.isDisplayed(), "TC-CA-01 FAIL: Submit button not visible.");

        System.out.println("TC-CA-01 PASS: All fields filled correctly. Submit button is clickable.");
        System.out.println("  NOTE: Actual form submission requires manual reCAPTCHA completion.");
    }

    // ---------------------------------------------------------------
    // TC-CA-02: Submit empty form
    // Verifies: HTML5 'required' validation prevents submission
    //           The Name field (first required field) becomes :invalid
    // ---------------------------------------------------------------
    @Test(priority = 2, description = "TC-CA-02: Empty form triggers HTML5 required-field validation")
    public void testEmptyFormValidation() {
        goToContactPage();

        // Click Submit without entering anything
        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(BTN_SUBMIT));
        submitBtn.click();
        sleep(500);

        // The browser blocks submission when required fields are empty.
        // We verify this by checking the field's HTML5 validity state via JS.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        boolean nameInvalid = !(Boolean) js.executeScript(
                "return arguments[0].validity.valid;", driver.findElement(FIELD_NAME));

        Assert.assertTrue(nameInvalid,
                "TC-CA-02 FAIL: Expected Name field to be invalid when empty.");

        System.out.println("TC-CA-02 PASS: Empty form correctly blocked by HTML5 required validation.");
    }

    // ---------------------------------------------------------------
    // TC-CA-03: Submit with email field left empty
    // Verifies: Email field specifically is marked invalid
    // ---------------------------------------------------------------
    @Test(priority = 3, description = "TC-CA-03: Missing email triggers email field validation error")
    public void testMissingEmailValidation() {
        goToContactPage();

        // Fill all fields EXCEPT email
        fillField(FIELD_NAME,    "Pamoda Test");
        fillField(FIELD_PHONE,   "0771234567");
        fillField(FIELD_MESSAGE, "Testing missing email validation via Selenium.");
        // Leave FIELD_EMAIL blank intentionally

        // Attempt to submit
        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(BTN_SUBMIT));
        submitBtn.click();
        sleep(500);

        // Verify email field is in an invalid state
        JavascriptExecutor js = (JavascriptExecutor) driver;
        boolean emailInvalid = !(Boolean) js.executeScript(
                "return arguments[0].validity.valid;", driver.findElement(FIELD_EMAIL));

        Assert.assertTrue(emailInvalid,
                "TC-CA-03 FAIL: Expected Email field to be invalid when empty.");

        System.out.println("TC-CA-03 PASS: Email field correctly flagged as invalid when blank.");
    }

    // ---------------------------------------------------------------
    // TC-CA-04: Invalid email format validation
    // Verifies: Entering a non-email string in the email field is caught
    // ---------------------------------------------------------------
    @Test(priority = 4, description = "TC-CA-04: Invalid email format is caught by browser validation")
    public void testInvalidEmailFormat() {
        goToContactPage();

        fillField(FIELD_NAME,    "Pamoda Test");
        fillField(FIELD_EMAIL,   "not-a-valid-email");   // intentionally bad format
        fillField(FIELD_PHONE,   "0771234567");
        fillField(FIELD_MESSAGE, "Testing invalid email format.");

        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(BTN_SUBMIT));
        submitBtn.click();
        sleep(500);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        boolean emailInvalid = !(Boolean) js.executeScript(
                "return arguments[0].validity.valid;", driver.findElement(FIELD_EMAIL));

        Assert.assertTrue(emailInvalid,
                "TC-CA-04 FAIL: Expected Email field to reject an invalid email format.");

        System.out.println("TC-CA-04 PASS: Invalid email format correctly rejected.");
    }

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------
    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}