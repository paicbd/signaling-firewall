package com.paic.esg.impl.rules.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Rule {
    private String nameRule;
    private Boolean regexRule;
    private String gtsAllowedListName;
    private String gtsBlockedListName;
    private Match matchElement;

    @XmlAttribute(name = "name")
    public String getName() {
        return nameRule;
    }

    public void setName(String name) {
        this.nameRule = name;
    }

    @XmlAttribute(name = "regex")
    public Boolean getRegex() {
        return regexRule;
    }

    public void setRegex(Boolean regex) {
        this.regexRule = regex;
    }

    @XmlAttribute(name = "gts-allowed")
    public String getGtsAllowed() {
        return gtsAllowedListName;
    }

    public void setGtsAllowed(String gtsAllowed) {
        this.gtsAllowedListName = gtsAllowed;
    }

    @XmlAttribute(name = "gts-blocked")
    public String getGtsBlocked() {
        return gtsBlockedListName;
    }

    public void setGtsBlocked(String gtsBlocked) {
        this.gtsBlockedListName = gtsBlocked;
    }


    @XmlElement(name = "Match")
    public Match getMatch() {
        return matchElement;
    }

    public void setMatch(Match match) {
        this.matchElement = match;
    }
}
