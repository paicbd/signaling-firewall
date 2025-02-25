package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.oam.MAPDialogOam;
import org.restcomm.protocols.ss7.map.api.service.oam.SendImsiRequest;
import org.restcomm.protocols.ss7.map.api.service.oam.SendImsiResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxySendIMSI
 */
public class MapProxySendIMSI {

  private static final Logger logger = Logger.getLogger(MapProxySendIMSI.class);

  private MapProxySendIMSI() {
  }

  /**
   * process response for SendImsiResponse
   * 
   * @param message SendImsiResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getResponse(Object message, String transactionId) {
    SendImsiResponse sendImsiResp = (SendImsiResponse) message;
    Long dialogId = sendImsiResp.getMAPDialog().getLocalDialogId();
    String messageType = sendImsiResp.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = sendImsiResp.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          sendImsiResp.getMessageType().toString(), dialogId, sendImsiResp.getInvokeId(),
          transactionId));

      DataElement dataElement = null;
      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
              sendImsiResp.getMAPDialog().getTCAPMessageType(), dialogId,
              sendImsiResp.getInvokeId(), sendImsiResp.getMAPDialog().getService().toString()));

      if (sendImsiResp.getMAPDialog().getTCAPMessageType() == MessageType.End
          || sendImsiResp.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      SendImsiRequest origEvent = (SendImsiRequest) dataElement.getRequestObject();
      MAPDialogOam origMapDialogOam = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();
      origMapDialogOam.setUserObject(invokeId);
      origMapDialogOam.addSendImsiResponse(invokeId, sendImsiResp.getImsi());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(origMapDialogOam);
      mapDialogOut.getMapDialog().setNetworkId(sendImsiResp.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("SendImsiResponse with DialogId " + dialogId + " failed " + transactionId
          + ". Exception caught '" + mapException + "'");
      logmsg = mapException.getMessage();
    } catch (Exception e) {
      logmsg = e.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  /**
   * process Send IMSI request
   * 
   * @param mapProxyDialog  MapProxyDialog
   * @param sendIMSIRequest SendImsiRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      SendImsiRequest sendIMSIRequest, String transactionId) {
    Long dialogId = sendIMSIRequest.getMAPDialog().getLocalDialogId();
    String messageType = sendIMSIRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, sendIMSIRequest.getInvokeId(),
                sendIMSIRequest.getMessageType().toString());
        logger.debug(logmsg);
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogOam mapDialogOam = sendIMSIRequest.getMAPDialog();
        mapDialogOam.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogOam);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] dialogId = '%d', InvokeId = '%d', %s",
                sendIMSIRequest.getMessageType().toString(), dialogId, sendIMSIRequest.getInvokeId(),
                transactionId));

        MAPDialogOam mapDialogOam = mapProxyDialog.getMapDialogOam();
        Long newInvokeId = mapDialogOam.addSendImsiRequest(sendIMSIRequest.getMsisdn());

        mapDialogOut = new MapDialogOut(mapDialogOam, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(sendIMSIRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("SendImsiRequest with DialogId " + dialogId + " failed. Exception caught '"
              + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
