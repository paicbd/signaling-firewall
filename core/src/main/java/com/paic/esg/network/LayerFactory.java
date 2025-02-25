package com.paic.esg.network;

import com.paic.esg.api.network.LayerInterface;
import com.paic.esg.api.network.LayerType;
import com.paic.esg.api.settings.LayerSettingsInterface;
import com.paic.esg.impl.settings.cap.CapSettings;
import com.paic.esg.impl.settings.m3ua.M3uaSettings;
import com.paic.esg.impl.settings.map.MapSettings;
import com.paic.esg.impl.settings.sccp.SccpSettings;
import com.paic.esg.impl.settings.sctp.SctpSettings;
import com.paic.esg.impl.settings.tcap.TcapSettings;
import com.paic.esg.network.layers.CapLayer;
import com.paic.esg.network.layers.M3uaLayer;
import com.paic.esg.network.layers.MapLayer;
import com.paic.esg.network.layers.SccpLayer;
import com.paic.esg.network.layers.SctpLayer;
import com.paic.esg.network.layers.TcapLayer;
import org.apache.log4j.Logger;

public class LayerFactory {

  private static final Logger logger = Logger.getLogger(LayerFactory.class);

  public static LayerInterface createLayerInstance(LayerType layerType,
      LayerSettingsInterface layerSettings, LayerInterface transportLayerName) {
    LayerInterface layerInterface = null;

    try {
      switch (layerType) {
        case M3ua:
          layerInterface =
              new M3uaLayer((M3uaSettings) layerSettings, (SctpLayer) transportLayerName);
          break;
        case Sccp:
          layerInterface =
              new SccpLayer((SccpSettings) layerSettings, (M3uaLayer) transportLayerName);
          break;
        case Tcap:
          layerInterface =
              new TcapLayer((TcapSettings) layerSettings, (SccpLayer) transportLayerName);
          break;
        case Map:
          layerInterface =
              new MapLayer((MapSettings) layerSettings, (TcapLayer) transportLayerName);
          break;
        case Cap:
          layerInterface =
              new CapLayer((CapSettings) layerSettings, (TcapLayer) transportLayerName);
          break;
          default:
          break;
      }
    } catch (Exception e) {
      logger.error("Caught exception while initializing layer '" + layerSettings.getName() + "'",
          e);
    }

    return layerInterface;
  }

  public static LayerInterface createLayerInstance(LayerType layerType,
      LayerSettingsInterface layerSettings) {
    LayerInterface layerInterface = null;

    try {
      switch (layerType) {
        case Sctp:
          layerInterface = new SctpLayer((SctpSettings) layerSettings);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      logger.error("Caught exception while initializing layer '" + layerSettings.getName() + "'",
          e);
    }

    return layerInterface;
  }
}
