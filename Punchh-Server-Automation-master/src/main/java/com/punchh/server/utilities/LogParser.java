package com.punchh.server.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

	public static void main(String[] args) {
		String logFile = System.getProperty("user.dir") + "/857 - regression-suite.txt"; // path to your logs.txt
		List<String> testCases = new ArrayList<>();
		int failedCount = 0;
		int skippedCount = 0;

		// Regex: extract last part of test name before Status
		Pattern pattern = Pattern.compile("TestCase: .*?([A-Za-z0-9_]+)Status: (Failed|Skipped)");

		try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					String testCaseName = matcher.group(1); // test case name
					String status = matcher.group(2); // Failed / Skipped

					String result = testCaseName + "Status: " + status;
					testCases.add(result);

					if (status.equalsIgnoreCase("Failed")) {
						failedCount++;
					} else if (status.equalsIgnoreCase("Skipped")) {
						skippedCount++;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Print results on console
		System.out.println("📌 Failed/Skipped Test Cases:");
		for (String test : testCases) {
			System.out.println(test);
		}

		// Print summary
		System.out.println("\n📊 Summary:");
		System.out.println("Total Failed : " + failedCount);
		System.out.println("Total Skipped: " + skippedCount);
		System.out.println("Grand Total  : " + (failedCount + skippedCount));

		// Write results into text file
		try (PrintWriter pw = new PrintWriter(new FileWriter("Failed_Skipped_Tests.txt"))) {
			for (String test : testCases) {
				pw.println(test);
			}
			pw.println();
			pw.println("Total Failed : " + failedCount);
			pw.println("Total Skipped: " + skippedCount);
			pw.println("Grand Total  : " + (failedCount + skippedCount));
			System.out.println("\n✅ Results also saved in Failed_Skipped_Tests.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
