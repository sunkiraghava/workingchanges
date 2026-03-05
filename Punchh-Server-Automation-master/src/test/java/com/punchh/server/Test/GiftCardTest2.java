package com.punchh.server.Test;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GiftCardTest2 {

	private static Logger logger = LogManager.getLogger(GiftCardTest2.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

//@author = Raja
	@SuppressWarnings("unused")
	@Test(description = "Verify new configs for GC in Mobile config section to support Mobileframework ", groups = "Regression", priority = 0)
	public void VerifyNewConfigforGCinMobileConfig() throws InterruptedException {

		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and disble the new flags
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().checkNewFlagsonGCPage();
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Purchase", "uncheck");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Reload", "uncheck");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Balance Transfer", "uncheck");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Card Consolidation", "uncheck");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Card Sharing", "uncheck");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Supports $0 Card Creation", "uncheck");

		// Card/meta V1 API validation

		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 cards is successful with response code : " + cardsResponse.statusCode());

		String enable_gift_card_purchase = cardsResponse.jsonPath().get("[0].enable_gift_card_purchase").toString();
		String enable_gift_card_reload = cardsResponse.jsonPath().get("[0].enable_gift_card_reload").toString();
		String enable_gift_card_transfer = cardsResponse.jsonPath().get("[0].enable_gift_card_transfer").toString();
		String enable_gift_card_consolidation = cardsResponse.jsonPath().get("[0].enable_gift_card_consolidation")
				.toString();
		String enable_gift_card_sharing = cardsResponse.jsonPath().get("[0].enable_gift_card_sharing").toString();
		String create_empty_gift_cards = cardsResponse.jsonPath().get("[0].create_empty_gift_cards").toString();

		Assert.assertEquals(enable_gift_card_purchase, "false");
		Assert.assertEquals(enable_gift_card_reload, "false");
		Assert.assertEquals(enable_gift_card_transfer, "false");
		Assert.assertEquals(enable_gift_card_consolidation, "false");
		Assert.assertEquals(enable_gift_card_sharing, "false");
		Assert.assertEquals(create_empty_gift_cards, "false");
		logger.info(",..............................enable_gift_card_reload key value is:" + enable_gift_card_reload);

		pageObj.mobileconfigurationPage().checkNewFlagsonGCPage();
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Purchase", "check");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Reload", "check");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Balance Transfer", "check");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Card Consolidation", "check");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Enable Card Sharing", "check");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Supports $0 Card Creation", "check");

		// Meta V2 API validation

		Response cardsResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse1.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 cards is successful with response code : " + cardsResponse1.statusCode());

		String enable_gift_card_purchase1 = cardsResponse1.jsonPath().get("enable_gift_card_purchase").toString();
		String enable_gift_card_reload1 = cardsResponse1.jsonPath().get("enable_gift_card_reload").toString();
		String enable_gift_card_transfer1 = cardsResponse1.jsonPath().get("enable_gift_card_transfer").toString();
		String enable_gift_card_consolidation1 = cardsResponse1.jsonPath().get("enable_gift_card_consolidation")
				.toString();
		String enable_gift_card_sharing1 = cardsResponse1.jsonPath().get("enable_gift_card_sharing").toString();
		String create_empty_gift_cards1 = cardsResponse1.jsonPath().get("create_empty_gift_cards").toString();

		Assert.assertEquals(enable_gift_card_purchase1, "true");
		Assert.assertEquals(enable_gift_card_reload1, "true");
		Assert.assertEquals(enable_gift_card_transfer1, "true");
		Assert.assertEquals(enable_gift_card_consolidation1, "true");
		Assert.assertEquals(enable_gift_card_sharing1, "true");
		Assert.assertEquals(create_empty_gift_cards1, "true");
		logger.info(",..............................enable_gift_card_reload key value is:" + enable_gift_card_reload1);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
