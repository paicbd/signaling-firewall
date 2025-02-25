package com.paic.esg.impl.app;

import java.util.Optional;
import com.paic.esg.api.chn.ChannelMessage;
import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProcessingNode;
import com.paic.esg.impl.app.map.MapProxyBuilder;
import com.paic.esg.impl.app.map.helper.MapProxyCDRWriter;
import com.paic.esg.impl.app.map.helper.MapProxyUtilsHelper;
import com.paic.esg.impl.rules.MapProxyApplicationRules;
import com.paic.esg.impl.settings.ApplicationSettings;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import com.paic.esg.network.layers.listeners.MapProxyContants;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;

public class MapProxy extends Application {

  private static final Logger logger = Logger.getLogger(MapProxy.class);

  public MapProxy(ApplicationSettings applicationSettings) {
    super(applicationSettings);
    MapProxyApplicationRules.getInstance()
        .addMapApplicationRules(applicationSettings.getRuleFileName());
    MapProxyUtilsHelper.setCDRName(applicationSettings.getCDRName());
  }

  @Override
  public void processMessage(ChannelMessage channelMessage) {
    try {
      // process incoming message extracted from incoming queue
      String messageType = (String) channelMessage.getParameter(MapProxyContants.MESSAGE_TYPE);
      MAPDialog mapDialog = (MAPDialog) channelMessage.getParameter(MapProxyContants.DIALOG);
      if (messageType == null) {
        logger.warn("[MAP::INVALID_MESSAGE_TYPE]. Message Type is NULL. Discarding message for "
                + channelMessage.toString());
        return;
      }
      logger.debug(String.format("Processing <%s>: Message '%s' received, sending reply.",
              messageType, channelMessage.toString()));

      if (messageType.endsWith("_Request")) {
        processRequestMessages(channelMessage);
      } else if (messageType.endsWith("_Response")) {
        processResponseMessages(channelMessage);
      } else if (messageType.equalsIgnoreCase(MapProxyContants.ON_ERROR_COMPONENT)) {
        // process when there is error
        MAPErrorMessage mapErrorMessage =
                (MAPErrorMessage) channelMessage.getParameter(MapProxyContants.MAP_ERROR_MESSAGE);
        long errorCode = Optional.ofNullable(mapErrorMessage).map(MAPErrorMessage::getErrorCode)
                .map(Long::longValue).orElse(-1L);
        String errorMsg = MapProxyContants.getMapErrorCodeToString(mapErrorMessage);
        MAPDialog mDialog = (MAPDialog) channelMessage.getParameter(MapProxyContants.DIALOG);
        if (mDialog != null) {
          Long dialogId = mDialog.getLocalDialogId();
          MapProxyCDRWriter.writeCDR(dialogId, "Error", errorCode, errorMsg);
        }
      } else {
        processSignals(messageType, mapDialog, channelMessage.toString());
      }
      // send a response back to the channel
      getChannelHandler().sendMessageResponse(channelMessage);
    } catch (Exception ex) {
      logger.error("Exception when processing " + channelMessage.toString() + ". Error: ", ex);
    }
  }

