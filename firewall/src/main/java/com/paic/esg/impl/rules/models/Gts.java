package com.paic.esg.impl.rules.models;

import jakarta.xml.bind.annotation.*;

import java.util.List;

public class Gts {
    private String name;
    private Boolean regex;
    private List<String> gtList;

    @XmlAttribute(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "regex")
    public Boolean getRegex() {
        return regex;
    }

    public void setRegex(Boolean regex) {
        this.regex = regex;
    }

    @XmlElement(name = "Gt")
    public List<String> getGtList() {
        return gtList;
    }

    public void setGtList(List<String> gtList) {
        this.gtList = gtList;
    }
}
