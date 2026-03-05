package com.punchh.server.apimodel.lineitemselector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineItemSelectorRule {

	private String name; // mandatory

	@JsonProperty("external_id")
	private String externalId;

	@JsonProperty("filter_item_set")
	private LineItemSelectorFilterItemSet filterItemSet; // mandatory enum

	@JsonProperty("exclude_non_payable")
	private Boolean excludeNonPayable;

	@JsonProperty("base_items")
	private BaseItemCriteria baseItems;

	private ModifierCriteria modifiers;

	/** Validation logic */
	public void validate() {
		if (filterItemSet == null) {
			throw new IllegalArgumentException("'filter_item_set' is mandatory");
		}

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("'name' is mandatory");
		}

		switch (filterItemSet) {
		case BASE_ONLY:
			if (baseItems == null || baseItems.getClauses() == null || baseItems.getClauses().isEmpty()) {
				throw new IllegalArgumentException(
						"'base_items' with at least 1 clause is required when filter_item_set=base_only");
			}
			break;

		case MODIFIERS_ONLY:
			if (baseItems == null || baseItems.getClauses() == null || baseItems.getClauses().isEmpty()) {
				throw new IllegalArgumentException(
						"'base_items' with at least 1 clause is required when filter_item_set=modifiers_only");
			}
			if (modifiers == null) {
				throw new IllegalArgumentException("'modifiers' is required when filter_item_set=modifiers_only");
			}
			break;

		case BASE_AND_MODIFIERS:
			if (baseItems == null || baseItems.getClauses() == null || baseItems.getClauses().isEmpty()) {
				throw new IllegalArgumentException(
						"'base_items' with at least 1 clause is required when filter_item_set=base_and_modifiers");
			}
			if (modifiers == null) {
				throw new IllegalArgumentException("'modifiers' is required when filter_item_set=base_and_modifiers");
			}
			break;

		default:
			throw new IllegalArgumentException("Unknown filter_item_set: " + filterItemSet);
		}
	}

}
