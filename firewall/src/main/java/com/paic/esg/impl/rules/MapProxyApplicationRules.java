package com.paic.esg.impl.rules;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.*;

import com.paic.esg.impl.rules.models.Gts;
import com.paic.esg.impl.rules.models.Match;
import com.paic.esg.impl.rules.models.Rule;
import com.paic.esg.impl.rules.models.Settings;
import org.apache.log4j.Logger;

/**
 * MapApplicationRules
 */
public class MapProxyApplicationRules {
  private static final Logger logger = Logger.getLogger(MapProxyApplicationRules.class);
  private CopyOnWriteArrayList<ApplicationRulesSetting> mapApplicationRules;
  private Settings mapApplicationSettings;
  static MapProxyApplicationRules sInstance = null;

  public static MapProxyApplicationRules getInstance() {
    if (sInstance == null)
      sInstance = new MapProxyApplicationRules();
    return sInstance;
  }

  private MapProxyApplicationRules() {
    mapApplicationRules = new CopyOnWriteArrayList<>();
    mapApplicationSettings  = new Settings();
  }

  public List<ApplicationRulesSetting> getApplicationRules() {
    return mapApplicationRules;
  }

  public void addMapApplicationRules(String filename) {
    try {
      XmlApplicationRules xmlApplicationRules = new XmlApplicationRules(filename);
     // mapApplicationRules.addAll(xmlApplicationRules.getApplicationRules());
      mapApplicationSettings = xmlApplicationRules.getAppSettings();
    } catch (Exception e) {
      logger.error("Exception caught:" + e);
    }
  }

  public void addCapApplicationRules(String filename) {
    try {
      XmlCapApplicationRules xmlCapAppRules = new XmlCapApplicationRules(filename);
      mapApplicationRules.addAll(xmlCapAppRules.getApplicationRules());
    } catch (Exception e) {
      logger.error("Exception caught reading CAP Application Rule xml file. Exception: " + e);
    }
  }

  public ApplicationRulesSetting findMAPApplicationRule(String callingGT, String calledGT,
      String imsiStr, String messageType) {
    Optional<ApplicationRulesSetting> appRulesSetting =
        this.mapApplicationRules.stream().filter(ruleSetting -> searchApplicationRules(ruleSetting,
            callingGT, calledGT, imsiStr, messageType)).findFirst();
    return appRulesSetting.orElse(null);
  }


  public Rule findMAPRule(FilterForRule filter) {
    Optional<Rule> rule  = this.mapApplicationSettings.getRules().stream().filter(
            r -> searchRule(r, filter)
    ).findFirst();
    return rule.orElse(null);
  }

  private boolean existInList(String nameList, String filterValue) {
    boolean existInlist = false;
    Optional<Gts> optionalGts = this.mapApplicationSettings.getGtsList().stream().filter(gts -> nameList.equals(gts.getName())).findFirst();
    if (optionalGts.isPresent()) {
      Gts listGt = optionalGts.get();
      if (listGt.getRegex() != null) {
        if (listGt.getRegex()) {
          //Match Regex
          Optional<String> gtMatcher = listGt.getGtList().stream().filter(gt -> Pattern.matches(gt, filterValue)).findFirst();
          existInlist = gtMatcher.isPresent();
        } else {
          existInlist = listGt.getGtList().contains(filterValue);
        }
      } else {
        //The list is not using regex
        existInlist = listGt.getGtList().contains(filterValue);
      }

    }
    return existInlist;
  }

