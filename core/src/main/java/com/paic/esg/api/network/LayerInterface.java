package com.paic.esg.api.network;

import com.paic.esg.api.settings.LayerSettingsInterface;
import com.paic.esg.impl.chn.ChannelHandler;

/**
 * LayerInterface
 */
public interface LayerInterface {

  void setChannelHandler(ChannelHandler channelHandler);

  String getName();

  void stop();

  LayerSettingsInterface getSetting();
}