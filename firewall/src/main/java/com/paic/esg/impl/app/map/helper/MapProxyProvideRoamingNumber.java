package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.callhandling.MAPDialogCallHandling;
import org.restcomm.protocols.ss7.map.api.service.callhandling.ProvideRoamingNumberRequest;
import org.restcomm.protocols.ss7.map.api.service.callhandling.ProvideRoamingNumberResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyProvideRoamingNumber
 */
public class MapProxyProvideRoamingNumber {

  private static final Logger logger = Logger.getLogger(MapProxyProvideRoamingNumber.class);

  private MapProxyProvideRoamingNumber() {
  }

  public static MapDialogOut getResponse(Object message, String transactionId) {
    ProvideRoamingNumberResponse event = (ProvideRoamingNumberResponse) message;
    Long dialogId = event.getMAPDialog().getLocalDialogId();
    String messageType = event.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = event.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      ProvideRoamingNumberResponseCopy clone = new ProvideRoamingNumberResponseCopy(event);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          event.getMessageType().toString(), dialogId, event.getInvokeId(), transactionId));

      DataElement dataElement = null;
      logger.debug(String.format("TCAP Message Type = '%s', dialogId = %d, Service = '%s'",
          event.getMAPDialog().getTCAPMessageType(), dialogId,
          event.getMAPDialog().getService().toString()));

      if (event.getMAPDialog().getTCAPMessageType() == MessageType.End
          || event.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      ProvideRoamingNumberRequest origEvent =
          (ProvideRoamingNumberRequest) dataElement.getRequestObject();
      MAPDialogCallHandling callHandlingOut = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      callHandlingOut.setUserObject(invokeId);
      callHandlingOut.addProvideRoamingNumberResponse(invokeId, clone.getRoamingNumber(),
          clone.getExtensionContainer(), clone.getReleaseResourcesSupported(),
          clone.getVmscAddress());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(callHandlingOut);
      mapDialogOut.getMapDialog().setNetworkId(event.getMAPDialog().getNetworkId());
      // return the builder
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("ProvideRoamingNumberResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
    }
    return null;
  }


  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      ProvideRoamingNumberRequest roamingNumberRequest, String transactionId) {
    Long dialogId = roamingNumberRequest.getMAPDialog().getLocalDialogId();
    String messageType = roamingNumberRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, roamingNumberRequest.getInvokeId(),
                roamingNumberRequest.getMessageType().toString());
        logger.debug(logmsg);
        logmsg = String.format("MAP Application Rule not found for DialogId = '%d', InvokeId = '%d'",
                dialogId, roamingNumberRequest.getInvokeId());
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogCallHandling mapDialogCallHandling = roamingNumberRequest.getMAPDialog();
        mapDialogCallHandling.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogCallHandling);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', invokeId = '%d', %s",
                roamingNumberRequest.getMessageType().toString(), dialogId,
                roamingNumberRequest.getInvokeId(), transactionId));

        MAPDialogCallHandling callHandlingOut = mapProxyDialog.getMapDialogCallHandling();
        Long newInvokeId = callHandlingOut.addProvideRoamingNumberRequest(mapProxyDialog.getImsi(),
                roamingNumberRequest.getMscNumber(), roamingNumberRequest.getMsisdn(),
                roamingNumberRequest.getLmsi(), roamingNumberRequest.getGsmBearerCapability(),
                roamingNumberRequest.getNetworkSignalInfo(),
                roamingNumberRequest.getSuppressionOfAnnouncement(),
                roamingNumberRequest.getGmscAddress(), roamingNumberRequest.getCallReferenceNumber(),
                roamingNumberRequest.getOrInterrogation(), roamingNumberRequest.getExtensionContainer(),
                roamingNumberRequest.getAlertingPattern(), roamingNumberRequest.getCcbsCall(),
                roamingNumberRequest.getSupportedCamelPhasesInInterrogatingNode(),
                roamingNumberRequest.getAdditionalSignalInfo(),
                roamingNumberRequest.getOrNotSupportedInGMSC(),
                roamingNumberRequest.getPrePagingSupported(), roamingNumberRequest.getLongFTNSupported(),
                roamingNumberRequest.getSuppressVtCsi(),
                roamingNumberRequest.getOfferedCamel4CSIsInInterrogatingNode(),
                roamingNumberRequest.getMtRoamingRetrySupported(), roamingNumberRequest.getPagingArea(),
                roamingNumberRequest.getCallPriority(), roamingNumberRequest.getMtrfIndicator(),
                roamingNumberRequest.getOldMSCNumber());
        mapDialogOut = new MapDialogOut(callHandlingOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(roamingNumberRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("ProvideRoamingNumberRequest with DialogId " + dialogId
              + " failed. Exception caught '" + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
