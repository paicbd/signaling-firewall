package com.paic.esg.impl.app.cap.helper;

import static com.paic.esg.api.settings.byteUtils.hexStringToByteArray;
import com.paic.esg.impl.app.cap.BcsmCallContent;
import com.paic.esg.impl.app.cap.BcsmCallStep;
import com.paic.esg.impl.app.cap.CapDialogOut;
import com.paic.esg.impl.app.cap.CapDialogType;
import com.paic.esg.impl.app.cap.CapProxyHelperUtils;
import com.paic.esg.impl.app.cap.CapDialogOut.WriteLogState;
import com.paic.esg.info.CapTransaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.cap.api.CAPException;
import org.restcomm.protocols.ss7.cap.api.primitives.AppendFreeFormatData;
import org.restcomm.protocols.ss7.cap.api.primitives.SendingSideID;
import org.restcomm.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.restcomm.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.restcomm.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.FCIBCCCAMELsequence1;
import org.restcomm.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.FreeFormatData;
import org.restcomm.protocols.ss7.cap.primitives.SendingSideIDImpl;
import org.restcomm.protocols.ss7.cap.service.circuitSwitchedCall.primitive.FCIBCCCAMELsequence1Impl;
import org.restcomm.protocols.ss7.cap.service.circuitSwitchedCall.primitive.FreeFormatDataImpl;
import org.restcomm.protocols.ss7.inap.api.primitives.LegType;

/**
 * CapProxyEventReportBCSMRequest
 */
public class CapProxyEventReportBCSMRequest {
  private static final String OANSWER = "oAnswer";
  private static final Logger logger = Logger.getLogger(CapProxyEventReportBCSMRequest.class);
  private EventReportBCSMRequest request;
  private String channelTransId;
  private String erbEventName;
  public CapProxyEventReportBCSMRequest(EventReportBCSMRequest request, String channelTransId) {
    this.request = request;
    this.channelTransId = channelTransId;
  }

  public CapDialogOut process() {
    try {
      logger.debug(String.format("[CAP::REQUEST<%s>] dialogId '%d', invokeId '%d', %s",
          request.getMessageType().toString(), request.getCAPDialog().getLocalDialogId(),
          request.getInvokeId(), channelTransId));
      Long dialogId = request.getCAPDialog().getLocalDialogId();
      logger.trace("Checking for which leg this transaction belongs");
      // check for the second leg if the call exist.
      BcsmCallContent callContent = null;
      boolean isLeg2 = false;
      callContent = CapTransaction.instance().getLeg2BcsmCall(dialogId); // leg2
      if (callContent == null) {
        // second leg does not exist. Check the first leg
        callContent = CapTransaction.instance().getScfBcsmCallContent(dialogId, false);
        if (callContent == null) {
          logger.debug(String.format("Transaction not found for DialogId = '%d'", dialogId));
          return CapProxyHelperUtils.closeDialog(request.getCAPDialog(), dialogId, channelTransId,
              true);
        }
        logger.trace(String.format("Transaction found in Leg1. DialogId = %d", dialogId));
      } else {
        logger.trace(String.format("Transaction found in Leg2. DialogId = %d", dialogId));
        isLeg2 = true;
      }

      erbEventName = request.getEventTypeBCSM().toString();
      // check for leg2
      if (isLeg2) {
        // leg2 active
        leg2EventTypes(callContent);
        if (request.getEventTypeBCSM().name().equalsIgnoreCase(OANSWER)) {
          return sendCancelAndFciToVPLMNonLeg2(callContent);
        }
      } else {
        // leg1 active
        leg1EventType(callContent);
        if (request.getEventTypeBCSM().name().equalsIgnoreCase(OANSWER)) {
          return relayERBForLeg1(callContent);
        }
        if (request.getEventTypeBCSM().name().equalsIgnoreCase("oDisconnect")) {
          return relayERBForLeg1(callContent);
        }
      }
    } catch (CAPException capEx) {
      logger.error("Processing CAP ERB Request failed " + channelTransId + "Details: ", capEx);
      return CapProxyHelperUtils.discardReason(CapDialogType.CircuitSwitchedCallControl,
          capEx.getMessage(), request.getMessageType().toString(), channelTransId);
    } catch (Exception e) {
      logger.error("Exception caught for " + channelTransId + ". Details: ", e);
      return CapProxyHelperUtils.discardReason(CapDialogType.CircuitSwitchedCallControl,
          e.getMessage(), request.getMessageType().toString(), channelTransId);
    }
    return null;
  }

