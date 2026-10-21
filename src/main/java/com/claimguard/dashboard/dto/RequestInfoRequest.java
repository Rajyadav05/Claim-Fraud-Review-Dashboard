package com.claimguard.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class RequestInfoRequest {

    @NotBlank(message = "Query remarks cannot be empty.")
    private String query;

    private List<String> requestedDocTypes;
    private String reviewerName;

    public RequestInfoRequest() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getRequestedDocTypes() {
        return requestedDocTypes;
    }

    public void setRequestedDocTypes(List<String> requestedDocTypes) {
        this.requestedDocTypes = requestedDocTypes;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
}
