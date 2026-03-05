package com.punchh.server.pages;

import java.util.List;

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
public class ConsolePage {

	static Logger logger = LogManager.getLogger(ConsolePage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;

	public ConsolePage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void setDiscountAmount(String value) throws InterruptedException {
		utils.longWaitInSeconds(5);
		WebElement field = utils.getLocator("consolePage.discountAmountField");
		Thread.sleep(3000);
		// selUtils.scrollToElement(field);

		char amount[] = value.toCharArray();
		WebElement Btn1 = utils.getLocator("consolePage.CalcBtn1");
		WebElement Btn2 = utils.getLocator("consolePage.CalcBtn2");
		WebElement Btn3 = utils.getLocator("consolePage.CalcBtn3");
		WebElement Btn4 = utils.getLocator("consolePage.CalcBtn4");
		WebElement Btn5 = utils.getLocator("consolePage.CalcBtn5");
		WebElement Btn6 = utils.getLocator("consolePage.CalcBtn6");
		WebElement Btn7 = utils.getLocator("consolePage.CalcBtn7");
		WebElement Btn8 = utils.getLocator("consolePage.CalcBtn8");
		WebElement Btn9 = utils.getLocator("consolePage.CalcBtn9");
		WebElement Btn0 = utils.getLocator("consolePage.CalcBtn0");
		// WebElement BtnDot = utils.getLocator("consolePage.CalcBtnDot");

		for (int i = 0; i < amount.length; i++) {

			if (amount[i] == '1') {
				Btn1.click();
			} else if (amount[i] == '2') {
				Btn2.click();
			} else if (amount[i] == '3') {
				Btn3.click();
			} else if (amount[i] == '4') {
				Btn4.click();
			} else if (amount[i] == '5') {
				Btn5.click();
			} else if (amount[i] == '6') {
				Btn6.click();
			} else if (amount[i] == '7') {
				Btn7.click();
			} else if (amount[i] == '8') {
				Btn8.click();
			} else if (amount[i] == '9') {
				Btn9.click();
			} else if (amount[i] == '0') {
				Btn0.click();
			} /*
				 * else if (amount[i] == '.') { BtnDot.click(); }
				 */
		}

		logger.info("Amount entered is :" + value);
	}

	public void setRedemptionCode(String value) throws InterruptedException {
		WebElement field = utils.getLocator("consolePage.redemptionCodeField");
		Thread.sleep(3000);
		// selUtils.scrollToElement(field);

		char amount[] = value.toCharArray();
		WebElement Btn1 = utils.getLocator("consolePage.codeCelBtn1");
		WebElement Btn2 = utils.getLocator("consolePage.codeCelBtn2");
		WebElement Btn3 = utils.getLocator("consolePage.codeCelBtn3");
		WebElement Btn4 = utils.getLocator("consolePage.codeCelBtn4");
		WebElement Btn5 = utils.getLocator("consolePage.codeCelBtn5");
		WebElement Btn6 = utils.getLocator("consolePage.codeCelBtn6");
		WebElement Btn7 = utils.getLocator("consolePage.codeCelBtn7");
		WebElement Btn8 = utils.getLocator("consolePage.codeCelBtn8");
		WebElement Btn9 = utils.getLocator("consolePage.codeCelBtn9");
		WebElement Btn0 = utils.getLocator("consolePage.codeCelBtn0");

		for (int i = 0; i < amount.length; i++) {

			if (amount[i] == '1') {
				Btn1.click();
			} else if (amount[i] == '2') {
				Btn2.click();
			} else if (amount[i] == '3') {
				Btn3.click();
			} else if (amount[i] == '4') {
				Btn4.click();
			} else if (amount[i] == '5') {
				Btn5.click();
			} else if (amount[i] == '6') {
				Btn6.click();
			} else if (amount[i] == '7') {
				Btn7.click();
			} else if (amount[i] == '8') {
				Btn8.click();
			} else if (amount[i] == '9') {
				Btn9.click();
			} else if (amount[i] == '0') {
				Btn0.click();
			}
		}

		logger.info("Redemption Code entered is :" + value);
	}

	public String processTransaction() {
		WebElement ele = utils.getLocator("consolePage.processTransactionBtn");
		selUtils.scrollToElement(ele);
		ele.click();
		utils.acceptAlert(driver);
		utils.longWaitInSeconds(2);
		utils.waitTillVisibilityOfElement(utils.getLocator("consolePage.successMsg"), "");
		String msg = utils.getLocator("consolePage.successMsg").getText();
		return msg;
	}

	public void selectRandomCategory() {
		utils.implicitWait(5);
		List<WebElement> categoryListWEle = utils.getLocatorList("consolePage.itemCategoryListXpath");
		if (categoryListWEle.size() != 0) {
			int randomIndex = Utilities.getRandomNoFromRange(0, categoryListWEle.size() - 1);
			categoryListWEle.get(randomIndex).click();
			logger.info(categoryListWEle.get(randomIndex).getText() + "  category is selected ");
			TestListeners.extentTest.get()
					.info(categoryListWEle.get(randomIndex).getText() + "  category is selected ");
		}
		utils.implicitWait(50);

	}

}
