package com.punchh.server.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.logging.Logger;

public class SelfHealingSelenium {
	private WebDriver driver;
	private WebDriverWait wait;
	private static final Logger logger = Logger.getLogger(SelfHealingSelenium.class.getName());

	// Constructor
	public SelfHealingSelenium(WebDriver driver, int timeoutSeconds) {
		this.driver = driver;
		this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
	}

	// Method to attempt to find an element with multiple locators
	public WebElement findElementWithSelfHealing(By... locators) {
		WebElement element = null;
		int attempt = 0;
		int maxAttempts = 3;
		boolean isElementFound = false;

		while (attempt < maxAttempts && !isElementFound) {
			for (By locator : locators) {
				try {
					logger.info("Attempting to find element with locator: " + locator);
					element = new WebDriverWait(driver, Duration.ofSeconds(10))
							.until(ExpectedConditions.visibilityOfElementLocated(locator));
					isElementFound = true;
					break;
				} catch (NoSuchElementException | TimeoutException e) {
					logger.warning("Element not found with locator: " + locator + ", attempt " + (attempt + 1));
					// If element is not found, attempt the next locator
				}
			}
			attempt++;
			if (!isElementFound) {
				logger.warning("Element not found after " + attempt + " attempts, retrying...");
				try {
					Thread.sleep(1000); // Sleep before retrying
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		if (!isElementFound) {
			logger.severe("Failed to find element after " + maxAttempts + " attempts.");
			throw new NoSuchElementException("Element not found after multiple attempts.");
		}

		return element;
	}

	// Retry method for waiting until an element is clickable
	public void clickElementWithRetry(By locator, int maxRetries, int retryInterval) {
		int attempts = 0;
		while (attempts < maxRetries) {
			try {
				WebElement element = findElementWithSelfHealing(locator);
				new WebDriverWait(driver, Duration.ofSeconds(10))
						.until(ExpectedConditions.elementToBeClickable(element));
				element.click();
				logger.info("Element clicked successfully.");
				break;
			} catch (Exception e) {
				attempts++;
				logger.warning("Click failed. Attempt " + attempts + " of " + maxRetries + ".");
				if (attempts >= maxRetries) {
					logger.severe("Max retry attempts reached. Unable to click the element.");
					throw e;
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	// Example of a fallback locator strategy
	public WebElement findElementWithFallback(By primaryLocator, By fallbackLocator) {
		WebElement element = null;
		try {
			element = findElementWithSelfHealing(primaryLocator);
		} catch (NoSuchElementException e) {
			logger.warning("Primary locator failed, attempting fallback locator.");
			element = findElementWithSelfHealing(fallbackLocator);
		}
		return element;
	}

	// Element state validation before interaction
	public boolean isElementReadyForInteraction(WebElement element) {
		try {
			return element.isDisplayed() && element.isEnabled();
		} catch (StaleElementReferenceException e) {
			logger.warning("Stale element reference, retrying...");
			return false;
		}
	}

	// Example of interacting with an element using a self-healing approach
	public void interactWithElement(By primaryLocator, By fallbackLocator) {
		WebElement element = findElementWithFallback(primaryLocator, fallbackLocator);
		// Example: click the element after finding it
		try {
			element.click();
			logger.info("Element clicked.");
		} catch (ElementClickInterceptedException e) {
			logger.warning("Click intercepted, retrying click.");
			interactWithElement(primaryLocator, fallbackLocator); // Retry logic
		}
	}

	// Example of checking visibility and interacting with an element
	public void checkAndInteract(By locator) {
		try {
			WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
					.until(ExpectedConditions.visibilityOfElementLocated(locator));
			element.click();
			logger.info("Element interacted with.");
		} catch (TimeoutException e) {
			logger.severe("Element not visible after waiting.");
			throw e;
		}
	}

	// Handle dynamic elements (sticky, lazy-loaded) using smart waits
	public WebElement waitForElementToBeStable(By locator) {
		WebElement element = null;
		try {
			element = new WebDriverWait(driver, Duration.ofSeconds(15))
					.until(ExpectedConditions.visibilityOfElementLocated(locator));
			new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.elementToBeClickable(element)); // Ensures
																														// the
																														// element
																														// is
																														// interactive
			logger.info("Element is stable for interaction.");
		} catch (TimeoutException e) {
			logger.severe("Timeout while waiting for element to stabilize.");
			throw e;
		}
		return element;
	}

	// Utility method for getting the page title (example of self-healing in case of
	// failure)
	public String getPageTitle() {
		try {
			return driver.getTitle();
		} catch (Exception e) {
			logger.warning("Failed to retrieve page title, retrying...");
			try {
				Thread.sleep(1000); // Retry logic with delay
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return driver.getTitle(); // Retry fetching the title
		}
	}

	// Closing the browser
	public void closeBrowser() {
		if (driver != null) {
			driver.quit();
			logger.info("Browser closed.");
		}
	}
}