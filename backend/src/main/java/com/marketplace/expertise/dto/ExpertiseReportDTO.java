package com.marketplace.expertise.dto;

import com.marketplace.expertise.entity.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpertiseReportDTO {

    private Long expertiseRequestId;

    private ProductCondition productCondition;
    private AuthenticityLevel authenticityLevel;
    private Double estimatedMinPrice;
    private Double estimatedMaxPrice;
    private Double recommendedStartPrice;
    private ExpertiseRecommendation recommendation;
    private String commentsPublic;
    private String commentsInternal;

}