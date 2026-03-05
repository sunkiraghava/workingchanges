package com.punchh.server.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class NewSegmentHomePage {
	static Logger logger = LogManager.getLogger(NewSegmentHomePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, segmentCount, pnCount, smsCount;

	public NewSegmentHomePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void switchToNewSegmentManagementToolBtn() {
		try {
			utils.waitTillPagePaceDone();
			utils.implicitWait(10);
			WebElement ele = utils.getLocator("newSegmentHomePage.switchToNewSegmentManagementToolBtn");
			if (ele.isDisplayed()) {
				utils.getLocator("newSegmentHomePage.switchToNewSegmentManagementToolBtn").click();
				utils.waitTillPagePaceDone();
				logger.info("Clicked On Switch To New Segment Management Tool Button");
				TestListeners.extentTest.get().info("Clicked On Switch To New Segment Management Tool Button");
				segmentAdvertiseBlock();
			} else {
				utils.implicitWait(50);
				logger.info("Switch To New Segment Management Tool Button is not displaped");
				TestListeners.extentTest.get().info("Switch To New Segment Management Tool Button is not displaped");
			}
		} catch (Exception e) {
			utils.implicitWait(50);
			logger.info("Already on New Segment Home Page ");
			TestListeners.extentTest.get().info("Already on New Segment Home Page");
		}
	}

	public void clickOnSwitchToClassicBtn() {
		try {
			utils.implicitWait(10);
			WebElement ele = utils.getLocator("newSegmentHomePage.clickOnSwitchToClassicBtn");
			if (ele.isDisplayed()) {
				utils.getLocator("newSegmentHomePage.clickOnSwitchToClassicBtn").click();
				utils.waitTillPagePaceDone();
				logger.info("Clicked On Switch To Classic Button");
				TestListeners.extentTest.get().info("Clicked On Switch To Classic Button");
				segmentAdvertiseBlock();
			} else {
				utils.implicitWait(50);
				logger.info("Switch To Classic Button is not displaped");
				TestListeners.extentTest.get().info("Switch To Classic Button is not displaped");
			}
		} catch (Exception e) {
			utils.implicitWait(50);
			logger.info("Already on Classic Segment Page ");
			TestListeners.extentTest.get().info("Already on Classic Segment Page ");
		}
	}

	public void verifyNewSegmentHomePage(String heading) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newSegmentHomePage.verifyHeadingH1"),
				"Current Page heading i.e. " + heading + " is not visible");
		String pageHeading = utils.getLocator("newSegmentHomePage.verifyHeadingH1").getText();
		Assert.assertEquals(pageHeading, heading, "Heading of the Page is not matching");
		logger.info("Heading of the Page is matching i.e. " + pageHeading);
		TestListeners.extentTest.get().pass("Heading of the Page is matching i.e. " + pageHeading);
	}

	public void clickOnHelpButton() {
		boolean flagDisplay = utils.getLocator("newSegmentHomePage.clickOnHelpButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "Help Button is not displaped");
		utils.getLocator("newSegmentHomePage.clickOnHelpButton").click();
		logger.info("Clicked On Help Button");
		TestListeners.extentTest.get().pass("Clicked On Help Button");
	}

	public void verifyHelpPage() {
		utils.switchToWindow();
//		utils.waitTillPagePaceDone();
		boolean flag = utils.verifyPartOfURLwithoutPagePaceDone("sptest.iamshowcase.com");
		Assert.assertTrue(flag, "Unable to open help page after clicking on help button");
		logger.info("Able to open help page after clicking on help button");
		TestListeners.extentTest.get().pass("Able to open help page after clicking on help buttonn");
		driver.close();
		utils.switchToParentWindow();
	}

	public void clickOnSmartSegmentButton() {
		boolean flagDisplay = utils.getLocator("newSegmentHomePage.clickOnSmartSegmentButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "Smart Segment Button is not displaped");
		utils.getLocator("newSegmentHomePage.clickOnSmartSegmentButton").click();
		logger.info("Clicked On Smart Segment Button");
		TestListeners.extentTest.get().pass("Clicked On Smart Segment Button");
	}

	public void verifySmartSegmentPage() {
		utils.switchToWindow();
		utils.longWaitInSeconds(7);
		String eleClass = utils.getLocator("newSegmentHomePage.verifySmartSegmentPage").getAttribute("class");
		boolean flag = utils.textContains(eleClass, "active");
		Assert.assertTrue(flag, "Unable to open Smart Segment page after clicking on Smart Segment button");
		logger.info("Able to open Smart Segment page after clicking on Smart Segment button");
		TestListeners.extentTest.get().pass("Able to open Smart Segment page after clicking on Smart Segment buttonn");
		utils.longWaitInSeconds(2);
		driver.close();
		utils.switchToParentWindow();
	}

	public void clickOnCreateSegmentButton() {
		boolean flagDisplay = utils.getLocator("newSegmentHomePage.clickOnCreateSegmentButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "Create Segment Button is not displaped");
		utils.getLocator("newSegmentHomePage.clickOnCreateSegmentButton").click();
		logger.info("Clicked On Create Segment Button");
		TestListeners.extentTest.get().pass("Clicked On Create Segment Button");
	}

	public void verifyCreateSegmentPage() {
		utils.switchToWindow();
		utils.longWaitInSeconds(7);
		utils.longWaitInSeconds(2);
		boolean flag = utils.verifyPartOfURL("segment_builders");
		Assert.assertTrue(flag, "Unable to open Create Segment page after clicking on Create Segment button");
		logger.info("Able to open Create Segment page after clicking on Create Segment button");
		TestListeners.extentTest.get()
				.pass("Able to open Create Segment page after clicking on Create Segment buttonn");
	}

	public void clickOnManageTagButton() {
		boolean flagDisplay = utils.getLocator("newSegmentHomePage.clickOnManageTagButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "Manage Tag Button is not displaped");
		utils.getLocator("newSegmentHomePage.clickOnManageTagButton").click();
		logger.info("Clicked On Manage Tag Button");
		TestListeners.extentTest.get().pass("Clicked On Manage Tag Button");
	}

	public void verifyDeleteOrManageTagPage(String displayingOption) {
		utils.longWaitInSeconds(4);
		String displayedHeading = utils.getLocator("newSegmentHomePage.verifyHeadingH3").getText();
		Assert.assertEquals(displayedHeading, displayingOption, displayingOption + " is not displayed");
		logger.info(displayingOption + " is displayed");
		TestListeners.extentTest.get().pass(displayingOption + " is displayed");
	}

	public boolean clickOnCreateTag(String displayingOption, String tagName) {
		verifyDeleteOrManageTagPage(displayingOption);
		utils.getLocator("newSegmentHomePage.createManageTagButton").click();
		logger.info("Manage Tag is clicked");
		TestListeners.extentTest.get().info("Manage Tag is clicked");
		utils.getLocator("newSegmentHomePage.enterCreateTag").click();
		utils.getLocator("newSegmentHomePage.enterCreateTag").sendKeys(tagName);
		utils.longWaitInSeconds(2);
		utils.getLocator("newSegmentHomePage.clickCreateTagButton").click();
		boolean flag = utils.getLocator("newSegmentHomePage.tagValidation").isDisplayed();
		Assert.assertTrue(flag, "Segment Tag  is not Created");
		logger.info("Segment Tag is Created");
		TestListeners.extentTest.get().info("Segment Tag is Created");
		return flag;
	}

	public void closeManageTagOrTag(String optionName) {
		String xpath = utils.getLocatorValue("newSegmentHomePage.closeManageTag").replace("$text", optionName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(3);
		driver.findElement(By.xpath(xpath)).click();
		verifyNewSegmentHomePage("All Segments");
		logger.info("Close " + optionName + " is clicked");
		TestListeners.extentTest.get().pass("Close " + optionName + " is clicked");
	}

	public void clickAuditLogEllipseButton() {
		utils.waitTillElementToBeClickable(utils.getLocator("newSegmentHomePage.clickAuditLogEllipseButton"));
		utils.getLocator("newSegmentHomePage.clickAuditLogEllipseButton").click();
		boolean flagDisplay = utils.getLocator("newSegmentHomePage.segmentHomePageAuditLogs").isDisplayed();
		Assert.assertTrue(flagDisplay, "Audit Logs is not displayed");
		logger.info("Clicked on Ellipse Button near Manage Tag");
		TestListeners.extentTest.get().pass("Clicked on Ellipse Button near Manage Tag");
	}

	public void clickOnMoreFilterButton() {
		boolean flagDisplay = utils.getLocator("newSegmentHomePage.clickOnMoreFilterButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "More Filter Button is not displaped");
		utils.getLocator("newSegmentHomePage.clickOnMoreFilterButton").click();
		String filterText = utils.getLocator("newSegmentHomePage.verifyMoreFilterPanel").getText();
		Assert.assertEquals(filterText, "Filter", "Filter Panel is not visible");
		logger.info("Clicked On More Filter Button");
		TestListeners.extentTest.get().pass("Clicked On More Filter Button");
	}

	public void closeMoreFilterPanel() {
		utils.scrollToElement(driver, utils.getLocator("newSegmentHomePage.closeMoreFilterPanel"));
		utils.getLocator("newSegmentHomePage.closeMoreFilterPanel").click();
		verifyNewSegmentHomePage("All Segments");
		logger.info("Close More Filter Panel is clicked");
		TestListeners.extentTest.get().pass("Close More Filter Panel is clicked");
	}

	public List<String> segmentList() {
		utils.longWaitInSeconds(4);
		List<WebElement> eleList = utils.getLocatorList("newSegmentHomePage.getSegmentNameList");
		List<String> segmentNameList = new ArrayList<String>();
		if (eleList.size() == 0) {
			logger.info("Segments are not present");
			TestListeners.extentTest.get().info("Segments are not present");
//			Assert.fail("Segments are not present");
			return segmentNameList;
		} else {

			for (int i = 0; i < eleList.size(); i++) {
				String text = eleList.get(i).getText();
				segmentNameList.add(text);
			}
			return segmentNameList;
		}
	}

	public void clickSortByFilter() {
		utils.clickByJSExecutor(driver, utils.getLocator("newSegmentHomePage.clickSortByDropDown"));
		utils.waitTillVisibilityOfElement(utils.getLocator("newSegmentHomePage.segmentSortByFilterVisible"),
				"sort by filter open");
		utils.getLocator("newSegmentHomePage.segmentSortByFilterVisible").isDisplayed();
		logger.info("clicked on the sort by filer");
		TestListeners.extentTest.get().info("clicked on the sort by filer");
	}

	public void selectSortByFilterValue(String optionName) {
		clickSortByFilter();
		String xpath = utils.getLocatorValue("newSegmentHomePage.selectSortByValuesList").replace("${optionVal}",
				optionName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("clicked the value -- " + optionName + " from the Sort By filter on Segment Home Page");
		TestListeners.extentTest.get()
				.info("clicked the value -- " + optionName + " from the Sort By filter on Segment Home Page");
	}

	public String searchSegmentonNewSegmentPage(String segmentName, String choice) {
		utils.waitTillNewCamsTableAppear();
		String segName = "";
		WebElement searchSegment = utils.getLocator("newSegmentHomePage.searchSegmentOnNewSegmentPage");

		searchSegment.click();
		searchSegment.clear();
		searchSegment.sendKeys(segmentName);
		searchSegment.sendKeys(Keys.ENTER);
		utils.longWaitInSeconds(4);
		segName = checkSegmentNameAfterSearch();
		switch (choice) {
		case "withAssertion":
			Assert.assertEquals(segName, segmentName, "Searched Segment Name is not matching");
			logger.info("Searched Segment Name is matching i.e. " + segName);
			TestListeners.extentTest.get().pass("Searched Segment Name is matching i.e. " + segName);
			break;
		case "withoutAssertion":
			logger.info("Searched Segment Name is matching i.e. " + segName);
			TestListeners.extentTest.get().info("Searched Segment Name is matching i.e. " + segName);
		}
		return segName;
	}

	public void selectAllSegmentCheckBox() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newSegmentHomePage.clickSegmentSelectAllCheckBox"),
				"check box");
		utils.getLocator("newSegmentHomePage.clickSegmentSelectAllCheckBox").click();
		logger.info("selected all the Segment present on the page");
		TestListeners.extentTest.get().info("selected all the Segment present on the page");
	}

	public boolean segmentCheckBoxListStatus(String boxStatus) {
		boolean statusFlag = true;
		List<WebElement> eleList = utils.getLocatorList("newSegmentHomePage.allCheckBoxSelecedOrNot");
		for (int i = 0; i < eleList.size(); i++) {
			String text = eleList.get(i).getText();
			if (!(text.equalsIgnoreCase(boxStatus))) {
				statusFlag = false;
				break;
			}
		}
		return statusFlag;
	}

	public void clickOnClearSelection() {
		utils.scrollToElement(driver, utils.getLocator("newSegmentHomePage.clearSelection"));
		utils.longWaitInSeconds(2);
		utils.clickByJSExecutor(driver, utils.getLocator("newSegmentHomePage.clearSelection"));
//		utils.getLocator("newSegmentHomePage.clearSelection").click();
		logger.info("clicked on the Clear selection");
		TestListeners.extentTest.get().info("clicked on the Clear selection");
	}

	public void setItemPerPage(String optionName) {
		utils.scrollToElement(driver, utils.getLocator("newSegmentHomePage.clickItemPerPageDropDown"));
		utils.getLocator("newSegmentHomePage.clickItemPerPageDropDown").click();
		String xpath = utils.getLocatorValue("newSegmentHomePage.selectItemPerPageValue").replace("${optionVal}",
				optionName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("clicked the value -- " + optionName + " from the Item Per Page Drop Down");
		TestListeners.extentTest.get().info("clicked the value -- " + optionName + " from the Item Per Page Drop Down");
	}

	public boolean clickOnPreviosOrNextNavigationPage(String choice, String urlPart) {
		String xpath = "";
		boolean flag = false;
		switch (choice) {
		case "Next":
			xpath = utils.getLocatorValue("newSegmentHomePage.clickOnPreviousOrNextPage").replace("$page",
					"chevron_right");
			utils.scrollToElement(driver, driver.findElement(By.xpath(xpath)));
			driver.findElement(By.xpath(xpath)).click();
			flag = utils.verifyPartOfURL(urlPart);
			break;
		case "Previous":
			xpath = utils.getLocatorValue("newSegmentHomePage.clickOnPreviousOrNextPage").replace("$page",
					"chevron_left");
			utils.scrollToElement(driver, driver.findElement(By.xpath(xpath)));
			driver.findElement(By.xpath(xpath)).click();
			flag = utils.verifyPartOfURL(urlPart);
			break;
		}
		return flag;
	}

	public void clickEllipsesOptions() {
		utils.waitTillElementToBeClickable(utils.getLocator("newSegmentHomePage.searchedSegmentEllipsesButton"));
		utils.getLocator("newSegmentHomePage.searchedSegmentEllipsesButton").click();
		logger.info("Clicked on Ellipses Option");
		TestListeners.extentTest.get().info("Clicked on Ellipses Option");
	}

	public List<String> getEllipsesOptionList() {
		clickEllipsesOptions();
		List<WebElement> eleList = utils.getLocatorList("newSegmentHomePage.getEllipsesOptionList");
		List<String> ellipsesOptionNameList = new ArrayList<String>();
		if (eleList.size() == 0) {
			logger.info("Segments Ellipses Options are not present");
			TestListeners.extentTest.get().fail("Segments Ellipses Options are not present");
			Assert.fail("Segments Ellipses Options are not present");
			return ellipsesOptionNameList;
		} else {

			for (int i = 0; i < eleList.size(); i++) {
				String text = eleList.get(i).getText();
				ellipsesOptionNameList.add(text);
			}
			return ellipsesOptionNameList;
		}
	}

	public void clickOptionsInEllipsesButton(String option) {
		// option -> "View details","Create
		// campaign","Edit","Tag","Duplicate","Freeze","Audit log","Schedule export"
		String xpath = utils.getLocatorValue("newSegmentHomePage.clickEllipseButtonOption").replace("$option", option);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.scrollToElement(driver, driver.findElement(By.xpath(xpath)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(7);
		logger.info("click on the " + option + " button from Ellipses Option ");
		TestListeners.extentTest.get().info("click on the " + option + " button from Ellipses Option ");
	}

	public void closeDeleteTab() {
		utils.waitTillElementToBeClickable(utils.getLocator("newSegmentHomePage.closeDeleteTab"));
		utils.getLocator("newSegmentHomePage.closeDeleteTab").click();
		verifyNewSegmentHomePage("All Segments");
		logger.info("Close Delete Popup is clicked");
		TestListeners.extentTest.get().pass("Close Delete Popup is clicked");
	}

	public void segmentAdvertiseBlock() {
		try {
			utils.implicitWait(5);
			if (utils.getLocator("newSegmentHomePage.segmentAdvertiseBlock").isDisplayed()) {
				utils.longWaitInSeconds(1);
				utils.getLocator("newSegmentHomePage.removeSegmentAdvertisement").click();
//				WebElement adElement = utils.getLocator("newSegmentHomePage.segmentAdvertiseBlock");
//				JavascriptExecutor js = (JavascriptExecutor) driver;
//				js.executeScript("arguments[0].remove();", adElement);
				logger.info("Advertisement appeared on the page and it is removed");
				TestListeners.extentTest.get().info("Advertisement appeared on the page and it is removed");
			}
			utils.implicitWait(50);
		} catch (Exception e) {
			utils.implicitWait(50);
			logger.info("Advertisement not appeared on the page");
			TestListeners.extentTest.get().info("Advertisement not appeared on the page");
		}
	}

	public void verifyClassicSegmentPage(String heading) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newSegmentHomePage.verifyHeadingH2"),
				"Current Page heading i.e. " + heading + " is not visible");
		String pageHeading = utils.getLocator("newSegmentHomePage.verifyHeadingH2").getText();
		Assert.assertEquals(pageHeading, heading, "Heading of the Page is not matching");
		logger.info("Heading of the Page is matching i.e. " + pageHeading);
		TestListeners.extentTest.get().pass("Heading of the Page is matching i.e. " + pageHeading);
	}

	public void clickAuditLoAftergEllipseButton() {
		utils.getLocator("newSegmentHomePage.clickAuditLoAftergEllipseButton").isDisplayed();
		utils.getLocator("newSegmentHomePage.clickAuditLoAftergEllipseButton").click();
		logger.info("Clicked on Audit Log");
		TestListeners.extentTest.get().pass("Clicked on Audit Log");
	}

	public void setSegmentBetaName(String segmentName) {
		utils.longwait(5);
		utils.getLocator("newSegmentHomePage.editSegmentNameBtn").isDisplayed();
		utils.getLocator("newSegmentHomePage.editSegmentNameBtn").click();

		// selUtils.longWait(2000);
		Actions action = new Actions(driver);
		selUtils.clearTextUsingJS(utils.getLocator("newSegmentHomePage.segmentNameTextbox"));
		utils.getLocator("newSegmentHomePage.segmentNameTextbox").sendKeys(segmentName);
		selUtils.longWait(2000);
		action.moveToElement(utils.getLocator("newSegmentHomePage.SegmentNameCheckbutton")).click().perform();
		logger.info("Segment name is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment name is set as: " + segmentName);
	}

	public void setSegmentNameNewSegmentPage(String segmentName) {
		clickOnCreateSegmentButton();
		utils.switchToWindow();
		utils.waitTillPagePaceDone();
		segmentAdvertiseBlock();
		WebElement ele = utils.getLocator("segmentBetaPage.editSegmentNameBtn");
		utils.waitTillElementToBeClickable(ele);
		ele.isDisplayed();
		ele.click();

		Actions action = new Actions(driver);
		WebElement txtBox = utils.getLocator("segmentBetaPage.segmentNameTextbox");
		selUtils.clearTextUsingJS(txtBox);
		txtBox.sendKeys(segmentName);
		selUtils.longWait(2000);
		action.moveToElement(utils.getLocator("segmentBetaPage.SegmentNameCheckbutton")).click().perform();
		logger.info("Segment name is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment name is set as: " + segmentName);

	}

	public String checkSegmentNameAfterSearch() {
		String segName = "";
		utils.implicitWait(8);
		try {
			utils.waitTillPagePaceDone();
			utils.waitTillNewCamsTableAppear();
			segName = utils.getLocator("newSegmentHomePage.searchedSegment").getText();
			logger.info("Searched Segment Name is " + segName);
			TestListeners.extentTest.get().info("Searched Segment Name is " + segName);
		} catch (Exception e) {
			logger.info("Searched Segment is not present");
			TestListeners.extentTest.get().info("Searched Segment is not present");
		} finally {
			utils.implicitWait(50);

		}
		return segName;
	}

	public void clickOnYesDelete() {
		utils.longWaitInSeconds(4);
		utils.getLocator("newSegmentHomePage.yesDeleteBtn").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("newSegmentHomePage.yesDeleteBtn"));
//		utils.getLocator("newSegmentHomePage.yesDeleteBtn").click();
		logger.info("Clicked on Yes Delete for deleting Segment");
		TestListeners.extentTest.get().pass("Clicked on Yes Delete for deleting Segment");
		waitTillErrorDisappear();
	}

	public void closeSidePanel() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.closeSidePanel"), "close side panel");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.closeSidePanel"));
		logger.info("close the side panel");
		TestListeners.extentTest.get().info("close the side panel");
	}

	public String getSegmentNameOnSegmentHomePage() {
		String segmentName = null;
		int attempts = 1;
		while (attempts < 10) {
			segmentName = driver
					.findElement(By.xpath(utils.getLocatorValue("newSegmentHomePage.getSegmentNameOnNewSegmentHomePage")
							.replace("$row", Integer.toString(attempts))))
					.getText();

			if ((segmentName.contains("Unnamed Segment") != true)) {
				break;
			} else {
				attempts++;
			}
		}
		return segmentName;
	}

	public void searchTagInTagBox(String tagName) {
		WebElement tagBox = driver.findElement(
				By.xpath(utils.getLocatorValue("newSegmentHomePage.searchTagOnNewSegmentPage").replace("$Tag", "Tag")));
		utils.waitTillElementToBeClickable(tagBox);
		tagBox.click();
		tagBox.clear();
		tagBox.sendKeys(tagName);
		logger.info("Searched tag in tag box on Create Tag Page");
		TestListeners.extentTest.get().info("Searched tag in tag box on Create Tag Page");
	}

	public void selectTagForSegment(String tagName) {
		searchTagInTagBox(tagName);
		String xpath = utils.getLocatorValue("newSegmentHomePage.selectTagForSegment").replace("${tagName}", tagName);
		String text = driver.findElement(By.xpath(xpath)).getText();
		if (!text.equalsIgnoreCase("check_box")) {
			utils.longWaitInSeconds(1);
			driver.findElement(By.xpath(xpath)).click();
			utils.longWaitInSeconds(3);
			text = "";
			text = driver.findElement(By.xpath(xpath)).getText();
			Assert.assertEquals(text, "check_box", "failed to select the tag -- " + tagName);
			logger.info("selected the tag " + tagName);
			TestListeners.extentTest.get().info("selected the tag " + tagName);
		}
	}

	public List<String> clickApplyBtn() {
		List<String> lst = new ArrayList<>();
		utils.waitTillElementToBeClickable(utils.getLocator("newSegmentHomePage.applyBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("newSegmentHomePage.applyBtn"));
		logger.info("clicked on the apply button");
		TestListeners.extentTest.get().info("clicked on the apply button");
		utils.longwait(2000);
		utils.waitTillVisibilityOfElement(utils.getLocator("newSegmentHomePage.updateSegmentTag"),
				"success msg for tag");
		String msg = utils.getLocator("newSegmentHomePage.updateSegmentTag").getText();
		String msg2 = utils.getLocator("newSegmentHomePage.segmentTagAdded").getText();
		lst.add(msg);
		lst.add(msg2);
		utils.longWaitInSeconds(10);
		return lst;
	}

	public void sidePanelDrpDownExpand(String filterName) {
		utils.longwait(800);
		String xpath = utils.getLocatorValue("newSegmentHomePage.filterSidePanelDrpDown").replace("${filterName}",
				filterName);
		utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(2);
		logger.info("expand the drp down -- " + filterName);
		TestListeners.extentTest.get().info("expand the drp down -- " + filterName);
	}

	public void selectDrpDownValFromSidePanel(String optionName, String filterName) {
		sidePanelDrpDownExpand(filterName);
		utils.longWaitInSeconds(2);
		String xpath = utils.getLocatorValue("newSegmentHomePage.selectFilterVal").replace("${filterName}", filterName)
				.replace("${value}", optionName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		// utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
		logger.info("select the value -- " + optionName);
		TestListeners.extentTest.get().info("select the value -- " + optionName);
	}

	public void clickSidePanelApplyBtn() {
		utils.waitTillElementToBeClickable(utils.getLocator("newSegmentHomePage.sidePanelApplyBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("newSegmentHomePage.sidePanelApplyBtn"));
		utils.waitTillPagePaceDone();
		logger.info("clicked on the side panel apply button");
		TestListeners.extentTest.get().info("clicked on the side panel apply button");
	}

	public List<String> searchedSegmentList() {
		utils.waitTillPagePaceDone();
		List<WebElement> eleList = utils.getLocatorList("newSegmentHomePage.getSegmentNameList");
		List<String> campList = new ArrayList<String>();
		if (eleList.size() == 0) {
			logger.info("Segment is not present");
			TestListeners.extentTest.get().fail("Segment is not present");
			return campList;
		} else {

			for (int i = 0; i < eleList.size(); i++) {
				String text = eleList.get(i).getText();
				campList.add(text);
			}
			return campList;
		}
	}

	public void deleteTag(String tagName) {
		// search and delete created tag
		utils.longWaitInSeconds(2);
		utils.waitTillPagePaceDone();
		WebElement manageTagBox = driver.findElement(By.xpath(
				utils.getLocatorValue("newSegmentHomePage.searchTagOnNewSegmentPage").replace("$Tag", "Manage Tags")));
		manageTagBox.clear();
		manageTagBox.sendKeys(tagName);
		utils.getLocator("newSegmentHomePage.manageTagsdotsIcon").click();
		utils.getLocator("newSegmentHomePage.dotsIconDeleteOption").click();
		clickOnYesDelete();
		try {
			String msgXpath = utils.getLocatorValue("newSegmentHomePage.deleteTagMsg");
			utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(msgXpath)), "Toast msg");
			String val = driver.findElement(By.xpath(msgXpath)).getText();
		} catch (Exception e) {
			logger.info("Tag delete msg is not displayed");
			TestListeners.extentTest.get().info("Tag delete msg is not displayed");
		}
		waitTillErrorDisappear();
		String xpath = utils.getLocatorValue("newSegmentHomePage.deleteTagDialogueBox");
		utils.waitTillInVisibilityOfElement(driver.findElement(By.xpath(xpath)), "Close button");

	}

	public void searchSegment(String segmentName) {
		utils.longWaitInSeconds(1);
		WebElement segment = utils.getLocator("newSegmentHomePage.searchSegmentOnNewSegmentPage");
		segment.clear();
		segment.sendKeys(segmentName);
		logger.info("segment searched on new cam home page");
		TestListeners.extentTest.get().pass("segment searched on new cam home page");
		utils.longWaitInSeconds(1);
	}

	public void selectOptionFromMoreOption(String option) {
		utils.getLocator("newSegmentHomePage.threeDotSegment").click();
		WebElement optionName = driver.findElement(
				By.xpath(utils.getLocatorValue("newSegmentHomePage.contextOption").replace("$temp", option)));
		optionName.click();
		utils.longWaitInSeconds(4);
		logger.info("Clicked on Ellipse Button and then clicked on " + option);
		TestListeners.extentTest.get().pass("Clicked on Ellipse Button and then clicked on " + option);
	}

	public void selectCheckBoxOptionInFilterPanel(String checkBoxName) {
		String checkboxNameXpath = utils.getLocatorValue("newSegmentHomePage.checkBoxOptionInFilterPanel")
				.replace("${checkBoxName}", checkBoxName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(checkboxNameXpath)));
		logger.info("checked the flag " + checkBoxName + " in Segment filter panel");
		TestListeners.extentTest.get().info("checked the flag " + checkBoxName + " in Segment filter panel");
	}

	public String getCampaignLinksStatusForSearchedSegment(String segmentName) {
		String status = utils.getLocator("newSegmentHomePage.campaignLinksStatus").getText();
		Assert.assertNotNull(status);
		logger.info("Campaign link status for " + segmentName + " is: " + status);
		TestListeners.extentTest.get().info("Campaign link status for " + segmentName + " is: " + status);
		return status;
	}

	public void searchSegmentAndClick(String segmentName) {
		WebElement searchSegment = utils.getLocator("segmentPage.serchSegment");
		searchSegment.isDisplayed();
		searchSegment.clear();
		searchSegment.sendKeys(segmentName);
		searchSegment.sendKeys(Keys.ENTER);
		String clickSearchedSegment = utils.getLocatorValue("newSegmentHomePage.clickSearchedSegment")
				.replace("$segmentName", segmentName);
		utils.waitTillNewCamsTableAppear();
		WebElement clickSearchedSegmentEle = driver.findElement(By.xpath(clickSearchedSegment));
		clickSearchedSegmentEle.isDisplayed();
		clickSearchedSegmentEle.click();
		logger.info("Searched segments is " + segmentName);
		TestListeners.extentTest.get().info("Searched segments is " + segmentName);
	}

	public String getSidePanelOptionValue(String option) {
		String optionValue = null;
		utils.longWaitInSeconds(2);
		String searchedOptionName = utils.getLocatorValue("newSegmentHomePage.getSidePanelOption").replace("$option",
				option);
		WebElement searchedOptionNameEle = driver.findElement(By.xpath(searchedOptionName));
		utils.waitTillVisibilityOfElement(searchedOptionNameEle, option);
		String searchedOption = utils.getLocatorValue("newSegmentHomePage.getSidePanelOptionValue").replace("$option",
				option);
		WebElement searchedOptionEle = driver.findElement(By.xpath(searchedOption));
		searchedOptionEle.isDisplayed();
		utils.scrollToElement(driver, searchedOptionEle);
		if (option.equalsIgnoreCase("Sample matching guests")) {
			String url = utils.getLocator("newSegmentHomePage.getSidePanelUserValue").getAttribute("href");
			String[] parts = url.split("/");
			optionValue = parts[parts.length - 1];
		} else {
			optionValue = searchedOptionEle.getText();
		}
		logger.info("Value of search option i.e. " + option + " in segment side panel is " + optionValue);
		TestListeners.extentTest.get()
				.info("Value of search option i.e. " + option + " in segment side panel is " + optionValue);
		if (optionValue.isEmpty() || optionValue == null) {
			Assert.fail("Value of search option i.e. " + option + " in segment side pnnel is blank");
		}
		return optionValue;
	}

	public void editOrViewSegmentInsideOnSidePanel(String option) {
		String checkboxNameXpath = utils.getLocatorValue("newSegmentHomePage.editOrViewSegmentInsideOnSidePanel")
				.replace("$option", option);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(checkboxNameXpath)));
		logger.info("Clicked on " + option + " in Segment Side panel");
		TestListeners.extentTest.get().info("Clicked on " + option + " in Segment Side panel");
	}

	public String performanceMatricData(String option) {
		String optionValue = null;
		String searchedOptionName = utils.getLocatorValue("newSegmentHomePage.performanceMatricData").replace("$option",
				option);
		WebElement searchedOptionNameEle = driver.findElement(By.xpath(searchedOptionName));
		utils.waitTillVisibilityOfElement(searchedOptionNameEle, option);
		String searchedOption = utils.getLocatorValue("newSegmentHomePage.performanceMatricData").replace("$option",
				option);
		WebElement searchedOptionEle = driver.findElement(By.xpath(searchedOption));
		searchedOptionEle.isDisplayed();
		optionValue = searchedOptionEle.getText();

		logger.info("Value of search option i.e. " + option + " in segment overview page on performance matric is "
				+ optionValue);
		TestListeners.extentTest.get().info("Value of search option i.e. " + option
				+ " in segment overview page on performance matric is " + optionValue);
		if (optionValue.isEmpty() || optionValue == null) {
			Assert.fail("Value of search option i.e. " + option
					+ " in segment overview page on performance matric is blank");
		}
		return optionValue;

	}

	public void waitTillSegmentSidePanelClickable(String segmentName, int maxAttempts) {
		int attempt = 0;
		boolean isClickable = false;

		while (attempt < maxAttempts && !isClickable) {
			utils.refreshPage();
			searchSegmentAndClick(segmentName);
			try {
				attempt++;
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(200));
				wait.until(ExpectedConditions
						.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'_skeleton-multi')]")));
				logger.info("Segment Side Panel Clickable");
				TestListeners.extentTest.get().info("Segment Side Panel Clickable");
				isClickable = true; // Exit the loop if successful
			} catch (Exception e) {
				logger.info("Attempt " + attempt + " failed: " + e.getMessage());
				TestListeners.extentTest.get().info("Attempt " + attempt + " failed: " + e.getMessage());
				if (attempt == maxAttempts) {
					logger.error("Failed to wait for Segment Side Panel to become clickable after " + maxAttempts
							+ " attempts.");
					TestListeners.extentTest.get()
							.fail("Failed to wait for Segment Side Panel to become clickable after " + maxAttempts
									+ " attempts.");
				}
			}
		}
	}

	public void waitTillErrorDisappear() {
		try {
			String errorDisappear = utils.getLocatorValue("newCamHomePage.exportReportMsg");
			WebElement ele = driver.findElement(By.xpath(errorDisappear));
			utils.waitTillElementDisappear(ele);
			logger.info("Pop-up message after segment deletion is disappeared");
			TestListeners.extentTest.get().info("Pop-up message after segment deletion is disappeared");
		} catch (Exception e) {
			logger.info("Pop-up message after segment deletion is not appeared");
			TestListeners.extentTest.get().info("Pop-up message after segment deletion is not appeared");
		}
	}

	public void customerReachabilityRefresh() {
		utils.getLocator("newSegmentHomePage.customerReachabilityRefresh").click();
		logger.info("Clicked on Customer Reachability Refresh button");
		TestListeners.extentTest.get().info("Clicked on Customer Reachability Refresh button");

	}

}
