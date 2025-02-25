package com.paic.esg.impl.app.map;

import java.util.Optional;

import com.paic.esg.impl.app.map.helper.*;
import com.paic.esg.impl.rules.*;
import com.paic.esg.impl.rules.models.Rule;
import com.paic.esg.info.DataElement;
import com.paic.esg.info.Transaction;
import com.paic.esg.network.layers.MapLayer;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.MAPMessageType;
import org.restcomm.protocols.ss7.map.api.MAPProvider;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.service.callhandling.ProvideRoamingNumberRequest;
import org.restcomm.protocols.ss7.map.api.service.callhandling.SendRoutingInformationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeSubscriptionInterrogationRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.DeleteSubscriberDataRequest;
import org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataRequest;
import org.restcomm.protocols.ss7.map.api.service.oam.SendImsiRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.*;
import org.restcomm.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;
import org.restcomm.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.restcomm.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * MapProcessingNode
 */
public class MapProcessingNode {
  private static final Logger logger = Logger.getLogger(MapProcessingNode.class);

  private MAPMessageType messageType;
  private Object message;
  private MapLayer map;
  private String transactionId;

  /**
   *
   * @param calledGt
   * @param callingGt
   * @param imsi
   * @return
   */
  @Deprecated
  public ReplacedValues getReplacedRule(String calledGt, String callingGt, String imsi) {
    if (this.messageType == null) {
      logger
          .error(String.format("<%s,%s>: Unknown Message Type: [ClgGt = %s, CldGt = %s, Imsi = %s]",
              this.messageType, this.transactionId, callingGt, calledGt, imsi));
      return null;
    }
    logger
        .info(String.format("<%s, %s>: Searching RULE for: ClgGt = '%s', CldGt = '%s', Imsi = '%s'",
            this.messageType, this.transactionId, callingGt, calledGt, imsi));

    ApplicationRulesSetting rulesSetting = MapProxyApplicationRules.getInstance()
        .findMAPApplicationRule(callingGt, calledGt, imsi, messageType.toString());
    if (rulesSetting != null) {
      logger.info(String.format(
          "<%s, %s>: Matching rule found. Rule Name = '%s', ClgGt = '%s', CldGt = '%s', Imsi = '%s'",
          this.messageType, this.transactionId, rulesSetting.getName(), callingGt, calledGt, imsi));
      ReplacedValues replaceRule =
          rulesSetting.getReplaceRule().applyReplaceRule(imsi, callingGt, calledGt);
      if (replaceRule != null) {
        replaceRule.setRuleName(rulesSetting.getName());
        if (replaceRule.getImsi() != null) {
          logger.info(String.format("<%s, %s>, Replaced Values: OldIMSI = '%s', newIMSI = '%s'",
              this.messageType, this.transactionId, imsi, replaceRule.getImsi()));
        }
        if (replaceRule.getCalledGlobalTitle() != null) {
          logger.info(String.format("<%s, %s>, Replaced Values: Old CldGt = '%s', new CldGt = '%s'",
              this.messageType, this.transactionId, calledGt,
              replaceRule.getCalledGlobalTitle().getDigits()));
        }
        if (replaceRule.getCallingGlobalTitle() != null) {
          logger.info(String.format("<%s, %s>, Replaced Values: Old ClgGt = '%s', new ClgGt = '%s'",
              this.messageType, this.transactionId, callingGt,
              replaceRule.getCallingGlobalTitle().getDigits()));
        }
        return replaceRule;
      }
    }
    logger.info(String.format("<%s, %s>: RULE not found for: ClgGt = %s, CldGt = %s, Imsi = %s",
        this.messageType, this.transactionId, callingGt, calledGt, imsi));
    return null;
  }

