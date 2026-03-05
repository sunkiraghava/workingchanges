package com.punchh.server.utilities;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

/*
 * Description: Utility class for Selenium Utilities 
 */

@Listeners(TestListeners.class)
public class SeleniumUtilities {
	WebDriver driver;
	static Logger logger = LogManager.getLogger(SeleniumUtilities.class);
	Properties prop;
	Utilities utils;

	public SeleniumUtilities(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
	}

	// Method to handle a alert
	public String checkAlert() {
		String alrtName = null;
		try {
			Alert alert = driver.switchTo().alert();
			alrtName = alert.getText();
			alert.accept();
			logger.info("Alert handled");
		} catch (Exception e) {
			logger.info("No alert found");
		}
		return alrtName;
	}

	// Scroll to a Element
	public void scrollToElement(WebElement ele) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", ele);
		} catch (Exception e) {
			logger.info("Error in scrolling to element " + e);
		}
	}

	// *********** Wait related Utilities **********

	// Implicit wait method
	public void implicitWait(long waitTime) {
		try {
			// configProperties =
			// FileLoader.loadPropertiesFile("ia.properties");
			// String wtype = configProperties.getProperty(waitType);
			// long waitDuration = Long.parseLong(wtype);
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(waitTime));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	public void waitTillElementToBeClickable(WebElement WebElement) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(50));
			wait.until(ExpectedConditions.elementToBeClickable(WebElement));
		} catch (Exception e) {
			logger.error("WebElement failed to load/appear: " + e);
			TestListeners.extentTest.get().info("WebElement failed to load/appear: " + e);
		}
	}

	public void waitTillVisibilityOfElement(WebElement element, String elementName) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(50));
			wait.until(ExpectedConditions.visibilityOf(element));
		} catch (Exception e) {
			TestListeners.extentTest.get().info(elementName + " present and webelement is " + element + e);
			logger.error(elementName + " is not present and webelement is " + element);
		}
	}

	// Mouse actions related Utilities

	public void mouseHoverOverElement(WebElement element) {
		try {
			Actions action = new Actions(driver);
			action.moveToElement(element).build().perform();
		} catch (Exception e) {
			logger.error("Error in mouse hover over element, Exception: " + e);
			TestListeners.extentTest.get().fail("Error in mouse hover over element, Exception: " + e);
		}
	}

	// Click and Hold a element and move it to another element
	public void clickAndHoldAndMove(WebElement sourceElement, WebElement DestinationElement) {
		Actions action = new Actions(driver);
		action.clickAndHold(sourceElement).moveToElement(DestinationElement).release().build().perform();
	}

	// get current frame of the element
	public String getCurrentIFrame() {
		String currentFrame = null;
		try {
			JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
			currentFrame = (String) jsExecutor.executeScript("return self.name");
		} catch (Exception e) {
			logger.error("Error in getting current frame: " + e);
			TestListeners.extentTest.get().fail("Error in getting current frame: " + e);
		}
		return currentFrame;
	}

	public void clickByOffset(WebElement ele, int xOffset, int yOffset) {
		try {
			Actions build = new Actions(driver);
			build.moveToElement(ele, xOffset, yOffset).click().build().perform();
		} catch (Exception e) {
			TestListeners.extentTest.get().fail("Unable to click element by offset" + e);
			logger.info("Unable to click element by offset" + e);
		}
	}

	public void dragAndDropActions(WebElement sourceLocator, WebElement destinationLocator) {
		try {
			Thread.sleep(10000);
			Actions action = new Actions(driver);
			action.dragAndDrop(sourceLocator, destinationLocator).build().perform();
		} catch (Exception e) {
			TestListeners.extentTest.get().fail("Error in drag and drop using actions" + e);
			logger.info("Error in drag and drop using actions" + e);
		}
	}

	public void dragAndDropActionsByOffset(WebElement sourceLocator, int xOffset, int yOffset) {
		try {
			Actions action = new Actions(driver);
			action.dragAndDropBy(sourceLocator, xOffset, yOffset).build().perform();
		} catch (Exception e) {
			TestListeners.extentTest.get().fail("Error in drag and drop using actions ByOffset " + e);
			logger.info("Error in drag and drop using actions ByOffset " + e);
		}
	}

	public void dragAndDropWithJSExecutor(WebElement sourceLocator, WebElement destinationLocator) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("function createEvent(typeOfEvent) {\n"
					+ "var event =document.createEvent(\"CustomEvent\");\n"
					+ "event.initCustomEvent(typeOfEvent,true, true, null);\n" + "event.dataTransfer = {\n"
					+ "data: {},\n" + "setData: function (key, value) {\n" + "this.data[key] = value;\n" + "},\n"
					+ "getData: function (key) {\n" + "return this.data[key];\n" + "}\n" + "};\n" + "return event;\n"
					+ "}\n" + "\n" + "function dispatchEvent(element, event,transferData) {\n"
					+ "if (transferData !== undefined) {\n" + "event.dataTransfer = transferData;\n" + "}\n"
					+ "if (element.dispatchEvent) {\n" + "element.dispatchEvent(event);\n"
					+ "} else if (element.fireEvent) {\n" + "element.fireEvent(\"on\" + event.type, event);\n" + "}\n"
					+ "}\n" + "\n" + "function simulateHTML5DragAndDrop(element, destination) {\n"
					+ "var dragStartEvent =createEvent('dragstart');\n" + "dispatchEvent(element, dragStartEvent);\n"
					+ "var dropEvent = createEvent('drop');\n"
					+ "dispatchEvent(destination, dropEvent,dragStartEvent.dataTransfer);\n"
					+ "var dragEndEvent = createEvent('dragend');\n"
					+ "dispatchEvent(element, dragEndEvent,dropEvent.dataTransfer);\n" + "}\n" + "\n"
					+ "var source = arguments[0];\n" + "var destination = arguments[1];\n"
					+ "simulateHTML5DragAndDrop(source,destination);", sourceLocator, destinationLocator);
			longWait(1500);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String AddScreenshot(String screenShotName) {
		String pathvalue = "";
		try {
			Utilities.screenShotCapture(driver, screenShotName);
			pathvalue = "Screenshots" + "\\" + screenShotName + ".png";
		} catch (Exception e) {
			TestListeners.extentTest.get().fail("Screenshot could not be captured for " + screenShotName);
			throw new NoSuchElementException("Screenshot could not be captured for " + screenShotName);
		}
		return pathvalue;
	}

	public void doubleClickEvent(WebDriver driver, WebElement ele) {
		try {
			Actions action = new Actions(driver).doubleClick(ele);
			action.perform();
		} catch (Exception e) {
			logger.info("Error in double clicking " + e);
		}
	}

	public void fireFoxClickEvent(WebElement ele) {
		try {
			if (prop.getProperty("browserName").equals("FIREFOX") || prop.getProperty("browserName").equals("IE")) {
				Actions action = new Actions(driver).click(ele);
				action.build().perform();
			}
		} catch (Exception e) {
			logger.info("Error in firefox clicking " + e);
		}
	}

	public void fireFoxDoubleClickEvent(WebElement ele) {
		try {
			if (prop.getProperty("browserName").equals("FIREFOX")) {
				Actions act = new Actions(driver);
				act.moveToElement(ele).doubleClick().perform();
				((JavascriptExecutor) driver).executeScript("var evt = document.createEvent('MouseEvents');"
						+ "evt.initMouseEvent('dblclick',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);"
						+ "arguments[0].dispatchEvent(evt);", ele);
			}
		} catch (Exception e) {
			logger.info("Error in firefox double clicking " + e);
		}
	}

	public void doubleClickAndEnterData(WebElement ele, String txt) {
		try {
			Actions builder = new Actions(driver);
			Actions seriesOfActions = builder.doubleClick(ele).sendKeys(txt);
			seriesOfActions.perform();
		} catch (Exception e) {
			logger.info("Error in double clicking and entering data " + e);
		}
	}

	public void doubleClickMouseEvent(WebElement element) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("var evt = document.createEvent('MouseEvents');"
					+ "evt.initMouseEvent('dblclick',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);"
					+ "arguments[0].dispatchEvent(evt);", element);
		} catch (Exception e) {
			logger.info("Error in double click using mouse events " + e);
		}
	}

	public void javaScriptHoverAndClick(WebElement hoverElement, WebElement clickElement) {
		String javaScript = "var evObj = document.createEvent('MouseEvents');"
				+ "evObj.initMouseEvent(\"mouseover\",true, false, window, 0, 0, 0, 0, 0,false, false, false, false, 0, null);"
				+ "arguments[0].dispatchEvent(evObj);";
		((JavascriptExecutor) driver).executeScript(javaScript, hoverElement);
		// waitTillVisibilityOfElement(clickElement, driver, "click element",
		// "mediumWait");
		TestListeners.extentTest.get().pass("Sub View type: " + clickElement.getText() + " selected successfully");
		logger.info("Sub View type: " + clickElement.getText() + " selected successfully");
	}

	public void hoverAndClick(WebDriver driver, WebElement hoverElement, WebElement clickElement) {
		Actions action = new Actions(driver);
		action.moveToElement(hoverElement).build().perform();
	}

	public void rightClickAndSelectMenuItem(WebElement ele, int index) {
		try {
			Actions action = new Actions(driver);
			action.contextClick(ele).build().perform();
			Robot robot = new Robot();
			for (int i = 1; i <= index; i++) {
				robot.keyPress(KeyEvent.VK_DOWN);
				robot.keyRelease(KeyEvent.VK_DOWN);
			}
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
		} catch (Exception e) {
			logger.error("Error in right click and menu selection" + e);
		}
	}

	public void rightClick(WebElement ele) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			String script = "var evt = document.createEvent('MouseEvents');" + "var RIGHT_CLICK_BUTTON_CODE = 2;"
					+ "evt.initMouseEvent('contextmenu', true, true, window, 1, 0, 0, 0, 0, false, false, false, false, RIGHT_CLICK_BUTTON_CODE, null);"
					+ "arguments[0].dispatchEvent(evt)";
			js.executeScript(script, ele);
		} catch (Exception e) {
			logger.error("Error in right click using java script" + e);
		}
	}

	public void rightClickWithActions(WebElement ele) {
		try {
			Actions action = new Actions(driver);
			action.moveToElement(ele);
			action.contextClick(ele).build().perform();
		} catch (Exception e) {
			logger.error("Error in rightclick using actions classes " + e);
		}
	}

	public void waitforPageLoad() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(300)).until(webDriver -> ((JavascriptExecutor) webDriver)
					.executeScript("return document.readyState").equals("complete"));
		} catch (Exception e) {
			logger.error("Error in waitng for page load" + e);
		}
	}

	public void jsClick(WebElement element) {
		if (element.isDisplayed()) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		}
	}

	public String OpenNewTabAndSwitch() {
		String parentWindow = driver.getWindowHandle();
		((JavascriptExecutor) driver).executeScript("window.open()");
		Set<String> set = driver.getWindowHandles();
		for (String window : set) {
			if (!(window.equals(parentWindow))) {
				driver.switchTo().window(window);
				break;
			}
		}
		return parentWindow;
	}

	public void switchToParentWindow() {
		String parentWindow = driver.getWindowHandle();
		driver.switchTo().window(parentWindow);
	}

	public void longWait(long waitDuration) {
		// long waitDuration = Long.parseLong(wtype);
		try {
			Thread.sleep(waitDuration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String switchToNewWindow() {
		String parentWindow = driver.getWindowHandle();
		try {
			Set<String> windowList = driver.getWindowHandles();
			if (windowList.size() > 1) {
				for (String str : windowList) {
					if (!str.equals(parentWindow)) {
						driver.switchTo().window(str);
						break;
					}
				}
			} else {
				logger.error("No new window is seen");
			}
		} catch (Exception e) {
			logger.error("Failed to switch to new window " + e);
		}
		return parentWindow;
	}

	/* SWitch to a window with window handle id */
	public void switchToWindow(String window) {
		try {
			driver.switchTo().window(window);
		} catch (Exception e) {
			logger.error("Failed to switch to new window " + e);
		}
	}

	/* Switch to nth window using window title */
	public String switchToThirdWindow(String title) {
		String parentWindow = driver.getWindowHandle();
		try {
			Set<String> windowList = driver.getWindowHandles();
			if (windowList.size() > 1) {
				for (String str : windowList) {
					driver.switchTo().window(str);
					if (driver.getTitle().equals(title))
						break;
				}
			} else {
				logger.error("No new window is seen");
			}
		} catch (Exception e) {
			logger.error("Failed to switch to new window " + e);
		}
		return parentWindow;
	}

	/* Switch to nth window using element locator */

	public String switchToThirdWindowUsingWebElement(String ele) {
		String parentWindow = driver.getWindowHandle();
		try {
			Set<String> windowList = driver.getWindowHandles();
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(4));
			if (windowList.size() > 1) {
				for (String str : windowList) {
					try {
						driver.switchTo().window(str);
						utils.getLocator(ele).isDisplayed();
						break;
					} catch (Exception e) {
					}
				}
			} else {
				logger.error("No new window is seen");
			}
		} catch (Exception e) {
			logger.error("Failed to switch to new window " + e);
		}
		return parentWindow;
	}

	public void switchToFrame(WebElement ele) {
		try {
			ele.isDisplayed();
			driver.switchTo().frame(ele);
		} catch (Exception e) {
			logger.error("Failed to switch to frame: " + ele + e);
			TestListeners.extentTest.get().fail("Failed to switch to frame: " + ele + e);
		}
	}

	public void verifyImageOnpage(WebElement element) {
		try {
			Boolean ImagePresent = (Boolean) ((JavascriptExecutor) driver).executeScript(
					"return arguments[0].complete && typeof arguments[0].naturalWidth != \"undefined\" && arguments[0].naturalWidth > 0",
					element);
			if (ImagePresent) {
				logger.info("Verfied banner image: " + element.getAttribute("src"));
				TestListeners.extentTest.get().pass("Verfied banner image: " + element.getAttribute("src"));
			} else {
				logger.error("Failed to verify image on page:" + element.getAttribute("src"));
				TestListeners.extentTest.get().fail("Verfied image: " + element.getAttribute("src"));
			}
		} catch (Exception e) {
			logger.error("Error in verifying images: " + e);
			TestListeners.extentTest.get().fail("Error in verifying images: " + e);
		}
	}

	public void testFailed(String failMsg) {
		logger.error(failMsg);
		TestListeners.extentTest.get().fail(failMsg);
		AddScreenshot(failMsg);
		Assert.fail(failMsg);
	}

	public void SendKeysViaJS(WebElement ele, String inputText) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].value='" + inputText + "';", ele);

	}

	public void navigateBack() {
		driver.navigate().back();
	}

	public void clearTextUsingJS(WebElement ele) {
		((JavascriptExecutor) driver).executeScript("arguments[0].value ='';", ele);
	}

	public String getTextUsingJS(WebElement ele) {
		return (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerHTML;", ele);
	}

	public List<String> getAllOpenedWindowId() {
		Set<String> windowIdSets = driver.getWindowHandles();
		List<String> list = new LinkedList<String>(windowIdSets);
		return list;
	}

	public void closeAllChildWindowAndSwitchToParentWindow() {
		String parentWindow = driver.getWindowHandle();
		List<String> windowList = getAllOpenedWindowId();
		for (String window : windowList) {
			if (!window.equals(parentWindow)) {
				driver.switchTo().window(window);
				driver.close();
			}
		}
		driver.switchTo().window(parentWindow);
	}

	public void switchToIframe(String tagName) {
		WebElement iframeElement = driver.findElement(By.tagName(tagName));
		driver.switchTo().frame(iframeElement);
	}

	public void mouseHoverAndClickBYMouseAction(WebDriver driver, WebElement mouseHoverWebElement,
			WebElement clickWebElement) {
		Actions act = new Actions(driver);
		act.moveToElement(mouseHoverWebElement).moveToElement(clickWebElement).click().build().perform();
	}

	public void clearAndSendKeys(WebElement element, String value) {
		element.clear();
		element.sendKeys(value);
	}

	public void setCheckbox(WebElement element, boolean shouldBeChecked, WebDriver driver) {
		if (element.isSelected() != shouldBeChecked) {
			utils.clickByJSExecutor(driver, element); // or element.click()
		}
	}
}
