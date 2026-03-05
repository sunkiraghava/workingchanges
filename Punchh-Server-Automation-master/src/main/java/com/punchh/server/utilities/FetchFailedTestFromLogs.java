package com.punchh.server.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchFailedTestFromLogs {
	public static void main(String[] args) throws IOException {

		String filePath = "/Users/punchh_naveenm/Downloads/regressionsuite19Aug.txt"; // change this

		Pattern failedPattern = Pattern.compile("Failed TestCase:\\s+([\\w\\.]+)");
		Pattern skippedPattern = Pattern.compile("TestCase:\\s+([\\w\\.]+)\\.\\w+.*Status:\\s+Skipped");

		Files.lines(Paths.get(filePath)).forEach(line -> {
			Matcher failedMatcher = failedPattern.matcher(line);
			Matcher skippedMatcher = skippedPattern.matcher(line);

			if (failedMatcher.find()) {
				System.out.println(extractClassName(failedMatcher.group(1)));
			} else if (skippedMatcher.find()) {
				System.out.println(extractClassName(skippedMatcher.group(1)));
			}
		});
	}

	private static String extractClassName(String fullName) {
		// Remove package and keep only class name
		if (fullName.contains(".")) {
			String[] parts = fullName.split("\\.");
			return parts[parts.length - 1];
		}
		return fullName;
	}
}