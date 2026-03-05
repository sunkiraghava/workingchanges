package com.punchh.server.pages;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.github.javafaker.Faker;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

public class PpccUtilities {

    static Logger logger = LogManager.getLogger(PpccUtilities.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
    PageObj pageObj;
    Faker faker;
    Actions actions;

    public PpccUtilities(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
        pageObj = new PageObj(driver);
        actions = new Actions(driver);
        faker = new Faker();
	}
    
    public int getLocationId(String searchName, boolean isProvisioned, String token) {
        logger.info("Get the location id");
        String provisionState = isProvisioned ? "true" : "false";
        String queryParam = "?is_provisioned=" + provisionState + "&search=" + searchName;
        Response response = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code 200 did not match for location list API");
        TestListeners.extentTest.get().pass("Location list API is giving 200 status code");
        int locationId = response.jsonPath().get("data[0].id");
        return locationId; 
    }

    public int getPolicyId(String policyName, String token)
    {
        logger.info("Get the policy id");
        String queryParam = "?search=" + policyName;
        Response policyListResponse = pageObj.endpoints().getPolicyList(token, queryParam);
        Assert.assertEquals(policyListResponse.getStatusCode(), 200,
                "Status code 200 did not match for policy list API");
        Assert.assertEquals(policyListResponse.jsonPath().get("data[0].name").toString(), policyName,
                "Policy Name do not matches");
        TestListeners.extentTest.get().pass("Policy list API is giving 200 status code");
        int policyId = policyListResponse.jsonPath().get("data[0].id");
        return policyId;
    }

    public void deletePolicy(int policyId, String token) {
        logger.info("Delete the policy");
        Response deleteApiResponse = pageObj.endpoints().deletePolicy(token, policyId);
        Assert.assertEquals(deleteApiResponse.getStatusCode(), 200,
                "Status code 200 did not match for provisioning API");
        Assert.assertEquals(deleteApiResponse.jsonPath().get("errors").toString(), "[]",
                "Error message for delete policy API did not match");
        TestListeners.extentTest.get().pass("Delete policy is giving 200 status code and expected message");
    }

    public String createPolicy(String token, String status)
    {
       logger.info("Create first policy");
       String policyName = "AUT " + faker.lorem().characters(5);
       int posTypeId = 1; // For Aloha pos Type Id is 1.
       String queryParam = "";
       Response createPolicyResponse = pageObj.endpoints().addPolicy(token, policyName, posTypeId, queryParam, status);

       Assert.assertEquals(createPolicyResponse.getStatusCode(), 200, "Status code is not 200");
       Assert.assertEquals(createPolicyResponse.jsonPath().get("data"), "Policy `" + policyName + "` created successfully", "Messages do not match");
       Assert.assertEquals(createPolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Metadata has some value");
       Assert.assertEquals(createPolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
       TestListeners.extentTest.get().pass("Add Policy API executed successfully");
       return policyName;
    }

	public boolean verifyPremiumTooltip(String elementXpath, String tooltipSelector, String expectedText,
			String context) throws InterruptedException {
		pageObj.newSegmentHomePage().segmentAdvertiseBlock();
		WebElement element = driver.findElement(By.xpath(elementXpath));
		actions.moveToElement(element).perform();

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		WebElement tooltip = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(tooltipSelector)));
		String actualText = tooltip.getText();
		if (expectedText.equals(actualText)) {
			logger.info(context + " displays correct premium tooltip: " + actualText);
			return true;
		} else {
			logger.warn(context + " tooltip mismatch. Expected: " + expectedText + ", but got: " + actualText);
			return false;
		}
	}

    public boolean isElementNotPresent(String locatorKey, String elementName) throws InterruptedException {
        String xpath = utils.getLocatorValue(locatorKey);
        List<WebElement> elements = driver.findElements(By.xpath(xpath));
        if (elements.isEmpty()) {
            logger.info(elementName + " is NOT present in DOM");
            return true;
        } else {
            logger.warn(elementName + " is present in DOM");
            return false;
        }
    }    

	public boolean isElementPresentAndClickable(String locatorKey, String elementName) throws InterruptedException {
       
        String xpath = utils.getLocatorValue(locatorKey);
        List<WebElement> elements = driver.findElements(By.xpath(xpath));

        if (elements.isEmpty()) {
            logger.warn(elementName + " is NOT present in DOM");
            return false;
        }

        WebElement element = elements.get(0);
        if (element.isDisplayed() && element.isEnabled()) {
            logger.info(elementName + " is present and clickable in DOM");
            return true;
        } else {
            logger.warn(elementName + " is present but NOT clickable/visible");
            return false;
        }
    }
    
    public String getText(String locator)
    {
        return driver.findElement(By.xpath(locator)).getText();
    }
}
