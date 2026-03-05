package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LineItemWithModifiersQCTest {

	private static Logger logger = LogManager.getLogger(LineItemWithModifiersQCTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private Utilities utils;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

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
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2637 Verify Modifier Cases for bundle creation", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2637_verifyModifierCasesForBundleCreation() throws InterruptedException {
		String item = "";
		List<List<String>> itemlists = new ArrayList<>();
		List<String> innerList1 = Arrays.asList("Rice", "Sandwich", "Omelette");
		List<String> innerList2 = Arrays.asList("111,112,113", "211,212,213", "611,612,613");
		// Adding innerLists to listOfLists
		itemlists.add(innerList1);
		itemlists.add(innerList2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create Line Item Selectors
		String successMsg = "";
		for (int i = 0; i < innerList1.size(); i++) {
			pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

			pageObj.lineItemSelectorPage().setLineItemName(itemlists.get(0).get(i));
			pageObj.lineItemSelectorPage().setBaseItemsAsItemid(itemlists.get(1).get(i));
			pageObj.lineItemSelectorPage().setFilterItemSet("Only Modifiers");
			pageObj.lineItemSelectorPage().createLIS();
		}
		// Assert.assertEquals(successMsg, "Line Item Selector created",
		// "Line Item selector created success message not displayed");
		TestListeners.extentTest.get().pass("Line Item Selector created successfully");

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
		// set item qualifiers
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", innerList1.get(2));
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully");
		// validate Receipt Qualification
		String qualifyingmsg = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"Rice|1|4|M|111|100|100|1.0^Modifier1|1|1|M|2001|100|100|1.1^Modifier2|1|1.1|M|2003|100|100|1.2^Sandwich|1|6|M|211|100|100|2.0^Modifier3|1|1.1|M|2002|100|100|2.1^Omelette|1|6|M|611|8010|156|3.0^Modifier5|1|1|M|3001|100|100|3.1");

		Assert.assertTrue(qualifyingmsg.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg.contains("Processed Value : 2.2"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg.contains(
						"Qualified Menu Items : Modifier2|1|1.1|M|2003|100|100|1.2^Modifier3|1|1.1|M|2002|100|100|2.1"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().deleteQC(qcName);

		/*
		 * Assert.assertEquals(delQCMsg, "Qualification Criterion destroyed",
		 * "Qualification Criterion deleted message not displayed");
		 */

		TestListeners.extentTest.get().pass("Qualification Criteria deleted successfully");
		// Delete Lis
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		for (int i = 0; i < innerList1.size(); i++) {
			pageObj.lineItemSelectorPage().deleteLineItemSelectors(innerList1.get(i));

			/*
			 * Assert.assertEquals(delLisMsg, "Line Item Selector destroyed",
			 * "Line Item Selector deleted message not displayed");
			 */

			TestListeners.extentTest.get().pass("Line Item Selector deleted successfully");
		}

	}

	@Test(description = "SQ-T2637 Verify Modifier Cases for bundle creation", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2637_verifyModifierCasesForBundleCreation_Two() throws InterruptedException {

		String item = "";
		List<List<String>> itemlists = new ArrayList<>();
		List<String> innerList1 = Arrays.asList("Fajitas", "Cornbread", "Cioppino");
		List<String> innerList2 = Arrays.asList("121,122,123", "221,222,223", "621,622,623");
		List<String> innerList3 = Arrays.asList("2001,2002,2003", "3001,3002,3003", "4001,4002,4003");
		// Adding innerLists to listOfLists
		itemlists.add(innerList1);
		itemlists.add(innerList2);
		itemlists.add(innerList3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create Line Item Selectors
		String successMsg = "";
		for (int i = 0; i < innerList1.size(); i++) {
			pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

			pageObj.lineItemSelectorPage().setLineItemName(itemlists.get(0).get(i));
			pageObj.lineItemSelectorPage().setBaseItemsAsItemid(itemlists.get(1).get(i));
			// set filteritemset
			pageObj.lineItemSelectorPage().setFilterItemSet("Base Items and Modifiers");

			// set modifiers itemid
			pageObj.lineItemSelectorPage().setModifiersAsItemid(itemlists.get(2).get(i));
			pageObj.lineItemSelectorPage().setModifierSelectionRule("1", "Maximum Unit Price");
			pageObj.lineItemSelectorPage().createLIS();
		}
		// Assert.assertEquals(successMsg, "Line Item Selector created",
		// "Line Item selector created success message not displayed");
		TestListeners.extentTest.get().pass("Line Item Selector created successfully");
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
		// set item qualifiers
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", innerList1.get(2));
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully");

		// validate Receipt Qualification
		String qualifyingmsg = pageObj.qualificationcriteriapage().testQualificationwithReceipt(
				"Fajitas|1|4|M|121|100|100|1.0^Modifier1|1|1.99|D|2001|100|100|1.1^Cornbread|1|6|M|221|100|100|2.0^Modifier4|1|2.78|D|3002|100|100|2.1^Cioppino1|1|6|M|621|8010|156|3.0^Modifier9|1|1|M|3001|100|100|3.1");

		Assert.assertTrue(qualifyingmsg.contains("Qualifies"), "Qualifies did not matched");
		Assert.assertTrue(qualifyingmsg.contains("Processed Value : 5.23"), "Processed value did not matched");
		Assert.assertTrue(
				qualifyingmsg.contains(
						"Qualified Menu Items : Fajitas|1|2.01|M|121|100|100|1.0^Cornbread|1|3.22|M|221|100|100|2.0"),
				"Qualified Menu Items did not matched");
		TestListeners.extentTest.get().pass("validate Receipt Qualification successfully :" + qualifyingmsg);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().deleteQC(qcName);
		/*
		 * Assert.assertEquals(delQCMsg, "Qualification Criterion destroyed",
		 * "Qualification Criterion deleted message not displayed");
		 */
		TestListeners.extentTest.get().pass("Qualification Criteria deleted successfully");
		// Delete Lis
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		for (int i = 0; i < innerList1.size(); i++) {
			pageObj.lineItemSelectorPage().deleteLineItemSelectors(innerList1.get(i));
			/*
			 * Assert.assertEquals(delLisMsg, "Line Item Selector destroyed",
			 * "Line Item Selector deleted message not displayed");
			 */
			TestListeners.extentTest.get().pass("Line Item Selector deleted successfully");
		}
	}

	@Test(description = "SQ-T4938 Verify error validation if no qualifier is added in receipt and activate the receipt tag"
			+ "SQ-T4935 Verify confirmation pop-up should appear when tap on activate button at QC page", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4938_verifyErrorMessageForNoQualifierAdded() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create QC1
		String qcName = "DoNotDelete TestQc" + CreateDateTime.getTimeDateString();
		String rctTagName = "Test QC" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().setQCName(qcName);
		pageObj.qualificationcriteriapage().setReceiptTagName(rctTagName);
		pageObj.qualificationcriteriapage().setProcessingFunction("Rate Rollback");
		pageObj.qualificationcriteriapage().setUnitDiscount("10");
		pageObj.qualificationcriteriapage().updateButton();

		// verify confirmation Pop Up
		String text = pageObj.qualificationcriteriapage().VerifyConfirmationPopup();
		Assert.assertEquals(text, "Confirmation Required");
		pageObj.utils().logPass("Confirmation Popup appreard when tap on activate button at QC page");

		// activate QC choosing Option Yes
		String errorMsg = pageObj.qualificationcriteriapage().VerifyErrorMsgOnActivatingTag(qcName, "Yes");
		Assert.assertEquals(errorMsg,
				"This Qualification Criterion does not have any receipt criteria or qualifying expressions that can be used to qualifying receipts.");
		pageObj.utils().logPass("error message verified on selecting Yes as :" + errorMsg);

		// activate QC choosing Option No
		String errorMsg1 = pageObj.qualificationcriteriapage().VerifyErrorMsgOnActivatingTag(qcName, "No");
		Assert.assertEquals(errorMsg1,
				"This Qualification Criterion does not have any receipt criteria or qualifying expressions that can be used to qualifying receipts.");
		pageObj.utils().logPass("error message verified on selecting No as :" + errorMsg1);
	}

	@Test(description = "SQ-T4936 Verify yes option functionality of confirmation pop-up when activate receipt tag", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4936_verifyYesOptionFunctionalityOfConfirmationPopup() throws InterruptedException {

		// user creation using pos signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Pos api checkin with menuItemid1 before activating receipt tag
		String menuItemid1 = "777" + CreateDateTime.getTimeDateString();
		String menuItemid2 = "888" + CreateDateTime.getTimeDateString();

		// Pos api checkin with menuItemid1 before activating receipt tag
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinQC(date, userEmail, key, txn, dataSet.get("locationKey"),
				menuItemid1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

//		// Pos api checkin with menuItemid2 before activating receipt tag
//		String key2 = CreateDateTime.getTimeDateString();
//		String txn2 = "123456" + CreateDateTime.getTimeDateString();
//		String date2 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
//		Response resp2 = pageObj.endpoints().posCheckinQC(date2, userEmail, key2, txn2, dataSet.get("locationKey"),
//				menuItemid2);
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Login to punchh dashboard
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String lisName = "TestLic0" + CreateDateTime.getTimeDateString();
		String lisName1 = "TestLic1" + CreateDateTime.getTimeDateString();

		// create Line Item Selector with menuItemid1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lisName, menuItemid1,
				dataSet.get("filterSetName"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// create Line Item Selector with item id menuItemid2

		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lisName1, menuItemid2,
				dataSet.get("filterSetName"));

		// qc and tag name
		String qcName = "TestQc" + CreateDateTime.getTimeDateString();
		String rctTagName = "RctTag" + CreateDateTime.getTimeDateString();
		// create qc
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(qcName, rctTagName, dataSet.get("amtCap"),
				dataSet.get("qcFucntionName"), dataSet.get("unitDiscount"), true, lisName);

		// activate receipt tag choosing Option Yes
		String successMsg = pageObj.qualificationcriteriapage().VerifyErrorMsgOnActivatingTag(rctTagName, "Yes");
		Assert.assertTrue(
				successMsg.contains(
						"receipt tag activated. All new receipts that come in will be evaluated for tagging."),
				"Success message did not verify on selecting Yes");
		pageObj.utils().logPass("Success message verified on selecting Yes as :" + successMsg);

		// Pos api checkin with menuItemid2 before activating receipt tag
		String key2 = CreateDateTime.getTimeDateString();
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp2 = pageObj.endpoints().posCheckinQC(date2, userEmail, key2, txn2, dataSet.get("locationKey"),
				menuItemid2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// navigate to user timeline , validate first existing checkin details ,tag
		// shouldn't be shown with the checkin
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String flag = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key);
		Assert.assertFalse(flag.contains(qcName),
				"Receipt tag is attached with checkin done before receipt tag activation");
		pageObj.utils().logPass("Receipt tag is not attached with previous checkin");

		// Pos api checkin with menuItemid1 after activating receipt tag
		String key1 = CreateDateTime.getTimeDateString();
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp1 = pageObj.endpoints().posCheckinQC(date1, userEmail, key1, txn1, dataSet.get("locationKey"),
				menuItemid1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// navigate to user timeline ,validate second new checkin details ,tag should be
		// shown with the checkin
		String flag1 = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key1);
		Assert.assertTrue(flag1.contains(rctTagName), "Receipt tag is not attached with thischeckin");
		pageObj.utils().logPass("Receipt tag is attached with this checkin ie : " + rctTagName);

		// deactivate tag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deactivateTagFromQc(qcName);
		pageObj.qualificationcriteriapage().removeLisFromQc(lisName);
		pageObj.qualificationcriteriapage().setLis(true, lisName1);
		// set item qualifiers
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", lisName1);
		pageObj.qualificationcriteriapage().updateButton();

		// activate tag choosing Option Yes
		String successMsg1 = pageObj.qualificationcriteriapage().VerifyErrorMsgOnActivatingTag(rctTagName, "Yes");
		Assert.assertTrue(
				successMsg1.contains(
						"receipt tag activated. All new receipts that come in will be evaluated for tagging."),
				"Success message did not verify on selecting Yes");
		pageObj.utils().logPass("Success message verified on selecting Yes as : " + successMsg1);

		// navigate to user timeline ,validate last checkin details ,tag shouldn't be
		// shown with the checkin
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String flag2 = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key2);
		Assert.assertFalse(flag2.contains(rctTagName), "Receipt tag is attached with previous checkin");
		pageObj.utils().logPass("Receipt tag is not attached with previous checkin");

		// Pos api checkin with menuItemid2 after activating tag
		String key3 = CreateDateTime.getTimeDateString();
		String txn3 = "123456" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp3 = pageObj.endpoints().posCheckinQC(date3, userEmail, key3, txn3, dataSet.get("locationKey"),
				menuItemid2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// navigate to user timeline ,validate second checkin details ,tag should be
		// shown with the checkin
		String flag3 = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key3);
		Assert.assertTrue(flag3.contains(rctTagName), "Receipt tag is not attached with thischeckin");
		pageObj.utils().logPass("Receipt tag is attached with this checkin ie : " + rctTagName);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deactivateTagFromQc(qcName);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deleteQC(qcName);

		// Delete Lis
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		pageObj.lineItemSelectorPage().deleteLineItemSelectors(lisName);
		pageObj.lineItemSelectorPage().deleteLineItemSelectors(lisName1);

	}

	@Test(description = "SQ-T4937 Verify no option functionality of confirmation pop-up when activate receipt tag", groups = {
			"regression", "unstable" })
	@Owner(name = "Rakhi Rawat")
	public void T4937_verifyNoOptionFunctionalityOfConfirmationPopup() throws InterruptedException {

		// user creation using pos signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Pos api checkin with menuItemid1 before activating receipt tag
		String menuItemid1 = "777" + CreateDateTime.getTimeDateString();
		String menuItemid2 = "888" + CreateDateTime.getTimeDateString();

		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp = pageObj.endpoints().posCheckinQC(date, userEmail, key, txn, dataSet.get("locationKey"),
				menuItemid1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

//		// Pos api checkin with menuItemid2 before activating receipt tag
//		String key2 = CreateDateTime.getTimeDateString();
//		String txn2 = "654321" + CreateDateTime.getTimeDateString();
//		String date2 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
//		Response resp2 = pageObj.endpoints().posCheckinQC(date2, userEmail, key2, txn2, dataSet.get("locationKey"),
//				menuItemid2);
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String lisName = "TestLic0" + menuItemid1;
		String lisName1 = "TestLic1" + menuItemid2;

		// create Line Item Selector with menuItemid1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lisName, menuItemid1,
				dataSet.get("filterSetName"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// create Line Item Selector with item id menuItemid2
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lisName1, menuItemid2,
				dataSet.get("filterSetName"));

		String qcName = "TestQc" + CreateDateTime.getTimeDateString();
		String rctTagName = "RctTag" + CreateDateTime.getTimeDateString();
		// create qc
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(qcName, rctTagName, dataSet.get("amtCap"),
				dataSet.get("qcFucntionName"), dataSet.get("unitDiscount"), true, lisName);

		// activate receipt tag choosing Option No
		String successMsg = pageObj.qualificationcriteriapage().VerifyErrorMsgOnActivatingTag(qcName, "No");
		Assert.assertTrue(successMsg.contains(
				"receipt tag activated. Old receipts will be evaluated using this receipt tag and new receipts that come in after the tag is activated will also be evaluated for tagging."),
				"Success message did not verify on selecting No");
		pageObj.utils().logPass("Success message verified on selecting No as :" + successMsg);

		// navigate to user timeline ,validate first checkin details ,tag should be
		// shown with this checkin also
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String flag = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key);
		Assert.assertTrue(flag.contains(rctTagName), "Receipt tag is not attached with previous checkin");
		pageObj.utils().logPass("Receipt tag is attached with previous checkin");

		// Pos api checkin with menuItemid1 after activating receipt tag
		String key1 = CreateDateTime.getTimeDateString();
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp1 = pageObj.endpoints().posCheckinQC(date1, userEmail, key1, txn1, dataSet.get("locationKey"),
				menuItemid1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// navigate to user timeline, validate second checkin details ,tag should be
		// shown with this checkin
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(1);
		String flag1 = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key1);
		Assert.assertTrue(flag1.contains(rctTagName), "Receipt tag is not attached with this checkin");
		pageObj.utils().logPass("Receipt tag is attached with this checkin as well ie : " + rctTagName);

		// Pos api checkin with menuItemid2 before activating receipt tag
		String key2 = CreateDateTime.getTimeDateString();
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp2 = pageObj.endpoints().posCheckinQC(date2, userEmail, key2, txn2, dataSet.get("locationKey"),
				menuItemid2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// deactivate tag

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deactivateTagFromQc(qcName);
		pageObj.qualificationcriteriapage().setReceiptTagName(rctTagName);
		pageObj.qualificationcriteriapage().removeLisFromQc(lisName);
		pageObj.qualificationcriteriapage().setLis(true, lisName1);
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", lisName1);
		pageObj.qualificationcriteriapage().updateButton();

		// activate receipt tag choosing Option No
		String successMsg1 = pageObj.qualificationcriteriapage().VerifyErrorMsgOnActivatingTag(qcName, "No");
		Assert.assertTrue(successMsg1.contains(
				"receipt tag activated. Old receipts will be evaluated using this receipt tag and new receipts that come in after the tag is activated will also be evaluated for tagging."),
				"Success message did not verify on selecting No");
		pageObj.utils().logPass("Success message verified on selecting No as : " + successMsg1);

		// navigate to user timeline, validate first checkin details ,tag should be
		// shown with this checkin also
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String flag2 = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key2);
		Assert.assertTrue(flag2.contains(rctTagName), "Receipt tag is not attached with previous checkin");
		pageObj.utils().logPass("Receipt tag is attached with previous checkin ie : " + rctTagName);

		// Pos api checkin with menuItemid2 after activating receipt tag
		String key3 = CreateDateTime.getTimeDateString();
		String txn3 = "654321" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp3 = pageObj.endpoints().posCheckinQC(date3, userEmail, key3, txn3, dataSet.get("locationKey"),
				menuItemid2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// navigate to user timeline,validate second checkin details ,tag should be
		// shown with this checkin
		String flag3 = pageObj.qualificationcriteriapage().verifyReceiptTagVisibleOnGuestTimeline(rctTagName, key3);
		Assert.assertTrue(flag3.contains(rctTagName), "Receipt tag is not attached with this checkin");
		pageObj.utils().logPass("Receipt tag is attached with this checkin as well ie : " + rctTagName);

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deactivateTagFromQc(qcName);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deleteQC(qcName);

		// Delete Lis
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		pageObj.lineItemSelectorPage().deleteLineItemSelectors(lisName);
		pageObj.lineItemSelectorPage().deleteLineItemSelectors(lisName1);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}