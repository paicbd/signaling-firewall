package com.paic.esg.impl.app.map.helper;

import com.paic.esg.impl.app.map.MapDialogOut;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.*;

public class MapProxyUtilsHelper {
  private static final Logger logger = Logger.getLogger(MapProxyUtilsHelper.class);
  private static String cdrName;
  private static boolean cdrIsEnabled = false;

  private MapProxyUtilsHelper() {
  }

  public static void setCDRName(String cdrName) {
    MapProxyUtilsHelper.cdrName = cdrName;
    MapProxyUtilsHelper.cdrIsEnabled = (cdrName != null && !cdrName.isEmpty());
  }

  public static String getCDRName() {
    return MapProxyUtilsHelper.cdrName;
  }

  public static boolean isCDREnabled() {
    return MapProxyUtilsHelper.cdrIsEnabled;
  }

  public static MapDialogOut discardReason(String message, String messageType,
      String transactionId, long originalDialogId) {
    MapDialogOut builder = new MapDialogOut();
    logger.debug(String.format("Discard for <%s>. TransactionId = %s, Reason = %s", messageType,
        transactionId, message));
    builder.setDiscardReason(message);
    builder.setOriginalDialogId(originalDialogId);
    return builder;
  }

  public static String getMsgData(SmsSignalInfo si, boolean isMO) {
    String msg = null;
    if (si != null) {
      try {
        SmsTpdu smsTpdu = si.decodeTpdu(isMO);
        if (isMO) {
          SmsSubmitTpdu smsSubmitTpdu = null;
          smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
          UserData ud = smsSubmitTpdu.getUserData();
          ud.decode();
          msg = ud.getDecodedMessage();
        } else {
          SmsDeliverTpdu dTpdu = null;
          dTpdu = (SmsDeliverTpdu) smsTpdu;
          UserData ud = dTpdu.getUserData();
          ud.decode();
          msg = ud.getDecodedMessage();
        }
      } catch (Exception ex) {
        logger.error("Error on get message data ");
      }
    }
    return msg;
  }

  public static String getImsi(IMSI imsi) {
    String imsiValue = "";
    if (imsi != null) {
      imsiValue = imsi.getData();
    }
    return imsiValue;
  }

  public static String getMsisdn(ISDNAddressString msisdn) {
    String msisdnValue = "";
    if (msisdn != null) {
      msisdnValue = msisdn.getAddress();
    }
    return msisdnValue;
  }
}
