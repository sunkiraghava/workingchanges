package com.punchh.server.utilities;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int maxRetryCount = 1;

    @Override
    public boolean retry(ITestResult result) {
        retryCount++;

        boolean willRetry = retryCount <= maxRetryCount;

        // Indicate if this is the final attempt
        result.setAttribute("IS_FINAL_ATTEMPT", !willRetry);
        result.setAttribute("RETRY_COUNT", retryCount);

        if (willRetry) {
        	//TestListeners.extentTest.get().info("Retrying test: " + result.getName() + ", Attempt: " + (retryCount + 1));
        }

        return willRetry;
    }
}