package com.punchh.server.apimodel.qc;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QCItemQualifier {
	private String expression_type;
	private String line_item_selector_id;
	private Double net_value;

	// NEW FIELD
	private Integer quantity;
}
