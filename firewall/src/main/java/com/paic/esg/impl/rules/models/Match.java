package com.paic.esg.impl.rules.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Match {
    private String primitiveMatch;
    private String imsiMatch;
    private String srcMsisdnMatch;
    private String dstMsisdnMatch;
    private String calledPartyAddressMatch;
    private String callingPartyAddressMatch;
    private String networkNodeNumberMatch;
    private String keywordMessageMatch;

    @XmlAttribute(name = "primitive")
    public String getPrimitive() {
        return primitiveMatch;
    }

    public void setPrimitive(String primitive) {
        this.primitiveMatch = primitive;
    }

    @XmlAttribute(name = "imsi")
    public String getImsi() {
        return imsiMatch;
    }

    public void setImsi(String imsi) {
        this.imsiMatch = imsi;
    }

    @XmlAttribute(name = "src-msisdn")
    public String getSrcMsisdn() {
        return srcMsisdnMatch;
    }

    public void setSrcMsisdn(String msisdn) {
        this.srcMsisdnMatch = msisdn;
    }

    @XmlAttribute(name = "dst-msisdn")
    public String getDstMsisdn() {
        return dstMsisdnMatch;
    }

    public void setDstMsisdn(String msisdn) {
        this.dstMsisdnMatch = msisdn;
    }

    @XmlAttribute(name = "called-party-address")
    public String getCalledPartyAddress() {
        return calledPartyAddressMatch;
    }

    public void setCalledPartyAddress(String calledPartyAddress) {
        this.calledPartyAddressMatch = calledPartyAddress;
    }

    @XmlAttribute(name = "calling-party-address")
    public String getCallingPartyAddress() {
        return callingPartyAddressMatch;
    }

    public void setCallingPartyAddress(String callingPartyAddress) {
        this.callingPartyAddressMatch = callingPartyAddress;
    }

    @XmlAttribute(name = "network-node-number")
    public String getNetworkNodeNumber() {
        return networkNodeNumberMatch;
    }

    public void setNetworkNodeNumber(String mscMatch) {
        this.networkNodeNumberMatch = mscMatch;
    }

    @XmlAttribute(name = "keyword-message")
    public String getKeywordMessage() {
        return keywordMessageMatch;
    }

    public void setKeywordMessage(String keywordMessage) {
        this.keywordMessageMatch = keywordMessage;
    }
    @Override
    public String toString() {
        return "Match{" +
                "primitive='" + getPrimitive() + '\'' +
                ", imsi=" + getImsi() +
                ", src-msisdn=" + getSrcMsisdn() +
                ", dst-msisdn=" + getDstMsisdn() +
                ", calledPartyAddress='" + getCalledPartyAddress() + '\'' +
                ", callingPartyAddress=" + getCallingPartyAddress() +
                ", keywordMessage='" + getKeywordMessage() + '\'' +
                '}';
    }

}
