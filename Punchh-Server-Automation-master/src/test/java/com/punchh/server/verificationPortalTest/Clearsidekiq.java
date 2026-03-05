package com.punchh.server.verificationPortalTest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.Utilities;

public class Clearsidekiq {
	private static Logger logger = LogManager.getLogger(verificationPortalTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private Utilities utils;

	@BeforeClass
	public void setup() throws Exception {

		utils = new Utilities();
		pageObj = new PageObj(driver);
	}

	@Test(description = "ClearSidekiq")
	public void Clearsidekiqtest() throws Exception {

		utils.flushSidekiq("pp");

	}

}