  private void leg1EventType(BcsmCallContent callContent) {
    callContent.setErb(request);
    callContent.getErbEventList().add(request);
    switch (request.getEventTypeBCSM()) {
      case collectedInfo:
        callContent.setStep(BcsmCallStep.collectedInfo);
        break;
      case analyzedInformation:
        callContent.setStep(BcsmCallStep.analizedInformation);
        break;
      case routeSelectFailure:
        callContent.setStep(BcsmCallStep.routeSelectFailure);
        break;
      case oNoAnswer:
        callContent.setStep(BcsmCallStep.noAnswer);
        break;
      case oAnswer:
        logger.debug("ERB " + request.getEventTypeBCSM() + " - event type captured on CAP proxy");
        callContent.setStep(BcsmCallStep.answerReceived);
        break;
      case oMidCall:
        callContent.setStep(BcsmCallStep.midCall);
        break;
      case oDisconnect:
        callContent.setStep(BcsmCallStep.disconnectReceived);
        logger.debug("ERB " + request.getEventTypeBCSM() + ", event type captured on CAP proxy");
        break;
      case oAbandon:
        callContent.setStep(BcsmCallStep.abandoned);
        logger.debug("ERB oAbandon event captured on CAP proxy");
        break;
      case tBusy:
        callContent.setStep(BcsmCallStep.busy);
        break;
      case tNoAnswer:
        callContent.setStep(BcsmCallStep.noAnswer);
        break;
      case tAnswer:
        callContent.setStep(BcsmCallStep.answerReceived);
        logger.debug("ERB tAnswer event captured on CAP proxy");
        break;
      case tMidCall:
        callContent.setStep(BcsmCallStep.midCall);
        break;
      case tDisconnect:
        callContent.setStep(BcsmCallStep.disconnected);
        logger.debug("ERB tDisconnect event captured on CAP proxy");
        break;
      case tAbandon:
        callContent.setStep(BcsmCallStep.abandoned);
        logger.debug("ERB tAbandon event captured on CAP proxy");
        break;
      case oTermSeized:
        callContent.setStep(BcsmCallStep.termSeized);
        break;
      case callAccepted:
        callContent.setStep(BcsmCallStep.callAccepted);
        break;
      case oChangeOfPosition:
        callContent.setStep(BcsmCallStep.changeOfPosition);
        break;
      case oServiceChange:
        callContent.setStep(BcsmCallStep.serviceChange);
        break;
      case tServiceChange:
        callContent.setStep(BcsmCallStep.serviceChange);
        break;
      default:
        callContent.setStep(BcsmCallStep.erbReceived);
        break;
    }
  }

  private void leg2EventTypes(BcsmCallContent callContent) {

    callContent.setErb(request);
    callContent.getErbEventList().add(request);
    switch (request.getEventTypeBCSM()) {
      case collectedInfo:
        callContent.setStep(BcsmCallStep.collectedInfo);
        break;
      case analyzedInformation:
        callContent.setStep(BcsmCallStep.analizedInformation);
        break;
      case routeSelectFailure:
        callContent.setStep(BcsmCallStep.routeSelectFailure);
        break;
      case oNoAnswer:
        callContent.setStep(BcsmCallStep.noAnswer);
        break;
      case oAnswer:
        logger.debug("ERB " + request.getEventTypeBCSM() + " event type captured on CAP proxy");
        callContent.setStep(BcsmCallStep.answerReceived);
        break;
      case oMidCall:
        callContent.setStep(BcsmCallStep.midCall);
        break;
      case oDisconnect:
        callContent.setStep(BcsmCallStep.disconnected);
        logger.debug("ERB " + request.getEventTypeBCSM() + " event type captured on CAP proxy");
        break;
      case oAbandon:
        callContent.setStep(BcsmCallStep.abandoned);
        logger.debug("ERB oAbandon event captured on CAP proxy");
        break;
      case tBusy:
        callContent.setStep(BcsmCallStep.busy);
        break;
      case tNoAnswer:
        callContent.setStep(BcsmCallStep.noAnswer);
        break;
      case tAnswer:
        callContent.setStep(BcsmCallStep.answerReceived);
        logger.debug("ERB tAnswer event captured on CAP proxy");
        break;
      case tMidCall:
        callContent.setStep(BcsmCallStep.midCall);
        break;
      case tDisconnect:
        callContent.setStep(BcsmCallStep.disconnected);
        logger.debug("ERB tDisconnect event captured on CAP proxy");
        break;
      case tAbandon:
        callContent.setStep(BcsmCallStep.abandoned);
        logger.debug("ERB tAbandon event captured on CAP proxy");
        break;
      case oTermSeized:
        callContent.setStep(BcsmCallStep.termSeized);
        break;
      case callAccepted:
        callContent.setStep(BcsmCallStep.callAccepted);
        break;
      case oChangeOfPosition:
        callContent.setStep(BcsmCallStep.changeOfPosition);
        break;
      case oServiceChange:
        callContent.setStep(BcsmCallStep.serviceChange);
        break;
      case tServiceChange:
        callContent.setStep(BcsmCallStep.serviceChange);
        break;
      default:
        callContent.setStep(BcsmCallStep.erbReceived);
        break;
    }
  }

