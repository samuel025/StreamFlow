package com.lockedfunds.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DataResponseDTO {
    private String status;
    private String message;
    private Map<String, Object> data;
}
