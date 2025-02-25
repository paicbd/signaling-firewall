package com.paic.esg.impl.rules;

import lombok.Data;

@Data
public class FilterForRule {
    private String primitive;
    private String imsi;
    private String srcMsisdn;
    private String dstMsisdn;
    private String calledPartyAddress;
    private String callingPartyAddress;
    private String networkNodeNumber;
    private String keywordMessage;

    public FilterForRule() {
    }

    public FilterForRule(String primitive, String imsi, String srcMsisdn, String dstMsisdn, String calledPartyAddress, String callingPartyAddress, String networkNodeNumber, String keywordMessage) {
        this.primitive = primitive;
        this.imsi = imsi;
        this.srcMsisdn = srcMsisdn;
        this.dstMsisdn = dstMsisdn;
        this.calledPartyAddress = calledPartyAddress;
        this.callingPartyAddress = callingPartyAddress;
        this.networkNodeNumber = networkNodeNumber;
        this.keywordMessage = keywordMessage;
    }

    @Override
    public String toString() {
        return "FilterForRule{" +
                "primitive='" + primitive + '\'' +
                ", imsi='" + imsi + '\'' +
                ", src-msisdn='" + srcMsisdn + '\'' +
                ", dst-msisdn='" + dstMsisdn + '\'' +
                ", calledPartyAddress='" + calledPartyAddress + '\'' +
                ", callingPartyAddress='" + callingPartyAddress + '\'' +
                ", networkNodeNumber='" + networkNodeNumber + '\'' +
                ", keywordMessage='" + keywordMessage + '\'' +
                '}';
    }
}
