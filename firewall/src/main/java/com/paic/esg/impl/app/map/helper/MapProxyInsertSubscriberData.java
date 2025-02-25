package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.app.map.MapProxyDialog;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.dialog.Reason;
import org.restcomm.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataResponse;
import org.restcomm.protocols.ss7.tcap.api.MessageType;

/**
 * MapProxyInsertSubscriberData
 */
public class MapProxyInsertSubscriberData {

  private static final Logger logger = Logger.getLogger(MapProxyInsertSubscriberData.class);

  private MapProxyInsertSubscriberData() {
  }

  /**
   * process response for InsertSubscriberData
   * 
   * @param message InsertSubscriberDataResponse
   * @return MapDialogOut
   */
  public static MapDialogOut getInsertSubDataResponse(Object message, String transactionId) {
    Long dialogId = 0L;
    String logmsg = "";
    String messageType = "";
    try {
      InsertSubscriberDataResponse insertSubDataResp = (InsertSubscriberDataResponse) message;
      dialogId = insertSubDataResp.getMAPDialog().getLocalDialogId();
      messageType = insertSubDataResp.getMessageType().toString();

      Long respInvokeId = insertSubDataResp.getInvokeId();
      MapDialogOut mapDialogOut = new MapDialogOut();
      mapDialogOut.setOriginalDialogId(dialogId);
      InsertSubscriberDataResponseCopy clone =
          new InsertSubscriberDataResponseCopy(insertSubDataResp);
      logger.debug(String.format("[MAP::RESPONSE<%s>] Incoming dialogId '%d', invokeId '%d' %s",
          insertSubDataResp.getMessageType().toString(), dialogId, insertSubDataResp.getInvokeId(),
          transactionId));
      DataElement dataElement = null;
      logger.debug(
          String.format("TCAP Message Type = '%s', dialogId = %d, InvokeId = %d, Service = '%s'",
              insertSubDataResp.getMAPDialog().getTCAPMessageType(), dialogId,
              insertSubDataResp.getInvokeId(),
              insertSubDataResp.getMAPDialog().getService().toString()));

      if (insertSubDataResp.getMAPDialog().getTCAPMessageType() == MessageType.End
          || insertSubDataResp.getMAPDialog().getTCAPMessageType() == MessageType.Abort) {
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
      InsertSubscriberDataRequest origEvent =
          (InsertSubscriberDataRequest) dataElement.getRequestObject();
      MAPDialogMobility mapDialogMobility = origEvent.getMAPDialog();
      Long invokeId = origEvent.getInvokeId();

      mapDialogMobility.setUserObject(invokeId);
      mapDialogMobility.addInsertSubscriberDataResponse(invokeId, clone.getTeleserviceList(),
          clone.getBearerServiceList(), clone.getSSList(), clone.getODBGeneralData(),
          clone.getRegionalSubscriptionResponse(), clone.getSupportedCamelPhases(),
          clone.getExtensionContainer(), clone.getOfferedCamel4CSIs(),
          clone.getSupportedFeatures());

      mapDialogOut.setLogInvokeIds(respInvokeId, invokeId);
      mapDialogOut.setMapDialog(mapDialogMobility);
      mapDialogOut.getMapDialog().setNetworkId(insertSubDataResp.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("InsertSubscriberDataResponse with DialogId " + dialogId + " failed. Error:",
          mapException);
      logmsg = mapException.getMessage();
    } catch (Exception e) {
      logmsg = e.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  /**
   * process request for InsertSubscriberData
   * 
   * @param mapProxyDialog MapProxyDialog
   * @param request        InsertSubscriberDataRequest
   * @return MapDialogOut
   */
  public static MapDialogOut getInsertSubDataRequest(MapProxyDialog mapProxyDialog,
      InsertSubscriberDataRequest request, String transactionId) {
    Long dialogId = request.getMAPDialog().getLocalDialogId();
    String messageType = request.getMessageType().toString();
    String logmsg = "";
    MapDialogOut mapDialogOut = new MapDialogOut();
    mapDialogOut.setOriginalDialogId(dialogId);

    try {
      if (mapProxyDialog == null) {
        logmsg = String.format(
                "%s, MAP Application Rule not found for DialogId = '%d', InvokeId = '%d', MessageType = '%s'. MAP Message will be discarded",
                transactionId, dialogId, request.getInvokeId(), request.getMessageType().toString());
        logger.debug(logmsg);
        logmsg = String.format("MAP Application Rule not found for DialogId = '%d', InvokeId = '%d'",
                dialogId, request.getInvokeId());
        mapDialogOut.setInvokeId(null);
        mapDialogOut.setDiscardReason("MAP Application Rule not found");
        MAPDialogMobility mapDialogMobility = request.getMAPDialog();
        mapDialogMobility.refuse(Reason.noReasonGiven);
        mapDialogOut.setMapDialog(mapDialogMobility);
      } else {
        logger.debug(String.format("[MAP::REQUEST<%s>] Incoming DialogId = '%d', InvokeId = '%d', %s",
                request.getMessageType().toString(), dialogId, request.getInvokeId(), transactionId));
        MAPDialogMobility mapMobilityOut = mapProxyDialog.getMapDialogMobility();

        Long newInvokeId = mapMobilityOut.addInsertSubscriberDataRequest(mapProxyDialog.getImsi(),
                request.getMsisdn(), request.getCategory(), request.getSubscriberStatus(),
                request.getBearerServiceList(), request.getTeleserviceList(), request.getProvisionedSS(),
                request.getODBData(), request.getRoamingRestrictionDueToUnsupportedFeature(),
                request.getRegionalSubscriptionData(), request.getVbsSubscriptionData(),
                request.getVgcsSubscriptionData(), request.getVlrCamelSubscriptionInfo(),
                request.getExtensionContainer(), request.getNAEAPreferredCI(),
                request.getGPRSSubscriptionData(),
                request.getRoamingRestrictedInSgsnDueToUnsupportedFeature(),
                request.getNetworkAccessMode(), request.getLSAInformation(), request.getLmuIndicator(),
                request.getLCSInformation(), request.getIstAlertTimer(),
                request.getSuperChargerSupportedInHLR(), request.getMcSsInfo(),
                request.getCSAllocationRetentionPriority(), request.getSgsnCamelSubscriptionInfo(),
                request.getChargingCharacteristics(), request.getAccessRestrictionData(),
                request.getIcsIndicator(), request.getEpsSubscriptionData(),
                request.getCsgSubscriptionDataList(), request.getUeReachabilityRequestIndicator(),
                request.getSgsnNumber(), request.getMmeName(), request.getSubscribedPeriodicLAUtimer(),
                request.getVplmnLIPAAllowed(), request.getMdtUserConsent(),
                request.getSubscribedPeriodicLAUtimer());

        mapDialogOut = new MapDialogOut(mapMobilityOut, newInvokeId, dialogId, mapProxyDialog);
        mapDialogOut.getMapDialog().setNetworkId(request.getMAPDialog().getNetworkId());
        return mapDialogOut;
      }
    } catch (MAPException mapException) {
      logger.error("InsertSubscriberDataRequest with DialogId " + dialogId
              + " failed. Exception caught '" + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }

  public static MapDialogOut sendUpdateLocation(InsertSubscriberDataRequest request,
      DataElement dataElement, String transactionId) {
    Long dialogId = request.getMAPDialog().getLocalDialogId();
    String messageType = request.getMessageType().toString();
    String logmsg = "";
    try {
      MAPDialogMobility mapDialogMobility;
      if (dataElement.getMessageType().equals("updateLocation_Request")) {
        UpdateLocationRequest updateLocationRequest =
            (UpdateLocationRequest) dataElement.getRequestObject();
        mapDialogMobility = updateLocationRequest.getMAPDialog();
      } else if (dataElement.getMessageType().equals("updateGprsLocation_Request")) {
        UpdateGprsLocationRequest upgprsLocation =
            (UpdateGprsLocationRequest) dataElement.getRequestObject();
        mapDialogMobility = upgprsLocation.getMAPDialog();
      } else {
        logmsg = "FAILED to process MAP Message: " + request.toString();
        return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
      }

      Long newInvokeId = mapDialogMobility.addInsertSubscriberDataRequest(request.getImsi(),
          request.getMsisdn(), request.getCategory(), request.getSubscriberStatus(),
          request.getBearerServiceList(), request.getTeleserviceList(), request.getProvisionedSS(),
          request.getODBData(), request.getRoamingRestrictionDueToUnsupportedFeature(),
          request.getRegionalSubscriptionData(), request.getVbsSubscriptionData(),
          request.getVgcsSubscriptionData(), request.getVlrCamelSubscriptionInfo(),
          request.getExtensionContainer(), request.getNAEAPreferredCI(),
          request.getGPRSSubscriptionData(),
          request.getRoamingRestrictedInSgsnDueToUnsupportedFeature(),
          request.getNetworkAccessMode(), request.getLSAInformation(), request.getLmuIndicator(),
          request.getLCSInformation(), request.getIstAlertTimer(),
          request.getSuperChargerSupportedInHLR(), request.getMcSsInfo(),
          request.getCSAllocationRetentionPriority(), request.getSgsnCamelSubscriptionInfo(),
          request.getChargingCharacteristics(), request.getAccessRestrictionData(),
          request.getIcsIndicator(), request.getEpsSubscriptionData(),
          request.getCsgSubscriptionDataList(), request.getUeReachabilityRequestIndicator(),
          request.getSgsnNumber(), request.getMmeName(), request.getSubscribedPeriodicLAUtimer(),
          request.getVplmnLIPAAllowed(), request.getMdtUserConsent(),
          request.getSubscribedPeriodicLAUtimer());

      MapDialogOut mapDialogOut = new MapDialogOut(mapDialogMobility, newInvokeId, dialogId);
      mapDialogOut.getMapDialog().setNetworkId(request.getMAPDialog().getNetworkId());
      return mapDialogOut;
    } catch (MAPException mapException) {
      logger.error("sendUpdateLocation with DialogId " + dialogId + " failed. Exception caught '"
          + mapException + "', " + transactionId);
      logmsg = mapException.getMessage();
    } catch (Exception ex) {
      logmsg = ex.getMessage();
    }
    return MapProxyUtilsHelper.discardReason(logmsg, messageType, transactionId, dialogId);
  }
}
