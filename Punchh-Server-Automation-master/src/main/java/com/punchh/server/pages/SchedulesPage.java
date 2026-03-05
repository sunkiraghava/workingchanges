package com.punchh.server.pages;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SchedulesPage {

	static Logger logger = LogManager.getLogger(SchedulesPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	private Map<String, By> locators;

	public SchedulesPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);

		locators = utils.getAllByMap();
	}

	public void selectScheduleType(String scheduleName) {
		utils.longWaitInMiliSeconds(500);
		WebElement scheduleTypeDrp = driver.findElement(locators.get("schedulesPage.scheduleTypeDrp"));
		scheduleTypeDrp.click();
		logger.info("Clicked schedule type dropdown");
		List<WebElement> scheduleTypeDrpList = driver.findElements(locators.get("schedulesPage.scheduleTypeDrpList"));
		utils.selectListDrpDwnValue(scheduleTypeDrpList, scheduleName);
	}

	public void findMassCampaignNameandRun(String campaignName) {
		utils.longWaitInSeconds(1);
		List<WebElement> schedulesTableList = driver.findElements(locators.get("schedulesPage.schedulesTableList"));
		for (int i = 0; i < schedulesTableList.size(); i++) {
			if (schedulesTableList.get(i).getText().equals(campaignName)) {
				logger.info("Campaign name is ==> " + schedulesTableList.get(i).getText());
				WebElement runBtn = schedulesTableList.get(i)
						.findElement(By.xpath("following-sibling::td//a[@class='btn btn-info btn-sm']"));
				runBtn.click();
				/*
				 * elements.get(i).findElement(By.xpath(utils.getLocatorValue(
				 * "schedulesPage.massCampRunButton"))) .click();
				 */
				break;
			}
		}
		utils.acceptAlert(driver);
		utils.longWaitInMiliSeconds(200);
		TestListeners.extentTest.get().pass("campaign executed successfully from schedules");
	}

	public void runSchedule() {
		WebElement runBtn = driver.findElement(By.xpath("//a[normalize-space()='Run Now']"));
		runBtn.click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		logger.info("campaign executed successfully from schedules");
		TestListeners.extentTest.get().pass("campaign executed successfully from schedules");
	}

	public void runSidekiqJob(String url, String job) throws InterruptedException {
		driver.navigate().to(url + "/sidekiq/scheduled");
		utils.longWaitInSeconds(3);
		driver.navigate().refresh();
		WebElement anyJobContent = driver.findElement(locators.get("schedulesPage.anyJobContentXpath"));
		anyJobContent.sendKeys(job);
		anyJobContent.sendKeys(Keys.ENTER);
		try {
			WebElement checkAllBox = driver.findElement(locators.get("schedulesPage.checkAllInputBox"));
			if (utils.checkElementPresent(checkAllBox)) {
				checkAllBox.click();
				WebElement addToQueueBox = driver.findElement(locators.get("schedulesPage.addToQueueBox"));
				addToQueueBox.click();
			} else {
				logger.info("Signup Referral Campaign jobs AutomaticReferralWorker did not found in sidq");
			}
		} catch (Exception e) {
			logger.info("Element is not present " + e);
		}
	}

	public void movetoSidekiq(String url) throws InterruptedException {
		String sidekqUrl = url + "/sidekiq/scheduled";
		driver.navigate().to(sidekqUrl);
		logger.info("Navigated to sidekiq");
		TestListeners.extentTest.get().info("Navigated to sidekiq");
		utils.longWaitInSeconds(3);
	}

	public String validateSidekiqJob(String job, String camId, String userId) throws InterruptedException {
		String searchedJobData = "";
		int attempts = 0;
		while (attempts <= 7) {
			try {
				WebElement filter = driver.findElement(By.xpath("//input[@placeholder='Any job content']"));
				filter.sendKeys(job);
				filter.sendKeys(Keys.ENTER);

				searchedJobData = getSearchedJobData(camId, userId);
				if (searchedJobData != null) {
					break;
				} else {
					throw new Exception(searchedJobData);
				}

			} catch (Exception e) {
				logger.info(
						"Job name BulkPrescheduleBusinessUserAnniversaryWorker did not matched in Sidekiq... pooling count is : "
								+ attempts);
				TestListeners.extentTest.get().info(
						"Job name BulkPrescheduleBusinessUserAnniversaryWorker did not matched in sidekiq... pooling count is : "
								+ attempts);
				utils.refreshPageWithCurrentUrl();
				utils.longWaitInSeconds(2);
			}

			attempts++;
		}

		return searchedJobData;
	}

	public String getSearchedJobData(String camId, String userId) {
		String searchedJobData = "";
		boolean flag = false;
		List<WebElement> rowList = driver.findElements(By.xpath("//tbody//tr//td[5]"));
		logger.info("row size :" + rowList.size());
		int i;
		for (i = 0; i < rowList.size(); i++) {
			searchedJobData = rowList.get(i).getText() + ",";
			logger.info("row num :" + i + "->" + searchedJobData);
			if (searchedJobData.contains(camId) && searchedJobData.contains(userId)) {
				logger.info("campaign id and user id found in job : " + searchedJobData);
				TestListeners.extentTest.get().pass("campaign id and user id found in job : " + searchedJobData);
				flag = true;
				break;

			}
		}
		if (flag) {
			i = i + 1;
			WebElement checkbox = driver.findElement(By.xpath("//tbody//tr[" + i + "]//td//input[@type='checkbox']"));
			utils.StaleElementclick(driver, checkbox);
			WebElement addToQueueBtn = driver.findElement(By.xpath("//input[@name='add_to_queue']"));
			utils.StaleElementclick(driver, addToQueueBtn);
			return searchedJobData;
		} else {
			return null;
		}
	}

	public void runJob() {
		WebElement ele = driver.findElement(By.xpath("//input[@class='check_all']"));
		if (utils.checkElementPresent(ele)) {
			ele.click();
			WebElement addToQueueBtn = driver.findElement(By.xpath("//input[@name='add_to_queue']"));
			addToQueueBtn.click();
		}
	}

	public int checkSidekiqJob(String url, String job) throws InterruptedException {
		driver.navigate().to(url + "/sidekiq/scheduled");
		utils.longwait(5);
		driver.navigate().refresh();
		utils.longwait(5);
		WebElement anyJobContent = driver.findElement(locators.get("schedulesPage.anyJobContentXpath"));
		anyJobContent.sendKeys(job);
		anyJobContent.sendKeys(Keys.ENTER);
		String xpath = utils.getLocatorValue("schedulesPage.searchJob").replace("{job}", job);
		List<WebElement> searchJob = driver.findElements(By.xpath(xpath));
		return searchJob.size();
	}

	public void runRecallScheule() {
		WebElement runBtn = driver.findElement(locators.get("schedulesPage.runBtn"));
		runBtn.click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		logger.info("Clicked on RUN NOW in schedule");
		TestListeners.extentTest.get().pass("Clicked on RUN NOW in schedule");
	}

	public boolean isScheduleTypeExist(String scheduleName) {
		WebElement scheduleTypeDrp = driver.findElement(locators.get("schedulesPage.scheduleTypeDrp"));
		utils.waitTillElementToBeClickable(scheduleTypeDrp);
		scheduleTypeDrp.click();
		logger.info("Clicked schedule type dropdown");
		List<WebElement> scheduleTypeDrpList = driver.findElements(locators.get("schedulesPage.scheduleTypeDrpList"));
		for (WebElement wEle : scheduleTypeDrpList) {
			String text = wEle.getText();
			if (text.equalsIgnoreCase(scheduleName)) {
				logger.info(scheduleName + " schedule is exist");
				TestListeners.extentTest.get().pass(scheduleName + " schedule is exist");
				return true;
			}
		}
		logger.info(scheduleName + " schedule is not exist");
		TestListeners.extentTest.get().info(scheduleName + " schedule is not exist");
		return false;
	}

	public void updateSchedule() {
		WebElement updateSchedule = driver.findElement(locators.get("schedulesPage.updateSchedule"));
		updateSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void selectDayOfMonth(String date) {
		if (date == "") {
			date = CreateDateTime.getCurrentDateOnly();
		}
		if (date.charAt(0) == '0') {
			date = Character.toString(date.charAt(1));
		}
		WebElement clickDayOfMonth = driver.findElement(locators.get("schedulesPage.clickDayOfMonth"));
		clickDayOfMonth.click();
		WebElement enterDayOfMonth = driver.findElement(locators.get("schedulesPage.enterDayOfMonth"));
		enterDayOfMonth.click();
		enterDayOfMonth.sendKeys(date);
		enterDayOfMonth.sendKeys(Keys.ENTER);
		logger.info("Day of Week is Selected as :- " + date);
		TestListeners.extentTest.get().info("Day of Week is Selected as :- " + date);
	}

	public void selectDayOfWeek(String day) {
		if (day == "") {
			day = CreateDateTime.getCurrentWeekDay();
		}
		WebElement clickDayOfWeek = driver.findElement(locators.get("schedulesPage.clickDayOfWeek"));
		clickDayOfWeek.click();
		List<WebElement> daysOfWeekList = driver.findElements(locators.get("schedulesPage.daysOfWeekList"));
		utils.selectListDrpDwnValue(daysOfWeekList, day);
		logger.info("Day of Week is Selected as :- " + day);
		TestListeners.extentTest.get().info("Day of Week is Selected as :- " + day);
	}

	public void validateSuccessMessage(String message) {
		WebElement successMessage = driver.findElement(locators.get("schedulesPage.successMessage"));
		String text = successMessage.getText();
		Assert.assertEquals(text, message, "Success message for Schedules is not matched");
		logger.info("Success message for Schedules is matched which is :- " + text);
		TestListeners.extentTest.get().info("Success message for Schedules is matched which is :- " + text);
	}

	public void createScheduleWOWorMOM(String scheduleHREF, String dateOfMonth, String dayOfWeek, String message) {
		utils.waitTillPagePaceDone();
		String schedule = utils.getLocatorValue("schedulesPage.createWOWschedule").replace("$link", scheduleHREF);
		WebElement createWOWschedule = driver.findElement(By.xpath(schedule));
		boolean flag = createWOWschedule.isDisplayed();
		if (flag) {
			createWOWschedule.click();
			utils.waitTillPagePaceDone();
			switch (scheduleHREF) {
			case "mom_report_schedules":
				selectDayOfMonth(dateOfMonth);
				break;
			case "wow_report_schedules":
				selectDayOfWeek(dayOfWeek);
				break;
			}
			pageObj.signupcampaignpage().setStartDate();
			WebElement clickStartTime = driver.findElement(locators.get("schedulesPage.clickStartTime"));
			clickStartTime.click();
			clickStartTime.click();
			updateSchedule();
			utils.waitTillPagePaceDone();
			validateSuccessMessage(message);
		} else {
			logger.info("Schedule is not present");
			TestListeners.extentTest.get().fail("Schedule is not present");
		}
	}

	public void runChallengeCampaignSchedule(String campaignName) {
		try {
			String xpath = utils.getLocatorValue("schedulesPage.challengeCampRunBtn").replace("$temp", campaignName);
			WebElement challengeCampRunBtn = driver.findElement(By.xpath(xpath));
			challengeCampRunBtn.isDisplayed();
			challengeCampRunBtn.click();
			utils.acceptAlert(driver);
			utils.waitTillPagePaceDone();
			logger.info("campaign executed successfully from schedules");
			TestListeners.extentTest.get().info("campaign executed successfully from schedules");
		} catch (Exception e) {
			logger.error("Error in running mass campaign schedule" + e);
			TestListeners.extentTest.get().fail("Error in running mass campaign schedule" + e);
		}
	}

	public void createEmployeeReviewSchedule(String frequency, String scheduleName, String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Employee Review Schedules");
		WebElement createEmployeeReviewSchedule = driver.findElement(locators.get("schedulesPage.createEmployeeReviewSchedule"));
		createEmployeeReviewSchedule.click();
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		WebElement frequencyDrp = driver.findElement(locators.get("schedulesPage.frequencyDrp"));
		frequencyDrp.isDisplayed();
		frequencyDrp.click();
		WebElement frequencyDrpList = driver.findElement(locators.get("schedulesPage.frequencyDrpList"));
		frequencyDrpList.isDisplayed();
		List<WebElement> frequencyDrpListItems = driver.findElements(locators.get("schedulesPage.frequencyDrpList"));
		utils.selectListDrpDwnValue(frequencyDrpListItems, frequency);
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createLocationSummarySchedule(String frequency, String scheduleName, String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Location Summary Export Schedules");
		WebElement createLocationSummarySchedule = driver.findElement(locators.get("schedulesPage.createLocationSummarySchedule"));
		createLocationSummarySchedule.click();
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		WebElement frequencyDrp = driver.findElement(locators.get("schedulesPage.frequencyDrp"));
		frequencyDrp.isDisplayed();
		frequencyDrp.click();
		WebElement frequencyDrpList = driver.findElement(locators.get("schedulesPage.frequencyDrpList"));
		frequencyDrpList.isDisplayed();
		List<WebElement> frequencyDrpListItems = driver.findElements(locators.get("schedulesPage.frequencyDrpList"));
		utils.selectListDrpDwnValue(frequencyDrpListItems, frequency);
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createSurveySchedule(String survey, String scheduleName, String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Survey Export Schedules");
		WebElement createSurveySchedule = driver.findElement(locators.get("schedulesPage.createSurveySchedule"));
		createSurveySchedule.click();
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		WebElement surveyDrp = driver.findElement(locators.get("schedulesPage.surveyDrp"));
		surveyDrp.isDisplayed();
		surveyDrp.click();
		WebElement surveyDrpList = driver.findElement(locators.get("schedulesPage.surveyDrpList"));
		surveyDrpList.isDisplayed();
		List<WebElement> surveyDrpListItems = driver.findElements(locators.get("schedulesPage.surveyDrpList"));
		utils.selectListDrpDwnValue(surveyDrpListItems, survey);
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createChallengeExportSchedule(String challengeCampaign, String scheduleName, String frequency,
			String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Challenge Export Schedules");
		WebElement createChallengeExportSchedule = driver.findElement(locators.get("schedulesPage.createChallengeExportSchedule"));
		createChallengeExportSchedule.click();
		WebElement challengeDrp = driver.findElement(locators.get("schedulesPage.challengeDrp"));
		challengeDrp.isDisplayed();
		challengeDrp.click();
		WebElement challengeDrpList = driver.findElement(locators.get("schedulesPage.challengeDrpList"));
		challengeDrpList.isDisplayed();
		List<WebElement> challengeDrpListItems = driver.findElements(locators.get("schedulesPage.challengeDrpList"));
		utils.selectListDrpDwnValue(challengeDrpListItems, challengeCampaign);
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		WebElement frequencyDrp = driver.findElement(locators.get("schedulesPage.frequencyDrp"));
		frequencyDrp.isDisplayed();
		frequencyDrp.click();
		WebElement frequencyDrpList = driver.findElement(locators.get("schedulesPage.frequencyDrpList"));
		frequencyDrpList.isDisplayed();
		List<WebElement> frequencyDrpListItems = driver.findElements(locators.get("schedulesPage.frequencyDrpList"));
		utils.selectListDrpDwnValue(frequencyDrpListItems, frequency);
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createSvsPaymentReconciliationSchedule(String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("SVS Payment Reconciliation Schedule");
		WebElement createSvsPaymentReconciliationSchedule = driver.findElement(locators.get("schedulesPage.createSvsPaymentReconciliationSchedule"));
		createSvsPaymentReconciliationSchedule.click();
		String date = CreateDateTime.getCurrentDateOnly();
		WebElement startDate = driver.findElement(locators.get("schedulesPage.startDate"));
		startDate.isDisplayed();
		startDate.click();
		startDate.sendKeys(date);
		startDate.sendKeys(Keys.ENTER);
		WebElement clickStartTime = driver.findElement(locators.get("schedulesPage.clickStartTime"));
		clickStartTime.isDisplayed();
		clickStartTime.click();
		WebElement selectrandomTime = driver.findElement(locators.get("schedulesPage.selectrandomTime"));
		selectrandomTime.click();
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createWoWSchedule(String dayOfWeek) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Week on Week Report Schedule");
		WebElement createWowSchedule = driver.findElement(locators.get("schedulesPage.createWowSchedule"));
		createWowSchedule.click();
		selectDayOfWeek(dayOfWeek);
		String date = CreateDateTime.getCurrentDateOnly();
		WebElement startDate = driver.findElement(locators.get("schedulesPage.startDate"));
		startDate.isDisplayed();
		startDate.click();
		startDate.sendKeys(date);
		startDate.sendKeys(Keys.ENTER);
		WebElement clickStartTime = driver.findElement(locators.get("schedulesPage.clickStartTime"));
		clickStartTime.isDisplayed();
		clickStartTime.click();
		WebElement selectrandomTime = driver.findElement(locators.get("schedulesPage.selectrandomTime"));
		selectrandomTime.click();
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createFranchiseSchedule(String dateOfMonth, String scheduleName, String franchise, String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Franchisee Report Schedules");
		WebElement createFranchiseSchedule = driver.findElement(locators.get("schedulesPage.createFranchiseSchedule"));
		createFranchiseSchedule.click();
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		selectDayOfMonth(dateOfMonth);
		String date = CreateDateTime.getCurrentDateOnly();
		WebElement startDate = driver.findElement(locators.get("schedulesPage.startDate"));
		startDate.isDisplayed();
		startDate.click();
		startDate.sendKeys(date);
		startDate.sendKeys(Keys.ENTER);
		WebElement franchiseDrp = driver.findElement(locators.get("schedulesPage.franchiseDrp"));
		franchiseDrp.isDisplayed();
		franchiseDrp.click();
		WebElement franchiseDrpList = driver.findElement(locators.get("schedulesPage.franchiseDrpList"));
		franchiseDrpList.isDisplayed();
		List<WebElement> franchiseDrpListItems = driver.findElements(locators.get("schedulesPage.franchiseDrpList"));
		utils.selectListDrpDwnValue(franchiseDrpListItems, franchise);
		WebElement franchiseEmail = driver.findElement(locators.get("schedulesPage.franchiseEmail"));
		franchiseEmail.isDisplayed();
		franchiseEmail.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createSocialCauseSchedule(String socialCauseCampaign, String scheduleName, String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Social Cause Export Schedules");
		WebElement createSocialCauseSchedule = driver.findElement(locators.get("schedulesPage.createSocialCauseSchedule"));
		createSocialCauseSchedule.click();
		WebElement socialDrp = driver.findElement(locators.get("schedulesPage.socialDrp"));
		socialDrp.isDisplayed();
		socialDrp.click();
		WebElement socialDrpList = driver.findElement(locators.get("schedulesPage.socialDrpList"));
		socialDrpList.isDisplayed();
		List<WebElement> socialDrpListItems = driver.findElements(locators.get("schedulesPage.socialDrpList"));
		utils.selectListDrpDwnValue(socialDrpListItems, socialCauseCampaign);
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createRedemptionStatsSchedule(String scheduleName, String frequency, String Email) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Redemption Stats Export Schedules");
		WebElement createRedemptionStatsSchedule = driver.findElement(locators.get("schedulesPage.createRedemptionStatsSchedule"));
		createRedemptionStatsSchedule.click();
		WebElement nameBox = driver.findElement(locators.get("schedulesPage.nameBox"));
		nameBox.isDisplayed();
		nameBox.clear();
		nameBox.sendKeys(scheduleName);
		WebElement frequencyDrp = driver.findElement(locators.get("schedulesPage.frequencyDrp"));
		frequencyDrp.isDisplayed();
		frequencyDrp.click();
		WebElement frequencyDrpList = driver.findElement(locators.get("schedulesPage.frequencyDrpList"));
		frequencyDrpList.isDisplayed();
		List<WebElement> frequencyDrpListItems = driver.findElements(locators.get("schedulesPage.frequencyDrpList"));
		utils.selectListDrpDwnValue(frequencyDrpListItems, frequency);
		WebElement emailBox = driver.findElement(locators.get("schedulesPage.emailBox"));
		emailBox.isDisplayed();
		emailBox.sendKeys(Email);
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public void createLocationscoreboardSchedule(String dayOfWeek) {
		utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Location Scoreboard Schedule");
		WebElement createLocationScoreboardSchedule = driver.findElement(locators.get("schedulesPage.createLocationScoreboardSchedule"));
		createLocationScoreboardSchedule.click();
		WebElement clickDayOfWeek = driver.findElement(locators.get("schedulesPage.clickDayOfWeek"));
		clickDayOfWeek.isDisplayed();
		selectDayOfWeek(dayOfWeek);
		WebElement clickStartTime = driver.findElement(locators.get("schedulesPage.clickStartTime"));
		clickStartTime.isDisplayed();
		clickStartTime.click();
		WebElement selectrandomTime = driver.findElement(locators.get("schedulesPage.selectrandomTime"));
		selectrandomTime.click();
		WebElement saveSchedule = driver.findElement(locators.get("schedulesPage.saveSchedule"));
		saveSchedule.click();
		logger.info("Schedule is Updated");
		TestListeners.extentTest.get().info("Schedule is Updated");
	}

	public boolean isCampaignExistsInSchedule(String campaignName) {
		utils.longWaitInSeconds(1);
		List<WebElement> schedulesTableList = driver.findElements(locators.get("schedulesPage.schedulesTableList"));
		for (WebElement wEle : schedulesTableList) {
			String text = wEle.getText();
			if (text.equalsIgnoreCase(campaignName)) {
				logger.info(campaignName + " exists in schedule");
				TestListeners.extentTest.get().pass(campaignName + " exists in schedule");
				return true;
			}
		}
		logger.info(campaignName + " campaign does not exist in schedule");
		TestListeners.extentTest.get().info(campaignName + " campaign does not exist in schedule");
		return false;
	}

	public int errorInSchedule() {
		List<WebElement> deletedCampaignError = driver.findElements(locators.get("schedulesPage.deletedCampaignError"));
		return deletedCampaignError.size();
	}

	public boolean clickOnScheduledFunctionalityName(String functionality, String camOrSegmentName) {
		String xpath = utils.getLocatorValue("schedulesPage.clickOnScheduledFunctionalityName")
				.replace("$functionalityName", camOrSegmentName);
		WebElement clickOnScheduledFunctionalityName = driver.findElement(By.xpath(xpath));
		clickOnScheduledFunctionalityName.click();
		utils.waitTillPagePaceDone();
		boolean displayflag = false;
		switch (functionality.toLowerCase()) {
		case "campaign":
			WebElement verifyCampaignHeading = driver.findElement(By.xpath(utils.getLocatorValue("schedulesPage.verifyCampaignHeading")
					.replace("$campaignName", camOrSegmentName)));
			displayflag = verifyCampaignHeading.isDisplayed();
			break;

		case "segment":
			WebElement verifySegmentHeading = driver.findElement(By.xpath(utils.getLocatorValue("schedulesPage.verifySegmentHeading")
					.replace("$segmentName", camOrSegmentName)));
			displayflag = verifySegmentHeading.isDisplayed();
			break;
		}
		return displayflag;
	}

	public void clickOnEditSchedule() {
		WebElement editScheduleBtn = driver.findElement(locators.get("schedulesPage.editScheduleBtn"));
		editScheduleBtn.click();
		logger.info("Clicked on Edit Schedule button");
		TestListeners.extentTest.get().info("Clicked on Edit Schedule button");
	}

	public void modifyURLAndRedirect() {
		String currentUrl = driver.getCurrentUrl();
		String modifiedUrl = currentUrl.replaceAll("/\\d+/edit$", "/new");
		driver.navigate().to(modifiedUrl);
		utils.waitTillPagePaceDone();
		logger.info("Modified URL: " + modifiedUrl);
		TestListeners.extentTest.get().info("Modified URL: " + modifiedUrl);
	}

	public void createSchedule() {
		pageObj.signupcampaignpage().setStartDate();
		WebElement clickStartTime = driver.findElement(locators.get("schedulesPage.clickStartTime"));
		clickStartTime.click();
		WebElement timeOnSchedulePage = driver.findElement(
				By.xpath(utils.getLocatorValue("schedulesPage.timeOnSchedulePage").replace("$time", "11:00 PM")));
		timeOnSchedulePage.click();
		WebElement saveScheduleButton = driver.findElement(locators.get("schedulePage.saveScheduleButton"));
		saveScheduleButton.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked on Save Schedule button");
		TestListeners.extentTest.get().info("Clicked on Save Schedule button");
	}

	public String getErrorMsgOnSchedulePage() {
		WebElement campaignScheduleErrorMessage = driver.findElement(locators.get("campaignSplitPage.campaignScheduleErrorMessage"));
		String errorMessage = campaignScheduleErrorMessage.getText();
		logger.info("Error message on schedule page is: " + errorMessage);
		TestListeners.extentTest.get().info("Error message on schedule page is: " + errorMessage);
		return errorMessage;
	}

	public boolean deactivateScheduleIfActivated() {
		boolean isDeactivated = true;
		List<WebElement> dotsIconActivateOption = driver.findElements(locators.get("campaignsPage.dotsIconActivateOption"));
		if (dotsIconActivateOption.size() == 0) {
			WebElement deactivateUser = driver.findElement(locators.get("guestTimeLine.deactivateUser"));
			deactivateUser.click();
			utils.acceptAlert(driver);
			isDeactivated = false;
		}
		logger.info("Clicked on Deactivate Schedule button");
		TestListeners.extentTest.get().info("Clicked on Deactivate Schedule button");
		return isDeactivated;
	}

	public boolean activateScheduleIfdeactivated() {
		boolean isActivated = true;
		List<WebElement> deactivateUserList = driver.findElements(locators.get("guestTimeLine.deactivateUser"));
		if (deactivateUserList.size() == 0) {
			WebElement dotsIconActivateOption = driver.findElement(locators.get("campaignsPage.dotsIconActivateOption"));
			dotsIconActivateOption.click();
			utils.acceptAlert(driver);
			isActivated = false;
		}
		logger.info("Clicked on Activate Schedule button");
		TestListeners.extentTest.get().info("Clicked on Activate Schedule button");
		return isActivated;
	}
}
