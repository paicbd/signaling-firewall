package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxySendAuthenticationInfo
 */
public class MapProxySendAuthenticationInfo {

  private static final Logger logger = Logger.getLogger(MapProxySendAuthenticationInfo.class);

  private MapProxySendAuthenticationInfo() {
    throw new IllegalStateException("Private constructor");
  }

  /**
   * process response for sendAuthenticationInfo_Response
   * 
   * @param message SendAuthenticationInfoResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getSendAuthInfoResponse(Object message, String transactionId) {
    SendAuthenticationInfoResponse sendAuthInfoResp = (SendAuthenticationInfoResponse) message;
    String messageType = sendAuthInfoResp.getMessageType().toString();
    Long dialogId = sendAuthInfoResp.getMAPDialog().getLocalDialogId();
    try {
      Long respInvokeId = sendAuthInfoResp.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      SendAuthenticationInfoResponseCopy clone =
          new SendAuthenticationInfoResponseCopy(sendAuthInfoResp);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d' %s",
          sendAuthInfoResp.getMessageType().toString(), dialogId, sendAuthInfoResp.getInvokeId(),
          transactionId));
      DataElement dataElement = null;

      logger.debug(String.format(
          "TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
          sendAuthInfoResp.getMAPDialog().getTCAPMessageType(), dialogId,
          sendAuthInfoResp.getInvokeId(), sendAuthInfoResp.getMAPDialog().getService().toString()));

      if (sendAuthInfoResp.getMAPDialog().getTCAPMessageType() == MessageType.End
          || sendAuthInfoResp.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
        logger.debug("Closing MAP Dialog. DialogId = " + dialogId);
        dataElement = Transaction.getInstance().removeDialogData(dialogId, respInvokeId);
        mapDialogOut.setIsResponse();
      } else {
        dataElement = Transaction.getInstance().getDialogData(dialogId, respInvokeId);
      }

      if (dataElement == null) {
        String logMsg = String.format("Dialog Id = %d not found in Transaction Map. %s", dialogId,
            transactionId);
        return MapProxyUtilsHelper.discardReason(logMsg, messageType, transactionId, dialogId);
      }
      SendAuthenticationInfoRequest origEvent =
          (SendAuthenticationInfoRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogMobility.setUserObject(invokeId);
      mapDialogMobility.addSendAuthenticationInfoResponse(invokeId,
          clone.getAuthenticationSetList(), clone.getExtensionContainer(),
          clone.getEpsAuthenticationSetList());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogMobility);
      mapDialogOut.getMapDialog().setNetworkId(sendAuthInfoResp.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("SendAuthenticationInfoResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
      return MapProxyUtilsHelper.discardReason(mapException.getMessage(), messageType, transactionId, dialogId);
    } catch(Exception e){
      logger.error("Send Authentication Info Error occurred. Error: ", e);
      return MapProxyUtilsHelper.discardReason(e.getMessage(), messageType, transactionId, dialogId);
    }
  }


  /**
   * process send authentication info request
   * 
   * @param mapProxyDialog  MapProxyDialog
   * @param sendAuthInfoReq SendAuthenticationInfoRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getSendAuthenticationInfoRequest(MapProxyDialog mapProxyDialog,
      SendAuthenticationInfoRequest sendAuthInfoReq, String transactionId) {
    Long dialogId = sendAuthInfoReq.getMAPDialog().getLocalDialogId();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'",
                transactionId, dialogId, sendAuthInfoReq.getInvokeId(),
                sendAuthInfoReq.getMessageType().toString());
        logger.debug(logmsg);
        logmsg = String.format("MAP Application Rule not found for DialogId = '%d', InvokeId = '%d'", dialogId, sendAuthInfoReq.getInvokeId());
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = sendAuthInfoReq.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                sendAuthInfoReq.getMessageType().toString(), dialogId, sendAuthInfoReq.getInvokeId(),
                transactionId));
        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();

        Long newInvokeId = mapMobilityOut.addSendAuthenticationInfoRequest(mapProxyDialog.getImsi(),
                sendAuthInfoReq.getNumberOfRequestedVectors(),
                sendAuthInfoReq.getSegmentationProhibited(),
                sendAuthInfoReq.getImmediateResponsePreferred(),
                sendAuthInfoReq.getReSynchronisationInfo(), sendAuthInfoReq.getExtensionContainer(),
                sendAuthInfoReq.getRequestingNodeType(), sendAuthInfoReq.getRequestingPlmnId(),
                sendAuthInfoReq.getNumberOfRequestedAdditionalVectors(),
                sendAuthInfoReq.getAdditionalVectorsAreForEPS());

        mapDialogOut =  new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(sendAuthInfoReq.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("AnyTimeInterrogationRequest with DialogId " + dialogId
              + " failed. Exception caught '" + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, sendAuthInfoReq.getMessageType().toString(), transactionId, dialogId);
  }
}
