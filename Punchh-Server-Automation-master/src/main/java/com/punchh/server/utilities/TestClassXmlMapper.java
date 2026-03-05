package com.punchh.server.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestClassXmlMapper {

	public static void main(String[] args) throws Exception {
		String testFolderPath = "src/test/java";
		// Use absolute paths for XML files
		String baseDir = new File(".").getAbsolutePath();
		List<String> xmlFiles = Arrays.asList(baseDir + "/regression.xml", baseDir + "/smoke.xml",
				baseDir + "/localRegression.xml", baseDir + "/dataExport.xml", baseDir + "/api.xml");

		// Step 1: Fetch all test classes recursively
		List<String> allTestClasses = new ArrayList<>();
		File baseTestDir = new File(testFolderPath);
		findAllTestClasses(baseTestDir, allTestClasses, baseTestDir.getAbsolutePath() + File.separator);

		System.out.println("Total test classes found: " + allTestClasses.size());

		// Step 2: Parse XMLs and collect classes used
		Map<String, List<String>> classToXmlMap = new HashMap<>();
		Set<String> usedClasses = new HashSet<>();

		for (String xmlPath : xmlFiles) {
			File xmlFile = new File(xmlPath);
			if (!xmlFile.exists()) {
				System.out.println("⚠️ XML file not found: " + xmlPath);
				continue;
			}

			List<String> classesInXml = getClassesFromXml(xmlFile);
			for (String cls : classesInXml) {
				if (!cls.trim().startsWith("<!--")) { // Skip commented classes
					usedClasses.add(cls);
					classToXmlMap.computeIfAbsent(cls, k -> new ArrayList<>()).add(xmlPath);
				}
			}
		}

		// Step 3: Report unused classes
		List<String> unused = new ArrayList<>();
		for (String cls : allTestClasses) {
			if (!usedClasses.contains(cls)) {
				unused.add(cls);
			}
		}

		// Step 4: Report duplicates
		List<String> duplicates = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : classToXmlMap.entrySet()) {
			if (entry.getValue().size() > 1) {
				duplicates.add(entry.getKey());
			}
		}

		// Step 5: Output
		System.out.println("\n🟩 Classes used in XMLs: " + usedClasses.size());
		System.out.println("🟥 Classes NOT in any XML: " + unused.size());
		unused.forEach(c -> System.out.println("  ❌ " + c));

		System.out.println("\n🟨 Duplicate entries in XMLs:");
		for (String cls : duplicates) {
			System.out.println("  🔁 " + cls + " → " + classToXmlMap.get(cls));
		}
	}

	// Recursively finds all .java test classes
	private static void findAllTestClasses(File dir, List<String> classList, String basePath) {
		for (File file : Objects.requireNonNull(dir.listFiles())) {
			if (file.isDirectory()) {
				findAllTestClasses(file, classList, basePath);
			} else if (file.getName().endsWith(".java")) {
				String relativePath = file.getAbsolutePath().replace(basePath, "").replace(File.separator, ".")
						.replace(".java", "");
				if (!relativePath.startsWith(".")) {
					classList.add(relativePath);
				}
			}
		}
	}

	// Extracts class names from a testng xml file
	private static List<String> getClassesFromXml(File xmlFile) throws Exception {
		List<String> classes = new ArrayList<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(xmlFile);
		NodeList classNodes = doc.getElementsByTagName("class");

		for (int i = 0; i < classNodes.getLength(); i++) {
			Element classElement = (Element) classNodes.item(i);
			String className = classElement.getAttribute("name").trim();
			if (!className.isEmpty()) {
				classes.add(className);
			}
		}
		return classes;
	}
}