  private void processRequestMessages(ChannelMessage channelMessage) {
    MapProcessingNode map = new MapProxyBuilder.Builder().setChannelMessage(channelMessage)
        .setMapLayer(getChannelHandler().getLayerInterface()).buildMapProcessingNode();

    Object message = channelMessage.getParameter(MapProxyContants.MESSAGE);
    String messageType = (String) channelMessage.getParameter(MapProxyContants.MESSAGE_TYPE);
    // check if there exist a message to be processed.
    // if the message does not exist, it will create a lot of null pointer exception
    // no message
    if (message == null) {
      logger
          .debug("Unable to process <" + messageType + "> request. The MapMessage Object is NULL");
      return;
    }

    MapDialogOut dialogOut = map.processRequest();
    if (dialogOut != null) {
      Long dialogId = dialogOut.getNewDialogId();
      Long newInvokeId = dialogOut.getInvokeId();

      DataElement dataElement = new DataElement(messageType, newInvokeId, dialogId, message);
      String logInvokeId = String.format("InvokeId = %d", newInvokeId);
      if (dialogId > 0 && newInvokeId != null) {
        logger.info(String.format(
            "[MAP::REQUEST<%s>] New DialogId = '%d', Original DialogId = '%d', %s, %s", messageType,
            dialogId, dialogOut.getOriginalDialogId(), logInvokeId, channelMessage.toString()));
        // store the corresponding dialog Id with request object
        Transaction.getInstance().setDialogData(dialogId, newInvokeId, dataElement);
        // add the dialog id to the store
        if (messageType.equals("updateLocation_Request")
            || messageType.equals("updateGprsLocation_Request")) {
          Transaction.getInstance().setDialogId(dialogId, dataElement);
        }
        logger.debug(String.format("Stored ObjectId = %d-%d, Message Type ='%s'", dialogId,
            newInvokeId, messageType));
        // setting up the initials values for the CDRS
        if (MapProxyUtilsHelper.isCDREnabled()) {
          MapProxyCDRWriter.addFields(dialogOut, messageType, channelMessage.getTransactionId());
        }
      } else {
        if (MapProxyUtilsHelper.isCDREnabled()) {
          MapProxyCDRWriter.addFields(dialogOut, messageType, channelMessage.getTransactionId());
        }
      }
      channelMessage.setParameter("DIALOGOUT", dialogOut);
    }
  }

  private void processResponseMessages(ChannelMessage channelMessage) {
    MapProcessingNode map = new MapProxyBuilder.Builder().setChannelMessage(channelMessage)
        .setMapLayer(getChannelHandler().getLayerInterface()).buildMapProcessingNode();
    MapDialogOut respDialogOut = map.processResponse();
    String messageType = (String) channelMessage.getParameter(MapProxyContants.MESSAGE_TYPE);
    // check if the message is not null to avoid nullpointer exceptions
    Object message = channelMessage.getParameter(MapProxyContants.MESSAGE);
    if (message == null) {
      logger
          .debug("Failed to process <" + messageType + "> response. The MapMessage Object is NULL");
      return;
    }
    if (respDialogOut != null) {
      logger.info(String.format("[MAP::RESPONSE<%s>] DialogId = %d, Original DialogId = %d, %s, %s",
          messageType, respDialogOut.getOriginalDialogId(), respDialogOut.getNewDialogId(),
          respDialogOut.getLogInvokeIds(), channelMessage.toString()));
      // use the original dialogId
      if (MapProxyUtilsHelper.isCDREnabled()) {
        if (respDialogOut.getIsResponse()) {
          // now write the cdr
          MapProxyCDRWriter.writeCDR(respDialogOut.getOriginalDialogId(), "Success", null, null);
        } else {
          respDialogOut.getReason().ifPresent(
              u -> MapProxyCDRWriter.writeCDR(respDialogOut.getOriginalDialogId(), u, null, null));
        }
      }
      channelMessage.setParameter("DIALOGOUT", respDialogOut);
    }
  }

  private void processSignals(String messageType, MAPDialog mapDialog, String channelId) {
    if (messageType.equalsIgnoreCase(MapProxyContants.ON_DIALOG_TIMEOUT)) {
      processOnDialogTimeout(mapDialog);
    } else if (messageType.equalsIgnoreCase(MapProxyContants.ON_DIALOG_CLOSE)) {
      // processOnDialogClose(mapDialog)
    } else {
      logger.info(
          String.format("MessageType = '%s' cannot be processed, %s", messageType, channelId));
    }
  }

  private void processOnDialogTimeout(MAPDialog mapDialog) {
    // write the CDR
    if (mapDialog != null) {
      Long dialogId = mapDialog.getLocalDialogId();
      MapProxyCDRWriter.writeCDR(dialogId, "Timeout", null, null);
    }
  }
}
