package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxySendRoutingInfoForSM
 */
public class MapProxySendRoutingInfoForSM {

  private static final Logger logger = Logger.getLogger(MapProxySendRoutingInfoForSM.class);

  private MapProxySendRoutingInfoForSM() {
  }

  public static MapDialogOut processRequest(MapProxyDialog mapProxyDialog,
      SendRoutingInfoForSMRequest request, String transactionId) {
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
        mapDialogOut.setDiscardReason(logmsg);
        MAPDialogSms smsHandlerIn = request.getMAPDialog();
        smsHandlerIn.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(smsHandlerIn);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] dialogId = '%d', InvokeId = '%d', %s",
            request.getMessageType().toString(), dialogId, request.getInvokeId(), transactionId));
        MAPDialogSms smsHandlerOut = mapProxyDialog.getMapDialogSms();
        Long newInvokeId = smsHandlerOut.addSendRoutingInfoForSMRequest(request.getMsisdn(),
            request.getSm_RP_PRI(), request.getServiceCentreAddress(),
            request.getExtensionContainer(), request.getGprsSupportIndicator(),
            request.getSM_RP_MTI(), request.getSM_RP_SMEA(), request.getSmDeliveryNotIntended(),
            request.getIpSmGwGuidanceIndicator(), mapProxyDialog.getImsi(),
            request.getT4TriggerIndicator(), request.getSingleAttemptDelivery(),
            request.getTeleservice(), request.getCorrelationID());

        mapDialogOut.setMapDialog(smsHandlerOut);
        mapDialogOut.setInvokeId(newInvokeId);
        mapDialogOut.setProxyDialog(mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(request.getMAPDialog().getNetworkId());
      }
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("Exception caugth for SendRoutingInfoForSM, tid '" + transactionId + "'", mapException);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  public static MapDialogOut processResponse(Object message, String transactionId) {
    SendRoutingInfoForSMResponse routingInfoForSMResponse = (SendRoutingInfoForSMResponse) message;
    Long dialogId = routingInfoForSMResponse.getMAPDialog().getLocalDialogId();
    String messageType = routingInfoForSMResponse.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = routingInfoForSMResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);

      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          routingInfoForSMResponse.getMessageType().toString(), dialogId,
          routingInfoForSMResponse.getInvokeId(), transactionId));

      DataElement dataElement = null;
      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, Service = '%s', InvokeId = %d",
              routingInfoForSMResponse.getMAPDialog().getTCAPMessageType(), dialogId,
              routingInfoForSMResponse.getMAPDialog().getService().toString(),
              routingInfoForSMResponse.getInvokeId()));

      if (routingInfoForSMResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || routingInfoForSMResponse.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      SendRoutingInfoForSMRequest origEvent =
          (SendRoutingInfoForSMRequest) dataElement.getRequestObject();
      MAPDialogSms mapDialogSms = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogSms.setUserObject(invokeId);
      mapDialogSms.addSendRoutingInfoForSMResponse(invokeId, routingInfoForSMResponse.getIMSI(),
          routingInfoForSMResponse.getLocationInfoWithLMSI(),
          routingInfoForSMResponse.getExtensionContainer(), routingInfoForSMResponse.getMwdSet(),
          routingInfoForSMResponse.getIpSmGwGuidance());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogSms);
      mapDialogOut.getMapDialog().setNetworkId(routingInfoForSMResponse.getMAPDialog().getNetworkId());

      return mapDialogOut;
    } catch (MAPException mapex) {
      logger.error("SendRoutingInfoForSMResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapex + "'");
      logmsg = mapex.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
