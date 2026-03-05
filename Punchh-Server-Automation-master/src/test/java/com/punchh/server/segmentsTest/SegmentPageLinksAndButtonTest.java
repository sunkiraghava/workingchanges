package com.punchh.server.segmentsTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SegmentPageLinksAndButtonTest {

	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail, redeemable;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	String signUpCampaignName;
	private Utilities utils;
	private static JsonNode segmentBuilderClauses;
	private Properties prop;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws JsonMappingException, JsonProcessingException, IOException {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		ObjectMapper mapper = new ObjectMapper();
		segmentBuilderClauses = mapper.readTree(Files
				.readString(Paths.get(System.getProperty("user.dir") + prop.getProperty("segmentBuilderClauseJson"))));

	}

	@Test(description = "SQ-T4356 Verify all links/buttons on segment page (List, Edit and overview page) are redirecting properly or not", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4356_LinkOrButtonOnSegmentPage() throws InterruptedException, IOException {
		String segmentName = dataSet.get("segmentName");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		boolean flag = pageObj.segmentsPage().brokenLinksUi();
		Assert.assertTrue(flag, "Links are broken in Segment List Page");
		utils.logPass("Links are not broken in Segment List Page");

		pageObj.segmentsPage().searchAndMarkSegmentFavorite(segmentName, dataSet.get("segmentId"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().filterByFavorite();
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, dataSet.get("segmentId"));

		boolean flag1 = pageObj.segmentsPage().brokenLinksUi();
		Assert.assertTrue(flag1, "Links are broken in Segment Overview Page");
		utils.logPass("Links are not broken in Segment Overview Page");

		pageObj.segmentsPage().segmentOverviewPageOptionList(dataSet.get("edit"));
		pageObj.segmentsBetaPage().saveSegment(segmentName);

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		String firstSegName = pageObj.segmentsPage().getInfoOfFirstSegment("1");
		Assert.assertNotEquals(firstSegName, null, "Not found any segment name on segment list page");
		utils.logit("Name of First Segment is : " + firstSegName);

		String firstSegDate = pageObj.segmentsPage().getInfoOfFirstSegment("2");
		Assert.assertNotEquals(firstSegDate, null, "Not found any segment date on segment list page");
		utils.logit("Date of First Segment is : " + firstSegDate);

		pageObj.segmentsPage().segmentSorting("NAME");
		String changedFirstSegName = pageObj.segmentsPage().getInfoOfFirstSegment("1");
		Assert.assertNotEquals(firstSegName, changedFirstSegName,
				"Sorting of segment NAME is not applied or only one segment is present");
		utils.logPass("Sorting is applied on segment Name and New first name is : " + changedFirstSegName);

		pageObj.segmentsPage().segmentSorting("DATE CREATED");
		String changedFirstSegDate = pageObj.segmentsPage().getInfoOfFirstSegment("1");
		Assert.assertNotEquals(firstSegDate, changedFirstSegDate,
				"Sorting of segment DATE CREATED is not applied or only one segment is present");
		utils.logPass("Sorting is applied on segment DATE CREATED and New first DATE CREATED is : " + changedFirstSegDate);

		pageObj.segmentsPage().segmentSorting("CREATED BY");
		pageObj.segmentsPage().segmentSorting("REACH");
		pageObj.segmentsPage().clickOnNextPageButton("Next Button", "");
		pageObj.segmentsPage().numberOfSegPerPage("20");
		pageObj.segmentsPage().searchSegmentOnly(segmentName, dataSet.get("segmentId"));

		boolean optionVisibility = pageObj.segmentsPage().checkActionOptionButtonIsClickable("Create Mass Gifting");
		Assert.assertTrue(optionVisibility, "Option button under Action column is not clickable");
		utils.logPass("Option button under Action column is clickable");

		// pageObj.segmentsPage().markSegmentFavourite("Favorite", segmentName);
		// String segmentId = pageObj.segmentsPage().getSegmentID();
		// Assert.assertNotEquals(segmentId, null, "Segment id is NULL");
		// pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);

	}

	@Test(description = "SQ-T5511 Verify all links on new bento design segment home page", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5511_NewSegmentHomePageTest() throws InterruptedException, IOException {
		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_shp_switch_buttons", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().verifyNewSegmentHomePage("All Segments");

		boolean flag1 = pageObj.segmentsPage().brokenLinksUi();
		Assert.assertTrue(flag1, "Links are broken in New Segment Home Page");
		utils.logPass("Links are not broken in New Segment Home Page");

		pageObj.newSegmentHomePage().clickOnHelpButton();
		pageObj.newSegmentHomePage().verifyHelpPage();

		pageObj.newSegmentHomePage().clickOnSmartSegmentButton();
		pageObj.newSegmentHomePage().verifySmartSegmentPage();
		pageObj.newSegmentHomePage().clickOnCreateSegmentButton();
		pageObj.newSegmentHomePage().verifyCreateSegmentPage();
		driver.close();
		utils.switchToParentWindow();
		pageObj.newSegmentHomePage().clickOnManageTagButton();
		pageObj.newSegmentHomePage().verifyDeleteOrManageTagPage("Manage Tags");
		pageObj.newSegmentHomePage().closeManageTagOrTag("Manage Tags");
		pageObj.newSegmentHomePage().clickAuditLogEllipseButton();
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().closeMoreFilterPanel();

		pageObj.newSegmentHomePage().selectSortByFilterValue("Name : A to Z");
		utils.waitTillPagePaceDone();

		List<String> segmentNameList = pageObj.newSegmentHomePage().segmentList();
		String firstSegmentBerofeSorting = segmentNameList.get(0);

		pageObj.newSegmentHomePage().selectSortByFilterValue("Name : Z to A");
		utils.waitTillPagePaceDone();
		List<String> segmentNameListAfterSorting = pageObj.newSegmentHomePage().segmentList();
		String firstSegmentAfterSorting = segmentNameListAfterSorting.get(0);

		boolean sortingFlag = true;
		if (firstSegmentBerofeSorting.equalsIgnoreCase(firstSegmentAfterSorting)) {
			sortingFlag = false;
		}
		Assert.assertTrue(sortingFlag, "Sorting on New Segment Home is not working properly");
		utils.logPass("Sorting on New Segment Home is working properly");
		utils.refreshPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withoutAssertion");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().selectAllSegmentCheckBox();
		boolean checkBoxStatusFlag = pageObj.newSegmentHomePage().segmentCheckBoxListStatus("check_box");
		Assert.assertTrue(checkBoxStatusFlag, "All check box are not checked after clicking on Select All check box");
		utils.logPass("All check box are checked after clicking on Select All check box");

		pageObj.newSegmentHomePage().clickOnClearSelection();

		boolean checkBoxStatusFlag2 = pageObj.newSegmentHomePage().segmentCheckBoxListStatus("check_box_outline_blank");
		Assert.assertTrue(checkBoxStatusFlag2, "All check box are not unchecked after clicking on Clear selection");
		utils.logPass("All check box are unchecked after clicking on Clear selection");

		pageObj.newSegmentHomePage().setItemPerPage("25");
		List<String> segmentListAfterItemPerPage = pageObj.newSegmentHomePage().segmentList();
		boolean itemPerPageFlag = false;
		if ((segmentListAfterItemPerPage.size() > 10) && (segmentListAfterItemPerPage.size() <= 25)) {
			itemPerPageFlag = true;
		}
		Assert.assertTrue(itemPerPageFlag, "Item Per Page on New Segment Home is not working properly");
		utils.logPass("Item Per Page on New Segment Home is working properly");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		boolean nextPageFlag = pageObj.newSegmentHomePage().clickOnPreviosOrNextNavigationPage("Next", "page=2");
		Assert.assertTrue(nextPageFlag, "Not able to navigate to next page");
		utils.logPass("Navigated to Next Page on New Segment Home Page");

		boolean previousPageFlag = pageObj.newSegmentHomePage().clickOnPreviosOrNextNavigationPage("Previous", "page");
		Assert.assertFalse(previousPageFlag, "Not able to navigate to Previous page");
		utils.logPass("Navigated to Previous Page on New Segment Home Page");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		List<String> ellipsesOptionNameList = pageObj.newSegmentHomePage().getEllipsesOptionList();
//		List<String> actualEllipsesOptionNameList =  dataSet.get("actualEllipsesOptionNameList");
		Assert.assertEquals(ellipsesOptionNameList, dataSet.get("actualEllipsesOptionNameList"),
				"option present in the Ellipse button are not equal");
		utils.logPass("Verified option present in the Ellipse button are equal");

		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("View details");
		boolean viewDetailsFlag = utils.verifyPartOfURL("segment-overview");
		Assert.assertTrue(viewDetailsFlag, "Not able to navigate to View details");
		utils.logPass("Navigated to View details on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Create campaign");
		boolean createCampaignFlag = utils.verifyPartOfURL("mass_gifting_campaigns");
		Assert.assertTrue(createCampaignFlag, "Not able to navigate to Create campaign");
		utils.logPass("Navigated to Create campaign on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Edit");
		boolean editFlag = utils.verifyPartOfURL("segment&state=edit");
		Assert.assertTrue(editFlag, "Not able to navigate to Edit");
		utils.logPass("Navigated to Edit on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newSegmentHomePage().verifyDeleteOrManageTagPage("Tag");
		pageObj.newSegmentHomePage().closeManageTagOrTag("Tag");
		utils.logPass("Navigated to Tag on New Segment Home Page");

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Duplicate");
		boolean duplicateFlag = utils.verifyPartOfURL("segment&state=duplicate");
		Assert.assertTrue(duplicateFlag, "Not able to navigate to Duplicate");
		utils.logPass("Navigated to Duplicate on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Freeze");
		boolean freezeFlag = utils.verifyPartOfURL("freeze_from_segment_id");
		Assert.assertTrue(freezeFlag, "Not able to navigate to Freeze");
		utils.logPass("Navigated to Freeze on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Audit log");
		boolean auditLogFlag = utils.verifyPartOfURL("audit");
		Assert.assertTrue(auditLogFlag, "Not able to navigate to Audit log");
		utils.logPass("Navigated to Audit log on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Schedule export");
		boolean scheduleExportFlag = utils.verifyPartOfURL("segment_export_schedules");
		Assert.assertTrue(scheduleExportFlag, "Not able to navigate to Schedule export");
		utils.logPass("Navigated to Schedule export on New Segment Home Page");
		utils.navigateBackPage();

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("Segment with Gender Female", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Delete");
		pageObj.newSegmentHomePage().verifyDeleteOrManageTagPage("Delete segment");
		pageObj.newSegmentHomePage().closeDeleteTab();
		utils.logPass("Navigated to Delete on New Segment Home Page");

		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.newSegmentHomePage().verifyClassicSegmentPage("Segments");

		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();
	}

	@Test(description = "SQ-T5397 Verify that the functionality for segment type Gift Card Payment is working fine.", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T5397_segmentTypeGiftCardPayment() throws Exception {
		String segmentName = CreateDateTime.getUniqueString("Gift Card Payments");
		String massCampaignName = "AutomationMassOffer_Gift Card Payments_" + CreateDateTime.getTimeDateString();
		// String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		int count = 0;
		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Gift Card Payments");
		List<String> actualAttributeNameList = pageObj.segmentsPage().getAttributeNameList();
		Assert.assertEquals(actualAttributeNameList, dataSet.get("expectedAttributeNameList"),
				"the user should be Unable to see the following Payment type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");
		utils.logPass(
				"The user should be able to see the following Payment type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");
		List<String> actualPaymentTypeAttributeList = pageObj.segmentsPage()
				.getAttributeFromSegmentType("Payment Types");
		List<String> expPaymentTypeAttributeList = Arrays.asList("Purchased", "Reloaded", "Gifted", "Card Added",
				"Auto Reloaded");
		boolean paymentTypeFlag = false;
		if (actualPaymentTypeAttributeList.containsAll(expPaymentTypeAttributeList)) {
			paymentTypeFlag = true;
		}
		Assert.assertTrue(paymentTypeFlag,
				"The user should be unable to see the following Payment type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");
		utils.logPass(
				"The user should be able to see the following Payment type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");

		// Gift Card Payments Payment Types Is Purchased Membership Tier Is Current
		// Membership Is Any At least 1 Ever Timezone Is Chennai
		segmentName = CreateDateTime.getUniqueString("Purchased_Membership_TierIs_CurrentMembership");
		String PurchasedMembershipTierIsCurrentMembership_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get(
						"GiftCardPaymentsPaymentTypesIsPurchasedMembershipTierIsCurrentMembershipIsAnyAtleast1EverTimezoneIsChennai"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, PurchasedMembershipTierIsCurrentMembership_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logit("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
		int segmentCount = Integer
				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount);
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

		utils.refreshPage();
		segmentName = CreateDateTime.getUniqueString("Gifted_Membership_TierIs_MembershipIsAnyGreaterthan");
		pageObj.segmentsPage().setSegmentName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Gift Card Payments");
		pageObj.segmentsBetaPage().selectAttribute("Payment Types", "Gifted");
		pageObj.segmentsBetaPage().setOperator("Operator", "Greater than");
		pageObj.segmentsBetaPage().setValue("0");
		pageObj.segmentsBetaPage().setDuration("Within a range");
		pageObj.segmentsBetaPage().selectAttribute("Timezone", "(GMT+00:00) UTC ( UTC )");
		int segmentCount3 = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount3 >= count, "Guest Count label not appeard");
		utils.logit("Guest Count label appeard");
		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId3 = pageObj.segmentsPage().getSegmentID();

//		segmentName = CreateDateTime.getUniqueString("Gifted_Membership_TierIs_MembershipIsAnyGreaterthan");
//		String GiftedMembershipTierIsMembershipIsAnyGreaterthan_builder_clause = utils
//				.convertToDoubleStringified(segmentBuilderClauses.get(
//						"GiftCardPaymentsPaymentTypesisGiftedMembershipTierisCurrentMembershipisAnyGreaterthan0WithinarangeCurrentmonthTimezoneisChennai"));
//
//		Response segmentCreationUsingBuilderClauseResponse3 = pageObj.endpoints().segmentCreationUsingBuilderClause(
//				dataSet.get("apiKey"), segmentName, GiftedMembershipTierIsMembershipIsAnyGreaterthan_builder_clause);
//		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//		String segmentId3 = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();
//
//		Response segmentCountresponse3 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId3);
//		Assert.assertEquals(segmentCountresponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for Fetch segment count dashboard api");
//		utils.logit("Fetch segment count Dashboard API is Successful");
//		String segmentCountMessage3 = segmentCountresponse3.jsonPath().getString("message");
//		Assert.assertEquals(segmentCountMessage3, "Fetched Count Successfully");
//		int segmentCount3 = Integer
//				.parseInt((segmentCountresponse3.jsonPath().get("count").toString()).replaceAll(",", ""));
//		Assert.assertTrue(segmentCount3 > count, "Segment count is not greater than 0.");
//		utils.logit("Segment count is greater than 0  ie " + segmentCount3);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId3 + "';";
		DBUtils.executeQuery(env, query);

		// Gift Card Payments Payment Types Is Purchased Membership Tier Is Current
		// Membership Is Any Greater than 450 Ever Timezone is Chennai
		segmentName = CreateDateTime.getUniqueString("Purchased_Membership_TierIs_MembershipIsAnyGreaterthan");
		String PurchasedMembershipTierIsMembershipIsAnyGreaterthan_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get(
						"GiftCardPaymentsPaymentTypesIsPurchasedMembershipTierIsCurrentMembershipIsAnyGreaterthan450EverTimezoneisChennai"));

		Response segmentCreationUsingBuilderClauseResponse2 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, PurchasedMembershipTierIsMembershipIsAnyGreaterthan_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId2 = segmentCreationUsingBuilderClauseResponse2.jsonPath().get("data.id").toString();

		Response segmentCountresponse2 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId2);
		Assert.assertEquals(segmentCountresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logit("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage2 = segmentCountresponse2.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage2, "Fetched Count Successfully");
		int segmentCount2 = Integer
				.parseInt((segmentCountresponse2.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount2 > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount2);

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId2);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getRandomGuestFromSegmentList(dataSet.get("apiKey"), segmentId2,
				8);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"), "Success message text did not matched");
		utils.logPass("Mass Campaign scheduled successuly : " + massCampaignName);

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		utils.logPass("Mass Offer campaign with name " + massCampaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + massCampaignName);

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Push Notification Status is :- " + pushNotificationStatus);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId2 + "';";
		DBUtils.executeQuery(env, query);

	}

	@Test(description = "SQ-T5400 Testing for Membership None attribute", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T5400_verifyMembershipNone() throws Exception {

//		String segmentName = CreateDateTime.getUniqueString("Profile_Details_MembershipTier_");
		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		// create new segment
		int count = 0;
		String segmentName = CreateDateTime.getUniqueString("Profile_Details_MembershipTierCurrentEqualtoNone");
		String ProfileDetailsMembershipTierCurrentEqualtoNone_builder_clause = utils.convertToDoubleStringified(
				segmentBuilderClauses.get("ProfileDetailsMembershipTierCurrentEqualtoNone"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, ProfileDetailsMembershipTierCurrentEqualtoNone_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logit("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
		int segmentCount = Integer
				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount);

		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getRandomGuestFromSegmentList(dataSet.get("apiKey"), segmentId,
				8);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), segmentName,
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not match");
		utils.logPass("Mass Offer campaign with name " + massCampaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + massCampaignName);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

	}

	@Test(description = "SQ-T5992 Ensure Segment ID from Business A Is Not Accessible in Business B (List & URL)", priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T5992_verifySegmentIdOfOneBusinessToOther() throws Exception {

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		String firstSegName = pageObj.segmentsPage().getSegmentNameOnSegmentHomePage();
		String segmentId = pageObj.segmentsPage().getSegmentId(firstSegName);

		// Navigate to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		// Select another business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("otherSlug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();

		// search segment id of first business in second business
		pageObj.segmentsPage().searchSegmentByNameOrID(segmentId);
		boolean segmentPresenceFlag = pageObj.segmentsPage().openSegmentWithId(segmentId, firstSegName);
		Assert.assertFalse(segmentPresenceFlag, "Segment ID of first business is present in second business");
		utils.logPass("Verified that Segment ID of first business is not present in second business");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		String firstSegNameOnOtherBusiness = pageObj.segmentsPage().getSegmentNameOnSegmentHomePage();
		pageObj.segmentsPage().searchSegmentByNameOrID(firstSegNameOnOtherBusiness);
		Assert.assertTrue(pageObj.segmentsPage().openSegmentWithName(firstSegNameOnOtherBusiness),
				"Not able to open segment with name " + firstSegNameOnOtherBusiness + " in second business");
		pageObj.segmentsPage().replaceSegmentIdInUrl(segmentId);
		utils.refreshPage();
		boolean resultFlag = pageObj.segmentsPage().segmentNotFoundWithInvalidURL(firstSegName);
		Assert.assertTrue(resultFlag, "Segment ID of first business is present in second business");
		utils.logPass(
				"Verified that Segment of first business is not opening in second business after altering the URL");

	}

	@Test(description = "SQ-T6058 verify first search segment by ID then DELETE it then again search by same ID", priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T6058_verifySegmentIdAfterSegmentDeletion() throws Exception {

		String segmentName = "AutomationSegment_Favorite Location_" + CreateDateTime.getTimeDateString();

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Favorite Location");
		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
		pageObj.segmentsBetaPage().selectAttribute("Location(s)", "Any");
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		utils.waitTillSpinnerDisappear(10);
		String segmentId = pageObj.segmentsPage().getSegmentID();

		// Select another business
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();

		pageObj.segmentsPage().searchSegmentByNameOrID(segmentId);
		boolean segmentPresenceFlag = pageObj.segmentsPage().openSegmentWithId(segmentId, segmentName);
		Assert.assertTrue(segmentPresenceFlag, "Segment ID of " + segmentName + " is not present");
		utils.logPass("Verified that Segment ID of " + segmentName + " is present");

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);

		// search segment after deletion by ID
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsPage().searchSegmentByNameOrID(segmentId);
		boolean segmentPresenceAfterDeletionFlag = pageObj.segmentsPage().openSegmentWithId(segmentId, segmentName);
		Assert.assertFalse(segmentPresenceAfterDeletionFlag,
				"Segment ID of " + segmentName + " is present after deleting segment");
		utils.logPass("Verified that Segment ID of " + segmentName + " is not present after deleting segment");

	}
	
	@Test(description = "SQ-T7003: Verify the cancel button functionality on the segment edit page")
	@Owner(name = "Shivam Agrawal")
	public void T7003_verifyCancelButtonOnSegmentEditPage() throws InterruptedException
	{
		String segmentName = "AutomationSegment_Verify Cancel Button_" + CreateDateTime.getTimeDateString();
		
		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		//From the new segment-homepage, click on the "create segment" button, click on Cancel>>don't save button without saving the segment
		//Verify that the user is redrected back to the new segment homepage in this case
		pageObj.newSegmentHomePage().setSegmentNameNewSegmentPage(segmentName);
		pageObj.segmentsBetaPage().clickOnCancelButton();
		pageObj.segmentsBetaPage().clickOnDontSaveButton();
		pageObj.newSegmentHomePage().verifyNewSegmentHomePage("All Segments");
		utils.logPass("Verified that cancel button redirects to the segment homepage when cancelled while creating a new segment");
		
		//Create a new segment and click on edit after it, during edit phase click on Cancel>>don't save
		//Verify that the user is redirected to the segment overview page from here.
		pageObj.newSegmentHomePage().setSegmentNameNewSegmentPage(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Gender");
		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		pageObj.segmentsBetaPage().clickOnCancelButton();
		pageObj.segmentsBetaPage().clickOnDontSaveButton();
		pageObj.newSegmentHomePage().verifyCreateSegmentPage();
		utils.logPass("Verified that cancel button redirects to the segment overview page when cancelled while editing the already saved "
				+ "segment");
		
		//Deleting the segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().searchSegment(segmentName);
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Delete");
		pageObj.newSegmentHomePage().clickOnYesDelete();
		utils.logPass("Verified that the segment is deleted");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}

}
