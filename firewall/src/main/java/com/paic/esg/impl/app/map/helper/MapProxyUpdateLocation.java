package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationResponse;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

public class MapProxyUpdateLocation {

  private static final Logger logger = Logger.getLogger(MapProxyUpdateLocation.class);

  private MapProxyUpdateLocation() {
  }

  public static MapDialogOut getLocationResponse(Object message, String transactionId) {
    UpdateLocationResponse locationResponse = (UpdateLocationResponse) message;
    String messageType = locationResponse.getMessageType().toString();
    Long dialogId = locationResponse.getMAPDialog().getLocalDialogId();
    try {
      Long respInvokeId = locationResponse.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      UpdateLocationResponseCopy clone = new UpdateLocationResponseCopy(locationResponse);

      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming DialogId '%d', invokeId '%d', %s",
          locationResponse.getMessageType().toString(),
          locationResponse.getMAPDialog().getLocalDialogId(), locationResponse.getInvokeId(),
          transactionId));
      DataElement dataElement = null;
      logger.debug(String.format("TCAP Message Type = '%s', dialogId = %d, Service = '%s'",
          locationResponse.getMAPDialog().getTCAPMessageType(), dialogId,
          locationResponse.getMAPDialog().getService().toString()));
      // last result

      if (locationResponse.getMAPDialog().getTCAPMessageType() == MessageType.End
          || locationResponse.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
        // close the dialog
        logger.debug("Closing for dialogId = " + dialogId);
        dataElement = Transaction.getInstance().removeDialogData(dialogId, respInvokeId);
        // close the dialog here
        mapDialogOut.setIsResponse();
      } else {
        dataElement = Transaction.getInstance().getDialogData(dialogId, respInvokeId);
      }
      // if the object is found
      if (dataElement != null) {
        UpdateLocationRequest originalRequest =
            (UpdateLocationRequest) dataElement.getRequestObject();
        MAPDialogMobility origDialogMobility = originalRequest.getMAPDialog();
        long invokeId = originalRequest.getInvokeId();

        origDialogMobility.setUserObject(invokeId);
        origDialogMobility.addUpdateLocationResponse(invokeId, clone.getHlrNumber(),
            clone.getExtensionContainer(), clone.getAddCapability(),
            clone.getPagingAreaCapability());

        mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
        mapDialogOut.setMapDialog(origDialogMobility);
        mapDialogOut.getMapDialog().setNetworkId(locationResponse.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
      String logMsg =
          String.format("Dialog Id = %d not found in Transaction Map. %s", dialogId, transactionId);
      return MapProxyUtilsHelper.discardReason(logMsg, messageType, transactionId, dialogId);
    } catch (MAPException mapException) {
      logger.error("UpdateLocationResponse with DialogId " + dialogId + " failed " + transactionId
          + ". Exception caught '" + mapException + "'");
      return MapProxyUtilsHelper.discardReason(mapException.getMessage(), messageType, transactionId, dialogId);
    } catch (Exception ex) {
      logger.error("Failed", ex);
      return MapProxyUtilsHelper.discardReason(ex.getMessage(), messageType, transactionId, dialogId);
    }
  }

