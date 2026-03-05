package com.punchh.server.LisCreationUtilityClasses;

import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.WebDriver;
import com.punchh.server.pages.PageObj;
import io.restassured.response.Response;

public class CreateLISandQC {
	private PageObj pageObj;

	public CreateLISandQC(WebDriver driver) {
		pageObj = new PageObj(driver);
	}

	public Response createLIS(String apiKey, String lisName, String baseItemClauses, String modifiersItemClauses,
			String external_id, String filter_item_set) {
		List<BaseItemClauses> listBaseItemClauses = new ArrayList();
		List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
		String adminKey = apiKey;

		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = baseItemClauses;
		String modifiersItemClauses1 = modifiersItemClauses;

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName, external_id,
				filter_item_set, listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		return createLISResponse;
	}

}
