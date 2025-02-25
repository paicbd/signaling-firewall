package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyAnyTimeInterrogation
 */
public class MapProxyAnyTimeInterrogation {

  private static final Logger logger = Logger.getLogger(MapProxyAnyTimeInterrogation.class);

  private MapProxyAnyTimeInterrogation() {
  }

  /**
   * process response for AnyTimeInterrogationResponse
   * 
   * @param message AnyTimeInterrogationResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getResponse(Object message, String transactionId) {
    AnyTimeInterrogationResponse anyTimeInterrogationResponse =
        (AnyTimeInterrogationResponse) message;
    Long dialogId = anyTimeInterrogationResponse.getMAPDialog().getLocalDialogId();
    String messageType = anyTimeInterrogationResponse.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = anyTimeInterrogationResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      AnyTimeInterrogationResponseCopy clone =
          new AnyTimeInterrogationResponseCopy(anyTimeInterrogationResponse);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          anyTimeInterrogationResponse.getMessageType().toString(), dialogId,
          anyTimeInterrogationResponse.getInvokeId(), transactionId));
      DataElement dataElement = null;

      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
              anyTimeInterrogationResponse.getMAPDialog().getTCAPMessageType(), dialogId,
              anyTimeInterrogationResponse.getInvokeId(),
              anyTimeInterrogationResponse.getMAPDialog().getService().toString()));

      if (anyTimeInterrogationResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || anyTimeInterrogationResponse.getMAPDialog()
              .getTCAPMessageType() == MessageType.Abort) {
        // close the dialog
        logger.debug("Closing for dialogId = " + dialogId);
        mapDialogOut.setIsResponse();
        dataElement = Transaction.getInstance().removeDialogData(dialogId, respInvokeId);
      } else {
        dataElement = Transaction.getInstance().getDialogData(dialogId, respInvokeId);
      }

      if (dataElement == null) {
        logmsg = String.format("Dialog Id = '%d' not found in Transaction Map. %s", dialogId,
            transactionId);
        return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
      }
      AnyTimeInterrogationRequest origEvent =
          (AnyTimeInterrogationRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogMobility.setUserObject(invokeId);
      mapDialogMobility.addAnyTimeInterrogationResponse(invokeId, clone.getSubscriberInfo(),
          clone.getExtensionContainer());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogMobility.setNetworkId(anyTimeInterrogationResponse.getMAPDialog().getNetworkId());
      mapDialogOut.setMapDialog(mapDialogMobility);
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("AnyTimeInterrogationResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  /**
   * process Any Time Interrogation request
   * 
   * @param mapProxyDialog              MapProxyDialog
   * @param anyTimeInterrogationRequest AnyTimeInterrogationRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      AnyTimeInterrogationRequest anyTimeInterrogationRequest, String transactionId) {
    Long dialogId = anyTimeInterrogationRequest.getMAPDialog().getLocalDialogId();
    String messageType = anyTimeInterrogationRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, anyTimeInterrogationRequest.getInvokeId(),
                anyTimeInterrogationRequest.getMessageType().toString());
        logger.debug(logmsg);
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = anyTimeInterrogationRequest.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId '%d', InvokeId '%d', %s",
                anyTimeInterrogationRequest.getMessageType().toString(), dialogId,
                anyTimeInterrogationRequest.getInvokeId(), transactionId));
        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();

        Long newInvokeId = mapMobilityOut.addAnyTimeInterrogationRequest(
                anyTimeInterrogationRequest.getSubscriberIdentity(),
                anyTimeInterrogationRequest.getRequestedInfo(),
                anyTimeInterrogationRequest.getGsmSCFAddress(),
                anyTimeInterrogationRequest.getExtensionContainer());


        mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(anyTimeInterrogationRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("AnyTimeInterrogationRequest with DialogId " + dialogId
              + " failed. Exception caught '" + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