  public static MapDialogOut getLocationRequest(MapProxyDialog mapProxyDialog,
      UpdateLocationRequest locRequest, String transactionId) {
    Long dialogId = locRequest.getMAPDialog().getLocalDialogId();
    String messageType = locRequest.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);
    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. ",
                transactionId, dialogId, locRequest.getInvokeId(),
                locRequest.getMessageType().toString());
        logger.debug(logmsg);
        logmsg =
                String.format("MAP Application Rule not found for DialogId = '%d', InvokeId = '%d'",
                        dialogId, locRequest.getInvokeId());
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = locRequest.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                locRequest.getMessageType().toString(), dialogId, locRequest.getInvokeId(),
                transactionId));

        IMSI updateLocationImsi = mapProxyDialog.getImsi();
        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();

        Long newInvokeId = mapMobilityOut.addUpdateLocationRequest(updateLocationImsi,
                locRequest.getMscNumber(), locRequest.getRoamingNumber(), locRequest.getVlrNumber(),
                locRequest.getLmsi(), locRequest.getExtensionContainer(), locRequest.getVlrCapability(),
                locRequest.getInformPreviousNetworkEntity(), locRequest.getCsLCSNotSupportedByUE(),
                locRequest.getVGmlcAddress(), locRequest.getADDInfo(), locRequest.getPagingArea(),
                locRequest.getSkipSubscriberDataUpdate(), locRequest.getRestorationIndicator());


        mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(locRequest.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("UpdateLocationRequest with DialogId " + dialogId + " failed. Exception caught '"
              + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();

    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  /**
   * update GPRS location response
   * 
   * @param message UpdateLocationResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getGprsLocationResponse(Object message, String transactionId) {
    UpdateGprsLocationResponse event = (UpdateGprsLocationResponse) message;
    Long dialogId = event.getMAPDialog().getLocalDialogId();
    String messageType = event.getMessageType().toString();
    DataElement dataElement = null;
    try {
      Long respInvokeId = event.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      UpdateGprsLocationResponseCopy clone = new UpdateGprsLocationResponseCopy(event);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming dialogId '%d', InvokeId '%d' %s",
          event.getMessageType().toString(), dialogId, event.getInvokeId(), transactionId));
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
        String logmsg = String.format("Dialog Id = %d not found in Transaction Map. %s", dialogId,
            transactionId);
        return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
      }
      UpdateGprsLocationRequest origEvent =
          (UpdateGprsLocationRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();
      mapDialogMobility.setUserObject(invokeId);

      mapDialogMobility.addUpdateGprsLocationResponse(invokeId, clone.getHlrNumber(),
          clone.getExtensionContainer(), clone.getAddCapability(),
          clone.getSgsnMmeSeparationSupported());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogMobility);
      mapDialogOut.getMapDialog().setNetworkId(event.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("UpdateGprsLocationResponse with DialogId " + dialogId + " failed "
          + transactionId + ". Exception caught '" + mapException + "'");
      return MapProxyUtilsHelper.discardReason(mapException.getMessage(), messageType, transactionId, dialogId);
    } catch (Exception ex) {
      logger.error("Error occurred: ", ex);
      return MapProxyUtilsHelper.discardReason(ex.getMessage(), messageType, transactionId, dialogId);
    }
  }

  /**
   * update GPRS location request
   * 
   * @param mapProxyDialog MapProxyDialog
   * @param updateGprs     UpdateGprsLocationRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getGprsLocationRequest(MapProxyDialog mapProxyDialog,
      UpdateGprsLocationRequest updateGprs, String transactionId) {
    Long dialogId = updateGprs.getMAPDialog().getLocalDialogId();
    String messageType = updateGprs.getMessageType().toString();
    if (mapProxyDialog == null) {
      logger.debug(String.format(
          "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'.",
          transactionId, dialogId, updateGprs.getInvokeId(),
          updateGprs.getMessageType().toString()));
      String logMsg =
          String.format("MAP Application Rule not found for DialogId = '%d', InvokeId = '%d'",
              dialogId, updateGprs.getInvokeId());
      return MapProxyUtilsHelper.discardReason(logMsg, messageType, transactionId, dialogId);
    }
    try {
      logger
          .debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId  = '%d', invokeId = '%d', %s",
              updateGprs.getMessageType().toString(), dialogId, updateGprs.getInvokeId(),
              transactionId));

      IMSI updateLocationImsi = mapProxyDialog.getImsi();
      MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();

      Long newInvokeId = mapMobilityOut.addUpdateGprsLocationRequest(updateLocationImsi,
          updateGprs.getSgsnNumber(), updateGprs.getSgsnAddress(),
          updateGprs.getExtensionContainer(), updateGprs.getSGSNCapability(),
          updateGprs.getInformPreviousNetworkEntity(), updateGprs.getPsLCSNotSupportedByUE(),
          updateGprs.getVGmlcAddress(), updateGprs.getADDInfo(), updateGprs.getEPSInfo(),
          updateGprs.getServingNodeTypeIndicator(), updateGprs.getSkipSubscriberDataUpdate(),
          updateGprs.getUsedRATType(), updateGprs.getGprsSubscriptionDataNotNeeded(),
          updateGprs.getNodeTypeIndicator(), updateGprs.getAreaRestricted(),
          updateGprs.getUeReachableIndicator(), updateGprs.getEpsSubscriptionDataNotNeeded(),
          updateGprs.getUESRVCCCapability());

      MapDialogOut mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
      mapDialogOut.getMapDialog().setNetworkId(updateGprs.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("UpdateGprsLocationRequest with DialogId " + dialogId
          + " failed. Exception caught '" + mapException + "', " + transactionId);
      return MapProxyUtilsHelper.discardReason(mapException.getMessage(), messageType, transactionId, dialogId);
    } catch (Exception ex) {
      logger.error("Error: ", ex);
      return MapProxyUtilsHelper.discardReason(ex.getMessage(), messageType, transactionId, dialogId);
    }
  }

}