  private CapDialogOut sendCancelAndFciToVPLMNonLeg2(BcsmCallContent callContent) {
    Long dialogId = request.getCAPDialog().getLocalDialogId();
    CapDialogOut capDialogOut =
        new CapDialogOut(CapDialogType.CircuitSwitchedCallControl, channelTransId);
    capDialogOut.setTransDialogId(dialogId);
    capDialogOut.setErbEventName(erbEventName);
    logger.debug(
        String.format("CAP Proxy to send CAP CAN and CAP FCI on leg 2 to VPLMN, DialogId = %d, %s",
            dialogId, channelTransId));
    try {
      CAPDialogCircuitSwitchedCall erbDialogOut = CapProxyHelperUtils.applyReplaceRulesOnly(
          request.getMessageType().toString(), request.getCAPDialog(), callContent.getCapDialog(),
          true, channelTransId, capDialogOut);
      erbDialogOut.addCancelRequest_AllRequests();
      // FIX ME: taken from provided Wireshark trace:
      String freeFormatDataHexStr =
          "a103800101a35ba32d04036001e20402630304026c0004091124159099000001f60404121056440404131256440407168353141220f1a42a04080e010045140801f6040915000079524471109204091400007952443100700408e1010045140801f6";
      byte[] freeFormatDataByteArray = hexStringToByteArray(freeFormatDataHexStr);
      FreeFormatData freeFormatData = new FreeFormatDataImpl(freeFormatDataByteArray);
      SendingSideID sendingSideID = new SendingSideIDImpl(LegType.leg2);
      AppendFreeFormatData appendFreeFormatData = null;
      FCIBCCCAMELsequence1 fcibcccameLsequence1 =
          new FCIBCCCAMELsequence1Impl(freeFormatData, sendingSideID, appendFreeFormatData);
      erbDialogOut.addFurnishChargingInformationRequest(fcibcccameLsequence1);
      callContent.setStep(BcsmCallStep.cancelSent);
      // close the dialog id
      capDialogOut.setIsClose(true);
      capDialogOut.setCapDialogCircuitSwitchedCall(erbDialogOut);
      logger.debug("On CAP ERB oAnswer for second leg, sent CAP CAN / FCI");
      capDialogOut.setWriteCDR(WriteLogState.ADDMORE);
      CapTransaction.instance().removeLeg2BcsmCall(dialogId);
      return capDialogOut;
    } catch (CAPException e) {
      logger.error("Exception caught for " + channelTransId + ". Details: ", e);
      return CapProxyHelperUtils.discardReason(CapDialogType.CircuitSwitchedCallControl,
          e.getMessage(), request.getMessageType().toString(), channelTransId);
    }
  }


  // LEG1
  private CapDialogOut relayERBForLeg1(BcsmCallContent callContent) throws CAPException {
    Long dialogId = request.getCAPDialog().getLocalDialogId();
    logger.trace("CAP Proxy about to relay ERB with event type " + request.getEventTypeBCSM()
        + " to HPLMN SCF via VPLMN STP SSF " + channelTransId);
    CapDialogOut capDialogOut =
        new CapDialogOut(CapDialogType.CircuitSwitchedCallControl, channelTransId);
    capDialogOut.setErbEventName(erbEventName);
    CAPDialogCircuitSwitchedCall ssfDialogOut =
        CapProxyHelperUtils.applyReplaceRulesOnly(request.getMessageType().toString(),
            request.getCAPDialog(), callContent.getSsfCapDialog(), channelTransId, capDialogOut);

    ssfDialogOut.addEventReportBCSMRequest(request.getEventTypeBCSM(),
        request.getEventSpecificInformationBCSM(), request.getLegID(), request.getMiscCallInfo(),
        null);

    logger.debug("CAP ERB sent to HPLMN SCF from CAP Proxy via VPLMN STP SSF over dialog: "
        + ssfDialogOut + "; Calling Party Address=" + ssfDialogOut.getLocalAddress()
        + "; Called Party Address=" + ssfDialogOut.getRemoteAddress() + "; " + channelTransId);
    capDialogOut.setTransDialogId(ssfDialogOut.getLocalDialogId());
    if (request.getEventTypeBCSM().name().equalsIgnoreCase(OANSWER)) {
      callContent.setStep(BcsmCallStep.answerSent);
    } else if (request.getEventTypeBCSM().name().equalsIgnoreCase("oDisconnect")) {
      callContent.setStep(BcsmCallStep.disconnectSent);
      // move the call for the SCF after disconnections
      // needs to be confirm if this will pose a problem to the memory usage
      // CapTransaction.instance().removSCFCallContent(dialogId)
    }
    CapTransaction.instance().updateSCFBcsmCallContent(dialogId, callContent);

    capDialogOut.setWriteCDR(WriteLogState.ADDMORE);
    capDialogOut.setCapDialogCircuitSwitchedCall(ssfDialogOut);

    return capDialogOut;
  }
}
