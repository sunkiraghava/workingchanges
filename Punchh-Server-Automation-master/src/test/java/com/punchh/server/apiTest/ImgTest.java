package com.punchh.server.apiTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentReports;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import org.apache.commons.net.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

@Listeners(TestListeners.class)
public class ImgTest {

	WebDriver driver;
	private static PageObj pageObj;
	static Logger logger = LogManager.getLogger(ImgTest.class);
	public static ExtentReports extentReport;
	Utilities utils;

	@Test
	public void Test() throws Exception {
		utils = new Utilities(driver);
		// WebDriverManager.chromedriver().setup();
		driver = new ChromeDriver();
		driver.manage().window().maximize();
		pageObj = new PageObj(driver);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("moesSlug"));
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignspage().clickNewCampaignBtn();
		Utilities.longWait(5000);
		// driver.manage().window().setSize(new Dimension(1024, 768));

		Screenshot screenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(1000))
				.takeScreenshot(driver);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(screenshot.getImage(), "PNG", out);
		byte[] bytes = out.toByteArray();
		String imagebase64 = Base64.encodeBase64String(bytes);
		String uid = getUID("New Mass Offer Campaign Page", "aa96afa0-46c0-4cf2-a330-24f09612c814");
		validateRequest("Step 1 - New Mass Campaign", uid, imagebase64);

		pageObj.signupcampaignpage().createWhatDetailsMassCampaign("ABC", "Gift Redeemable", "Enjoy A FREE Burrito");
		Utilities.longWait(5000);

		// Screenshot for second page
		imagebase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
		validateRequest("Step 2 - Campaign Whom Page", uid, imagebase64);

		driver.quit();
	}

	public static String getUID(String testName, String projectKey) {
		try {
			RequestSpecification request = RestAssured.given();
			request.header("content-type", "application/json");
			JSONObject json = new JSONObject();
			json.put("TestName", testName);
			json.put("ProjectKey", projectKey);
			request.body(json.toString());
			Response response = request.when().post("http://sandbox.imagium.io/api/GetUID");
			int code = response.getStatusCode();
			String response_id = response.getBody().asString();
			return response_id;
		} catch (Exception ex) {
			return ex.toString();
		}
	}

	public static void validateRequest(String stepNam, String uid, String imagebase64) throws IOException {
		RequestSpecification request = RestAssured.given();
		request.header("content-type", "application/json");
		JSONObject jo = new JSONObject();
		jo.put("TestRunID", uid.replace("\"", ""));
		jo.put("StepName", stepNam);
		jo.put("ImageBase64", imagebase64);
		System.out.println("imagebase64:" + imagebase64);
		request.body(jo.toString());
		Response response1 = request.when().post("http://sandbox.imagium.io/api/Validate");
		String response_id1 = response1.getBody().asString();
		System.out.println("response id1:" + response_id1);
	}
}
