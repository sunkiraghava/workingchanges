package com.punchh.server.apimodel.redeemable;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedeemableReceiptRule {
	private String qualifier_type;
	private String redeeming_criterion_id;
	private Double discount_amount;
	private Map<String, Object> redeeming_criterion; // Can be empty
}
