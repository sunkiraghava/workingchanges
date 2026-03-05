package com.punchh.server.utilities;

import org.testng.annotations.Listeners;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/*
�*@Author: Naveen Mehra
�*@Desc: Class to handle Extent Reporting
�*/
@Listeners(TestListeners.class)
public class ExtentReportManager {

	public static ExtentReports extent;
	public static ExtentTest test;
	// private static ExtentHtmlReporter htmlReporter;
	public static String reportPathNValue;
	public static String folderPathValue;
	private static ExtentSparkReporter spark;

	public static ExtentReports getExtent() {
		if (extent != null)
			return extent;
		extent = new ExtentReports();
		extent.attachReporter(getHtmlReporter());
		return extent;
	}

	private static ExtentSparkReporter getHtmlReporter() {
		// folderPathValue = CreateDateTime.createDateTimeFolder("Automation After
		// Execution Results");
		folderPathValue = System.getProperty("user.dir") + "/test-output/automation-results";
		reportPathNValue = folderPathValue;
		System.out.println("Report File Path: " + reportPathNValue);

		// String path=System.getProperty("user.dir")+"//test-output/automatio-results";
		spark = new ExtentSparkReporter(reportPathNValue + "/AutomationReport.html");
		spark.config().setDocumentTitle("Punchh Server Automation Report");
		spark.config().setReportName("Server QA Automation Test");
		spark.config().setReporter(spark);
		spark.config().setTheme(Theme.STANDARD);

		return spark;
	}

	public static ExtentTest createTest(String name, String description) {
		test = extent.createTest(name, description);
		return test;
	}
}