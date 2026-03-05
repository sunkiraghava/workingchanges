package com.punchh.server.LisCreationUtilityClasses;

import org.json.JSONArray;
import org.json.JSONObject;

public class GenerateJson {

	// Method to create a list item WITH modifiers
	public JSONObject createListItemWithModifiers(String name, String externalId, String filterItemSet,
			String excludeNonPayable, String baseItemAttribute, String baseItemOperator, String baseItemValue,
			String modifierAttributes, String modifierOperator, String modifierValue, String maxDiscountUnits,
			String processingMethod) {
		JSONObject item = new JSONObject();
		item.put("name", name);
		item.put("external_id", externalId);
		item.put("filter_item_set", filterItemSet);
		item.put("exclude_non_payable", excludeNonPayable);

		// Base Items
		JSONObject baseItems = new JSONObject();
		JSONArray baseClauses = new JSONArray();
		JSONObject baseClause = new JSONObject();
		baseClause.put("attribute", baseItemAttribute);
		baseClause.put("operator", baseItemOperator);
		baseClause.put("value", baseItemValue);
		baseClauses.put(baseClause);
		baseItems.put("clauses", baseClauses);
		item.put("base_items", baseItems);

		// Modifiers (if provided)
		if (modifierValue != null && modifierOperator != null) {
			JSONObject modifiers = new JSONObject();
			if (maxDiscountUnits != null)
				modifiers.put("max_discount_units", maxDiscountUnits);
			if (processingMethod != null)
				modifiers.put("processing_method", processingMethod);

			JSONArray modifierClauses = new JSONArray();
			JSONObject modifierClause = new JSONObject();
			modifierClause.put("attribute", baseItemAttribute);
			modifierClause.put("operator", modifierOperator);
			modifierClause.put("value", modifierValue);
			modifierClauses.put(modifierClause);
			modifiers.put("clauses", modifierClauses);
			item.put("modifiers", modifiers);
		}

		return item;
	}

	// Method to create a list item WITHOUT modifiers
	@SuppressWarnings("unused")
	private static JSONObject createListItemWithoutModifiers(String name, String externalId, String filterItemSet,
			String excludeNonPayable, String baseItemValue, String baseItemOperator) {
		JSONObject item = new JSONObject();
		item.put("name", name);
		item.put("external_id", externalId);
		item.put("filter_item_set", filterItemSet);
		item.put("exclude_non_payable", excludeNonPayable);

		// Base Items
		JSONObject baseItems = new JSONObject();
		JSONArray baseClauses = new JSONArray();
		JSONObject baseClause = new JSONObject();
		baseClause.put("attribute", "item_id");
		baseClause.put("operator", baseItemOperator);
		baseClause.put("value", baseItemValue);
		baseClauses.put(baseClause);
		baseItems.put("clauses", baseClauses);
		item.put("base_items", baseItems);

		return item;
	}

}
