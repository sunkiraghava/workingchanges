package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class SidekiqPage {

	private WebDriver driver;
	static Logger logger = LogManager.getLogger(SidekiqPage.class);
	Utilities utils;
	Properties prop;
	PageObj pageObj;

	public SidekiqPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		prop = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);

	}

	public void navigateToSidekiq(String url) {
		driver.navigate().to(url + "/sidekiq");
		utils.waitTillPagePaceDone();
		logger.info("Navigated to Sidekiq");
	}

	public void navigateToSidekiqScheduled(String url) {
		navigateToSidekiq(url);
		driver.navigate().to(url + "/sidekiq/scheduled");
		utils.waitTillPagePaceDone();
		logger.info("Navigated to Sidekiq schedules page");
	}

	public void filterByJob(String job) {
		utils.getLocator("sidekiqPage.filterTxtBox").clear();
		utils.getLocator("sidekiqPage.filterTxtBox").sendKeys(job);
		utils.getLocator("sidekiqPage.filterTxtBox").sendKeys(Keys.ENTER);
		utils.longWaitInSeconds(5);
		utils.logit("Filtered Sidekiq jobs by job: " + job);
	}

	public void checkSelectAllJobsCheckBox() {
		WebElement element = null;
		try{
			element = utils.getLocator("sidekiqPage.checkAllChkBox");
			element.click();
		} catch (StaleElementReferenceException e) {
			logger.info("StaleElementReferenceException caught, retrying to click the checkbox.");
			element = utils.getLocator("sidekiqPage.checkAllChkBox");
			utils.StaleElementclick(driver, element);
		}
		logger.info("Selected all displayed Sidekiq jobs.");
	}

	public void navigateToSidekiqFailures(String url) {
		navigateToSidekiq(url);
		driver.navigate().to(url + "/sidekiq/failures");
		logger.info("Navigated to Sidekiq failures page");
		TestListeners.extentTest.get().info("Navigated to Sidekiq failures page");
	}

	public void clickAddToQueue() {
		try {
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
					utils.getLocator("sidekiqPage.addToQueueBtn"));
			utils.getLocator("sidekiqPage.addToQueueBtn").click();
			logger.info("Clicked 'Add to Queue' button.");
			//Wait for job to process
			utils.longWaitInSeconds(5);
		} catch (Exception e) {
			logger.error("Error occurred during clicking 'Add to Queue' button: " + e.getMessage());
		}
	}

	public String getUTCTimeOfJob(String job) {
		String dateTime = "";
		String xpath = utils.getLocatorValue("sidekiqPage.jobTime").replace("${job}", job);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "");
		dateTime = driver.findElement(By.xpath(xpath)).getAttribute("datetime");
		return dateTime;
	}

	public String getConvertedTimeHour(String userId, String job, String timezone, CreateDateTime createDateTime,
			String baseUrl) {
		int attempts = 0;
		List<WebElement> searchedJobElement;
		String searchedJobXpath = utils.getLocatorValue("sidekiqPage.jobTime").replace("${job}", job);
		while (attempts < 5) {
			try {
				WebElement filterTxtBox = utils.getLocator("sidekiqPage.filterTxtBox");
				utils.waitTillElementToBeClickable(filterTxtBox);
				filterTxtBox.click();
				filterByJob(userId);
				searchedJobElement = driver.findElements(By.xpath(searchedJobXpath));
				if (searchedJobElement.size() == 1) {
					logger.info("Searched job '" + job + "' is found for user ID: " + userId);
					TestListeners.extentTest.get().info("Searched job '" + job + "' is found for user ID: " + userId);
					break;
				}
			} catch (Exception e) {
				logger.error("Error occurred while filtering by job: " + e);
				TestListeners.extentTest.get().info("Error occurred while filtering by job: " + e);
			}
			attempts++;
			// Navigate to the scheduled jobs page again and wait before retrying
			navigateToSidekiqScheduled(baseUrl);
			utils.longWaitInSeconds(3);
			logger.info("Searched job '" + job + "' is not found for user ID '" + userId + "' in attempt " + attempts);
			TestListeners.extentTest.get().info(
					"Searched job '" + job + "' is not found for user ID '" + userId + "' in attempt " + attempts);
		}
		// Get the UTC time of the job and convert it to the user's timezone
		String[] utcTime = getUTCTimeOfJob(job).split("T");
		String[] hour = utcTime[1].split(":");
		String actualTime = createDateTime.convertUtcIn12HourFormat(hour[0] + ":" + hour[1], timezone);
		String actualHour = actualTime.split(":")[0];
		logger.info("Actual hour for User ID " + userId + " is: " + actualHour);
		TestListeners.extentTest.get().info("Actual hour for User ID " + userId + " is: " + actualHour);
		return actualHour;
	}

	public String failureErrorMessage() {
		String message = utils.getLocator("sidekiqPage.errorMessage").getText();
		logger.info("Collected error message text ");
		TestListeners.extentTest.get().info("Collected error message text ");
		return message;
	}

