package com.paic.esg.impl.app.map.helper;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.paic.esg.impl.app.map.MapDialogOut;
import com.paic.esg.impl.cdr.Cdr;
import com.paic.esg.impl.cdr.CdrImpl;
import com.paic.esg.info.MapProxyCdrRecords;
import org.apache.log4j.Logger;

public class MapProxyCDRWriter {

  private static final Logger logger = Logger.getLogger(MapProxyCDRWriter.class);

  private MapProxyCDRWriter() {
  }

  private static final String FORMAT_DATE = "yyyyddMMHHmmssSSSZ";
  private static long incrId = 0;

  public static void addFields(MapDialogOut dialogOut, String messageType, String sessionId) {
    if (CdrImpl.getInstance().verifyOperationType(messageType)) {
      Map<String, Object> fields = new HashMap<>();
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT_DATE);
      if (dialogOut.getProxyDialog() == null) {
        // Rule not match.
        fields.put("ORIGINAL_IMSI", "");
        fields.put("NEW_IMSI", "");
        fields.put("START_TIME", simpleDateFormat.format(new Date()));
        fields.put("TIMESTAMP", simpleDateFormat.format(new Date()));
        fields.put("_computeDurationOnly", Instant.now());
        fields.put("LOCAL_DIALOG_ID", dialogOut.getOriginalDialogId());
        fields.put("REMOTE_DIALOG_ID", dialogOut.getRemoteDialogId());
        fields.put("NEW_DIALOG_ID", dialogOut.getNewDialogId());
        fields.put("PRIMITIVE", dialogOut.getDialogOutName(messageType));
        fields.put("RULE_NAME", "");
        fields.put("SESSION_ID", sessionId);
        fields.put("LOCAL_GT", getValue("LOCAL_GT", dialogOut));
        fields.put("LOCAL_ROUTING_INDICATOR", getValue("LOCAL_ROUTING_INDICATOR", dialogOut));
        fields.put("LOCAL_SSN", getValue("LOCAL_SSN", dialogOut));
        fields.put("LOCAL_SPC", getValue("LOCAL_SPC", dialogOut));
        fields.put("REMOTE_GT", getValue("REMOTE_GT", dialogOut));
        fields.put("REMOTE_ROUTING_INDICATOR", getValue("REMOTE_ROUTING_INDICATOR", dialogOut));
        fields.put("REMOTE_SSN", getValue("REMOTE_SSN", dialogOut));
        fields.put("REMOTE_SPC", getValue("REMOTE_SPC", dialogOut));

        // new generated GT for calling and called
        fields.put("NEW_CALLING_GT", "");
        fields.put("NEW_CALLED_GT", "");
        // new spc, ri, ssn
        fields.put("NEW_LOCAL_ROUTING_INDICATOR", "");
        fields.put("NEW_LOCAL_SSN", "");
        fields.put("NEW_LOCAL_SPC", "");
        fields.put("NEW_REMOTE_GT", "");
        fields.put("NEW_REMOTE_ROUTING_INDICATOR", "");
        fields.put("NEW_REMOTE_SSN", "");
        fields.put("NEW_REMOTE_SPC", "");
        MapProxyCdrRecords.getInstance().addCDRFields(dialogOut.getOriginalDialogId(), fields);
      } else {
        fields.put("ORIGINAL_IMSI", dialogOut.getProxyDialog().getOriginalImsi());
        fields.put("NEW_IMSI", dialogOut.getProxyDialog().getNewIMSI());
        fields.put("START_TIME", simpleDateFormat.format(new Date()));
        fields.put("TIMESTAMP", simpleDateFormat.format(new Date()));
        fields.put("_computeDurationOnly", Instant.now());
        fields.put("LOCAL_DIALOG_ID", dialogOut.getOriginalDialogId());
        fields.put("REMOTE_DIALOG_ID", dialogOut.getRemoteDialogId());
        fields.put("NEW_DIALOG_ID", dialogOut.getNewDialogId());
        fields.put("PRIMITIVE", dialogOut.getDialogOutName(messageType));
        fields.put("RULE_NAME", dialogOut.getProxyDialog().getRuleName());
        fields.put("SESSION_ID", sessionId);
        if (dialogOut.getProxyDialog().getCalledAddress() != null) {
          fields.put("LOCAL_GT",
                  dialogOut.getProxyDialog().getCalledAddress().getGlobalTitle().getDigits());
          fields.put("LOCAL_ROUTING_INDICATOR", dialogOut.getProxyDialog().getCalledAddress()
                  .getAddressIndicator().getRoutingIndicator().getValue());
          fields.put("LOCAL_SSN", dialogOut.getProxyDialog().getCalledAddress().getSubsystemNumber());
          fields.put("LOCAL_SPC",
                  dialogOut.getProxyDialog().getCalledAddress().getSignalingPointCode());
        } else {
          fields.put("LOCAL_GT", "");
          fields.put("LOCAL_ROUTING_INDICATOR", "");
          fields.put("LOCAL_SSN", "");
          fields.put("LOCAL_SPC", "");
        }

        if (dialogOut.getProxyDialog().getCallingAddress() != null) {
          fields.put("REMOTE_GT",
                  dialogOut.getProxyDialog().getCallingAddress().getGlobalTitle().getDigits());
          fields.put("REMOTE_ROUTING_INDICATOR", dialogOut.getProxyDialog().getCallingAddress()
                  .getAddressIndicator().getRoutingIndicator().getValue());
          fields.put("REMOTE_SSN", dialogOut.getProxyDialog().getCallingAddress().getSubsystemNumber());
          fields.put("REMOTE_SPC",
                  dialogOut.getProxyDialog().getCallingAddress().getSignalingPointCode());
        } else {
          fields.put("REMOTE_GT", "");
          fields.put("REMOTE_ROUTING_INDICATOR", "");
          fields.put("REMOTE_SSN", "");
          fields.put("REMOTE_SPC", "");
        }
        // new generated GT for calling and called
        fields.put("NEW_CALLING_GT", dialogOut.getProxyDialog().getNewCallingGt());
        fields.put("NEW_CALLED_GT", dialogOut.getProxyDialog().getNewCalledGt());
        // new spc, ri, ssn
        if (dialogOut.getProxyDialog().getNewCalledAddress() != null) {
          fields.put("NEW_LOCAL_ROUTING_INDICATOR", dialogOut.getProxyDialog().getNewCalledAddress()
                  .getAddressIndicator().getRoutingIndicator().getValue());
          fields.put("NEW_LOCAL_SSN",
                  dialogOut.getProxyDialog().getNewCalledAddress().getSubsystemNumber());
          fields.put("NEW_LOCAL_SPC",
                  dialogOut.getProxyDialog().getNewCalledAddress().getSignalingPointCode());
        } else {
          fields.put("NEW_LOCAL_ROUTING_INDICATOR", "");
          fields.put("NEW_LOCAL_SSN", "");
          fields.put("NEW_LOCAL_SPC", "");
        }
        if (dialogOut.getProxyDialog().getNewCallingAddress() != null) {
          fields.put("NEW_REMOTE_GT",
                  dialogOut.getProxyDialog().getNewCallingAddress().getGlobalTitle().getDigits());
          fields.put("NEW_REMOTE_ROUTING_INDICATOR", dialogOut.getProxyDialog().getNewCallingAddress()
                  .getAddressIndicator().getRoutingIndicator().getValue());
          fields.put("NEW_REMOTE_SSN",
                  dialogOut.getProxyDialog().getNewCallingAddress().getSubsystemNumber());
          fields.put("NEW_REMOTE_SPC",
                  dialogOut.getProxyDialog().getNewCallingAddress().getSignalingPointCode());
        } else {
          fields.put("NEW_REMOTE_GT", "");
          fields.put("NEW_REMOTE_ROUTING_INDICATOR", "");
          fields.put("NEW_REMOTE_SSN", "");
          fields.put("NEW_REMOTE_SPC", "");
        }
        MapProxyCdrRecords.getInstance().addCDRFields(dialogOut.getNewDialogId(), fields);
      }
    }
  }


  private static String getValue(String field, MapDialogOut mapDialogOut) {
    String result = "";
    try {
      switch (field) {
        case "LOCAL_GT":
          result = mapDialogOut.getMapDialog().getLocalAddress().getGlobalTitle().getDigits();
          break;
        case "LOCAL_ROUTING_INDICATOR":
          result = mapDialogOut.getMapDialog().getLocalAddress().getAddressIndicator().getRoutingIndicator().getValue() + "";
          break;
        case "LOCAL_SSN":
          result = String.valueOf(mapDialogOut.getMapDialog().getLocalAddress().getSubsystemNumber());
          break;
        case "LOCAL_SPC":
          result = String.valueOf(mapDialogOut.getMapDialog().getLocalAddress().getSignalingPointCode());
          break;
        case "REMOTE_GT":
          result = mapDialogOut.getMapDialog().getRemoteAddress().getGlobalTitle().getDigits();
          break;
        case "REMOTE_ROUTING_INDICATOR":
          result = mapDialogOut.getMapDialog().getRemoteAddress().getAddressIndicator().getRoutingIndicator().getValue() + "";
          break;
        case "REMOTE_SSN":
          result = String.valueOf(mapDialogOut.getMapDialog().getRemoteAddress().getSubsystemNumber());
          break;
        case "REMOTE_SPC":
          result = String.valueOf(mapDialogOut.getMapDialog().getRemoteAddress().getSignalingPointCode());
          break;
        default:
          result = "";
          break;
      }
    } catch (NullPointerException exception) {
      result = "";
      logger.warn("error on get value of field " + field );
    }
    return result;
  }

  public static void writeCDR(Long dialogId, String status, Long errorCode, String errorMsg) {
    if (dialogId == null) {
      return;
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT_DATE);
    Optional.ofNullable(MapProxyCdrRecords.getInstance().getCDRFields(dialogId))
        .ifPresent(fields -> {
          Cdr cdrRecord = new Cdr(fields, MapProxyUtilsHelper.getCDRName());
          Instant starttime = (Instant) fields.get("_computeDurationOnly");
          long duration = ChronoUnit.MILLIS.between(starttime, Instant.now());
          cdrRecord.addField("DURATION", duration);
          cdrRecord.addField("STATUS", status);
          cdrRecord.addField("ERROR_CODE", errorCode);
          cdrRecord.addField("ERROR_CODE_MESSAGE", errorMsg);
          cdrRecord.addField("ENDTIME", simpleDateFormat.format(new Date()));
          incrId += 1;
          cdrRecord.addField("ID", incrId);
          CdrImpl.getInstance().write(cdrRecord);
        }
        );
  }
}
