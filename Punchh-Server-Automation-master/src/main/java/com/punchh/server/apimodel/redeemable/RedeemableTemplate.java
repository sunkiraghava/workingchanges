package com.punchh.server.apimodel.redeemable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedeemableTemplate {
	private String redemption_message;
	private String short_prompt;
	private String standard_prompt;
}
