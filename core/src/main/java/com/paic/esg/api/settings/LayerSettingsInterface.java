package com.paic.esg.api.settings;

import com.paic.esg.api.network.LayerType;

/**
 * LayerSettingsInterface
 */
public interface LayerSettingsInterface {

  public String getTransportName();

  public LayerType getType();

  public String getName();
  
  public boolean isEnabled();
  
}