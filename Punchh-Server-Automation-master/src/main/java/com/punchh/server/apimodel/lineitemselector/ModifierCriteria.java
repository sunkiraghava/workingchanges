package com.punchh.server.apimodel.lineitemselector;

import java.util.List;

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
public class ModifierCriteria {

	@JsonProperty("max_discount_units")
	private Integer maxDiscountUnits;

	@JsonProperty("processing_method")
	private String processingMethod;

	private List<LineItemSelectorCondition> clauses;
}