  public Rule getRule(FilterForRule filterForRule) {
    if (this.messageType == null) {
      logger
              .error(String.format("<%s,%s>: Unknown Message Type: [ClgGt = %s, CldGt = %s, Imsi = %s]",
                      null, this.transactionId, filterForRule.getCallingPartyAddress(),
                      filterForRule.getCalledPartyAddress(), filterForRule.getImsi()));
      return null;
    }
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("<%s, %s>: Searching RULE for: ClgGt = '%s', CldGt = '%s', Imsi = '%s'",
              this.messageType, this.transactionId, filterForRule.getCallingPartyAddress(),
              filterForRule.getCalledPartyAddress(), filterForRule.getImsi()));
    }
    Rule rulesSetting = MapProxyApplicationRules.getInstance().findMAPRule(filterForRule);
    if (rulesSetting != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format(
                "<%s, %s>: Matching rule found. Rule Name = '%s', ClgGt = '%s', CldGt = '%s', Imsi = '%s'",
                this.messageType, this.transactionId, rulesSetting.getName(), filterForRule.getCallingPartyAddress(),
                filterForRule.getCalledPartyAddress(), filterForRule.getImsi()));
      }


      return rulesSetting;
    }
    logger.warn(String.format("<%s, %s>: RULE not found for: ClgGt = %s, CldGt = %s, Imsi = %s",
            this.messageType, this.transactionId, filterForRule.getCallingPartyAddress(),
            filterForRule.getCalledPartyAddress(), filterForRule.getImsi()));
    return null;
  }

  private MapProxyDialog getMapProxyDialog(MAPDialog mapDialog, FilterForRule filter,
      MapDialogType mapDialogType, Long invokeId) throws MAPException {
    SccpAddress callingSccpAddress = mapDialog.getRemoteAddress(); // calling party
    SccpAddress calledSccpAddress = mapDialog.getLocalAddress();

    Long dialogId = mapDialog.getLocalDialogId();

    logger.debug(String.format(
        "Processing Message Type = '%s', DialogId = '%d', InvokeId = '%d', Channel Message Id = %s",
        this.messageType, dialogId, invokeId, this.transactionId));

    Rule rule = getRule(filter);
    if (rule == null) {
      return null;
    }
    MapProxyDialog mapProxyObj = new MapProxyDialog(callingSccpAddress, calledSccpAddress, filter.getImsi());
    // set the dialog type
    mapProxyObj.setMapDialogType(mapDialogType);
    mapProxyObj.setRuleName(rule.getName());

    logger.trace(String.format("Changing pc = 0 for %s", calledSccpAddress.getGlobalTitle()));

    calledSccpAddress =
            new SccpAddressImpl(calledSccpAddress.getAddressIndicator().getRoutingIndicator(),
                    calledSccpAddress.getGlobalTitle(), 0, calledSccpAddress.getSubsystemNumber());
    mapProxyObj.setNewCalledAddress(calledSccpAddress);
    mapProxyObj.setNewCallingAddress(callingSccpAddress);
    // set the updated IMSI
    mapProxyObj.setUpdatedImsi(filter.getImsi());

    //TODO REPLACE LOGIC

    /*
    // apply the rules and changes the necessary values
    ReplacedValues replacedValues = getReplacedRule(calledGT, callingGT, imsi);
    // if not rule is found return null
    if (replacedValues == null) {
      return null;
    }
    MapProxyDialog mapProxyObj = new MapProxyDialog(callingSccpAddress, calledSccpAddress, imsi);
    // set the dialog type
    mapProxyObj.setMapDialogType(mapDialogType);
    mapProxyObj.setRuleName(replacedValues.getRuleName());
    // called GT
    if (replacedValues.getCalledGlobalTitle() != null) {
      GlobalTitle gt = replacedValues.getCalledGlobalTitle();
      RoutingIndicator ri = replacedValues.getCalledSccpAddressParam()
          .flatMap(PatternSccpAddress::getRoutingIndicator)
          .orElse(calledSccpAddress.getAddressIndicator().getRoutingIndicator());
      int dpc = replacedValues.getCalledSccpAddressParam()
          .flatMap(PatternSccpAddress::getDestPointCode).orElse(0);
      int ssn =
          replacedValues.getCalledSccpAddressParam().flatMap(PatternSccpAddress::getSubSystemNumber)
              .orElse(calledSccpAddress.getSubsystemNumber());
      // save new called GT
      mapProxyObj.setNewCalledGt(gt.getDigits());
      logger.trace(String.format("Called Address pc = %d, ssn = %d, RI = %s, %s", dpc, ssn,
          ri.toString(), this.transactionId));
      calledSccpAddress = new SccpAddressImpl(ri, gt, dpc, ssn);
      logger.info(String.format("<%s, %s> calledGT Changed: SccpAddress = '%s', New GT = '%s'",
          this.messageType, this.transactionId, calledSccpAddress.toString(), gt.toString()));
    } else {
      // change only the point code to 0 for better routing which uses the configuration
      // rules
      logger.trace(String.format("Changing pc = 0 for %s", calledSccpAddress.getGlobalTitle()));
      calledSccpAddress =
          new SccpAddressImpl(calledSccpAddress.getAddressIndicator().getRoutingIndicator(),
              calledSccpAddress.getGlobalTitle(), 0, calledSccpAddress.getSubsystemNumber());
    }
    // for CDR for the new calling SccpAddress
    mapProxyObj.setNewCalledAddress(calledSccpAddress);
    // calling GT
    if (replacedValues.getCallingGlobalTitle() != null) {
      GlobalTitle gt = replacedValues.getCallingGlobalTitle();
      RoutingIndicator ri = replacedValues.getCallingSccpAddressParam()
          .flatMap(PatternSccpAddress::getRoutingIndicator)
          .orElse(callingSccpAddress.getAddressIndicator().getRoutingIndicator());
      int dpc = replacedValues.getCallingSccpAddressParam()
          .flatMap(PatternSccpAddress::getDestPointCode).orElse(0);
      int ssn = replacedValues.getCallingSccpAddressParam()
          .flatMap(PatternSccpAddress::getSubSystemNumber)
          .orElse(callingSccpAddress.getSubsystemNumber());

      mapProxyObj.setNewCallingGt(gt.getDigits());
      logger.trace(String.format("Calling Address pc = %d, ssn = %d, RI = %s, %s", dpc, ssn,
          ri.toString(), this.transactionId));
      callingSccpAddress = new SccpAddressImpl(ri, gt, dpc, ssn);
      logger.info(String.format("<%s, %s> callingGT Changed: SccpAddress = '%s', New GT = '%s'",
          this.messageType, this.transactionId, callingSccpAddress.toString(), gt.toString()));
    }
    mapProxyObj.setNewCallingAddress(callingSccpAddress);

*/
    /*
     * The origination address should be the same as the origination address received and not the
     * local address MAPApplicationContext appCntx, SccpAddress origAddress, AddressString
     * origReference, SccpAddress destAddress, AddressString destReference
     */



    MAPProvider mapProvider = this.map.getMapStack().getMAPProvider();
    switch (mapDialogType) {
      case Mobility:
        mapProxyObj.setMapDialogMobility(
            mapProvider.getMAPServiceMobility().createNewDialog(mapDialog.getApplicationContext(),
                callingSccpAddress, mapDialog.getReceivedOrigReference(), calledSccpAddress,
                mapDialog.getReceivedOrigReference()));

        break;
      case CallHandling:
        mapProxyObj.setMapDialogCallHandling(mapProvider.getMAPServiceCallHandling()
            .createNewDialog(mapDialog.getApplicationContext(), callingSccpAddress,
                mapDialog.getReceivedOrigReference(), calledSccpAddress,
                mapDialog.getReceivedOrigReference()));
        break;

      case SMS:
        mapProxyObj.setMapDialogSms(
            mapProvider.getMAPServiceSms().createNewDialog(mapDialog.getApplicationContext(),
                callingSccpAddress, mapDialog.getReceivedOrigReference(), calledSccpAddress,
                mapDialog.getReceivedOrigReference()));
        break;
      case OAM:
        mapProxyObj.setMapDialogOam(
            mapProvider.getMAPServiceOam().createNewDialog(mapDialog.getApplicationContext(),
                callingSccpAddress, mapDialog.getReceivedOrigReference(), calledSccpAddress,
                mapDialog.getReceivedOrigReference()));
        break;
      case Supplementary:
        mapProxyObj.setMapDialogSupplementary(mapProvider.getMAPServiceSupplementary()
            .createNewDialog(mapDialog.getApplicationContext(), callingSccpAddress,
                mapDialog.getReceivedOrigReference(), calledSccpAddress,
                mapDialog.getReceivedOrigReference()));
    }
    return mapProxyObj;
  }

  /**
   * Invoke the appropriate method to process the primitive type
   *
   * @return MapDialogOut
   */
  public MapDialogOut processRequest() {
    if (map == null) {
      String logMsg =
          String.format("<%s, %s> The Maplayer is not found", this.transactionId, this.messageType);
      logger.error(logMsg);
      return discardReason(new IllegalStateException(logMsg));
    }

    try {
      switch (this.messageType) {
        /********** MOBILITY ***************/
        case updateLocation_Request:
          return processLocationUpdate();
        case updateGprsLocation_Request:
          return processGPRSLocationUpdate();
        case insertSubscriberData_Request:
          return processInsertSubscriberDataRequest();
        case sendAuthenticationInfo_Request:
          return processSendAuthentication();
        case anyTimeInterrogation_Request:
          return processAnyTimeInterrogation();
        case checkIMEI_Request:
          return processCheckIMEI();
        case purgeMS_Request:
          return processPurgeMS();
        case sendIdentification_Request:
          return processSendIdentification();
        case provideSubscriberInfo_Request:
          return processProvideSubscriberInfo();
        case deleteSubscriberData_Request:
          return processDeleteSubscriber();
        case anyTimeSubscriptionInterrogation_Request:
          return processAnyTimeSubscription();
        /************* CALL HANDLING *************/
        case provideRoamingNumber_Request:
          return processProvideRoamingNumber();
        case sendRoutingInfo_Request:
          return processSendRoutingInfo();
        /************* SMS *********************/
        case mtForwardSM_Request:
          return processMtForwardSM();
        case moForwardSM_Request:
          return processMoForwardSM();
        case sendRoutingInfoForSM_Request:
          return processSendRoutingInfoForSm();
        case forwardSM_Request:
          return processForwardSM();

        /************ OAM **************/
        case sendIMSI_Request:
          return processSendIMSI();

        /*************** Supplementary ***************/
        case processUnstructuredSSRequest_Request:
          return processProcessUSSR();
        case unstructuredSSRequest_Request:
          return processUSSR();

        default:
          logger.info(String.format("<%s>. Unhandled Message Type: '%s' ", this.transactionId,
              this.messageType));
          break;
      }
    } catch (Exception ex) {
      logger.error("Failed to process request for " + this.transactionId + ". Error: " + ex);
    }
    return null;
  }

  private MapDialogOut processInsertSubscriberDataRequest() {
    try {
      InsertSubscriberDataRequest request = (InsertSubscriberDataRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(request.getImsi()),
              MapProxyUtilsHelper.getMsisdn(request.getMsisdn()),
              "",
              request.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              request.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      Long dialogId = request.getMAPDialog().getLocalDialogId();
      DataElement dataElement = Transaction.getInstance().getDialogId(dialogId);

      if (dataElement == null) {
        MapProxyDialog mapProxyDialog;
        mapProxyDialog = getMapProxyDialog(request.getMAPDialog(),
                filterForRule, MapDialogType.Mobility, request.getInvokeId());
        return MapProxyInsertSubscriberData.getInsertSubDataRequest(mapProxyDialog, request,
                this.transactionId);
      }
      if (logger.isDebugEnabled()) {
        logger.info(String.format(
                "[MAP::CONTINUE<%s>] Continue from '%s', dialogId = '%d', InvokeId = '%d', [ISD Request: Dialog Id = '%d', Invoke Id = '%d'], %s",
                request.getMessageType().toString(), dataElement.getMessageType(),
                dataElement.getDialogId(), dataElement.getInvokeId(), dialogId, request.getInvokeId(),
                this.transactionId));
      }
      return MapProxyInsertSubscriberData.sendUpdateLocation(request, dataElement,
              this.transactionId);
    } catch (Exception ex) {
      logger.error("Exception caught: " + ex + ". " + this.transactionId);
    }
    return null;
  }


  private MapDialogOut processLocationUpdate() {
    try {
      UpdateLocationRequest updateLocationRequest = (UpdateLocationRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(updateLocationRequest.getImsi()),
              "",
              "",
              updateLocationRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              updateLocationRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      return MapProxyUpdateLocation
          .getLocationRequest(getMapProxyDialog(updateLocationRequest.getMAPDialog(),
                  filterForRule, MapDialogType.Mobility,
              updateLocationRequest.getInvokeId()), updateLocationRequest, this.transactionId);
    } catch (Exception e) {
      logger.error("Error processing Location Update. Error: ", e);
      // return the reason for the failure
      return discardReason(e);
    }
  }

  private MapDialogOut processGPRSLocationUpdate() {
    try {
      UpdateGprsLocationRequest updateGprs = (UpdateGprsLocationRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(updateGprs.getImsi()),
              "",
              "",
              updateGprs.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              updateGprs.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog =
          getMapProxyDialog(updateGprs.getMAPDialog(), filterForRule,
              MapDialogType.Mobility, updateGprs.getInvokeId());
      return MapProxyUpdateLocation.getGprsLocationRequest(mapProxyDialog, updateGprs,
          this.transactionId);
    } catch (Exception e) {
      logger.error("Error processing GPRS Location Update. Error: ", e);
      return discardReason(e);
    }
  }



  private MapDialogOut processSendAuthentication() {
    try {
      SendAuthenticationInfoRequest sendAuthInfoReq = (SendAuthenticationInfoRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(sendAuthInfoReq.getImsi()),
              "",
              "",
              sendAuthInfoReq.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              sendAuthInfoReq.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(sendAuthInfoReq.getMAPDialog(),
              filterForRule, MapDialogType.Mobility,
          sendAuthInfoReq.getInvokeId());
      return MapProxySendAuthenticationInfo.getSendAuthenticationInfoRequest(mapProxyDialog,
          sendAuthInfoReq, this.transactionId);
    } catch (Exception e) {
      logger.error(
          "Error prorcessing sendAuthenicationInfo_Request. " + this.transactionId + " Error: ", e);
      return discardReason(e);
    }
  }

  private MapDialogOut processAnyTimeInterrogation() {
    try {
      AnyTimeInterrogationRequest anyTimeInterrogationRequest =
          (AnyTimeInterrogationRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              anyTimeInterrogationRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              anyTimeInterrogationRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog =
          getMapProxyDialog(anyTimeInterrogationRequest.getMAPDialog(), filterForRule,
              MapDialogType.Mobility, anyTimeInterrogationRequest.getInvokeId());
      return MapProxyAnyTimeInterrogation.getRequest(mapProxyDialog, anyTimeInterrogationRequest,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processCheckIMEI() {
    try {
      CheckImeiRequest imeiRequest = (CheckImeiRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(imeiRequest.getIMSI()),
              "",
              "",
              imeiRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              imeiRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog =
          getMapProxyDialog(imeiRequest.getMAPDialog(), filterForRule,
              MapDialogType.Mobility, imeiRequest.getInvokeId());
      return MapProxyCheckIMEI.getRequest(mapProxyDialog, imeiRequest, this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processPurgeMS() {
    try {
      PurgeMSRequest purgeMSRequest = (PurgeMSRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(purgeMSRequest.getImsi()),
              "",
              "",
              purgeMSRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              purgeMSRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(purgeMSRequest.getMAPDialog(),
              filterForRule, MapDialogType.Mobility,
          purgeMSRequest.getInvokeId());
      return MapProxyPurgeMS.getRequest(mapProxyDialog, purgeMSRequest, this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processSendIdentification() {
    try {
      SendIdentificationRequest sendIdentificationRequest =
          (SendIdentificationRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              sendIdentificationRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              sendIdentificationRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog =
          getMapProxyDialog(sendIdentificationRequest.getMAPDialog(), filterForRule,
              MapDialogType.Mobility, sendIdentificationRequest.getInvokeId());
      return MapProxySendIdentification.getRequest(mapProxyDialog, sendIdentificationRequest,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processProvideSubscriberInfo() {
    try {
      ProvideSubscriberInfoRequest subInfoRequest = (ProvideSubscriberInfoRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(subInfoRequest.getImsi()),
              "",
              "",
              subInfoRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              subInfoRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(subInfoRequest.getMAPDialog(),
              filterForRule, MapDialogType.Mobility,
          subInfoRequest.getInvokeId());
      return MapProxyProvideSubscriberInfo.getRequest(mapProxyDialog, subInfoRequest,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processDeleteSubscriber() {
    try {
      DeleteSubscriberDataRequest deleteSubDataRequest = (DeleteSubscriberDataRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(deleteSubDataRequest.getImsi()),
              "",
              "",
              deleteSubDataRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              deleteSubDataRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(deleteSubDataRequest.getMAPDialog(),
              filterForRule, MapDialogType.Mobility,
          deleteSubDataRequest.getInvokeId());
      return MapProxyDeleteSubscriberData.getRequest(mapProxyDialog, deleteSubDataRequest,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processAnyTimeSubscription() {
    try {
      AnyTimeSubscriptionInterrogationRequest anyTimeSubscriptionRequest =
          (AnyTimeSubscriptionInterrogationRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              anyTimeSubscriptionRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              anyTimeSubscriptionRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog =
          getMapProxyDialog(anyTimeSubscriptionRequest.getMAPDialog(), filterForRule,
              MapDialogType.Mobility, anyTimeSubscriptionRequest.getInvokeId());
      return MapProxyAnyTimeSubscriptionInterrogation.getRequest(mapProxyDialog,
          anyTimeSubscriptionRequest, this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processProvideRoamingNumber() {
    try {
      ProvideRoamingNumberRequest roamingNumberRequest = (ProvideRoamingNumberRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(roamingNumberRequest.getImsi()),
              MapProxyUtilsHelper.getMsisdn(roamingNumberRequest.getMsisdn()),
              "",
              roamingNumberRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              roamingNumberRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(roamingNumberRequest.getMAPDialog(),
              filterForRule, MapDialogType.CallHandling,
          roamingNumberRequest.getInvokeId());
      return MapProxyProvideRoamingNumber.getRequest(mapProxyDialog, roamingNumberRequest,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processSendRoutingInfo() {
    try {
      SendRoutingInformationRequest sendRoutingInfoReq = (SendRoutingInformationRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              sendRoutingInfoReq.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              sendRoutingInfoReq.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(sendRoutingInfoReq.getMAPDialog(),
              filterForRule, MapDialogType.CallHandling, sendRoutingInfoReq.getInvokeId());
      return MapProxySendRoutingInfo.getRequest(mapProxyDialog, sendRoutingInfoReq,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processMtForwardSM() {
    try {
      MtForwardShortMessageRequest mtShortMessage = (MtForwardShortMessageRequest) this.message;
      String messageText = MapProxyUtilsHelper.getMsgData(mtShortMessage.getSM_RP_UI(), false);
      Long dialogId = mtShortMessage.getMAPDialog().getLocalDialogId();
      DataElement dataElement = Transaction.getInstance().getDialogId(dialogId);
      if (dataElement != null) {
        logger.info(String.format(
            "[MAP::CONTINUE<%s>] Continue from '%s', dialogId = '%d', InvokeId = '%d', [ISD Request: Dialog Id = '%d', Invoke Id = '%d'], %s",
            mtShortMessage.getMessageType().toString(), dataElement.getMessageType(),
            dataElement.getDialogId(), dataElement.getInvokeId(), dialogId,
            mtShortMessage.getInvokeId(), this.transactionId));
        return MapProxyMoMtForwardSM.contMtForwardSm(mtShortMessage, dataElement, transactionId);
      }
      String msisdn = null;
      Optional<IMSI> smImsi = Optional.ofNullable(mtShortMessage.getSM_RP_DA()).map(SM_RP_DA::getIMSI);
      SmsSignalInfo si = mtShortMessage.getSM_RP_UI();
      SmsTpdu smsTpdu = si.decodeTpdu(false);
      if (smsTpdu instanceof SmsDeliverTpdu) {
        SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
        msisdn = smsDeliverTpdu.getOriginatingAddress().getAddressValue();
      }

      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(smImsi.orElse(null)),
              msisdn,
              "",
              mtShortMessage.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              mtShortMessage.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              messageText
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(mtShortMessage.getMAPDialog(), filterForRule,
          MapDialogType.SMS, mtShortMessage.getInvokeId());
      return MapProxyMoMtForwardSM.getMtForwardSMRequest(mapProxyDialog, mtShortMessage,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processMoForwardSM() {
    try {
      MoForwardShortMessageRequest moForwSmInd = (MoForwardShortMessageRequest) this.message;
      Optional<ISDNAddressString> smMsisdn= Optional.ofNullable(moForwSmInd.getSM_RP_OA()).map(SM_RP_OA::getMsisdn);
      String dstMsisdn = null;
      SmsSignalInfo si = moForwSmInd.getSM_RP_UI();
      // decodeTpdu -> adding true as a param cause is a MO message
      SmsTpdu smsTpdu = si.decodeTpdu(true);
      if (smsTpdu instanceof SmsSubmitTpdu) {
        SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
        dstMsisdn = smsSubmitTpdu.getDestinationAddress().getAddressValue();
      }
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(moForwSmInd.getIMSI()),
              MapProxyUtilsHelper.getMsisdn(smMsisdn.orElse(null)),
              dstMsisdn,
              moForwSmInd.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              moForwSmInd.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              MapProxyUtilsHelper.getMsgData(moForwSmInd.getSM_RP_UI(), true)
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(moForwSmInd.getMAPDialog(),
              filterForRule, MapDialogType.SMS, moForwSmInd.getInvokeId());
      return MapProxyMoMtForwardSM.getMoForwardSMRequest(mapProxyDialog, moForwSmInd,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processSendRoutingInfoForSm() {
    try {
      SendRoutingInfoForSMRequest request = (SendRoutingInfoForSMRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(request.getImsi()),
              "",
              MapProxyUtilsHelper.getMsisdn(request.getMsisdn()),
              request.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              request.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(request.getMAPDialog(),
              filterForRule, MapDialogType.SMS, request.getInvokeId());
      return MapProxySendRoutingInfoForSM.processRequest(mapProxyDialog, request,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processForwardSM() {
    try {
      ForwardShortMessageRequest request = (ForwardShortMessageRequest) this.message;
      Optional<ISDNAddressString> smMsisdn= Optional.ofNullable(request.getSM_RP_OA()).map(SM_RP_OA::getMsisdn);
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              MapProxyUtilsHelper.getImsi(request.getSM_RP_DA().getIMSI()),
              MapProxyUtilsHelper.getMsisdn(smMsisdn.orElse(null)),
              "",
              request.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              request.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(request.getMAPDialog(),
              filterForRule, MapDialogType.SMS,
          request.getInvokeId());
      return MapProxyForwardShortMessage.processRequest(mapProxyDialog, request,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processSendIMSI() {
    try {
      SendImsiRequest sendIMSIRequest = (SendImsiRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              sendIMSIRequest.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              sendIMSIRequest.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(sendIMSIRequest.getMAPDialog(),
              filterForRule, MapDialogType.OAM, sendIMSIRequest.getInvokeId());
      return MapProxySendIMSI.getRequest(mapProxyDialog, sendIMSIRequest, this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processProcessUSSR() {
    try {
      ProcessUnstructuredSSRequest procUnstrReqInd = (ProcessUnstructuredSSRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              procUnstrReqInd.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              procUnstrReqInd.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(procUnstrReqInd.getMAPDialog(),
              filterForRule, MapDialogType.Supplementary, procUnstrReqInd.getInvokeId());
      return MapProxyProcessUnstructuredSSRequest.getRequest(mapProxyDialog, procUnstrReqInd,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut processUSSR() {
    try {
      UnstructuredSSRequest unstrReqInd = (UnstructuredSSRequest) this.message;
      FilterForRule filterForRule = new FilterForRule(
              this.messageType.name(),
              "",
              "",
              "",
              unstrReqInd.getMAPDialog().getLocalAddress().getGlobalTitle().getDigits(),
              unstrReqInd.getMAPDialog().getRemoteAddress().getGlobalTitle().getDigits(),
              "",
              ""
      );
      MapProxyDialog mapProxyDialog = getMapProxyDialog(unstrReqInd.getMAPDialog(),
              filterForRule, MapDialogType.Supplementary, unstrReqInd.getInvokeId());
      return MapProxyUnstructuredSSRequest.getRequest(mapProxyDialog, unstrReqInd,
          this.transactionId);
    } catch (Exception e) {
      return discardReason(e);
    }
  }

  private MapDialogOut discardReason(Exception e) {
    MapDialogOut builder = new MapDialogOut();
    logger.debug(
        String.format("Error process %s. TransactionId = %s", this.messageType, this.transactionId),
        e);
    builder.setDiscardReason(e.getMessage());
    return builder;
  }

  public MapDialogOut processResponse() {
    // get the original dialog request
    switch (this.messageType) {
      case updateLocation_Response:
        return MapProxyUpdateLocation.getLocationResponse(this.message, this.transactionId);
      case updateGprsLocation_Response:
        return MapProxyUpdateLocation.getGprsLocationResponse(this.message, this.transactionId);
      case insertSubscriberData_Response:
        return MapProxyInsertSubscriberData.getInsertSubDataResponse(this.message,
            this.transactionId);
      case sendAuthenticationInfo_Response:
        return MapProxySendAuthenticationInfo.getSendAuthInfoResponse(this.message,
            this.transactionId);
      case forwardSM_Response:
        return MapProxyForwardShortMessage.processResponse(this.message, this.transactionId);
      case sendRoutingInfoForSM_Response:
        return MapProxySendRoutingInfoForSM.processResponse(this.message, this.transactionId);
      case mtForwardSM_Response:
        return MapProxyMoMtForwardSM.getMtForwardSMResponse(this.message, this.transactionId);
      case moForwardSM_Response:
        return MapProxyMoMtForwardSM.getMOForwardSMResponse(this.message, this.transactionId);
      case privideRoamingNumber_Response:
        return MapProxyProvideRoamingNumber.getResponse(this.message, this.transactionId);
      case checkIMEI_Response:
        return MapProxyCheckIMEI.getResponse(this.message, this.transactionId);
      case anyTimeInterrogation_Response:
        return MapProxyAnyTimeInterrogation.getResponse(this.message, this.transactionId);
      case sendIdentification_Response:
        return MapProxySendIdentification.getResponse(this.message, this.transactionId);
      case purgeMS_Response:
        return MapProxyPurgeMS.getResponse(this.message, this.transactionId);
      case provideSubscriberInfo_Response:
        return MapProxyProvideSubscriberInfo.getResponse(this.message, this.transactionId);
      case deleteSubscriberData_Response:
        return MapProxyDeleteSubscriberData.getResponse(this.message, this.transactionId);
      case anyTimeSubscriptionInterrogation_Response:
        return MapProxyAnyTimeSubscriptionInterrogation.getResponse(this.message,
            this.transactionId);
      case sendIMSI_Response:
        return MapProxySendIMSI.getResponse(this.message, this.transactionId);
      case sendRoutingInfo_Response:
        return MapProxySendRoutingInfo.getResponse(this.message, this.transactionId);
      case processUnstructuredSSRequest_Response:
        return MapProxyProcessUnstructuredSSRequest.getResponse(this.message, this.transactionId);
      case unstructuredSSRequest_Response:
        return MapProxyUnstructuredSSRequest.getResponse(this.message, this.transactionId);
      default:
        return null;
    }
  }


  public void setMessageType(MAPMessageType messageType) {
    this.messageType = messageType;
  }

  public void setMessage(Object message) {
    this.message = message;
  }

  public void setMapLayer(MapLayer map) {
    this.map = map;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

}
