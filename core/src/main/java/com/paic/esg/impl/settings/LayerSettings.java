package com.paic.esg.impl.settings;

import com.paic.esg.api.settings.LayerSettingsInterface;

public class LayerSettings implements ISettings {

  private LayerSettingsInterface settings;
  private String name;
  private String parentName;

  public String getName() {
    return this.name;
  }

  public LayerSettingsInterface getSettings() {
    return settings;
  }

  public String getParentNodeName(){
    return this.parentName;
  }

  public LayerSettings(String name, String parentNodeName, LayerSettingsInterface settings) {
    this.settings = settings;
    this.name = name;
    this.parentName = parentNodeName;
  }
}
