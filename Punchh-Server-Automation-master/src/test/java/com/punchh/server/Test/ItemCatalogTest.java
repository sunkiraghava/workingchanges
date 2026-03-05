package com.punchh.server.Test;

import java.awt.HeadlessException;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class ItemCatalogTest {

	private static Logger logger = LogManager.getLogger(ItemCatalogTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	String amount = "301.0";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Item Catalog Functionality is removed from Production
//	@Test(description = "SQ-T3131 Validate that user is able to search using Brand,Product, Department, Category and Subcategory ||"
//			+ "SQ-T3132 Validate that All search keyword/filters should get cleared on clicking Reset button. ||"
//			+ "SQ-T3129 Validate that if user changes the value in show entries dropdown, data is displayed correctly according to chosen value ||"
//			+ "SQ-T3868 Validate that ‘Copy to clipboard’ button is visible on ‘Items catalog’ page", groups = "Regression")
	public void T3131_verfyItemCatalogPage()
			throws InterruptedException, HeadlessException, UnsupportedFlavorException, IOException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings -> Items Catalog
		pageObj.menupage().navigateToSubMenuItem("Settings", "Items Catalog");

		// setting all the 3 filters
		pageObj.itemCatalogPage().setValueInSearchBox1("Brand", "Mrs. Freshley`s");
		pageObj.itemCatalogPage().setValueInSearchBox2("Department", "Frozen");
		pageObj.itemCatalogPage().setValueInSearchBox3("Category", "Frozen Bakery");

		// click on search button
		pageObj.itemCatalogPage().clickOnSearchButton();

		// setting no. of entries per page
		pageObj.itemCatalogPage().setNumberOfEntriesPerPage("10");

		// verify that filter is working properly
		pageObj.itemCatalogPage().verifyItemCatalogList("2", "6", "7", dataSet.get("Brand_Name"),
				dataSet.get("Department_Name"), dataSet.get("Category_Name"));

		// verify that text is copied after clicking on click on copy to clipboard
		// button
		boolean flag = pageObj.itemCatalogPage().clickOnCopyButton();
		Assert.assertEquals(true, flag, "Text is not copied to the clipboard");
		logger.info("Text is copied to the clipboard");
		TestListeners.extentTest.get().pass("Text is not copied to the clipboard");

		// click on rest button
		pageObj.itemCatalogPage().clickOnResetButton();

		// verifying pagenation is working properly
		pageObj.itemCatalogPage().clickOnNextPage();

	}

	// Amit
	@Test(description = "SQ-T4204 RedeemableImageUploadAndRemove", priority = 2)
	@Owner(name = "Amit Kumar")
	public void T4204_RedeemableImageUploadAndRemove() throws Exception {
		String redeemableName = "Redeemable" + CreateDateTime.getTimeDateString();

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().setRedeemableName(redeemableName);
		// Upload redeemable image
		String uploadedImage = pageObj.redeemablePage().uploadRedeemableimage();
		pageObj.redeemablePage().selectRecieptRule("1");

		pageObj.redeemablePage().expiryInDays("", "1");
		pageObj.redeemablePage().clickOnFinishButton();
		String status = pageObj.redeemablePage().successConfirmationMessage();
		Assert.assertTrue(status.contains("Redeemable successfully saved."),
				"Redeemable creation success msg did not matched");
		logger.info("Redeemable with image upload created successfully");
		TestListeners.extentTest.get().pass("Redeemable with image upload created successfully");
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		// Remove redeemable image to be added
		String removedImage = pageObj.redeemablePage().removeUploadedRedeemableimage();
		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().expiryInDays("", "1");
		pageObj.redeemablePage().clickOnFinishButton();
		String statusImageRemoved = pageObj.redeemablePage().successConfirmationMessage();
		Assert.assertTrue(statusImageRemoved.contains("Redeemable successfully saved."),
				"Redeemable creation success msg did not matched");
		logger.info("Redeemable with image upload created successfully");
		TestListeners.extentTest.get().pass("Redeemable with image upload created successfully");
		Assert.assertNotEquals(uploadedImage, removedImage,
				"Uploaded image and after Removed image , image icon scr path found same .. some issue in uploading/removing redeemabel image");
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
