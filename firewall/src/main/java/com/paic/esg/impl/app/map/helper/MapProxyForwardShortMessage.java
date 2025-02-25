package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyForwardShortMessage
 */
public class MapProxyForwardShortMessage {

  private MapProxyForwardShortMessage() {
  }

  private static final Logger logger = Logger.getLogger(MapProxyForwardShortMessage.class);

  public static MapDialogOut processRequest(MapProxyDialog mapProxyDialog,
      ForwardShortMessageRequest request, String transactionId) {
    Long dialogId = request.getMAPDialog().getLocalDialogId();
    String messageType = request.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, request.getInvokeId(), request.getMessageType().toString());
        logger.debug(logmsg);
        mapDialogOut.setInvokeId(null);
        MAPDialogSms smsHandlerIn = request.getMAPDialog();
        smsHandlerIn.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(smsHandlerIn);
      } else {

        logger.debug(String.format("[MAP::REQUEST<%s>] dialogId = '%d', InvokeId = '%d', %s",
            request.getMessageType().toString(), dialogId, request.getInvokeId(), transactionId));

        MAPDialogSms smsHandlerOut = mapProxyDialog.getMapDialogSms();
        Long newInvokeId = smsHandlerOut.addForwardShortMessageRequest(request.getSM_RP_DA(),
            request.getSM_RP_OA(), request.getSM_RP_UI(), request.getMoreMessagesToSend());
        mapDialogOut.setMapDialog(smsHandlerOut);
        mapDialogOut.setInvokeId(newInvokeId);
        mapDialogOut.setProxyDialog(mapProxyDialog);
      }
      mapDialogOut.getMapDialog().setNetworkId(request.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("Processing forward SM Request failed " + mapException + ", " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  public static MapDialogOut processResponse(Object message, String transactionId) {
    ForwardShortMessageResponse response = (ForwardShortMessageResponse) message;
    Long dialogId = response.getMAPDialog().getLocalDialogId();
    String messageType = response.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = response.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);

      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          response.getMessageType().toString(), dialogId, response.getInvokeId(), transactionId));

      DataElement dataElement = null;
      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, Service = '%s', InvokeId = %d",
              response.getMAPDialog().getTCAPMessageType(), dialogId,
              response.getMAPDialog().getService().toString(), response.getInvokeId()));

      if (response.getMAPDialog().getTCAPMessageType() == MessageType.End
          || response.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
        logger.debug("Closing for dialogId = " + dialogId);
        dataElement = Transaction.getInstance().removeDialogData(dialogId, respInvokeId);
        mapDialogOut.setIsResponse();
      } else {
        dataElement = Transaction.getInstance().getDialogData(dialogId, respInvokeId);
      }
      if (dataElement == null) {
        logmsg = String.format("Dialog Id = %d not found in Transaction Map. %s", dialogId,
            transactionId);
        return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
      }
      ForwardShortMessageRequest origEvent =
          (ForwardShortMessageRequest) dataElement.getRequestObject();
      MAPDialogSms mapDialogSms = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogSms.setUserObject(invokeId);
      mapDialogSms.addForwardShortMessageResponse(invokeId);

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogSms);
      mapDialogOut.getMapDialog().setNetworkId(response.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("ForwardShortMessageResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }


}
