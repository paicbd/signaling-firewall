package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyProvideSubscriberInfo
 */
public class MapProxyProvideSubscriberInfo {

  private static final Logger logger = Logger.getLogger(MapProxyProvideSubscriberInfo.class);

  private MapProxyProvideSubscriberInfo() {
  }

  /**
   * process response for ProvideSubscriberInfoResponse
   * 
   * @param message ProvideSubscriberInfoResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getResponse(Object message, String transactionId) {
    ProvideSubscriberInfoResponse subInfoResponse = (ProvideSubscriberInfoResponse) message;
    Long dialogId = subInfoResponse.getMAPDialog().getLocalDialogId();
    String messageType = subInfoResponse.getMessageType().toString();
    String logmsg = "";
    try {
      Long respInvokeId = subInfoResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      ProvideSubscriberInfoResponseCopy clone =
          new ProvideSubscriberInfoResponseCopy(subInfoResponse);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          subInfoResponse.getMessageType().toString(), dialogId, subInfoResponse.getInvokeId(),
          transactionId));
      DataElement dataElement = null;

      logger.debug(String.format(
          "TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
          subInfoResponse.getMAPDialog().getTCAPMessageType(), dialogId,
          subInfoResponse.getInvokeId(), subInfoResponse.getMAPDialog().getService().toString()));

      if (subInfoResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || subInfoResponse.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      ProvideSubscriberInfoRequest origEvent =
          (ProvideSubscriberInfoRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogMobility.setUserObject(invokeId);
      mapDialogMobility.addAnyTimeInterrogationResponse(invokeId, clone.getSubscriberInfo(),
          clone.getExtensionContainer());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogMobility);
      mapDialogOut.getMapDialog().setNetworkId(subInfoResponse.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("UpdateGprsLocationResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  /**
   * process Provide Subscriber Info request
   * 
   * @param mapProxyDialog MapProxyDialog
   * @param subInfoRequest ProvideSubscriberInfoRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      ProvideSubscriberInfoRequest subInfoRequest, String transactionId) {
    Long dialogId = subInfoRequest.getMAPDialog().getLocalDialogId();
    String messageType = subInfoRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, subInfoRequest.getInvokeId(),
                subInfoRequest.getMessageType().toString());
        logger.debug(logmsg);
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = subInfoRequest.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                subInfoRequest.getMessageType().toString(), dialogId, subInfoRequest.getInvokeId(),
                transactionId));
        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();
        Long newInvokeId = mapMobilityOut.addProvideSubscriberInfoRequest(mapProxyDialog.getImsi(),
                subInfoRequest.getLmsi(), subInfoRequest.getRequestedInfo(),
                subInfoRequest.getExtensionContainer(), subInfoRequest.getCallPriority());
        mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(subInfoRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("ProvideSubscriberInfoRequest with DialogId " + dialogId
              + " failed. Exception caught '" + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
