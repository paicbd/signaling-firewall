package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.imei.CheckImeiResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyCheckIMEI
 */
public class MapProxyCheckIMEI {
  private static final Logger logger = Logger.getLogger(MapProxyCheckIMEI.class);

  private MapProxyCheckIMEI() {
  }

  /**
   * process response for CheckImeiResponse
   * 
   * @param message CheckImeiResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getResponse(Object message, String transactionId) {
    CheckImeiResponse checkImeiResponse = (CheckImeiResponse) message;
    Long dialogId = checkImeiResponse.getMAPDialog().getLocalDialogId();
    String messageType = checkImeiResponse.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = checkImeiResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);

      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          checkImeiResponse.getMessageType().toString(), dialogId, checkImeiResponse.getInvokeId(),
          transactionId));

      DataElement dataElement = null;
      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
              checkImeiResponse.getMAPDialog().getTCAPMessageType(), dialogId,
              checkImeiResponse.getInvokeId(),
              checkImeiResponse.getMAPDialog().getService().toString()));

      if (checkImeiResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || checkImeiResponse.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      CheckImeiRequest origEvent = (CheckImeiRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogMobility.setUserObject(invokeId);
      mapDialogMobility.addCheckImeiResponse(invokeId, checkImeiResponse.getEquipmentStatus(),
          checkImeiResponse.getBmuef(), checkImeiResponse.getExtensionContainer());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogMobility);
      mapDialogOut.getMapDialog().setNetworkId(checkImeiResponse.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("CheckImeiResponse with DialogId " + dialogId + " failed " + transactionId
          + ". Exception caught '" + mapException + "'");
      logmsg = mapException.getMessage();
    } catch (Exception e) {
      logmsg = e.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  /**
   * process check IMEI request
   * 
   * @param mapProxyDialog MapProxyDialog
   * @param imeiRequest    CheckImeiRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog, CheckImeiRequest imeiRequest,
      String transactionId) {
    Long dialogId = imeiRequest.getMAPDialog().getLocalDialogId();
    String messageType = imeiRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, imeiRequest.getInvokeId(),
                imeiRequest.getMessageType().toString());
        logger.debug(logmsg);
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = imeiRequest.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId '%d', InvokeId '%d', %s",
                imeiRequest.getMessageType().toString(), dialogId, imeiRequest.getInvokeId(),
                transactionId));
        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();
        Long newInvokeId = mapMobilityOut.addCheckImeiRequest(imeiRequest.getIMEI(),
                imeiRequest.getRequestedEquipmentInfo(), imeiRequest.getExtensionContainer());

        mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(imeiRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("CheckImeiRequest with DialogId " + dialogId + " failed. Exception caught '"
              + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
