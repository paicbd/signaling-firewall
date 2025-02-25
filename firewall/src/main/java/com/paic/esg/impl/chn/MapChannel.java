package com.paic.esg.impl.chn;

import java.util.ArrayList;
import java.util.List;
import com.paic.esg.api.chn.ChannelMessage;
import com.paic.esg.api.network.LayerInterface;
import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.helper.MapProxyCDRWriter;
import com.paic.esg.impl.settings.ChannelSettings;
import com.paic.esg.info.Transaction;
import com.paic.esg.network.layers.MapLayer;
import com.paic.esg.network.layers.listeners.MapProxyContants;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;

/**
 * MapChannel
 */
public class MapChannel extends ChannelHandler {

  private static final Logger logger = Logger.getLogger(MapChannel.class);
  private static final List<String> handleSignalList = new ArrayList<>();
  static {
    handleSignalList.add(MapProxyContants.ON_DIALOG_TIMEOUT);
    handleSignalList.add(MapProxyContants.ON_INVOKE_TIMEOUT);
    handleSignalList.add(MapProxyContants.ON_DIALOG_CLOSE);
  }

  private MapLayer map = null;
  private ChannelSettings channelSetting = null;

  public MapChannel(ChannelSettings channelSettings) {
    super(channelSettings);
    this.channelSetting = channelSettings;
  }

  @Override
  public void channelInitialize(LayerInterface[] layerInterface) {
    map = (MapLayer) layerInterface[0];
    try {
      // Get the MaxDialog from the TCAP to initialize the TransactionMap maxTransactions
      // get the TCAPStack
      int maxDialogs = map.getMapStack().getTCAPStack().getMaxDialogs();
      Transaction.getInstance().setMaxDialog(maxDialogs);
    } catch (Exception ex) {
      logger.error("ERROR: Failed to get Max Dialogs from TCAP Layer. Setting default value");
    }
    logger.debug("MapChannel initialized complete!");
  }

  private void logMessages(String messageType, String chnMessage) {
    if (messageType.isEmpty())
      return;
    if (messageType.endsWith("Request")) {
      logger.debug(String.format("[MAP::REQUEST<%s>]   '%s' to application.",
          messageType, chnMessage));
    } else if (messageType.endsWith("Response")) {
      logger.debug(String.format("[MAP::RESPONSE<%s>] Sending message '%s' to application.",
          messageType, chnMessage));
    } else {
      logger.debug(String.format("Sending message '%s' to application.", chnMessage));
    }
  }

  @Override
  public void receiveMessageRequest(ChannelMessage channelMessage) {
    try {
      Object message = channelMessage.getParameter(MapProxyContants.MESSAGE);
      String messageType = (String) channelMessage.getParameter(MapProxyContants.MESSAGE_TYPE);
      if (message != null && messageType != null) {
        logMessages(messageType, channelMessage.toString());
        // only send message which are defined in the configurations
        if (this.channelSetting != null && this.channelSetting.isPrimitiveExist(messageType)) {
          sendMessageRequest(channelMessage);
        }
      } else {
        MAPDialog mapDialog = (MAPDialog) channelMessage.getParameter("dialog");
        if (mapDialog != null) {
          logger.debug(String.format(
              "[MAP::SIGNAL<%s>] dialogId = '%d', appCtx<%s>, NetworkId = %d, %s", messageType,
              mapDialog.getLocalDialogId(), mapDialog.getApplicationContext().toString(),
              mapDialog.getNetworkId(), channelMessage.toString()));
          // handle onDialogTimeout
          if (handleSignalList.contains(messageType)) {
            sendMessageRequest(channelMessage);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Error occurred forwarding message to MapProxy. Error: ", ex);
    }
  }

  @Override
  public int sendMessageResponse(ChannelMessage channelMessage) {
    // send a response back to the channel
    String messageType = "";
    try {
      messageType = (String) channelMessage.getParameter(MapProxyContants.MESSAGE_TYPE);
      Object paramDialogOut = channelMessage.getParameter("DIALOGOUT");
      if (paramDialogOut != null) {
        MapDialogOut dialogOut = (MapDialogOut) paramDialogOut;
        // check if there is discard message then don't send the MAP message
        String discardMsg = dialogOut.getDiscardReason();
        if (discardMsg != null && !discardMsg.isEmpty()) {
          logger.info(String.format("MAP::DISCARD<%s>] Reason: %s, %s", messageType,
              dialogOut.getDiscardReason(), channelMessage.toString()));

          MapProxyCDRWriter.writeCDR(dialogOut.getOriginalDialogId(), "Error", -1L, dialogOut.getDiscardReason());
        } else {
          String classDialogOut = dialogOut.getDialogOutName(messageType);
          logger.info(String.format(
              "MAP-message '%s' sending from '%s' dialog '%d' to remote '%s' dialog '%d', %s",
              classDialogOut, dialogOut.getLocalAddress(), dialogOut.getLocalDialogId(),
              dialogOut.getRemoteAddress(), dialogOut.getRemoteDialogId(),
              channelMessage.toString()));
          dialogOut.send();
        }
      } else {
        if (!messageType.equals("onDialogClose")) {
          logger
              .info(String.format("MAP::DISCARD<%s>] Discarding message for %s with unknown reason",
                  messageType, channelMessage.toString()));
        }
      }
    } catch (Exception e) {
      logger.error("Exception caught for <" + messageType + ">: " + channelMessage.toString(), e);
    }
    return 0;

  }

  @Override
  public LayerInterface getLayerInterface() {
    return map;
  }

  @Override
  public LayerInterface getLayerInterface(String serviceFunctionName) { // scf, ssf
    return null;
  }

}
