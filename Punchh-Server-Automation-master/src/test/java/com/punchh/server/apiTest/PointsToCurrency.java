package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.Utilities;

public class PointsToCurrency {
	static Logger logger = LogManager.getLogger(PosAPITest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	String email = "AutoApiTemp@punchh.com";
	Properties uiProp;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";

}
