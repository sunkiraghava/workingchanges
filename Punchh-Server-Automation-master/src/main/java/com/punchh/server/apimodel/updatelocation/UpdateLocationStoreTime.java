package com.punchh.server.apimodel.updatelocation;

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
public class UpdateLocationStoreTime {

	private String day;

	@JsonProperty("start_time")
	private String startTime;

	@JsonProperty("end_time")
	private String endTime;
}