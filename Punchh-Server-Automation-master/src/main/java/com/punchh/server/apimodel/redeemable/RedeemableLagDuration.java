package com.punchh.server.apimodel.redeemable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedeemableLagDuration {
	private Integer value;
	private String units;
}
