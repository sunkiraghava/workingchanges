package com.punchh.server.pages;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v126.network.Network;
import org.openqa.selenium.devtools.v126.network.model.PostDataEntry;
import org.openqa.selenium.devtools.v126.network.model.Request;
import org.openqa.selenium.devtools.v126.network.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

/**
 * Utility page object for capturing network (GraphQL) traffic using Selenium
 * Chrome DevTools.
 *
 * Usage: ChromeNetworkCallPage networkPage = new ChromeNetworkCallPage(driver);
 * List<String> gqlResponses =
 * networkPage.captureGraphQLNetworkTraffic("/graphql", "https://example.com",
 * "GetBusinesses");
 */
public class ChromeNetworkCallPage {

	private static final Logger logger = LoggerFactory.getLogger(ChromeNetworkCallPage.class);

	private final WebDriver driver;
	private final Utilities utils;

	public ChromeNetworkCallPage(WebDriver driver) {
		this.driver = driver;
		this.utils = new Utilities(driver);
	}

	/**
	 * Captures GraphQL requests/responses.
	 *
	 * @param graphqlEndpoint Substring to match GraphQL endpoint requests (e.g.
	 *                        "/graphql")
	 * @param appUrl          URL of the app to load
	 * @param operationName   Optional GraphQL operation name (can be null for ANY)
	 * @return List of matching GraphQL response bodies
	 */
	@SuppressWarnings("unchecked")
	public List<String> captureGraphQLNetworkTraffic(String graphqlEndpoint, String appUrl, String operationName) {
		List<String> graphqlResponses = new CopyOnWriteArrayList<>();
		Set<String> matchingRequestIds = ConcurrentHashMap.newKeySet();

		if (!(driver instanceof ChromeDriver)) {
			logger.error("WebDriver is not ChromeDriver. Cannot capture network traffic.");
			return graphqlResponses;
		}

		ChromeDriver chromeDriver = (ChromeDriver) driver;
		DevTools devTools = chromeDriver.getDevTools();
		utils.longWaitInSeconds(6);
		devTools.createSession();

		// Enable network tracking
		devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
		logger.info("Started capturing network traffic for endpoint: {}", graphqlEndpoint);
		TestListeners.extentTest.get().info("Started capturing network traffic for endpoint: " + graphqlEndpoint);

		// Capture requests
		devTools.addListener(Network.requestWillBeSent(), request -> {
			Request req = request.getRequest();
			String url = req.getUrl();

			if (url.contains(graphqlEndpoint)) {
				req.getPostDataEntries().ifPresent(entries -> {
					for (PostDataEntry entry : entries) {
						entry.getBytes().ifPresent(raw -> {
							try {
								// raw is a Base64 string
								String body = new String(Base64.getDecoder().decode(raw));
								logger.info("GraphQL Candidate Request Body: {}", body);
								TestListeners.extentTest.get().info("GraphQL Candidate Request Body: " + body);

								boolean matchesOp = (operationName == null || body.contains(operationName));
								if (matchesOp) {
									matchingRequestIds.add(request.getRequestId().toString());
									logger.info("Captured GraphQL request (operation: {})",
											operationName != null ? operationName : "ANY");
								}
							} catch (Exception ex) {
								logger.error("Failed to decode GraphQL request body", ex);
							}
						});
					}
				});
			}
		});

		// Capture responses
		devTools.addListener(Network.responseReceived(), response -> {
			Response res = response.getResponse();
			String url = res.getUrl();

			if (url != null && url.contains(graphqlEndpoint)) {
				String reqId = response.getRequestId().toString();
				if (matchingRequestIds.contains(reqId)) {
					try {
						Map<String, Object> bodyMap = (Map<String, Object>) devTools
								.send(Network.getResponseBody(response.getRequestId()));
						Object bodyObj = bodyMap.get("body");
						if (bodyObj != null) {
							String body = bodyObj.toString();
							graphqlResponses.add(body);
							logger.info("GraphQL Response Body: {}", body);
							TestListeners.extentTest.get().info("GraphQL Response Body: " + body);
						}
					} catch (Exception e) {
						logger.error("Error getting GraphQL response body", e);
					}
				}
			}
		});

		// Open the application
		try {
			driver.get(appUrl);
			Thread.sleep(20000); // Replace with smarter wait logic if needed
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Thread interrupted during network capture", e);
		} catch (Exception e) {
			logger.error("Error during network capture", e);
		}

		return graphqlResponses;
	}
}