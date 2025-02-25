package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.callhandling.MAPDialogCallHandling;
import org.restcomm.protocols.ss7.map.api.service.callhandling.SendRoutingInformationRequest;
import org.restcomm.protocols.ss7.map.api.service.callhandling.SendRoutingInformationResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxySendRoutingInfo
 */
public class MapProxySendRoutingInfo {

  private static final Logger logger = Logger.getLogger(MapProxySendRoutingInfo.class);

  private MapProxySendRoutingInfo(){}

  public static MapDialogOut getResponse(Object message, String transactionId) {
    SendRoutingInformationResponse sendRoutingInfoResponse =
        (SendRoutingInformationResponse) message;
    Long dialogId = sendRoutingInfoResponse.getMAPDialog().getLocalDialogId();
    String messageType = sendRoutingInfoResponse.getMessageType().toString();
    try {
      Long respInvokeId = sendRoutingInfoResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);

      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          sendRoutingInfoResponse.getMessageType().toString(), dialogId,
          sendRoutingInfoResponse.getInvokeId(), transactionId));
      DataElement dataElement = null;
      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
              sendRoutingInfoResponse.getMAPDialog().getTCAPMessageType(), dialogId,
              sendRoutingInfoResponse.getInvokeId(),
              sendRoutingInfoResponse.getMAPDialog().getService().toString()));

      if (sendRoutingInfoResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || sendRoutingInfoResponse.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
        logger.debug("Closing for dialogId = " + dialogId);
        dataElement = Transaction.getInstance().removeDialogData(dialogId, respInvokeId);
        mapDialogOut.setIsResponse();
      } else {
        dataElement = Transaction.getInstance().getDialogData(dialogId, respInvokeId);
      }

      if (dataElement == null) {
        String logmsg = String.format("Dialog Id = %d not found in Transaction Map. %s", dialogId,
            transactionId);
        return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
      }
      SendRoutingInformationRequest origEvent =
          (SendRoutingInformationRequest) dataElement.getRequestObject();
      MAPDialogCallHandling origCallHandlingOut = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();
      origCallHandlingOut.setUserObject(invokeId);
      origCallHandlingOut.addSendRoutingInformationResponse(invokeId,
          sendRoutingInfoResponse.getIMSI(), sendRoutingInfoResponse.getCUGCheckInfo(),
          sendRoutingInfoResponse.getRoutingInfo2());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(origCallHandlingOut);
      mapDialogOut.getMapDialog().setNetworkId(sendRoutingInfoResponse.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("SendRoutingInformationResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
          return MapProxyUtilsHelper.discardReason(mapException.getMessage(), messageType, transactionId, dialogId);
    } catch(Exception ex){
      return MapProxyUtilsHelper.discardReason(ex.getMessage(), messageType, transactionId, dialogId);
    }
  }


  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      SendRoutingInformationRequest dialogInSendRoutingInfo, String transactionId) {
    Long dialogId = 0L;
    String messageType = dialogInSendRoutingInfo.getMessageType().toString();
    try {
      dialogId = dialogInSendRoutingInfo.getMAPDialog().getLocalDialogId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      if (mapProxyDialog == null) {
        String logMsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, dialogInSendRoutingInfo.getInvokeId(),
                dialogInSendRoutingInfo.getMessageType().toString());
        logger.debug(logMsg);
        MAPDialogCallHandling dialogIn = dialogInSendRoutingInfo.getMAPDialog();
        dialogIn.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(dialogIn);
        mapDialogOut.setInvokeId(null);
      } else {
        logger
            .debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                dialogInSendRoutingInfo.getMessageType().toString(), dialogId,
                dialogInSendRoutingInfo.getInvokeId(), transactionId));

        MAPDialogCallHandling callHandlingOut = mapProxyDialog.getMapDialogCallHandling();
        Long newInvokeId = callHandlingOut.addSendRoutingInformationRequest(
            dialogInSendRoutingInfo.getMsisdn(), dialogInSendRoutingInfo.getCUGCheckInfo(),
            dialogInSendRoutingInfo.getNumberOfForwarding(),
            dialogInSendRoutingInfo.getInterrogationType(),
            dialogInSendRoutingInfo.getORInterrogation(), dialogInSendRoutingInfo.getORCapability(),
            dialogInSendRoutingInfo.getGmscOrGsmSCFAddress(),
            dialogInSendRoutingInfo.getCallReferenceNumber(),
            dialogInSendRoutingInfo.getForwardingReason(),
            dialogInSendRoutingInfo.getBasicServiceGroup(),
            dialogInSendRoutingInfo.getNetworkSignalInfo(), dialogInSendRoutingInfo.getCamelInfo(),
            dialogInSendRoutingInfo.getSuppressionOfAnnouncement(),
            dialogInSendRoutingInfo.getExtensionContainer(),
            dialogInSendRoutingInfo.getAlertingPattern(), dialogInSendRoutingInfo.getCCBSCall(),
            dialogInSendRoutingInfo.getSupportedCCBSPhase(),
            dialogInSendRoutingInfo.getAdditionalSignalInfo(),
            dialogInSendRoutingInfo.getIstSupportIndicator(),
            dialogInSendRoutingInfo.getPrePagingSupported(),
            dialogInSendRoutingInfo.getCallDiversionTreatmentIndicator(),
            dialogInSendRoutingInfo.getLongFTNSupported(),
            dialogInSendRoutingInfo.getSuppressVtCSI(),
            dialogInSendRoutingInfo.getSuppressIncomingCallBarring(),
            dialogInSendRoutingInfo.getGsmSCFInitiatedCall(),
            dialogInSendRoutingInfo.getBasicServiceGroup2(),
            dialogInSendRoutingInfo.getNetworkSignalInfo2(),
            dialogInSendRoutingInfo.getSuppressMTSS(),
            dialogInSendRoutingInfo.getMTRoamingRetrySupported(),
            dialogInSendRoutingInfo.getCallPriority());

        mapDialogOut.setMapDialog(callHandlingOut);
        mapDialogOut.setInvokeId(newInvokeId);
        mapDialogOut.setProxyDialog(mapProxyDialog);
      }
      mapDialogOut.getMapDialog().setNetworkId(dialogInSendRoutingInfo.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("SendRoutingInformationRequest with DialogId " + dialogId
          + " failed. Exception caught '" + mapException + "', " + transactionId);
          return MapProxyUtilsHelper.discardReason(mapException.getMessage(), messageType, transactionId, dialogId);
    } catch (Exception ex) {
      logger.error("Error occurred: ", ex);
      return MapProxyUtilsHelper.discardReason(ex.getMessage(), messageType, transactionId, dialogId);
    }
  }
}
