package com.paic.esg.impl.cdr;

import com.paic.esg.impl.settings.cdr.CdrSettings;
import org.apache.log4j.Logger;

import java.util.List;

public class CdrImpl {

  private final Logger logger = Logger.getLogger(CdrImpl.class);
  private List<CdrSettings> cdrSettings = null;
  private static CdrImpl instance = null;

  public static CdrImpl getInstance() {
    if (instance == null) {
      instance = new CdrImpl();
    }
    return instance;
  }

  private CdrImpl() {
  }

  public void setCdrSettings(List<CdrSettings> cdrSettings) {
    this.cdrSettings = cdrSettings;
  }

  public void write(Cdr cdr) {
    if (cdrSettings != null) {
      CdrSettings settings = null;

      for (CdrSettings lookUpSettings : cdrSettings) {
        if (lookUpSettings.getName().equalsIgnoreCase(cdr.getName())) {
          settings = lookUpSettings;
        }
      }

      if (settings != null) {
        Logger cdrLogger = Logger.getLogger(settings.getLogger());
        if (!settings.isDisplayName()) {
          cdrLogger.info(cdr.stringifyValues(settings.getSeparator(), settings.getFields()));
        } else {
          cdrLogger.info(cdr.stringifyAll(settings.getSeparator(), settings.getFields()));
        }
      } else {
        logger.error("Could not print CDR, CDR name '" + cdr.getName()
            + "' not found in any CDR Setting name. Not printed CDR: '" + cdr.stringifyAll(",", null)
            + "'");
      }
    } else {
      logger.error("Trying to use CDR Impl without loading up the configurations");
    }
  }

  public boolean verifyOperationType(String operationType) {
    if (cdrSettings != null) {
      CdrSettings settings = cdrSettings.get(0);
      operationType = operationType.split("_")[0];

      if (settings.isAllOperationsTypes()) {
        return true;
      }

      return settings.getOperationsTypes().contains(operationType);
    }
    return false;
  }
}
