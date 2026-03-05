package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class LineItemWithBaseitemQCTest {

	private static Logger logger = LogManager.getLogger(LineItemWithBaseitemQCTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private Utilities utils;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	List<List<String>> itemlists = new ArrayList<>();
	List<String> innerList1 = Arrays.asList("Drink", "Pizza", "Soup", "Fries", "Burger");
	List<String> innerList2 = Arrays.asList("101,102,103", "201,202,203", "601,602,603", "701,702,703", "801,802,803");

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Adding innerLists to listOfLists
		itemlists.add(innerList1);
		itemlists.add(innerList2);

	}

	@Test(description = "SQ-T2636 Verify Enable Menu Item Aggregator OnOff for bundle creation with Sum of Amounts Incremental", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2636_verifyEnableMenuItemAggregatorOnOffForBundlCreationWithSumOfAmountsIncremental()
			throws InterruptedException {
		String item = "";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create QC1
		String qcName = "TestQc" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().setQCName(qcName);
		pageObj.qualificationcriteriapage().setProcessingFunction("Sum of Amounts Incremental"); // here fix needed

		// Set line item filters
		for (int i = 0; i < 2; i++) {
			item = innerList1.get(i);
			pageObj.qualificationcriteriapage().setLineItemFilters(i, item, "Maximum Unit Price", "Qty: 1");
		}
		// set item qualifiers
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", innerList1.get(2));
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully");
		// validate Receipt Qualification
		String qualifyingmsg = pageObj.qualificationcriteriapage()
				.testQualificationwithReceipt(dataSet.get("recieptA"));

		Assert.assertTrue(qualifyingmsg.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg.contains("Processed Value : 10"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg
						.contains("Qualified Menu Items : drink3|1|2.0|M|102|800|150|^pizza1|1|8.0|M|201|801|151|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg);

		// Edit QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().SearchQC(qcName);
		pageObj.qualificationcriteriapage().EditQCEnableMenuItemAggregator("Item Id");
		String qcSuccessMsg1 = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg1, "Qualification Criterion updated",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria Updated successfully");

		// validate Receipt Qualification
		String qualifyingmsg1 = pageObj.qualificationcriteriapage()
				.testQualificationwithReceipt(dataSet.get("recieptB"));
		Assert.assertTrue(qualifyingmsg1.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg1.contains("Processed Value : 18.97"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg1
						.contains("Qualified Menu Items : drink1|1|3.97|M|102|800|150|^pizza1|1|15.0|M|201|801|151|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg1);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().deleteQC(qcName);
		/*
		 * Assert.assertEquals(delQCMsg, "Qualification Criterion destroyed",
		 * "Qualification Criterion deleted message not displayed");
		 */
		TestListeners.extentTest.get().pass("Qualification Criteria deleted successfully");

	}

	@Test(description = "SQ-T2633 Verify number of bundles formation when Maximum Discounted Units is Zero Blank AnyValue with Sum of Amounts Incremental", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2633_verifyNumberOfBundlesFormationWhenMaximumDiscountedUnitIsZeroBlankAnyValueWithSumOfAmountsIncremental()
			throws InterruptedException {

		String item = "";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create QC1
		String qcName = "TestQc" + CreateDateTime.getTimeDateString();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().setQCName(qcName);
		pageObj.qualificationcriteriapage().setProcessingFunction("Sum of Amounts Incremental");

		// Set line item filters
		for (int i = 0; i < 1; i++) {
			item = innerList1.get(i);
			pageObj.qualificationcriteriapage().setLineItemFilters(i, item, "Maximum Unit Price", "Qty: 1");
		}
		// set item qualifiers
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", innerList1.get(2));
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully");

		// validate Receipt Qualification
		String qualifyingmsg = pageObj.qualificationcriteriapage()
				.testQualificationwithReceipt("drink1|1|1|M|101|800|150^pizza1|1|10|M|201|801|151");
		System.out.println(qualifyingmsg);
		Assert.assertTrue(qualifyingmsg.contains("Does not Qualify"), "Does not Qualify did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg);

		// Edit QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().SearchQC(qcName);
///    	pageObj.qualificationcriteriapage().setMaximumDiscountedUnits("0");
		pageObj.qualificationcriteriapage().setMaximumDiscountedUnits("");
		String qcSuccessMsg1 = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg1, "Qualification Criterion updated",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria Updated successfully");

		// validate Receipt Qualification
		String qualifyingmsg1 = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"drink1|1|1|M|101|800|150^drink2|1|1.5|M|102|800|150^drink3|1|2|M|103|800|150^soup1|1|10|M|601|801|151^soup2|1|15|M|602|801|151^soup3|1|20|M|603|801|151");
		Assert.assertTrue(qualifyingmsg1.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg1.contains("Processed Value : 4.5"), "Processed value did not matched");
		Assert.assertTrue(qualifyingmsg1.contains(
				"Qualified Menu Items : drink3|1|2.0|M|103|800|150|^drink2|1|1.5|M|102|800|150|^drink1|1|1.0|M|101|800|150|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg1);

		// Edit QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().SearchQC(qcName);
		pageObj.qualificationcriteriapage().setMaximumDiscountedUnits("2");
		String qcSuccessMsg2 = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg2, "Qualification Criterion updated",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria Updated successfully");

		// validate Receipt Qualification
		String qualifyingmsg2 = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"drink1|1|1|M|101|800|150^drink2|1|1.5|M|102|800|150^drink3|1|2|M|103|800|150^soup1|1|10|M|601|801|151^soup2|1|15|M|602|801|151^soup3|1|20|M|603|801|151");
		Assert.assertTrue(qualifyingmsg2.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg2.contains("Processed Value : 3.5"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg2
						.contains("Qualified Menu Items : drink3|1|2.0|M|103|800|150|^drink2|1|1.5|M|102|800|150|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg2);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().deleteQC(qcName);
		/*
		 * Assert.assertEquals(delQCMsg, "Qualification Criterion destroyed",
		 * "Qualification Criterion deleted message not displayed");
		 */
		TestListeners.extentTest.get().pass("Qualification Criteria deleted successfully");
	}

	@Test(description = "SQ-T2634 Verify QC Processed value based on LIF Processing function and Quantity", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2634_verifyQCProcessedValueBasedOnLIFProcessingFunctionAndQuantity() throws InterruptedException {

		String item = "";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create QC1 with percentage amount
		String qcName = "TestQc" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().setQCName(qcName);
		pageObj.qualificationcriteriapage().setPercentageOfProcessedAmount("20");
		pageObj.qualificationcriteriapage().setProcessingFunction("Sum of Amounts Incremental");

		// Set line item filters Drink and Pizza
		for (int i = 0; i < 2; i++) {
			item = innerList1.get(i);
			pageObj.qualificationcriteriapage().setLineItemFilters(i, item, "Maximum Unit Price", "Qty: 1");
			pageObj.qualificationcriteriapage().setQty(i, "Qty: 1");
		}
		// set item qualifiers Soup
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", innerList1.get(2));
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully");
		// validate Receipt Qualification
		String qualifyingmsg = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"drink1|1|10|M|101|800|150^drink2|1|15|M|102|800|150^drink3|1|20|M|103|800|150^pizza1|1|5|M|201|801|151^pizza2|1|10|M|201|801|151^pizza3|1|15|M|201|801|151^soup1|1|10|M|601|803|152");

		Assert.assertTrue(qualifyingmsg.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg.contains("Processed Value : 7"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg
						.contains("Qualified Menu Items : drink3|1|20.0|M|103|800|150|^pizza3|1|15.0|M|201|801|151|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg);

		// Edit QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().SearchQC(qcName);
		// Set line item filters Drink and Pizza
		for (int i = 0; i < 2; i++) {
			item = innerList1.get(i);
			pageObj.qualificationcriteriapage().setLineItemFilters(i, item, "Maximum Unit Price", "Qty: 2");
			pageObj.qualificationcriteriapage().setQty(i, "Qty: 2");

		}
		pageObj.qualificationcriteriapage().clearPercentageOfProcessedAmount(); // No value

		String qcSuccessMsg1 = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg1, "Qualification Criterion updated",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria Updated successfully");

		// validate Receipt Qualification
		String qualifyingmsg1 = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"drink1|1|1|M|101|800|150^drink2|1|1.5|M|102|800|150^pizza1|1|3|M|201|801|151^pizza2|1|2|M|202|801|151^soup1|1|10|M|601|803|152");
		Assert.assertTrue(qualifyingmsg1.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg1.contains("Processed Value : 7.5"), "Processed value did not matched");
		Assert.assertTrue(qualifyingmsg1.contains(
				"Qualified Menu Items : drink2|1|1.5|M|102|800|150|^drink1|1|1.0|M|101|800|150|^pizza1|1|3.0|M|201|801|151|^pizza2|1|2.0|M|202|801|151|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg1);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().deleteQC(qcName);
		/*
		 * Assert.assertEquals(delQCMsg, "Qualification Criterion destroyed",
		 * "Qualification Criterion deleted message not displayed");
		 */
		TestListeners.extentTest.get().pass("Qualification Criteria deleted successfully");

	}

	@Test(description = "SQ-T2635 Verify Item Qualifiers eligibilities for bundle creation", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2635_verifyItemQualifiersEligibilitiesForBundleCreation() throws InterruptedException {

		String item = "";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create QC1
		String qcName = "TestQc" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().setQCName(qcName);
		pageObj.qualificationcriteriapage().setProcessingFunction("Sum of Amounts Incremental");

		// Set line item filters
		for (int i = 0; i < 2; i++) {
			item = innerList1.get(i);
			pageObj.qualificationcriteriapage().setLineItemFilters(i, item, "Maximum Unit Price", "Qty: 1");
		}
		// no Item qualifires it is blank
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully");

		String qualifyingmsg = pageObj.qualificationcriteriapage()
				.testQualificationwithReceipt("drink1|1|1|M|101|800|150^pizza1|1|10|M|201|801|151");
		Assert.assertTrue(qualifyingmsg.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg.contains("Processed Value : 0"), "Processed value did not matched");
		Assert.assertTrue(qualifyingmsg.contains("Qualified Menu Items :"), "Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg);

		// Edit QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().SearchQC(qcName);
		for (int i = 2; i < innerList1.size(); i++) {
			pageObj.qualificationcriteriapage().setItemQualifiers(i, "Line Item Exists", innerList1.get(i));
		}
		String qcSuccessMsg1 = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg1, "Qualification Criterion updated",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria Updated successfully");

		String qualifyingmsg1 = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"drink1|1|1|M|101|800|150^pizza1|1|1.5|M|201|900|151^soup1|1|10|M|601|8010|156^fries1|1|1.99|M|701|990|152^burger1|1|1.78|M|801|7001|153");
		Assert.assertTrue(qualifyingmsg1.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg1.contains("Processed Value : 2.5"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg1
						.contains("Qualified Menu Items : drink1|1|1.0|M|101|800|150|^pizza1|1|1.5|M|201|900|151|"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg1);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().deleteQC(qcName);
		/*
		 * Assert.assertEquals(delQCMsg, "Qualification Criterion destroyed",
		 * "Qualification Criterion deleted message not displayed");
		 */
		TestListeners.extentTest.get().pass("Qualification Criteria deleted successfully");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}