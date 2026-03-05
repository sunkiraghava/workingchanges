package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.CreateLISandQC;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SingletonDBUtils;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

// Author - Vansham
@Listeners(TestListeners.class)
public class RedeemableImageUpload {
	static Logger logger = LogManager.getLogger(RedeemableImageUpload.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	public Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		apiUtils = new ApiUtils();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5668 Verify the file formats (jpg,jpeg,png,gif,webp,svg) for image upload, "
			+ "SQ-T5667 Verify the image upload for file format- gif and size less than 500kb, "
			+ "SQ-T5666 Verify the boundary value image upload for size -501kb, "
			+ "SQ-T5665 Verify the api with valid image upload under 500kb and valid external_id", groups = { "regression", "dailyrun" })
	public void T5668_redeemableImageUpload() throws Exception {

		// valid image upload under 500kb and valid external_id
		Response uploadRedeemableImageResponse = pageObj.endpoints().uploadRedeemableImage(dataSet.get("apiKey"),
				dataSet.get("externalId"), dataSet.get("imageUrl1"));
		Assert.assertEquals(uploadRedeemableImageResponse.getStatusCode(), 200);
		logger.info(
				"Verified the upload redeemable image api with valid image upload under 500kb and valid external_id");
		TestListeners.extentTest.get().pass(
				"Verified the upload redeemable image api with valid image upload under 500kb and valid external_id");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable(dataSet.get("redeemableName"));
		String redeemableImageUrl = pageObj.redeemablePage().redeemableImageUrl();
		Assert.assertNotEquals(redeemableImageUrl, dataSet.get("defaultUrl"), "Image not uploaded");
		logger.info("Verified the api with valid image upload under 500kb and valid external_id");
		TestListeners.extentTest.get()
				.pass("Verified the api with valid image upload under 500kb and valid external_id");

		// gif file upload and size less than 500 kb
		Response uploadRedeemableImageResponse2 = pageObj.endpoints().uploadRedeemableImage(dataSet.get("apiKey"),
				dataSet.get("externalId"), dataSet.get("gifImage"));
		Assert.assertEquals(uploadRedeemableImageResponse2.getStatusCode(), 200);
		utils.refreshPage();
		String redeemableImageUrl2 = pageObj.redeemablePage().redeemableImageUrl();
		Assert.assertNotEquals(redeemableImageUrl2, dataSet.get("defaultUrl"), "Image not uploaded");
		logger.info("Verified the image upload for file format- gif and size less than 500kb");
		TestListeners.extentTest.get().pass("Verified the image upload for file format- gif and size less than 500kb");

		// png file upload and size less than 500 kb
		Response uploadRedeemableImageResponse3 = pageObj.endpoints().uploadRedeemableImage(dataSet.get("apiKey"),
				dataSet.get("externalId"), dataSet.get("pngImage"));
		Assert.assertEquals(uploadRedeemableImageResponse3.getStatusCode(), 200);
		utils.refreshPage();
		String redeemableImageUrl3 = pageObj.redeemablePage().redeemableImageUrl();
		Assert.assertNotEquals(redeemableImageUrl3, dataSet.get("defaultUrl"), "Image not uploaded");
		logger.info("Verified the image upload for file format- png and size less than 500kb");
		TestListeners.extentTest.get().pass("Verified the image upload for file format- png and size less than 500kb");

		// webp file upload and size less than 500 kb
		Response uploadRedeemableImageResponse4 = pageObj.endpoints().uploadRedeemableImage(dataSet.get("apiKey"),
				dataSet.get("externalId"), dataSet.get("webpImage"));
		Assert.assertEquals(uploadRedeemableImageResponse4.getStatusCode(), 200);
		utils.refreshPage();
		String redeemableImageUrl4 = pageObj.redeemablePage().redeemableImageUrl();
		Assert.assertNotEquals(redeemableImageUrl4, dataSet.get("defaultUrl"), "Image not uploaded");
		logger.info("Verified the image upload for file format- webp and size less than 500kb");
		TestListeners.extentTest.get().pass("Verified the image upload for file format- webp and size less than 500kb");

		// svg file upload and size less than 500 kb
		Response uploadRedeemableImageResponse5 = pageObj.endpoints().uploadRedeemableImage(dataSet.get("apiKey"),
				dataSet.get("externalId"), dataSet.get("svgImage"));
		Assert.assertEquals(uploadRedeemableImageResponse5.getStatusCode(), 200);
		utils.refreshPage();
		String redeemableImageUrl5 = pageObj.redeemablePage().redeemableImageUrl();
		Assert.assertNotEquals(redeemableImageUrl5, dataSet.get("defaultUrl"), "Image not uploaded");
		logger.info("Verified the image upload for file format- svg and size less than 500kb");
		TestListeners.extentTest.get().pass("Verified the image upload for file format- svg and size less than 500kb");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
