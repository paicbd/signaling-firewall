package com.paic.esg.impl.settings.cdr;

import java.util.List;

public class CdrSettings {
    String name;
    String logger;
    boolean displayName=false;
    String separator="|";
    List<String> fields;
    List<String> operationsTypes;
    boolean allOperationsTypes = false;

    public CdrSettings(String name, String appender, boolean displayName, String separator, List<String> fields, List<String> operationsTypes) {
        this.name = name;
        this.logger = appender;
        this.displayName = displayName;
        this.separator = separator;
        this.fields = fields;
        this.operationsTypes = operationsTypes;
    }

    public CdrSettings() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public boolean isDisplayName() {
        return displayName;
    }

    public void setDisplayName(boolean displayName) {
        this.displayName = displayName;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getOperationsTypes() {
        return operationsTypes;
    }

    public void setOperationsTypes(List<String> operationTypes) {
        this.operationsTypes = operationTypes;
    }

    public boolean isAllOperationsTypes() {
        return allOperationsTypes;
    }

    public void setAllOperationsTypes(boolean allOperationsTypes) {
        this.allOperationsTypes = allOperationsTypes;
    }
}

