package com.punchh.server.pages;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedeemablesPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String redeemable_image_url;
	private PageObj pageObj;

	public RedeemablesPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void searchAndselectRedeemable(String name) {
		utils.waitTillElementToBeClickable(utils.getLocator("redeemablesPage.searchBar"));
		utils.getLocator("redeemablesPage.searchBar").click();
		utils.getLocator("redeemablesPage.searchBar").sendKeys(name);
		utils.getLocator("redeemablesPage.searchBar").sendKeys(Keys.ENTER);
		utils.getLocator("redeemablesPage.baseRedeemable").click();

	}

	public String uploadRedeemableimage() throws InterruptedException {

		WebElement imgInput = utils.getLocator("redeemablesPage.imageInput");
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, imgInput);
		imgInput.sendKeys(System.getProperty("user.dir") + "/resources/images.png");

		WebElement nextBtn = utils.getLocator("redeemablesPage.nextBtn");
		utils.StaleElementclick(driver, nextBtn);
		Thread.sleep(2000);
		utils.StaleElementclick(driver, nextBtn);
		/*
		 * WebElement finshBtn = utils.getLocator("redeemablesPage.nextBtn");
		 * utils.StaleElementclick(driver, finshBtn);
		 */
		utils.getLocator("redeemablesPage.finshBtn").click();
		return utils.getLocator("redeemablesPage.successMsg").getText().trim();
	}

	public String setDefaultRedeemableImage() throws InterruptedException {
		utils.getLocator("redeemablesPage.removeBtn").click();
		WebElement nextBtn = utils.getLocator("redeemablesPage.nextBtn");
		utils.StaleElementclick(driver, nextBtn);
		Thread.sleep(2000);
		utils.StaleElementclick(driver, nextBtn);
		/*
		 * WebElement finshBtn = utils.getLocator("redeemablesPage.nextBtn");
		 * utils.StaleElementclick(driver, finshBtn);
		 */
		utils.getLocator("redeemablesPage.finshBtn").click();
		return utils.getLocator("redeemablesPage.successMsg").getText().trim();

	}

	public void clickNextBtn() {
		utils.getLocator("redeemablesPage.nextBtn").click();

	}

	public void clickFinishBtn() {
		utils.getLocator("redeemablesPage.finshBtn").click();
	}

	public void clickRemoveBtn() {
		utils.getLocator("redeemablesPage.removeBtn").click();
	}

	public String getValueinJsonArray(Response response) {

		JsonPath js = new JsonPath(response.asString());
		int count = js.getInt("array.size()");
		for (int i = 0; i < count; i++) {
			try {
				redeemable_image_url = js.get("[" + i + "].event_details.redeemable_image_url");
				if (redeemable_image_url != null)
					break;
			} catch (Exception e) {

			}
		}
		return redeemable_image_url;
	}

	public String checkRedeemableImageUrl(Response response) {

		JsonPath js = new JsonPath(response.asString());
		int count = js.getInt("array.size()");
		for (int i = 0; i < count; i++) {
			try {
				redeemable_image_url = js.get("[" + i + "].redeemable.redeemable_image_url");
				if (redeemable_image_url != null)
					break;
			} catch (Exception e) {

			}
		}
		return redeemable_image_url;
	}

	public String getRewardId(String token, String client, String secret, String redeemableID)
			throws InterruptedException {
		String rewardId = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			utils.longwait(1000);
			try {
				// fetch reward id from API
				Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, client, secret);
				int expectedRedeemableId = Integer.parseInt(redeemableID);

				List<Object> resultList = rewardResponse.jsonPath().getList("redeemable_id");
				for (int j = 0; j < resultList.size(); j++) {
					int actualRedeemableId = rewardResponse.jsonPath().getInt("redeemable_id[" + j + "]");
					if (actualRedeemableId == expectedRedeemableId) {
						rewardId = rewardResponse.jsonPath().get("id[" + j + "]") + "";
					}
				}
				if (rewardId != "") {
					flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info("Reward id not found ");
			}
			attempts++;
		}
		logger.info(rewardId + " is the reward id of redeemable - " + redeemableID);
		TestListeners.extentTest.get().pass(rewardId + " is the reward id of redeemable - " + redeemableID);

		return rewardId;
	}

	public String getDealRedeemableUuid(String client, String secret, String token, String dealName)
			throws InterruptedException {
		String redeemable_uuid = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			utils.longwait(1000);
			try {
				// fetch reward id from API
				Response listApi1DealsResponse = pageObj.endpoints().Api1ListAllDeals(client, secret, token);
				Assert.assertEquals(listApi1DealsResponse.getStatusCode(), 200,
						"Status code 200 did not matched for api1 list all deals");
				String expectedDealName = dealName;

				List<Object> resultList = listApi1DealsResponse.jsonPath().getList("name");
				for (int j = 0; j < resultList.size(); j++) {
					String actualDealName = listApi1DealsResponse.jsonPath().getString("[" + j + "].name");
					if (actualDealName.equalsIgnoreCase(expectedDealName)) {
						redeemable_uuid = listApi1DealsResponse.jsonPath().getString("[" + j + "].redeemable_uuid");
					}
				}
				if (redeemable_uuid != "") {
					flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info("redeemable_uuid id not found ");
			}
			attempts++;
		}
		return redeemable_uuid;
	}

	public List<String> getRewardIdList(String token, String client, String secret, String redeemableID ,int MAX_RETRY_ATTEMPTS )
			throws InterruptedException {
		List<String> rewardIdList = new LinkedList<String>();
		String rewardId = "";
		String stringRewardId = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < MAX_RETRY_ATTEMPTS) {
			utils.longwait(1000);
			try {
				// fetch reward id from API
				Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, client, secret);
				int expectedRedeemableId = Integer.parseInt(redeemableID);

				List<Object> resultList = rewardResponse.jsonPath().getList("redeemable_id");
				for (int j = 0; j < resultList.size(); j++) {
					int actualRedeemableId = rewardResponse.jsonPath().getInt("redeemable_id[" + j + "]");
					if (actualRedeemableId == expectedRedeemableId) {
						rewardId = rewardResponse.jsonPath().get("id[" + j + "]") + "";
						stringRewardId = stringRewardId + rewardId + ",";
						rewardIdList.add(rewardId);
					}
				}
				if (rewardIdList.size() > 0) {
					flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info("Reward id not found ");
				TestListeners.extentTest.get().pass("Reward id not found ");
			}
			attempts++;
		}
		logger.info(stringRewardId + " is the reward id of redeemable - " + redeemableID);
		TestListeners.extentTest.get().pass(stringRewardId + " is the reward id of redeemable - " + redeemableID);

		return rewardIdList;
	}

	// Add rewards to basket N times
	public void addRewardsToBasket(List<String> rewardIds, String token, String client, String secret,
			String externalUID) {
		for (String rewardId : rewardIds) {
			Response response = pageObj.endpoints().addDiscountToBasketAUTH(token, client, secret, "reward", rewardId,
					externalUID);
			Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			utils.logit("AUTH add discount to basket is successful. Reward ID " + rewardId + " added to basket");
		}
	}

	// Send redeemable to user N times
	public void sendRedeemableNTimes(String userID, String apiKey, String redeemableId, String nTimes) {
		for (int i = 0; i < Integer.parseInt(nTimes); i++) {
			Response response = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", redeemableId, "", "");
			Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
			utils.logit("Redeemable ID " + redeemableId + " is sent to user ID " + userID);
		}
	}

	public int ListAllDealsMobileApi(String client, String secret, String token, int noOfAttempts) {
		int statusCode = 0;
		int attempts = 0;
		while (attempts < noOfAttempts) {
			utils.longWaitInSeconds(2);
			try {
				// Mobile Api -> List all deals
				Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(client, secret, token);
				statusCode = listdealsResponse.getStatusCode();
				if (statusCode == 200) {
					statusCode = 200;
					break;
				}
			} catch (Exception e) {
				logger.info("Deal not found " + attempts);
				TestListeners.extentTest.get().pass("Deal not found " + attempts);
			}
			attempts++;
		}

		return statusCode;
	}

}
