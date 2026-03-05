package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v126.runtime.Runtime;
import org.openqa.selenium.devtools.v126.runtime.model.ConsoleAPICalled;
import org.openqa.selenium.devtools.v126.runtime.model.RemoteObject;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PARCommerceIframePage {
	static Logger logger = LogManager.getLogger(PARCommerceIframePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	// Default values for PAR Commerce payment form
	private static final String DEFAULT_FIRST_NAME = "Nitesh";
	private static final String DEFAULT_LAST_NAME = "Shekhawat";
	private static final String DEFAULT_CARD_NUMBER = "4111111111111111";
	private static final String DEFAULT_MONTH = "11";
	private static final String DEFAULT_YEAR = "2028"; // 2-digit year format (28 = 2028)
	private static final String DEFAULT_CVV = "123";

	public PARCommerceIframePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	/**
	 * Opens PAR Commerce iframe URL in a new tab and switches to it
	 * 
	 * @param iframeUrl The iframe URL to open
	 * @return The window handle of the new tab
	 */
	public String openIframeInNewTab(String iframeUrl) {
		try {
			// Open new tab using JavaScript
			((JavascriptExecutor) driver).executeScript("window.open('" + iframeUrl + "', '_blank');");

			// Wait for new tab to open
			Utilities.longWait(2000);

			// Get all window handles
			ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());

			// Switch to the new tab (last one)
			String newTabHandle = tabs.get(tabs.size() - 1);
			driver.switchTo().window(newTabHandle);

			// Wait for page to load
			Utilities.longWait(3000);

			logger.info("Successfully opened iframe in new tab: " + iframeUrl);
			TestListeners.extentTest.get().info("Successfully opened iframe in new tab: " + iframeUrl);

			return newTabHandle;

		} catch (Exception e) {
			logger.error("Error opening iframe in new tab: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error opening iframe in new tab: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Fills credit card details in the payment form using default values Default
	 * values: Name: "Nitesh Shekhawat", Card: "4111111111111111", Exp: "11/28",
	 * CVV: "123"
	 */
	public void fillCreditCardDetails() {
		fillCreditCardDetails(DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME, DEFAULT_CARD_NUMBER, DEFAULT_CVV, DEFAULT_MONTH,
				DEFAULT_YEAR);
	}

	/**
	 * Fills credit card details in the payment form
	 * 
	 * @param firstName  First name
	 * @param lastName   Last name
	 * @param cardNumber Credit card number
	 * @param cvv        CVV code
	 * @param month      Expiration month (MM format)
	 * @param year       Expiration year (YY or YYYY format)
	 */
	public void fillCreditCardDetails(String firstName, String lastName, String cardNumber, String cvv, String month,
			String year) {
		try {
			// Wait for form to be ready
			Utilities.longWait(2000);

			// Fill first name
			WebElement firstNameField = utils.getLocator("PARCommerceIframePage.firstNameTxtBx");
			utils.waitTillElementToBeClickable(firstNameField);
			firstNameField.clear();
			firstNameField.sendKeys(firstName);
			logger.info("Entered first name: " + firstName);

			// Fill last name
			WebElement lastNameField = utils.getLocator("PARCommerceIframePage.lastNameTxtBx");
			lastNameField.clear();
			lastNameField.sendKeys(lastName);
			logger.info("Entered last name: " + lastName);

			// Fill card number
			WebElement iframe = utils.getLocator("PARCommerceIframePage.iFrameCardNumber");
			driver.switchTo().frame(iframe);

			WebElement cardNumberField = utils.getLocator("PARCommerceIframePage.cardNumberTxtBx");
			utils.waitTillElementToBeClickable(cardNumberField);
			cardNumberField.clear();
			cardNumberField.sendKeys(cardNumber);
			logger.info("Entered card number: " + cardNumber.replaceAll("\\d(?=\\d{4})", "*"));
			driver.switchTo().defaultContent();

			// Fill expiration month and year
			WebElement monthField = utils.getLocator("PARCommerceIframePage.monthTxtBx");
			monthField.clear();
			monthField.sendKeys(month);
			logger.info("Entered expiration month: " + month);

			WebElement yearField = utils.getLocator("PARCommerceIframePage.yearTxtBx");
			yearField.clear();
			yearField.sendKeys(year);
			logger.info("Entered expiration year: " + year);

			// Fill CVV
			WebElement iframecvv = utils.getLocator("PARCommerceIframePage.iFrameCVV");
			driver.switchTo().frame(iframecvv);
			WebElement cvvField = utils.getLocator("PARCommerceIframePage.cvvTxtBx");
			cvvField.clear();
			cvvField.sendKeys(cvv);
			logger.info("Entered CVV: " + cvv.replaceAll("\\d", "*"));

			// Switch back to default content after filling CVV
			driver.switchTo().defaultContent();

			TestListeners.extentTest.get().info("Credit card details filled successfully");

		} catch (Exception e) {
			logger.error("Error filling credit card details: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error filling credit card details: " + e.getMessage());
			Assert.fail("Failed to fill credit card details: " + e.getMessage());
		}
	}

	/**
	 * Submits the payment form Note: Submit button is in the main document, NOT
	 * inside any iframe
	 */
	public void submitPaymentForm() {
		try {
			// Ensure we're in default content (not inside any iframe)
			// Submit button is in the main document, not in an iframe
			driver.switchTo().defaultContent();

			WebElement submitButton = utils.getLocator("PARCommerceIframePage.submitPaymentBtn");
			utils.waitTillElementToBeClickable(submitButton);
			submitButton.click();
			logger.info("Clicked submit payment button");
			TestListeners.extentTest.get().info("Clicked submit payment button");

			// Wait for processing
			Utilities.longWait(2000);

		} catch (Exception e) {
			logger.error("Error submitting payment form: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error submitting payment form: " + e.getMessage());
			Assert.fail("Failed to submit payment form: " + e.getMessage());
		}
	}

	/**
	 * Waits for and verifies the "Transaction Successful!" message
	 * 
	 * @return true if success message is displayed
	 */
	public boolean waitForTransactionSuccess() {
		try {
			// Wait for success message to appear (with timeout)
			int maxWaitTime = 30; // seconds
			int waited = 0;
			boolean successFound = false;

			while (waited < maxWaitTime && !successFound) {
				try {
					String pageText = driver.getPageSource();
					if (pageText.contains("Transaction Successful!") || pageText.contains("transaction successful")) {
						successFound = true;
						logger.info("Transaction Successful message found");
						TestListeners.extentTest.get().pass("Transaction Successful message displayed");
						break;
					}
				} catch (Exception e) {
					// Continue waiting
				}
				Utilities.longWait(1000);
				waited++;
			}

			if (!successFound) {
				logger.error("Transaction success message not found within timeout");
				TestListeners.extentTest.get().fail("Transaction success message not found within timeout");
				return false;
			}

			return true;

		} catch (Exception e) {
			logger.error("Error waiting for transaction success: " + e.getMessage());
			TestListeners.extentTest.get().info("Error while waiting for transaction success: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Captures the transaction token from browser console logs using an existing
	 * DevTools session
	 * 
	 * @param devTools The DevTools instance (already with active session)
	 * @return The transaction token, or null if not found
	 */
	private String captureTokenFromConsoleWithDevTools(DevTools devTools) {
		final CompletableFuture<String> tokenFuture = new CompletableFuture<>();
		List<String> capturedMessages = new CopyOnWriteArrayList<>();

		try {
			// Enable Runtime domain (if not already enabled)
			devTools.send(Runtime.enable());

			// Listen for console API calls
			devTools.addListener(Runtime.consoleAPICalled(), event -> {
				try {
					ConsoleAPICalled consoleEvent = event;
					List<RemoteObject> args = consoleEvent.getArgs();

					if (args != null && !args.isEmpty()) {
						for (RemoteObject arg : args) {
							// Get value from Optional
							Optional<Object> valueOpt = arg.getValue();
							if (valueOpt.isPresent()) {
								Object valueObj = valueOpt.get();
								String value = valueObj != null ? valueObj.toString() : null;

								if (value != null && !value.isEmpty()) {
									capturedMessages.add(value);

									// Check if this is a transaction:success event
									if (value.contains("transaction:success")
											|| value.contains("Transaction Successful")) {
										logger.info("Found transaction:success event in console: " + value);

										// Try to extract token from the console message
										String token = extractTokenFromConsoleMessage(value);
										if (token != null && !token.isEmpty()) {
											logger.info("Token extracted from console: " + token);
											tokenFuture.complete(token);
											return;
										}
									}
								}
							}

							// If token not in the string value, try to get object description
							Optional<String> descriptionOpt = arg.getDescription();
							if (descriptionOpt.isPresent()) {
								String description = descriptionOpt.get();
								if (description != null && !description.isEmpty()) {
									String token = extractTokenFromConsoleMessage(description);
									if (token != null && !token.isEmpty()) {
										logger.info("Token extracted from description: " + token);
										tokenFuture.complete(token);
										return;
									}
								}
							}
						}
					}
				} catch (Exception e) {
					logger.error("Error processing console event: " + e.getMessage());
				}
			});

			logger.info("Console listener set up. Waiting for transaction:success event...");
			TestListeners.extentTest.get().info("Console listener set up. Waiting for transaction:success event...");

			// Wait for token with timeout (30 seconds)
			try {
				String token = tokenFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);
				if (token != null && !token.isEmpty()) {
					logger.info("Successfully captured token from console: " + token);
					TestListeners.extentTest.get().pass("Successfully captured token from console");
					return token;
				}
			} catch (java.util.concurrent.TimeoutException e) {
				logger.warn("Timeout waiting for token in console. Checking captured messages...");
				// Try alternative method - parse captured messages
				for (String msg : capturedMessages) {
					String token = extractTokenFromConsoleMessage(msg);
					if (token != null && !token.isEmpty()) {
						logger.info("Token found in captured messages: " + token);
						return token;
					}
				}
				logger.error("No token found in captured console messages");
			} catch (Exception e) {
				logger.error("Error waiting for token: " + e.getMessage());
				// Try alternative method - parse captured messages
				for (String msg : capturedMessages) {
					String token = extractTokenFromConsoleMessage(msg);
					if (token != null && !token.isEmpty()) {
						logger.info("Token found in captured messages: " + token);
						return token;
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error setting up console listener: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error setting up console listener: " + e.getMessage());
		}

		logger.error("Failed to capture token from console");
		TestListeners.extentTest.get().fail("Failed to capture token from console");
		return null;
	}

	/**
	 * Extracts token from captured console logs (MANDATORY method) This reads from
	 * the JavaScript console override we set up Expected format: {type:
	 * 'transaction:success', data: {token: '01K9BYBYFDPAQ60FYZJSZ6Z8EA', ...}}
	 * 
	 * @return The token if found, null otherwise
	 */
	private String extractTokenFromCapturedConsoleLogs() {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;

			// Method 1: Try to get token directly from JavaScript by querying the console
			// event object
			// This is more reliable than string parsing
			try {
				Object tokenObj = js.executeScript("try { "
						+ "  // Check if there's a transaction:success event object in window or console "
						+ "  if (window._lastTransactionEvent && window._lastTransactionEvent.data && window._lastTransactionEvent.data.token) { "
						+ "    return window._lastTransactionEvent.data.token; " + "  } " + "  return null; "
						+ "} catch(e) { return null; }");
				if (tokenObj != null && tokenObj instanceof String) {
					String token = (String) tokenObj;
					if (!token.isEmpty() && token.length() >= 20) {
						logger.info("Token extracted directly from JavaScript event object: " + token);
						return token;
					}
				}
			} catch (Exception e) {
				logger.debug("Could not extract token directly from event object: " + e.getMessage());
			}

			// Method 2: Get all captured console logs
			Object consoleLogs = js.executeScript("return window._capturedConsoleLogs || [];");
			if (consoleLogs == null) {
				logger.warn("No console logs captured (window._capturedConsoleLogs is null)");
				return null;
			}

			if (consoleLogs instanceof java.util.List) {
				@SuppressWarnings("unchecked")
				java.util.List<String> logs = (java.util.List<String>) consoleLogs;

				logger.info("Checking " + logs.size() + " captured console log entries...");

				// Search for transaction:success messages
				for (String logEntry : logs) {
					if (logEntry != null) {
						logger.debug("Checking console log entry: "
								+ logEntry.substring(0, Math.min(200, logEntry.length())));

						// Look for "transaction:success" or "Iframe Event" messages
						if (logEntry.contains("transaction:success") || logEntry.contains("Transaction Successful")
								|| (logEntry.contains("transaction") && logEntry.contains("success"))
								|| (logEntry.contains("Iframe Event") && logEntry.contains("transaction:success"))) {

							logger.info("Found transaction:success in console logs: " + logEntry);
							String token = extractTokenFromConsoleMessage(logEntry);
							if (token != null && !token.isEmpty()) {
								logger.info("Token extracted from captured console log: " + token);
								return token;
							}
						}
					}
				}

				// If no transaction:success found, check all logs for token patterns
				logger.info("No transaction:success message found, searching all logs for token patterns...");
				for (String logEntry : logs) {
					if (logEntry != null) {
						String token = extractTokenFromConsoleMessage(logEntry);
						if (token != null && !token.isEmpty() && token.length() >= 20) {
							logger.info("Token pattern found in console log: " + token);
							return token;
						}
					}
				}
			} else {
				logger.warn("Captured console logs is not a List: " + consoleLogs.getClass().getName());
			}

		} catch (Exception e) {
			logger.error("Error extracting token from captured console logs: " + e.getMessage());
		}

		return null;
	}

	/**
	 * Extracts token from page source using JavaScript (fallback method) Tries to
	 * find token in window variables, localStorage, or page content
	 * 
	 * @return The token if found, null otherwise
	 */
	private String extractTokenFromPageSource() {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;

			// Try 2: Check window variables (common pattern: window.token,
			// window.transactionToken, etc.)
			String[] windowVars = { "token", "transactionToken", "paymentToken", "parToken", "oneTimeToken" };
			for (String varName : windowVars) {
				try {
					Object tokenObj = js.executeScript(
							"return typeof window." + varName + " !== 'undefined' ? window." + varName + " : null;");
					if (tokenObj != null && tokenObj instanceof String) {
						String token = (String) tokenObj;
						if (token != null && !token.isEmpty() && token.length() >= 20) {
							logger.info("Token found in window." + varName + ": " + token);
							return token;
						}
					}
				} catch (Exception e) {
					// Continue to next variable
				}
			}

			// Try 3: Check localStorage
			try {
				Object localStorageToken = js.executeScript(
						"var keys = ['token', 'transactionToken', 'paymentToken', 'parToken', 'oneTimeToken']; "
								+ "for (var i = 0; i < keys.length; i++) { "
								+ "  var value = localStorage.getItem(keys[i]); "
								+ "  if (value && value.length >= 20) return value; " + "} return null;");
				if (localStorageToken != null && localStorageToken instanceof String) {
					String token = (String) localStorageToken;
					logger.info("Token found in localStorage: " + token);
					return token;
				}
			} catch (Exception e) {
				// Continue
			}

			// Try 4: Search page source for token pattern (alphanumeric string, typically
			// 27+ chars)
			String pageSource = driver.getPageSource();
			java.util.regex.Pattern tokenPattern = java.util.regex.Pattern.compile("([A-Z0-9]{27,})");
			java.util.regex.Matcher matcher = tokenPattern.matcher(pageSource);
			if (matcher.find()) {
				String candidate = matcher.group(1);
				// Verify it's likely a token (not just a random string)
				if (candidate.length() >= 27 && candidate.length() <= 50) {
					logger.info("Token pattern found in page source: " + candidate);
					return candidate;
				}
			}

			// Try 5: Execute JavaScript to find any object with token property
			try {
				Object tokenFromJS = js.executeScript("try { "
						+ "  var scripts = document.getElementsByTagName('script'); "
						+ "  for (var i = 0; i < scripts.length; i++) { "
						+ "    var scriptText = scripts[i].innerHTML; "
						+ "    var match = scriptText.match(/token['\":\\s]*[:=]['\":\\s]*['\"]([A-Z0-9]{27,})['\"]/i); "
						+ "    if (match) return match[1]; " + "  } " + "  return null; "
						+ "} catch(e) { return null; }");
				if (tokenFromJS != null && tokenFromJS instanceof String) {
					String token = (String) tokenFromJS;
					logger.info("Token found via JavaScript execution: " + token);
					return token;
				}
			} catch (Exception e) {
				logger.warn("JavaScript token extraction failed: " + e.getMessage());
			}

		} catch (Exception e) {
			logger.error("Error extracting token from page source: " + e.getMessage());
		}

		return null;
	}

	/**
	 * Extracts token from console message string Looks for token in various formats
	 * in the console message Expected format: {type: 'transaction:success', data:
	 * {token: '01K9BYBYFDPAQ60FYZJSZ6Z8EA', ...}}
	 * 
	 * @param consoleMessage The console message string
	 * @return The token if found, null otherwise
	 */
	private String extractTokenFromConsoleMessage(String consoleMessage) {
		try {
			if (consoleMessage == null || consoleMessage.isEmpty()) {
				return null;
			}

			// Try to parse as JSON if it looks like JSON
			// Handle both full JSON strings and partial JSON
			if (consoleMessage.trim().startsWith("{") || consoleMessage.contains("\"token\"")
					|| consoleMessage.contains("'token'") || consoleMessage.contains("token:")) {
				ObjectMapper mapper = new ObjectMapper();
				try {
					// Try to parse as complete JSON
					@SuppressWarnings("unchecked")
					Map<String, Object> jsonMap = mapper.readValue(consoleMessage, Map.class);

					// Try to extract token from nested structure
					String token = extractTokenFromMap(jsonMap);
					if (token != null) {
						return token;
					}
				} catch (Exception e) {
					// Not valid complete JSON, try to find JSON-like structure
					// Look for {...data: {...token: '...'...}...} pattern
					try {
						// Try to extract JSON object from the string
						int jsonStart = consoleMessage.indexOf("{");
						int jsonEnd = consoleMessage.lastIndexOf("}");
						if (jsonStart >= 0 && jsonEnd > jsonStart) {
							String jsonStr = consoleMessage.substring(jsonStart, jsonEnd + 1);
							@SuppressWarnings("unchecked")
							Map<String, Object> jsonMap = mapper.readValue(jsonStr, Map.class);
							String token = extractTokenFromMap(jsonMap);
							if (token != null) {
								return token;
							}
						}
					} catch (Exception e2) {
						// Continue to regex patterns
					}
				}
			}

			// Try regex patterns to find token
			// Pattern 1: "token": "01K9BYBYFDPAQ60FYZJSZ6Z8EA" (double quotes)
			java.util.regex.Pattern pattern1 = java.util.regex.Pattern
					.compile("[\"']token[\"']\\s*:\\s*[\"']([A-Z0-9]{20,})[\"']");
			java.util.regex.Matcher matcher1 = pattern1.matcher(consoleMessage);
			if (matcher1.find()) {
				String token = matcher1.group(1);
				if (token != null && token.length() >= 20) {
					return token;
				}
			}

			// Pattern 2: token: "01K9BYBYFDPAQ60FYZJSZ6Z8EA" (no quotes around key)
			java.util.regex.Pattern pattern2 = java.util.regex.Pattern
					.compile("token\\s*:\\s*[\"']([A-Z0-9]{20,})[\"']");
			java.util.regex.Matcher matcher2 = pattern2.matcher(consoleMessage);
			if (matcher2.find()) {
				String token = matcher2.group(1);
				if (token != null && token.length() >= 20) {
					return token;
				}
			}

			// Pattern 3: Look for alphanumeric token-like strings (typically 27+ chars,
			// matching the screenshot format)
			// Tokens like "01K9BYBYFDPAQ60FYZJSZ6Z8EA" are 27 characters
			java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("([A-Z0-9]{27,})");
			java.util.regex.Matcher matcher3 = pattern3.matcher(consoleMessage);
			while (matcher3.find()) {
				String candidate = matcher3.group(1);
				// Likely token if it's 27+ characters (matching the format in screenshot)
				if (candidate.length() >= 27 && candidate.length() <= 50) {
					return candidate;
				}
			}

			// Pattern 4: Look for shorter tokens (20-26 chars) if no 27+ char found
			java.util.regex.Pattern pattern4 = java.util.regex.Pattern.compile("([A-Z0-9]{20,26})");
			java.util.regex.Matcher matcher4 = pattern4.matcher(consoleMessage);
			while (matcher4.find()) {
				String candidate = matcher4.group(1);
				// Only return if it looks like a token (starts with alphanumeric, not
				// surrounded by other chars)
				if (candidate.length() >= 20) {
					return candidate;
				}
			}

		} catch (Exception e) {
			logger.error("Error extracting token from console message: " + e.getMessage());
		}

		return null;
	}

	/**
	 * Recursively extracts token from a nested map structure
	 * 
	 * @param map The map to search
	 * @return The token if found, null otherwise
	 */
	@SuppressWarnings("unchecked")
	private String extractTokenFromMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}

		// Check if token is directly in this map
		if (map.containsKey("token")) {
			Object tokenObj = map.get("token");
			if (tokenObj != null) {
				return tokenObj.toString();
			}
		}

		// Check in data.externalResponse.token structure
		if (map.containsKey("data")) {
			Object dataObj = map.get("data");
			if (dataObj instanceof Map) {
				Map<String, Object> dataMap = (Map<String, Object>) dataObj;

				// Check data.token
				if (dataMap.containsKey("token")) {
					Object tokenObj = dataMap.get("token");
					if (tokenObj != null) {
						return tokenObj.toString();
					}
				}

				// Check data.externalResponse.token
				if (dataMap.containsKey("externalResponse")) {
					Object extResponseObj = dataMap.get("externalResponse");
					if (extResponseObj instanceof Map) {
						Map<String, Object> extResponseMap = (Map<String, Object>) extResponseObj;
						if (extResponseMap.containsKey("token")) {
							Object tokenObj = extResponseMap.get("token");
							if (tokenObj != null) {
								return tokenObj.toString();
							}
						}
					}
				}
			}
		}

		// Recursively search in nested maps
		for (Object value : map.values()) {
			if (value instanceof Map) {
				String token = extractTokenFromMap((Map<String, Object>) value);
				if (token != null) {
					return token;
				}
			}
		}

		return null;
	}

	/**
	 * Navigates back to the master/original window
	 * 
	 * @param originalWindowHandle The handle of the original window
	 */
	public void navigateBackToMasterWindow(String originalWindowHandle) {
		try {
			// Close current tab (payment iframe tab)
			driver.close();

			// Switch back to original window
			driver.switchTo().window(originalWindowHandle);

			logger.info("Successfully navigated back to master window");
			TestListeners.extentTest.get().info("Successfully navigated back to master window");

		} catch (Exception e) {
			logger.error("Error navigating back to master window: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error navigating back to master window: " + e.getMessage());

			// Try alternative approach - switch to any available window
			try {
				ArrayList<String> windows = new ArrayList<>(driver.getWindowHandles());
				if (!windows.isEmpty()) {
					driver.switchTo().window(windows.get(0));
					logger.info("Switched to first available window as fallback");
				}
			} catch (Exception fallbackError) {
				logger.error("Fallback window switch also failed: " + fallbackError.getMessage());
				Assert.fail("Failed to navigate back to master window");
			}
		}
	}

	/**
	 * Complete PAR Commerce payment flow using default values Default values: Name:
	 * "Nitesh Shekhawat", Card: "4111111111111111", Exp: "11/28", CVV: "123"
	 * 
	 * @param iframeUrl The iframe URL to open
	 * @return The transaction token, or null if failed
	 */
	public String completeParCommercePaymentFlow(String iframeUrl) {
		return completeParCommercePaymentFlow(iframeUrl, DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME, DEFAULT_CARD_NUMBER,
				DEFAULT_CVV, DEFAULT_MONTH, DEFAULT_YEAR);
	}

	/**
	 * Complete PAR Commerce payment flow: open iframe, fill card details, submit,
	 * wait for success, capture token from console, and navigate back
	 * 
	 * @param iframeUrl  The iframe URL to open
	 * @param firstName  First name
	 * @param lastName   Last name
	 * @param cardNumber Credit card number
	 * @param cvv        CVV code
	 * @param month      Expiration month (MM format)
	 * @param year       Expiration year (YY or YYYY format)
	 * @return The transaction token, or null if failed
	 */
	public String completeParCommercePaymentFlow(String iframeUrl, String firstName, String lastName, String cardNumber,
			String cvv, String month, String year) {
		String originalWindow = driver.getWindowHandle();
		String transactionToken = null;
		DevTools devTools = null;

		try {
			logger.info("Starting PAR Commerce payment flow");
			TestListeners.extentTest.get().info("Starting PAR Commerce payment flow");

			// Step 1: Open iframe in new tab
			String newTabHandle = openIframeInNewTab(iframeUrl);
			if (newTabHandle == null) {
				throw new Exception("Failed to open iframe in new tab");
			}

			// Step 2: Set up console listener BEFORE filling form to catch all events
			// This MUST be done early to capture the transaction:success event when it
			// happens
			// Note: DevTools may fail if Chrome version doesn't match - this is expected
			// and handled gracefully
			if (driver instanceof ChromeDriver) {
				try {
					ChromeDriver chromeDriver = (ChromeDriver) driver;
					devTools = chromeDriver.getDevTools();
					devTools.createSession();
					devTools.send(Runtime.enable());
					logger.info("DevTools console listener set up successfully");
					TestListeners.extentTest.get().info("DevTools console listener set up for token capture");
				} catch (Exception devToolsError) {
					// DevTools failure is expected if Chrome version doesn't match DevTools version
					// This is handled gracefully - we'll use JavaScript/page source extraction
					// instead
					logger.info(
							"DevTools not available (Chrome version mismatch expected). Will use JavaScript extraction method.");
					logger.debug("DevTools error details: " + devToolsError.getMessage());
					TestListeners.extentTest.get()
							.info("DevTools not available - will use alternative token extraction method");
					devTools = null; // Ensure it's null so we use fallback
				}
			} else {
				logger.info("WebDriver is not ChromeDriver - will use JavaScript extraction method");
			}

			// Set up JavaScript console override to capture console.log messages (MANDATORY
			// - primary method)
			// This captures ALL console messages including transaction:success events
			// This works regardless of DevTools version compatibility
			// Also stores the transaction event object for direct access
			JavascriptExecutor js = (JavascriptExecutor) driver;
			try {
				// Simplified and fixed JavaScript code - no inline comments that might cause
				// issues
				String consoleOverrideScript = "(function() { "
						+ "  window._capturedConsoleLogs = window._capturedConsoleLogs || []; "
						+ "  if (!window._consoleOverridden) { " + "    var originalLog = console.log; "
						+ "    var originalInfo = console.info; " + "    var originalError = console.error; "
						+ "    var originalWarn = console.warn; " + "    var originalDir = console.dir; "
						+ "    var capture = function() { " + "      var args = Array.from(arguments); "
						+ "      var message = args.map(function(arg) { " + "        if (typeof arg === 'object') { "
						+ "          try { " + "            var jsonStr = JSON.stringify(arg); "
						+ "            if (arg && arg.type === 'transaction:success' && arg.data && arg.data.token) { "
						+ "              window._lastTransactionEvent = arg; " + "            } "
						+ "            return jsonStr; " + "          } catch(err) { "
						+ "            if (arg && arg.type === 'transaction:success' && arg.data && arg.data.token) { "
						+ "              window._lastTransactionEvent = arg; " + "            } "
						+ "            return String(arg); " + "          } " + "        } "
						+ "        return String(arg); " + "      }).join(' '); "
						+ "      window._capturedConsoleLogs.push(message); " + "    }; "
						+ "    console.log = function() { capture.apply(null, arguments); originalLog.apply(console, arguments); }; "
						+ "    console.info = function() { capture.apply(null, arguments); originalInfo.apply(console, arguments); }; "
						+ "    console.error = function() { capture.apply(null, arguments); originalError.apply(console, arguments); }; "
						+ "    console.warn = function() { capture.apply(null, arguments); originalWarn.apply(console, arguments); }; "
						+ "    console.dir = function(obj) { " + "      try { "
						+ "        var jsonStr = JSON.stringify(obj); "
						+ "        if (obj && obj.type === 'transaction:success' && obj.data && obj.data.token) { "
						+ "          window._lastTransactionEvent = obj; " + "        } " + "        capture(jsonStr); "
						+ "      } catch(err) { "
						+ "        if (obj && obj.type === 'transaction:success' && obj.data && obj.data.token) { "
						+ "          window._lastTransactionEvent = obj; " + "        } "
						+ "        capture(String(obj)); " + "      } "
						+ "      originalDir.apply(console, arguments); " + "    }; "
						+ "    window._consoleOverridden = true; " + "  } " + "})();";

				js.executeScript(consoleOverrideScript);
				logger.info("JavaScript console override set up successfully - all console messages will be captured");
				TestListeners.extentTest.get().info("Console message capture initialized");
			} catch (Exception e) {
				logger.error("CRITICAL: Failed to set up console override: " + e.getMessage());
				TestListeners.extentTest.get().fail("Failed to set up console capture - token extraction may fail");
				throw new Exception("Failed to set up mandatory console capture: " + e.getMessage());
			}

			// Step 3: Fill credit card details
			fillCreditCardDetails(firstName, lastName, cardNumber, cvv, month, year);

			// Step 4: Submit payment form
			submitPaymentForm();

			// Step 5: Wait for transaction success message
			boolean success = waitForTransactionSuccess();
			if (!success) {
				throw new Exception("Transaction success message not found");
			}

			// Step 6: Capture token from console (MANDATORY)
			// Wait a bit for transaction to complete and console event to be logged
			Utilities.longWait(3000);

			// Method 1: Try DevTools console capture (if available and working)
			if (devTools != null) {
				logger.info("Attempting to capture token from console using DevTools...");
				transactionToken = captureTokenFromConsoleWithDevTools(devTools);
			}

			// Method 2: Extract token from captured console logs (MANDATORY - primary
			// method)
			// This uses the JavaScript console override we set up earlier
			if (transactionToken == null || transactionToken.isEmpty()) {
				logger.info("Extracting token from captured console logs...");
				transactionToken = extractTokenFromCapturedConsoleLogs();
			}

			// Method 3: Additional fallback - search page source
			if (transactionToken == null || transactionToken.isEmpty()) {
				logger.warn("Token not found in console logs, trying page source extraction...");
				transactionToken = extractTokenFromPageSource();
			}

			// Last resort: Check URL for token
			if (transactionToken == null || transactionToken.isEmpty()) {
				logger.warn("Checking URL for token as last resort...");
				String currentUrl = driver.getCurrentUrl();
				logger.info("Current URL: " + currentUrl);
				if (currentUrl.contains("token=")) {
					transactionToken = currentUrl.split("token=")[1].split("&")[0];
					logger.info("Token extracted from URL: " + transactionToken);
				}
			}

			// Verify token was captured (MANDATORY)
			if (transactionToken == null || transactionToken.isEmpty()) {
				logger.error("CRITICAL: Token was not captured from console or any other method!");
				// Try one more time with a longer wait
				Utilities.longWait(2000);
				transactionToken = extractTokenFromCapturedConsoleLogs();
				if (transactionToken == null || transactionToken.isEmpty()) {
					// Get all captured logs for debugging
					try {
						Object allLogs = js.executeScript("return window._capturedConsoleLogs || [];");
						logger.error("All captured console logs: " + allLogs);
						TestListeners.extentTest.get().fail("Token capture failed. Captured logs: " + allLogs);
					} catch (Exception e) {
						logger.error("Could not retrieve captured logs: " + e.getMessage());
					}
					throw new Exception(
							"MANDATORY: Failed to capture token from console. Transaction may have completed but token extraction failed.");
				}
			}

			// Step 7: Navigate back to master window
			navigateBackToMasterWindow(originalWindow);

			if (transactionToken != null && !transactionToken.isEmpty()) {
				logger.info("PAR Commerce payment flow completed successfully. Token: " + transactionToken);
				TestListeners.extentTest.get()
						.pass("PAR Commerce payment flow completed successfully. Token: " + transactionToken);
			} else {
				logger.error("Payment flow completed but token was not captured");
				TestListeners.extentTest.get().fail("Payment flow completed but token was not captured");
			}

		} catch (Exception e) {
			logger.error("Error in complete PAR Commerce payment flow: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error in complete PAR Commerce payment flow: " + e.getMessage());

			// Ensure we switch back to original window even if there's an error
			try {
				driver.switchTo().window(originalWindow);
			} catch (Exception switchError) {
				logger.error("Error switching back to original window: " + switchError.getMessage());
			}
		}

		return transactionToken;
	}
}