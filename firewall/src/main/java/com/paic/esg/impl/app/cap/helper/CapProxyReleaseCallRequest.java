package com.paic.esg.impl.app.cap.helper;

import com.paic.esg.impl.app.cap.BcsmCallContent;
import com.paic.esg.impl.app.cap.BcsmCallStep;
import com.paic.esg.impl.app.cap.CapDialogOut;
import com.paic.esg.impl.app.cap.CapDialogType;
import com.paic.esg.impl.app.cap.CapProxyHelperUtils;
import com.paic.esg.info.CapTransaction;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.cap.api.CAPException;
import org.restcomm.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.restcomm.protocols.ss7.cap.api.service.circuitSwitchedCall.ReleaseCallRequest;

/**
 * CapProxyReleaseCallRequest
 */
public class CapProxyReleaseCallRequest {
  private ReleaseCallRequest request;
  private String channelTransId;
  private static final Logger logger = Logger.getLogger(CapProxyReleaseCallRequest.class);

  public CapProxyReleaseCallRequest(ReleaseCallRequest request, String channelTransId) {
    this.request = request;
    this.channelTransId = channelTransId;
  }

  public CapDialogOut process() {
    try {
      logger.debug(String.format("[CAP::REQUEST<%s>] dialogId '%d', invokeId '%d', %s",
          request.getMessageType().toString(), request.getCAPDialog().getLocalDialogId(),
          request.getInvokeId(), channelTransId));
      Long dialogId = request.getCAPDialog().getLocalDialogId();
      CapDialogOut capDialogOut =
          new CapDialogOut(CapDialogType.CircuitSwitchedCallControl, channelTransId);
          capDialogOut.setTransDialogId(dialogId);
      BcsmCallContent callContent = CapTransaction.instance().getSSFBcsmCallContent(dialogId, true);
      if (callContent == null) {
        // call content not found
        logger.debug(
            "onReleaseCallRequest event, HPLMN CAP dialog closed via a TC-Close due to a previous event from the VPLMN (call disconnected) "
                + channelTransId);
        return CapProxyHelperUtils.closeDialog(request.getCAPDialog(), dialogId, channelTransId,
            false);
      }
      // call released
      callContent.setRel(request);
      callContent.setStep(BcsmCallStep.relReceived);
      CAPDialogCircuitSwitchedCall relDialogOut =
          CapProxyHelperUtils.applyReplaceRulesOnly(request.getMessageType().toString(),
              request.getCAPDialog(), callContent.getCapDialog(), channelTransId, capDialogOut);

      relDialogOut.addReleaseCallRequest(request.getCause());
      logger.debug("CAP REL to be sent on CAP proxy to VPLMN over dialog: " + relDialogOut
          + "; Calling Party Address=" + relDialogOut.getLocalAddress() + "; Called Party Address="
          + relDialogOut.getRemoteAddress() + "; " + channelTransId);

      callContent.setStep(BcsmCallStep.relSent);
      // return the cap dialog out
      
      capDialogOut.setWriteCDR(CapDialogOut.WriteLogState.WRITE);
      capDialogOut.setCapDialogCircuitSwitchedCall(relDialogOut);
      
      capDialogOut.setIsClose(true);
      return capDialogOut;

    } catch (CAPException capEx) {
      logger.error("Processing CAP ERB Request failed for " + channelTransId + "Details: ", capEx);
      return CapProxyHelperUtils.discardReason(CapDialogType.CircuitSwitchedCallControl,
          capEx.getMessage(), request.getMessageType().toString(), channelTransId);
    } catch (Exception e) {
      logger.error("Exception caught for " + channelTransId + ". Details: ", e);
      return CapProxyHelperUtils.discardReason(CapDialogType.CircuitSwitchedCallControl,
          e.getMessage(), request.getMessageType().toString(), channelTransId);
    }
  }



}
