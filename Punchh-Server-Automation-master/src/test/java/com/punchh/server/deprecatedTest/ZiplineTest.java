package com.punchh.server.deprecatedTest;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ZiplineTest {
	static Logger logger = LogManager.getLogger(ZiplineTest.class);
	public WebDriver driver;
	private String userEmail;
	private String email = "AutoApiTemp@punchh.com";
	// private String cardNumber = "68Temp";
	private Properties prop;
	private PageObj pageObj;

	@BeforeMethod(alwaysRun = true)
	public void before() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		// userCard = cardNumber.replace("Temp", CreateDateTime.getDateTimeString());
		// new fetchCard().setCardNo(userCard);
		userEmail = email.replace("Temp", CreateDateTime.getTimeDateString());
		pageObj = new PageObj(driver);
	}

	@Test(description = "INTD- 320 API1 sign up and login for loyalty card with masking ", priority = 0)
	public void Api1MaskingValidation() throws InterruptedException {
		// verifying External Vendor and masking is on
		pageObj.instanceDashboardPage().navigateToPunchhInstance("https://aayushisingh.punchh.io/");
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness("fatpizza");
		pageObj.instanceDashboardPage().goToCockpit();
		boolean enableExternalVendorStatus = pageObj.instanceDashboardPage().enableExternalVendor();
		boolean maskingStatus = pageObj.instanceDashboardPage().masking();
		try {
			SoftAssert softassert = new SoftAssert();
			softassert.assertTrue(enableExternalVendorStatus, "External Vendor flag is not enabled");
			softassert.assertTrue(maskingStatus, "masking is enabled");
			softassert.assertAll();

			pageObj.utils().logPass(
					"Flag details: External Vendor flag is successfully disabled and masking is successfully enabled");
		} catch (Exception e) {
			logger.error("Error in validating Flag details" + e);
			TestListeners.extentTest.get().fail("Error in validating Flag details" + e);
		}
		logger.info("== API 1 user signup Test ==");
		Response signUpResponse = pageObj.endpoints().Api1UserSignUpZiplineMasking(userEmail,
				Utilities.getApiConfigProperty("client"), Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());
		logger.info("== API 1 user login Test ==");
		Response loginResponse = pageObj.endpoints().Api1UserLogin(userEmail, Utilities.getApiConfigProperty("client"),
				Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "API 1 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());
	}

	@Test(description = "INTD- 320 API2 sign up and login for loyalty card with masking", priority = 1)
	public void Api2MaskingValidation() throws InterruptedException {
		// Verifying External Vendor and masking is on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(prop.getProperty("slug"));
		pageObj.instanceDashboardPage().goToCockpit();
		boolean enableExternalVendorStatus = pageObj.instanceDashboardPage().enableExternalVendor();
		boolean maskingStatus = pageObj.instanceDashboardPage().masking();
		try {
			SoftAssert softassert = new SoftAssert();
			softassert.assertTrue(enableExternalVendorStatus, "External Vendor flag is not enabled");
			softassert.assertTrue(maskingStatus, "masking is enabled");
			softassert.assertAll();
			pageObj.utils().logPass(
					"Flag details: External Vendor flag is successfully disabled and masking is successfully enabled");
		} catch (Exception e) {
			logger.error("Error in validating Flag details" + e);
			TestListeners.extentTest.get().fail("Error in validating Flag details" + e);
		}

		logger.info("== API 2 user signup Test ==");
		Response signUpResponse = pageObj.endpoints().Api2SignUpZiplineMasking(userEmail,
				Utilities.getApiConfigProperty("client"), Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, Utilities.getApiConfigProperty("client"),
				Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "API 2 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());

	}

	@Test(description = "INTD- 319 API1 sign up and login for loyalty card", priority = 2)
	public void Api1UnmaskingValidation() throws InterruptedException {

		// verifying External Vendor and Unmasking is on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(prop.getProperty("slug"));
		pageObj.instanceDashboardPage().goToCockpit();
		boolean enableExternalVendorStatus = pageObj.instanceDashboardPage().enableExternalVendor();
		boolean unmaskingStatus = pageObj.instanceDashboardPage().Unmasking();
		try {
			SoftAssert softassert = new SoftAssert();
			softassert.assertTrue(enableExternalVendorStatus, "External Vendor flag is not enabled");
			softassert.assertTrue(unmaskingStatus, "unmasking is enabled");
			softassert.assertAll();

			pageObj.utils().logPass(
					"Flag details: External Vendor flag is successfully disabled and unmasking is successfully enabled");
		} catch (Exception e) {
			logger.error("Error in validating Flag details" + e);
			TestListeners.extentTest.get().fail("Error in validating Flag details" + e);
		}

		logger.info("== API 1 user signup Test ==");
		Response signUpResponse = pageObj.endpoints().Api1UserSignUpZiplineUnmasking(userEmail,
				Utilities.getApiConfigProperty("client"), Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());
		logger.info("== API 1 user login Test ==");
		Response loginResponse = pageObj.endpoints().Api1UserLogin(userEmail, Utilities.getApiConfigProperty("client"),
				Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "API 1 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());

	}

	@Test(description = "INTD- 319 API2 sign up and login for loyalty card", priority = 3)
	public void Api2UnmaskingValidation() throws InterruptedException {

		// Verifying External Vendor and masking is on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(prop.getProperty("slug"));
		pageObj.instanceDashboardPage().goToCockpit();
		boolean enableExternalVendorStatus = pageObj.instanceDashboardPage().enableExternalVendor();
		boolean unmaskingStatus = pageObj.instanceDashboardPage().Unmasking();
		try {
			SoftAssert softassert = new SoftAssert();
			softassert.assertTrue(enableExternalVendorStatus, "External Vendor flag is not enabled");
			softassert.assertTrue(unmaskingStatus, "unmasking is enabled");
			softassert.assertAll();

			pageObj.utils().logPass(
					"Flag details: External Vendor flag is successfully disabled and unmasking is successfully enabled");
		} catch (Exception e) {
			logger.error("Error in validating Flag details" + e);
			TestListeners.extentTest.get().fail("Error in validating Flag details" + e);
		}

		logger.info("== API 2 user signup Test ==");
		Response signUpResponse = pageObj.endpoints().Api2SignUpZiplineUnmasking(userEmail,
				Utilities.getApiConfigProperty("client"), Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, Utilities.getApiConfigProperty("client"),
				Utilities.getApiConfigProperty("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "API 2 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());

	}

	@AfterMethod(alwaysRun = true)
	public void after() {
		Utilities.screenShotCapture(driver, this.getClass().getName());
		driver.close();
		driver.quit();
	}
}
