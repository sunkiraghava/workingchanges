package com.punchh.server.apimodel.lineitemselector;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LineItemSelectorFilterItemSet {

	@JsonProperty("base_only")
	BASE_ONLY,

	@JsonProperty("modifiers_only")
	MODIFIERS_ONLY,

	@JsonProperty("base_and_modifiers")
	BASE_AND_MODIFIERS
}
