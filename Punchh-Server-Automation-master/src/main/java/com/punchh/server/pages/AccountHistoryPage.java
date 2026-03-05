package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AccountHistoryPage {

	static Logger logger = LogManager.getLogger(AccountHistoryPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	private Map<String, By> locators;

	public AccountHistoryPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);

		locators = utils.getAllByMap();
	}

	public List<String> getAccountDetailsforItemEarned(String matcher) {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.itemEarnedRow"));
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			if (val.contains(matcher)) {
				accountData.add(val);
			}
		}
		return accountData;
	}

	public List<String> getAccountDetailsforGiftedItem() {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.tableRow1"));
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			accountData.add(val);
		}
		return accountData;
	}

	public List<String> getAccountDetailsforRewardEarned() {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.rewardEarnedRow"));
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			accountData.add(val);
		}
		return accountData;
	}

	public List<String> getAccountDetailsforCardRedeemed() {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.cardRedeemedRow"));
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			accountData.add(val);
		}
		return accountData;
	}

	public List<String> getAccountDetailsforItemRedeemed() {
		utils.longWaitInSeconds(5);
		List<String> accountData = new ArrayList<String>();
		try {
			List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.itemRedeemedRow"));
			int col = tableRowOne.size();
			for (int i = 0; i < col; i++) {
				String val = tableRowOne.get(i).getText();
				accountData.add(val);
			}
		} catch (Exception e) {
		}
		return accountData;
	}

	public List<String> getAccountDetailsforBonusPointsEarned() {
		List<String> accountData = new ArrayList<String>();
		try {
			List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.bonuspointsEarnedRow"));
			int col = tableRowOne.size();
			for (int i = 0; i < col; i++) {
				String val = tableRowOne.get(i).getText();
				accountData.add(val);
			}
		} catch (Exception e) {

		}
		return accountData;
	}

	public int getCheckinCount() {
		List<WebElement> ordersEarnedLabels = driver.findElements(locators.get("accountHistoryPage.ordersEarnedLabel"));
		return ordersEarnedLabels.size();
	}

	public List<String> getAccountDetailsforRewardRedeemed() {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.rewardRedeemedRow"));
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			accountData.add(val);
		}
		return accountData;

	}

	public boolean getAccountDetailsforRewardEarned(String expMessage, String earnedType) {
		List<WebElement> tableRowOne = new ArrayList<WebElement>();
		boolean flag = false;
		switch (earnedType) {
		case "Rewards":
			tableRowOne = driver.findElements(locators.get("accountHistoryPage.rewardEarnedRow"));
			break;
		case "Bonus":
			tableRowOne = driver.findElements(locators.get("accountHistoryPage.bonuspointsEarnedRow"));
			break;
		case "Item":
			tableRowOne = driver.findElements(locators.get("accountHistoryPage.itemGiftedRow"));
			break;
		default:
			break;
		}

		try {
			for (WebElement wEle : tableRowOne) {
				String actualMessage = wEle.getText();
				if (actualMessage.trim().contains(expMessage)) {
					flag = true;
					return flag;

				} else {
					flag = false;
				}
			}
		} catch (Exception e) {
			logger.error("AccountHistoryPage.getAccountDetailsforRewardEarned()");

		}
		return flag;

	}

	public String getPageData() {
		utils.longWaitInSeconds(2);
		String pageSource = driver.getPageSource();
		return pageSource;

	}

	public String getAccountHistorydetailsForAnyevent(String name) {
		boolean flag = false;
		WebElement table = driver
				.findElement(By.xpath("//table[contains(@class,'table table-striped table-lg table-hover')]"));
		List<WebElement> rowsList = table.findElements(By.tagName("tr"));
		List<WebElement> columnsList = null;
		String val = "";
		for (WebElement row : rowsList) {
			if (flag == true) {
				break;
			}
			columnsList = row.findElements(By.tagName("td"));
			for (WebElement column : columnsList) {
				val = column.getText() + ",";
				System.out.println(val);
				if (val.contains(name)) {
					flag = true;
					logger.info("Item gifted rflected in account history : " + val);
					TestListeners.extentTest.get().pass("Item gifted rflected in account history : " + val);
					break;

				}
			}
		}

		return val;
	}

	public List<String> getAccountDetailsforPointsRedeemed() {
		List<String> accountData = new ArrayList<String>();
		try {
			List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.pointsRedeemedRow"));
			int col = tableRowOne.size();
			for (int i = 0; i < col; i++) {
				String val = tableRowOne.get(i).getText();
				accountData.add(val);
			}
		} catch (Exception e) {
			logger.error("Points redeemed is not present at the account details page");
			TestListeners.extentTest.get().info("Points redeemed is not present at the account details page");
		}
		return accountData;

	}

	public List<String> getAccountDetailsforCardsRedeemed() {
		List<String> accountData = new ArrayList<String>();
		try {
			List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.cardsRedeemedRow"));
			int col = tableRowOne.size();
			for (int i = 0; i < col; i++) {
				String val = tableRowOne.get(i).getText();
				accountData.add(val);
			}
		} catch (Exception e) {
			logger.error("Cards redeemed is not present at the account details page");
			TestListeners.extentTest.get().fail("Cards redeemed is not present at the account details page");
		}
		return accountData;

	}

	public int getAccountDetailsforItemGifted(String value) {
		int count = 0;
		try {
			List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.itemGiftedRow"));
			int col = tableRowOne.size();
			for (int i = 0; i < col; i++) {
				String val = tableRowOne.get(i).getText();
				if (val.contains(value)) {
					count++;
				}
			}
		} catch (Exception e) {
			logger.error("Item gifted is not present at the account details page");
			TestListeners.extentTest.get().fail("Item gifted is not present at the account details page");
		}
		return count;
	}

	public int getAccountDetailsforItemGiftedWithPooling(String cname, int expectedcount) throws InterruptedException {
		int attempts = 0;
		int count = 0;
		while (attempts <= 20) {
			TestListeners.extentTest.get().info("Pooling Attempt to get gift count from account histroy : " + attempts);
			try {
				utils.longWaitInSeconds(1);
				List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.itemGiftedRow"));
				int col = tableRowOne.size();
				for (int i = 0; i < col; i++) {
					String val = tableRowOne.get(i).getText();
					if (val.contains(cname)) {
						count++;
					}
				}
				if (count == expectedcount) {
					logger.info("Gifted count by campaign " + cname + " matched on the timeline");
					TestListeners.extentTest.get()
							.pass("Gifted count by campaign " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {

			}
			utils.refreshPage();
			attempts++;
		}
		return count;
	}

	public boolean getAccountDetailsforBonusPointsEarnedWithPooling(String cname, int expectedcount)
			throws InterruptedException {
		int attempts = 0;
		int count = 0;
		boolean flag = false;
		while (attempts <= 10) {
			TestListeners.extentTest.get().info("Pooling Attempt to get gift count from account histroy : " + attempts);
			try {
				utils.longWaitInSeconds(1);
				List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.bonuspointsEarnedRow"));
				int col = tableRowOne.size();
				for (int i = 0; i < col; i++) {
					String val = tableRowOne.get(i).getText();
					if (val.contains(cname)) {
						count++;
					}
				}
				if (count == expectedcount) {
					logger.info("Gifted count by campaign " + cname + " matched on the timeline");
					TestListeners.extentTest.get()
							.pass("Gifted count by campaign " + cname + " matched on the timeline");
					flag = true;
					break;
				}
			} catch (Exception e) {

			}
			utils.refreshPage();
			attempts++;
		}
		return flag;
	}

	public boolean getAccountDetailsforRewardEarnedWithPooling(String cname, int expectedcount)
			throws InterruptedException {
		int attempts = 0;
		int count = 0;
		boolean flag = false;
		while (attempts <= 10) {
			TestListeners.extentTest.get()
					.info("Pooling Attempt to get reward earned from account histroy : " + attempts);
			try {
				utils.longWaitInSeconds(1);
				List<WebElement> tableRowOne = driver.findElements(locators.get("accountHistoryPage.rewardEarnedRow"));
				int col = tableRowOne.size();
				for (int i = 0; i < col; i++) {
					String val = tableRowOne.get(i).getText();
					if (val.contains(cname)) {
						count++;
					}
				}
				if (count == expectedcount) {
					logger.info("Gifted count by campaign " + cname + " matched on the timeline");
					TestListeners.extentTest.get()
							.pass("Gifted count by campaign " + cname + " matched on the timeline");
					flag = true;
					break;
				}
			} catch (Exception e) {

			}
			utils.refreshPage();
			attempts++;
		}
		return flag;
	}
}
