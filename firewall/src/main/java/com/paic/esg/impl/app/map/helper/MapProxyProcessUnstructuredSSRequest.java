package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.supplementary.MAPDialogSupplementary;
import org.restcomm.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;
import org.restcomm.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyProcessUnstructuredSSRequest
 */
public class MapProxyProcessUnstructuredSSRequest {

  private static final Logger logger = Logger.getLogger(MapProxyProcessUnstructuredSSRequest.class);

  private MapProxyProcessUnstructuredSSRequest() {
  }

  public static MapDialogOut getResponse(Object message, String transactionId) {
    ProcessUnstructuredSSResponse procUnstrResInd = (ProcessUnstructuredSSResponse) message;
    Long dialogId = procUnstrResInd.getMAPDialog().getLocalDialogId();
    String messageType = procUnstrResInd.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = procUnstrResInd.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          procUnstrResInd.getMessageType().toString(),
          procUnstrResInd.getMAPDialog().getLocalDialogId(), procUnstrResInd.getInvokeId(),
          transactionId));

      DataElement dataElement = null;
      logger.debug(String.format(
          "TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
          procUnstrResInd.getMAPDialog().getTCAPMessageType(), dialogId,
          procUnstrResInd.getInvokeId(), procUnstrResInd.getMAPDialog().getService().toString()));

      if (procUnstrResInd.getMAPDialog().getTCAPMessageType() == MessageType.End
          || procUnstrResInd.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      ProcessUnstructuredSSRequest origEvent =
          (ProcessUnstructuredSSRequest) dataElement.getRequestObject();
      MAPDialogSupplementary mapDialogSupplementary = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogSupplementary.setUserObject(invokeId);
      mapDialogSupplementary.addProcessUnstructuredSSResponse(invokeId,
          procUnstrResInd.getDataCodingScheme(), procUnstrResInd.getUSSDString());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogSupplementary);
      mapDialogOut.getMapDialog().setNetworkId(procUnstrResInd.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("ProcessUnstructuredSSResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }


  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      ProcessUnstructuredSSRequest procUnstrReqInd, String transactionId) {
    Long dialogId = procUnstrReqInd.getMAPDialog().getLocalDialogId();
    String messageType = procUnstrReqInd.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);



    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, procUnstrReqInd.getInvokeId(),
                procUnstrReqInd.getMessageType().toString());
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogSupplementary mapDialogSupplementary = procUnstrReqInd.getMAPDialog();
        mapDialogSupplementary.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogSupplementary);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                messageType, dialogId, procUnstrReqInd.getInvokeId(), transactionId));


        MAPDialogSupplementary mapDialogSupplementary = mapProxyDialog.getMapDialogSupplementary();
        Long newInvokeId = mapDialogSupplementary.addProcessUnstructuredSSRequest(
                procUnstrReqInd.getDataCodingScheme(), procUnstrReqInd.getUSSDString(),
                procUnstrReqInd.getAlertingPattern(), procUnstrReqInd.getMSISDNAddressString());

        mapDialogOut = new MapDialogOut(mapDialogSupplementary, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(procUnstrReqInd.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("ProcessUnstructuredSSResponse with DialogId " + dialogId
              + " failed. Exception caught '" + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