//	This Code is Used To check Number of Jobs in any Queue in Sidekiq (like Scheduled, Enqueued)
	public boolean checkSidekiqJobs(String url, String queueName, int range) {
		boolean flag = false;
		String xpath;
		String numbersOfJobs;
		Long numOfJobs;
		switch (queueName) {
		case "Scheduled":
			driver.navigate().to(url + prop.getProperty("sidekiqScheduledJobsLink")); // "/sidekiq/scheduled");
			utils.longWaitInSeconds(4);
			driver.navigate().refresh();
			xpath = utils.getLocatorValue("sidekiqPage.jobsInSidekiq").replace("${queueName}", queueName);
			numbersOfJobs = driver.findElement(By.xpath(xpath)).getText();
			if (numbersOfJobs.contains(",")) {
				numbersOfJobs = numbersOfJobs.replace(",", "");
			}
			numOfJobs = Long.parseLong(numbersOfJobs);
			if (range >= numOfJobs) {
				flag = true;
			}
			logger.info("number of Scheduled jobs i.e. " + numOfJobs + " present is Sidekiq jobs " + queueName
					+ " is  under range = " + flag);
			TestListeners.extentTest.get().info("number of Scheduled jobs i.e. " + numOfJobs
					+ " present is Sidekiq jobs " + queueName + " is  under range = " + flag);
			break;

		case "Enqueued":
			driver.navigate().to(url + prop.getProperty("sidekiqEnqueuedJobsLink")); // "/sidekiq/queues");
			utils.longWaitInSeconds(4);
			driver.navigate().refresh();
			xpath = utils.getLocatorValue("sidekiqPage.jobsInSidekiq").replace("${queueName}", queueName);
			numbersOfJobs = driver.findElement(By.xpath(xpath)).getText();
			if (numbersOfJobs.contains(",")) {
				numbersOfJobs = numbersOfJobs.replace(",", "");
			}
			numOfJobs = Long.parseLong(numbersOfJobs);
			if (range >= numOfJobs) {
				flag = true;
			}
			logger.info("number of Enqueued jobs i.e. " + numOfJobs + " present is Sidekiq jobs " + queueName
					+ " is  under range = " + flag);
			TestListeners.extentTest.get().info("number of Enqueued jobs i.e. " + numOfJobs
					+ " present is Sidekiq jobs " + queueName + " is  under range = " + flag);
			break;
		}
		return flag;

	}

	public int checkSidekiqJobWithPolling(String url, String job) throws InterruptedException {
		List<WebElement> ele = new ArrayList<WebElement>();
		int attempts = 0;
		while (attempts < 10) {
			driver.navigate().to(url + "/sidekiq/scheduled");
			utils.longwait(5);
			driver.navigate().refresh();
			utils.longwait(5);
			utils.getLocator("schedulesPage.anyJobContentXpath").sendKeys(job);
			utils.getLocator("schedulesPage.anyJobContentXpath").sendKeys(Keys.ENTER);
			String xpath = utils.getLocatorValue("schedulesPage.searchJob").replace("{job}", job);
			ele = driver.findElements(By.xpath(xpath));
			if (ele.size() >= 1) {
				break;
			} else {
				driver.navigate().to(url + "/sidekiq/queues");
				utils.longwait(2);
			}
			attempts++;
		}
		return ele.size();
	}

	public void listAllBusinessesDrpDownInFeatureRollouts(String flagname, String dataType, String value,
			String option) {
		utils.getLocator("featureRolloutsPage.businessFlagName").isDisplayed();
		utils.getLocator("featureRolloutsPage.businessFlagName").sendKeys(flagname);
		utils.getLocator("featureRolloutsPage.dataTypeOfflagNameDropdown").isDisplayed();
		utils.getLocator("featureRolloutsPage.dataTypeOfflagNameDropdown").click();
		String xpath = utils.getLocatorValue("featureRolloutsPage.dataTypeOfflagName").replace("$datatype", dataType);
		WebElement el = driver.findElement(By.xpath(xpath));
		el.isDisplayed();
		el.click();
		utils.getLocator("featureRolloutsPage.value").isDisplayed();
		utils.getLocator("featureRolloutsPage.value").sendKeys(value);
		WebElement drpLocation = utils.getLocator("featureRolloutsPage.listAllBusinessesDrpDown");
		utils.selectDrpDwnValue(drpLocation, option);
		utils.getLocator("featureRolloutsPage.saveBtn").click();
		logger.info("Buisness selected from dropdown : " + option);
		TestListeners.extentTest.get().info("Buisness selected from dropdown : " + option);
	}

	public void sidekiqCheck(String baseUrl) {
		int expectedSidekiqLimitSchedule = Integer.parseInt(prop.getProperty("expectedSidekiqScheduledLimit"));
		int expectedSidekiqLimitEnqueued = Integer.parseInt(prop.getProperty("expectedSidekiqEnqueuedLimit"));
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		boolean sidekiqIndicatorScheduledFlag = checkSidekiqJobs(baseUrl, "Scheduled", expectedSidekiqLimitSchedule);

		if (!sidekiqIndicatorScheduledFlag) {
			logger.info("Sidekiq jobs is greater than our excepted limit i.e. " + expectedSidekiqLimitSchedule
					+ " so skipping Data Export Test Case");
			TestListeners.extentTest.get().info("Sidekiq jobs is greater than our excepted limit i.e. "
					+ expectedSidekiqLimitSchedule + " so skipping Data Export Test Case");
			throw new SkipException("Sidekiq Jobs is more than expected i.e. " + expectedSidekiqLimitSchedule);
		}

		boolean sidekiqIndicatorEnqueuedFlag = checkSidekiqJobs(baseUrl, "Enqueued", expectedSidekiqLimitEnqueued);
		if (!sidekiqIndicatorEnqueuedFlag) {
			logger.info("Sidekiq jobs is greater than our excepted limit i.e. " + expectedSidekiqLimitEnqueued
					+ " so skipping Data Export Test Case");
			TestListeners.extentTest.get().info("Sidekiq jobs is greater than our excepted limit i.e. "
					+ expectedSidekiqLimitEnqueued + " so skipping Data Export Test Case");
			throw new SkipException("Sidekiq Jobs is more than expected i.e. " + expectedSidekiqLimitEnqueued);
		}

	}
	
	// It checks for Sidekiq Enqueued and Scheduled jobs with custom limits
	public void sidekiqCheckWithCustomLimits(String baseUrl, String customScheduledLimit, String customEnqueuedLimit) {
		if (!customScheduledLimit.isEmpty()) {
			int scheduledLimit = Integer.parseInt(customScheduledLimit);
			boolean scheduledFlag = checkSidekiqJobs(baseUrl, "Scheduled", scheduledLimit);
			if (!scheduledFlag) {
				logger.info("Scheduled Sidekiq jobs exceed the custom limit of " + scheduledLimit + ". Skipping test.");
				TestListeners.extentTest.get().info(
						"Scheduled Sidekiq jobs exceed the custom limit of " + scheduledLimit + ". Skipping test.");
				throw new SkipException("Scheduled Sidekiq jobs exceed the custom limit of " + scheduledLimit);
			}
		}
		if (!customEnqueuedLimit.isEmpty()) {
			int enqueuedLimit = Integer.parseInt(customEnqueuedLimit);
			boolean enqueuedFlag = checkSidekiqJobs(baseUrl, "Enqueued", enqueuedLimit);
			if (!enqueuedFlag) {
				logger.info("Enqueued Sidekiq jobs exceed the custom limit of " + enqueuedLimit + ". Skipping test.");
				TestListeners.extentTest.get()
						.info("Enqueued Sidekiq jobs exceed the custom limit of " + enqueuedLimit + ". Skipping test.");
				throw new SkipException("Enqueued Sidekiq jobs exceed the custom limit of " + enqueuedLimit);
			}
		}
	}

	public void runSidekiqJobsBasedOnID(String url, String jobName, List<String> idList) throws InterruptedException {

		int listSize = checkSidekiqJobWithPolling(url, jobName);
		boolean flag = false;
		if (listSize != 0) {
			for (String strID : idList) {
				try {
					String checkBoxXpath = utils.getLocatorValue("schedulesPage.sidekiqJobCheckBox")
							.replace("$idToClick", strID);
					driver.findElement(By.xpath(checkBoxXpath)).click();

					utils.getLocator("cockpitPage.sidekiqAddToQueue").click();
					flag = true;
					logger.info("Worker jobs " + jobName + "  found in sidekiq and triggered for the id " + strID);
					TestListeners.extentTest.get()
							.info("Worker jobs " + jobName + "  found in sidekiq and triggered for the id " + strID);
					break;
				} catch (Exception e) {
					logger.info(
							"Worker jobs " + jobName + " NOT found in sidekiq and NOT triggered for the id " + strID);
					TestListeners.extentTest.get().info(
							"Worker jobs " + jobName + " NOT found in sidekiq and NOT triggered for the id " + strID);
				}

			}

		} else {
			logger.info(jobName + " job is not found");
			TestListeners.extentTest.get().info(jobName + " job is not found");

		}
		Assert.assertTrue(flag, " Job is not found for the given ID ");
	}

	public void deleteSidekiqJob() {
		try {
			utils.getLocator("sidekiqPage.deleteBtn").click();
			logger.info("Clicked 'Delete' button.");
		} catch (Exception e) {
			logger.error("Error occurred during clicking 'Delete' button: " + e.getMessage());
		}
	}

    public int checkSidekiqJob(String url, String job, int noOfAttempt)
        throws InterruptedException {


		int attempts = 0;
		List<WebElement> ele = new ArrayList<>();
        while (attempts < noOfAttempt) {
			driver.navigate().to(url + "/sidekiq/filter/metrics");
			utils.getLocator("schedulesPage.filterJobSearch").sendKeys(job);
			utils.getLocator("schedulesPage.filterJobSearch").sendKeys(Keys.ENTER);
			String xpath = utils.getLocatorValue("schedulesPage.filterJob").replace("{job}", job);
			ele = driver.findElements(By.xpath(xpath));
			Thread.sleep(2000);
			if (!ele.isEmpty()) {
              logger.info("searched job is present: " + job + " at attempt " + attempts);
				TestListeners.extentTest.get().pass("searched job is present: " + job);
				break;
			} else  {
              logger.info("searched job not found: " + job + " at attempt " + attempts);
				TestListeners.extentTest.get().info("searched job not found: " + job);
			}
			attempts++;
		}
		return ele.size();
	}
    public int checkSidekiqJobWithId(String url, String job, String id ) throws InterruptedException {
		List<WebElement> ele = new ArrayList<WebElement>();
		int attempts = 0;
		while (attempts < 10) {
			driver.navigate().to(url + "/sidekiq/scheduled");
			utils.longwait(5);
			driver.navigate().refresh();
			utils.longwait(5);
			utils.getLocator("schedulesPage.anyJobContentXpath").sendKeys(job);
			utils.getLocator("schedulesPage.anyJobContentXpath").sendKeys(Keys.ENTER);
			String xpath = utils.getLocatorValue("schedulesPage.searchJobWithId").replace("{job}", job).replace("$id", id);
			ele = driver.findElements(By.xpath(xpath));
			if (ele.size() >= 1) {
				break;
			} else {
				driver.navigate().to(url + "/sidekiq/queues");
				utils.longwait(2);
			}
			attempts++;
		}
		return ele.size();
	}
    public String checkProfileUpdateCampaignGroupByJobs() {
		utils.longWaitInSeconds(1);
		String list = "";
		List<WebElement> ProfileUpdateCampaignGroupByJobs = utils
				.getLocatorList("schedulePage.profileUpdateGroupByJobs");
		for (WebElement element : ProfileUpdateCampaignGroupByJobs) {
			list += element.getText() + " ";
		}
		logger.info(" Profile update sidekiq jobs data with execution delay: " + list);
		TestListeners.extentTest.get().pass(" Profile update sidekiq jobs data with execution delay: " + list);
		return list;
	}
}