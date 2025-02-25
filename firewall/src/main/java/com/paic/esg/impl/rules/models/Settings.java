package com.paic.esg.impl.rules.models;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlRootElement(name = "Settings")
public class Settings {

    private List<Gts> gtsList;
    private List<Rule> rules;

    @XmlElementWrapper(name = "FiltersGts")
    @XmlElement(name = "Gts")
    public List<Gts> getGtsList() {
        return gtsList;
    }

    public void setGtsList(List<Gts> gtsList) {
        this.gtsList = gtsList;
    }

    @XmlElementWrapper(name = "Rules")
    @XmlElement(name = "Rule")
    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}
