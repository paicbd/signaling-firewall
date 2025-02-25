package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyPurgeMS
 */
public class MapProxyPurgeMS {

  private static final Logger logger = Logger.getLogger(MapProxyPurgeMS.class);

  private MapProxyPurgeMS() {
  }

  /**
   * process response for PurgeMSResponse
   * 
   * @param message PurgeMSResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getResponse(Object message, String transactionId) {
    PurgeMSResponse purgeMSResponse = (PurgeMSResponse) message;
    Long dialogId = purgeMSResponse.getMAPDialog().getLocalDialogId();
    try {

      Long respInvokeId = purgeMSResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          purgeMSResponse.getMessageType().toString(),
          purgeMSResponse.getMAPDialog().getLocalDialogId(), purgeMSResponse.getInvokeId(),
          transactionId));

      DataElement dataElement = null;
      logger.debug(String.format(
          "TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
          purgeMSResponse.getMAPDialog().getTCAPMessageType(), dialogId,
          purgeMSResponse.getInvokeId(), purgeMSResponse.getMAPDialog().getService().toString()));

      if (purgeMSResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || purgeMSResponse.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
        logger.debug("Closing for dialogId = " + dialogId);
        dataElement = Transaction.getInstance().removeDialogData(dialogId, respInvokeId);
        mapDialogOut.setIsResponse();
      } else {
        dataElement = Transaction.getInstance().getDialogData(dialogId, respInvokeId);
      }

      if (dataElement == null) {
        logger.debug(String.format("Dialog Id = %d not found in Transaction Map. %s", dialogId,
            transactionId));
        return null;
      }
      PurgeMSRequest origEvent = (PurgeMSRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();
      mapDialogMobility.setUserObject(invokeId);
      mapDialogMobility.addPurgeMSResponse(invokeId, purgeMSResponse.getFreezeTMSI(),
          purgeMSResponse.getFreezePTMSI(), purgeMSResponse.getExtensionContainer(),
          purgeMSResponse.getFreezeMTMSI());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogMobility);
      mapDialogOut.getMapDialog().setNetworkId(purgeMSResponse.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("PurgeMSResponse with DialogId " + dialogId + " failed " + transactionId
          + ". Exception caught '" + mapException + "'");
    }
    return null;
  }

  /**
   * process Purge MS request
   * 
   * @param mapProxyDialog MapProxyDialog
   * @param purgeMSRequest PurgeMSRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getRequest(MapProxyDialog mapProxyDialog,
      PurgeMSRequest purgeMSRequest, String transactionId) {
    Long dialogId = purgeMSRequest.getMAPDialog().getLocalDialogId();
    String messageType = purgeMSRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, purgeMSRequest.getInvokeId(),
                purgeMSRequest.getMessageType().toString());
        logger.debug(logmsg);
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = purgeMSRequest.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                messageType, dialogId, purgeMSRequest.getInvokeId(), transactionId));


        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();

        Long newInvokeId =
                mapMobilityOut.addPurgeMSRequest(mapProxyDialog.getImsi(), purgeMSRequest.getVlrNumber(),
                        purgeMSRequest.getSgsnNumber(), purgeMSRequest.getExtensionContainer());

        mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(purgeMSRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("PurgeMSRequest with DialogId " + dialogId + " failed. Exception caught '"
              + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