  private boolean searchRule(Rule currentRule, FilterForRule filter) {
    if (logger.isDebugEnabled())
      logger.debug("Search rule using -> " + filter.toString());

    boolean result = false;
    boolean filterList = true;
    //Check Primitive
    if (currentRule.getMatch().getPrimitive() != null) {
      result = filter.getPrimitive().startsWith(currentRule.getMatch().getPrimitive());
    }
    //Check the GTs list
    if (result) {
      if (this.mapApplicationSettings.getGtsList() != null) {
        //Filter GT-List
        //Checking allowed rules
        if (currentRule.getGtsAllowed() != null) {
          filterList = existInList(currentRule.getGtsAllowed(), filter.getCalledPartyAddress());
        }

        //Checking blocked rules
        if (currentRule.getGtsBlocked() != null) {
          filterList = !existInList(currentRule.getGtsBlocked(), filter.getCalledPartyAddress());
        }
      }
      result = filterList;
      if (result) {
        //Continue checking the rules in the match tag
        Match ruleMatcher = currentRule.getMatch();
        if (ruleMatcher == null) {
          logger.warn("The Match part of the rule is empty");
          return false;
        }

        boolean useRegex = currentRule.getRegex() != null ? currentRule.getRegex() : false ;
        if (useRegex) {
          // check for IMSI pattern
          if (ruleMatcher.getImsi() != null) {
            String regexImsi = ruleMatcher.getImsi();
            result = isPatternMatch(regexImsi, filter.getImsi());
          } else {
            if (logger.isDebugEnabled())
              logger.debug("This Rule doesnt contains IMSI");
          }

          if (result) {
            // SRC-MSISDN
            if (ruleMatcher.getSrcMsisdn() != null) {
              String regexMsisdn = ruleMatcher.getSrcMsisdn();
              result = isPatternMatch(regexMsisdn, filter.getSrcMsisdn());
            } else {
              if (logger.isDebugEnabled())
                logger.debug("This Rule doesnt contains SRC-MSISDN");
            }
            if (result) {
              // DST-MSISDN
              if (ruleMatcher.getDstMsisdn() != null) {
                String regexMsisdn = ruleMatcher.getDstMsisdn();
                result = isPatternMatch(regexMsisdn, filter.getDstMsisdn());
              } else {
                if (logger.isDebugEnabled())
                  logger.debug("This Rule doesnt contains DST-MSISDN");
              }

              if (result) {
                // Called Party Address
                if (ruleMatcher.getCalledPartyAddress() != null) {
                  String regexSourceGlobalTitle = ruleMatcher.getCalledPartyAddress();
                  result = isPatternMatch(regexSourceGlobalTitle, filter.getCalledPartyAddress());
                } else {
                  if (logger.isDebugEnabled())
                    logger.debug("This Rule doesnt contains Called Party Address");
                }

                if (result) {
                  // Calling Party Address
                  if (ruleMatcher.getCallingPartyAddress() != null) {
                    String regexRemoteGlobalTitle = ruleMatcher.getCallingPartyAddress();
                    result = isPatternMatch(regexRemoteGlobalTitle, filter.getCallingPartyAddress());
                  } else {
                    if (logger.isDebugEnabled())
                      logger.debug("This Rule doesnt contains Calling Party Address");
                  }

                  if (result) {
                    if (ruleMatcher.getKeywordMessage() != null) {
                      String regexKeywordMessage = ruleMatcher.getKeywordMessage();
                      result = isPatternMatch(regexKeywordMessage, filter.getKeywordMessage());
                    } else {
                      if (logger.isDebugEnabled())
                        logger.debug("This Rule doesnt contains Keyword Message");
                    }

                    if (!result) {
                      if (logger.isDebugEnabled())
                        logger.debug("Keyword Message doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getKeywordMessage());
                    }

                  } else {
                    if (logger.isDebugEnabled())
                      logger.debug("Calling Party Address doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getCallingPartyAddress());
                  }
                } else {
                  if (logger.isDebugEnabled())
                    logger.debug("Called Party Address doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getCalledPartyAddress());
                }

              } else {
                if (logger.isDebugEnabled())
                  logger.debug("DST-MSISDN doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getSrcMsisdn());
              }
            } else {
              if (logger.isDebugEnabled())
                logger.debug("SRC-MSISDN doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getSrcMsisdn());
            }

          } else {
            if (logger.isDebugEnabled())
              logger.debug("IMSI doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getImsi());
          }

        } else {
          // check for IMSI pattern
          if (ruleMatcher.getImsi() != null) {
            String ruleImsi = ruleMatcher.getImsi();
            result = isStartWith(filter.getImsi(), ruleImsi);
          } else {
            if (logger.isDebugEnabled())
              logger.debug("This Rule doesnt contains IMSI");
          }

          if (result) {
            // SRC-MSISDN
            if (ruleMatcher.getSrcMsisdn() != null) {
              String ruleMsisdn = ruleMatcher.getSrcMsisdn();
              result = isStartWith(ruleMsisdn, filter.getSrcMsisdn());
            } else {
              if (logger.isDebugEnabled())
                logger.debug("This Rule doesnt contains SRC-MSISDN");
            }

            if (result) {
              //DST-MSISDN
              if (ruleMatcher.getDstMsisdn() != null) {
                String ruleMsisdn = ruleMatcher.getDstMsisdn();
                result = isStartWith(ruleMsisdn, filter.getDstMsisdn());
              } else {
                if (logger.isDebugEnabled())
                  logger.debug("This Rule doesnt contains DST-MSISDN");
              }

              if (result) {
                if (ruleMatcher.getCalledPartyAddress() != null) {
                  String ruleSourceGlobalTitle = ruleMatcher.getCalledPartyAddress();
                  result = isStartWith(filter.getCalledPartyAddress(), ruleSourceGlobalTitle);
                } else {
                  if (logger.isDebugEnabled())
                    logger.debug("This Rule doesnt contains Called Party Address");
                }

                if (result) {
                  if (ruleMatcher.getCallingPartyAddress() != null) {
                    String ruleRemoteGlobalTitle = ruleMatcher.getCallingPartyAddress();
                    result = isStartWith(filter.getCallingPartyAddress(), ruleRemoteGlobalTitle);
                  } else {
                    if (logger.isDebugEnabled())
                      logger.debug("This Rule doesnt contains Calling Party Address");
                  }

                  if (result) {
                    if (ruleMatcher.getKeywordMessage() != null) {
                      String keywordMessage = ruleMatcher.getKeywordMessage();
                      result = isStartWith(filter.getKeywordMessage(), keywordMessage);
                    } else {
                      if (logger.isDebugEnabled())
                        logger.debug("This Rule doesnt contains Keyword Message");
                    }

                    if (!result) {
                      if (logger.isDebugEnabled())
                        logger.debug("Keyword Message doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getKeywordMessage());
                    }
                  } else {
                    if (logger.isDebugEnabled())
                      logger.debug("Calling Party Address doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getCallingPartyAddress());
                  }

                } else {
                  if (logger.isDebugEnabled())
                    logger.debug("Called Party Address doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getCalledPartyAddress());

                }
              } else {
                if (logger.isDebugEnabled())
                  logger.debug("DST-MSISDN doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getDstMsisdn());
              }
            } else {
              if (logger.isDebugEnabled())
                logger.debug("SRC-MSISDN doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getSrcMsisdn());
            }
          } else {
            if (logger.isDebugEnabled())
              logger.debug("IMSI doesnt match in rule -> " + currentRule.getName() + " value -> " + filter.getImsi());
          }

        }
      }
    }
    return result;
  }

  private boolean isPatternMatch(String regexStr, String string) {
    if (regexStr == null || regexStr.isEmpty() || string == null) {
      return false;
    }
    return Pattern.matches(regexStr, string);
  }

  private boolean isStartWith(String stringToCompare, String string) {
    if (stringToCompare == null || stringToCompare.isEmpty() || string == null) {
      return false;
    }
    return stringToCompare.startsWith(string);
  }


  @Deprecated
  private boolean searchApplicationRules(ApplicationRulesSetting ruleSetting, String callingGT,
      String calledGT, String imsiStr, String messageType) {
    // ensure there is match rules
    ApplicationMatchRule matchRule = ruleSetting.getMatchRule();
    if (matchRule == null) {
      return false;
    }
    boolean result =
        matchRule.getMessageTypes().stream().anyMatch(p -> p.equalsIgnoreCase(messageType));
    if (!result) {
      // return - no message type found
      return false;
    }
    if (matchRule.getRegexEnabled()) {
      // check for IMSI pattern
      String regexImsi = ruleSetting.getMatchRule().getImsi();
      result = isPatternMatch(regexImsi, imsiStr, result);
      // called GT
      String regexCldGT = ruleSetting.getMatchRule().getCalledGt();
      result = isPatternMatch(regexCldGT, calledGT, result);
      // calling GT
      String regexClgGT = ruleSetting.getMatchRule().getCallingGt();
      result = isPatternMatch(regexClgGT, callingGT, result);
    } else {
      // check for IMSI pattern
      String ruleImsi = ruleSetting.getMatchRule().getImsi();
      result = isStartWith(imsiStr, ruleImsi, result);
      // called GT
      String ruleCldGT = ruleSetting.getMatchRule().getCalledGt();
      result = isStartWith(calledGT, ruleCldGT, result);
      // calling GT
      String ruleClgGT = ruleSetting.getMatchRule().getCallingGt();
      result = isStartWith(callingGT, ruleClgGT, result);
    }
    return result;
  }

  @Deprecated
  private boolean isPatternMatch(String regexStr, String str2, boolean result) {
    if (regexStr == null || regexStr.isEmpty() || str2 == null) {
      return result;
    }
    return result && Pattern.matches(regexStr, str2);
  }

  @Deprecated
  private boolean isStartWith(String paramStr1, String paramStr2, boolean defaultValue) {
    if (paramStr1 == null || paramStr1.isEmpty() || paramStr2 == null) {
      return defaultValue;
    }
    return defaultValue && paramStr1.startsWith(paramStr2);
  }
}
