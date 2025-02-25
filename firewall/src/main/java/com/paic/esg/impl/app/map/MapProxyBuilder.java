package com.paic.esg.impl.app.map;

import com.paic.esg.api.chn.ChannelMessage;
import com.paic.esg.api.network.LayerInterface;
import com.paic.esg.network.layers.MapLayer;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPMessageType;

/**
 * MapProxyBuilder
 */
public class MapProxyBuilder {

  private static final Logger logger = Logger.getLogger(MapProxyBuilder.class);

  public static class Builder {
    private MAPMessageType messageType;
    private MapLayer map;
    private Object message;
    private String transactionId;

    public Builder setChannelMessage(ChannelMessage channelMessage) {
      try {
        String messagetype = (String) channelMessage.getParameter("messageType");
        // check the message
        Object requestMessage = channelMessage.getParameter("message");
        if (requestMessage != null) {
          this.message = requestMessage;
          this.messageType = MAPMessageType.valueOf(messagetype);
        }
        this.transactionId = channelMessage.toString();

      } catch (Exception ex) {
        logger.error("Exception: Message Type: '" + messageType + "'. Error " + ex);
      }
      return this;
    }

    public Builder setMapLayer(LayerInterface channelParameter) {
      MapLayer mapLayer = (MapLayer) channelParameter;
      this.map = mapLayer;
      return this;
    }

    public MapProcessingNode buildMapProcessingNode() {
      MapProcessingNode mapProc = new MapProcessingNode();
      mapProc.setMessageType(messageType);
      mapProc.setMapLayer(map);
      mapProc.setMessage(message);
      mapProc.setTransactionId(transactionId);
      return mapProc;
    }
  }
  private MapProxyBuilder(){
    throw new IllegalStateException("Private Constructor");
  }
}
