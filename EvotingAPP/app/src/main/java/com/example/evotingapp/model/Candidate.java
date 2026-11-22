package com.example.evotingapp.model;

import java.text.NumberFormat;
import java.util.Locale;

public class Candidate {
    private final String candidateId;
    private final String fullName;
    private final String visionMission;
    private final double campaignFundAmount;
    private final String fundSummary;

    public Candidate(String candidateId, String fullName, String visionMission,
                     double campaignFundAmount, String fundSummary) {
        this.candidateId = candidateId;
        this.fullName = fullName;
        this.visionMission = visionMission;
        this.campaignFundAmount = campaignFundAmount;
        this.fundSummary = fundSummary;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getVisionMission() {
        return visionMission;
    }

    public double getCampaignFundAmount() {
        return campaignFundAmount;
    }

    public String getFundSummary() {
        return fundSummary;
    }

    public String getFormattedFund() {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        format.setMaximumFractionDigits(0);
        return "Rp " + format.format(campaignFundAmount);
    }
}
