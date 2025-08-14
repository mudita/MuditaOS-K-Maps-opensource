package net.osmand.util;

import gnu.trove.list.array.TIntArrayList;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class OpeningHoursParser {
   private static final String[] daysStr;
   private static String[] localDaysStr;
   private static final String[] monthsStr;
   private static String[] localMothsStr;
   private static final Map<String, String> additionalStrings = new HashMap<>();
   private static final int LOW_TIME_LIMIT = 120;
   private static final int WITHOUT_TIME_LIMIT = -1;
   private static final int CURRENT_DAY_TIME_LIMIT = -2;
   private static boolean twelveHourFormatting;
   private static DateFormat twelveHourFormatter;
   private static DateFormat twelveHourFormatterAmPm;
   private static String sunrise = "07:00";
   private static String sunset = "21:00";
   private static String endOfDay = "24:00";

   private static void initLocalStrings() {
      initLocalStrings(null);
   }

   public static void initLocalStrings(Locale locale) {
      DateFormatSymbols dateFormatSymbols = locale == null ? DateFormatSymbols.getInstance() : DateFormatSymbols.getInstance(locale);
      localMothsStr = dateFormatSymbols.getShortMonths();
      localDaysStr = getLettersStringArray(dateFormatSymbols.getShortWeekdays(), 3);
   }

   public static void setTwelveHourFormattingEnabled(boolean enabled, Locale locale) {
      twelveHourFormatting = enabled;
      if (enabled) {
         initTwelveHourFormatters(locale);
      }
   }

   private static void initTwelveHourFormatters(Locale locale) {
      twelveHourFormatter = new SimpleDateFormat("h:mm", locale);
      twelveHourFormatterAmPm = DateFormat.getTimeInstance(3, locale);
      TimeZone timeZone = TimeZone.getTimeZone("UTC");
      twelveHourFormatter.setTimeZone(timeZone);
      twelveHourFormatterAmPm.setTimeZone(timeZone);
   }

   public static void setAdditionalString(String key, String value) {
      additionalStrings.put(key, value);
   }

   private static String[] getLettersStringArray(String[] strings, int letters) {
      String[] newStrings = new String[strings.length];

      for(int i = 0; i < strings.length; ++i) {
         if (strings[i] != null) {
            if (strings[i].length() > letters) {
               newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i].substring(0, letters));
            } else {
               newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i]);
            }
         }
      }

      return newStrings;
   }

   private static int getDayIndex(int i) {
      switch(i) {
         case 0:
            return 2;
         case 1:
            return 3;
         case 2:
            return 4;
         case 3:
            return 5;
         case 4:
            return 6;
         case 5:
            return 7;
         case 6:
            return 1;
         default:
            return -1;
      }
   }

   public static void parseRuleV2(String r, int sequenceIndex, List<OpeningHoursParser.OpeningHoursRule> rules) {
      r = r.trim();
      String[] daysStr = new String[]{"mo", "tu", "we", "th", "fr", "sa", "su"};
      String[] monthsStr = new String[]{"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
      String[] holidayStr = new String[]{"ph", "sh", "easter"};
      String sunrise = "07:00";
      String sunset = "21:00";
      String endOfDay = "24:00";
      r = r.replace('(', ' ');
      r = r.replace(')', ' ');
      OpeningHoursParser.BasicOpeningHourRule basic = new OpeningHoursParser.BasicOpeningHourRule(sequenceIndex);
      if (r.startsWith("|| ")) {
         r = r.replace("|| ", "");
         basic.fallback = true;
      }

      String localRuleString = r.replaceAll("(?i)sunset", sunset).replaceAll("(?i)sunrise", sunrise).replaceAll("\\+", "-" + endOfDay);
      boolean[] days = basic.getDays();
      boolean[] months = basic.getMonths();
      if ("24/7".equals(localRuleString)) {
         Arrays.fill(days, true);
         basic.hasDays = true;
         Arrays.fill(months, true);
         basic.addTimeRange(0, 1440);
         rules.add(basic);
      } else {
         List<OpeningHoursParser.Token> tokens = new ArrayList<>();
         int startWord = 0;
         StringBuilder commentStr = new StringBuilder();
         boolean comment = false;

         for(int i = 0; i <= localRuleString.length(); ++i) {
            char ch = i == localRuleString.length() ? 32 : localRuleString.charAt(i);
            boolean delimiter = false;
            OpeningHoursParser.Token del = null;
            if (Character.isWhitespace(ch)) {
               delimiter = true;
            } else if (ch == ':') {
               del = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_COLON, ":");
            } else if (ch == '-') {
               del = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_DASH, "-");
            } else if (ch == ',') {
               del = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_COMMA, ",");
            } else if (ch == '"') {
               if (!comment) {
                  comment = true;
                  continue;
               }

               if (commentStr.length() > 0) {
                  tokens.add(new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_COMMENT, commentStr.toString()));
               }

               startWord = i + 1;
               commentStr.setLength(0);
               comment = false;
            }

            if (comment) {
               commentStr.append(ch);
            } else if (delimiter || del != null) {
               String wrd = localRuleString.substring(startWord, i).trim();
               if (wrd.length() > 0) {
                  tokens.add(new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_UNKNOWN, wrd.toLowerCase()));
               }

               startWord = i + 1;
               if (del != null) {
                  tokens.add(del);
               }
            }
         }

         for(OpeningHoursParser.Token t : tokens) {
            if (t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN) {
               findInArray(t, daysStr, OpeningHoursParser.TokenType.TOKEN_DAY_WEEK);
            }

            if (t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN) {
               findInArray(t, monthsStr, OpeningHoursParser.TokenType.TOKEN_MONTH);
            }

            if (t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN) {
               findInArray(t, holidayStr, OpeningHoursParser.TokenType.TOKEN_HOLIDAY);
            }

            if (t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN && ("off".equals(t.text) || "closed".equals(t.text))) {
               t.type = OpeningHoursParser.TokenType.TOKEN_OFF_ON;
               t.mainNumber = 0;
            }

            if (t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN && ("24/7".equals(t.text) || "open".equals(t.text))) {
               t.type = OpeningHoursParser.TokenType.TOKEN_OFF_ON;
               t.mainNumber = 1;
            }
         }

         for(int i = tokens.size() - 1; i > 0; --i) {
            if (tokens.get(i).type == OpeningHoursParser.TokenType.TOKEN_COLON) {
               if (i < tokens.size() - 1
                  && tokens.get(i - 1).type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN
                  && tokens.get(i - 1).mainNumber != -1
                  && tokens.get(i + 1).type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN
                  && tokens.get(i + 1).mainNumber != -1) {
                  tokens.get(i).mainNumber = 60 * tokens.get(i - 1).mainNumber + tokens.get(i + 1).mainNumber;
                  tokens.get(i).type = OpeningHoursParser.TokenType.TOKEN_HOUR_MINUTES;
                  tokens.remove(i + 1);
                  tokens.remove(i - 1);
               }
            } else if (tokens.get(i).type == OpeningHoursParser.TokenType.TOKEN_OFF_ON && tokens.get(i - 1).type == OpeningHoursParser.TokenType.TOKEN_OFF_ON) {
               tokens.remove(i - 1);
            }
         }

         boolean monthSpecified = false;

         for(OpeningHoursParser.Token t : tokens) {
            if (t.type == OpeningHoursParser.TokenType.TOKEN_MONTH) {
               monthSpecified = true;
               break;
            }
         }

         for(int i = 0; i < tokens.size(); ++i) {
            OpeningHoursParser.Token t = tokens.get(i);
            if (t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN && t.mainNumber >= 0) {
               if (monthSpecified && t.mainNumber <= 31) {
                  t.type = OpeningHoursParser.TokenType.TOKEN_DAY_MONTH;
                  --t.mainNumber;
               } else if (t.mainNumber > 1000) {
                  t.type = OpeningHoursParser.TokenType.TOKEN_YEAR;
               }
            }
         }

         buildRule(basic, tokens, rules);
      }
   }

   private static void buildRule(
      OpeningHoursParser.BasicOpeningHourRule basic, List<OpeningHoursParser.Token> tokens, List<OpeningHoursParser.OpeningHoursRule> rules
   ) {
      OpeningHoursParser.TokenType currentParse = OpeningHoursParser.TokenType.TOKEN_UNKNOWN;
      OpeningHoursParser.TokenType currentParseParent = OpeningHoursParser.TokenType.TOKEN_UNKNOWN;
      List<OpeningHoursParser.Token[]> listOfPairs = new ArrayList<>();
      Set<OpeningHoursParser.TokenType> presentTokens = EnumSet.noneOf(OpeningHoursParser.TokenType.class);
      OpeningHoursParser.Token[] currentPair = new OpeningHoursParser.Token[2];
      listOfPairs.add(currentPair);
      OpeningHoursParser.Token prevToken = null;
      OpeningHoursParser.Token prevYearToken = null;
      int indexP = 0;

      for(int i = 0; i <= tokens.size(); ++i) {
         OpeningHoursParser.Token t = i == tokens.size() ? null : tokens.get(i);
         if (i == 0 && t != null && t.type == OpeningHoursParser.TokenType.TOKEN_UNKNOWN) {
            return;
         }

         if (t == null || t.type.ord() > currentParse.ord()) {
            presentTokens.add(currentParse);
            if (currentParse != OpeningHoursParser.TokenType.TOKEN_MONTH
               && currentParse != OpeningHoursParser.TokenType.TOKEN_DAY_MONTH
               && currentParse != OpeningHoursParser.TokenType.TOKEN_DAY_WEEK
               && currentParse != OpeningHoursParser.TokenType.TOKEN_HOLIDAY) {
               if (currentParse == OpeningHoursParser.TokenType.TOKEN_HOUR_MINUTES) {
                  for(OpeningHoursParser.Token[] pair : listOfPairs) {
                     if (pair[0] != null && pair[1] != null) {
                        basic.addTimeRange(pair[0].mainNumber, pair[1].mainNumber);
                     }
                  }
               } else if (currentParse == OpeningHoursParser.TokenType.TOKEN_OFF_ON) {
                  OpeningHoursParser.Token[] l = (OpeningHoursParser.Token[])listOfPairs.get(0);
                  if (l[0] != null && l[0].mainNumber == 0) {
                     basic.off = true;
                  }
               } else if (currentParse == OpeningHoursParser.TokenType.TOKEN_COMMENT) {
                  OpeningHoursParser.Token[] l = (OpeningHoursParser.Token[])listOfPairs.get(0);
                  if (l[0] != null && !Algorithms.isEmpty(l[0].text)) {
                     basic.comment = l[0].text;
                  }
               } else if (currentParse == OpeningHoursParser.TokenType.TOKEN_YEAR) {
                  OpeningHoursParser.Token[] l = (OpeningHoursParser.Token[])listOfPairs.get(0);
                  if (l[0] != null && l[0].mainNumber > 1000) {
                     prevYearToken = l[0];
                  }
               }
            } else {
               boolean tokenDayMonth = currentParse == OpeningHoursParser.TokenType.TOKEN_DAY_MONTH;
               boolean[] array = currentParse == OpeningHoursParser.TokenType.TOKEN_MONTH ? basic.getMonths() : (tokenDayMonth ? null : basic.getDays());

               for(OpeningHoursParser.Token[] pair : listOfPairs) {
                  if (pair[0] != null && pair[1] != null) {
                     OpeningHoursParser.Token firstMonthToken = pair[0].parent;
                     OpeningHoursParser.Token lastMonthToken = pair[1].parent;
                     if (tokenDayMonth && firstMonthToken != null) {
                        if (lastMonthToken != null && lastMonthToken.mainNumber != firstMonthToken.mainNumber) {
                           OpeningHoursParser.Token[] p = new OpeningHoursParser.Token[]{firstMonthToken, lastMonthToken};
                           fillRuleArray(basic.getMonths(), p);
                           OpeningHoursParser.Token t1 = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_DAY_MONTH, pair[0].mainNumber);
                           OpeningHoursParser.Token t2 = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_DAY_MONTH, 30);
                           p = new OpeningHoursParser.Token[]{t1, t2};
                           boolean[] var33 = basic.getDayMonths(firstMonthToken.mainNumber);
                           fillRuleArray(var33, p);
                           t1 = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_DAY_MONTH, 0);
                           t2 = new OpeningHoursParser.Token(OpeningHoursParser.TokenType.TOKEN_DAY_MONTH, pair[1].mainNumber);
                           p = new OpeningHoursParser.Token[]{t1, t2};
                           array = basic.getDayMonths(lastMonthToken.mainNumber);
                           fillRuleArray(array, p);
                           if (firstMonthToken.mainNumber <= lastMonthToken.mainNumber) {
                              for(int month = firstMonthToken.mainNumber + 1; month < lastMonthToken.mainNumber; ++month) {
                                 Arrays.fill(basic.getDayMonths(month), true);
                              }
                           } else {
                              for(int month = firstMonthToken.mainNumber + 1; month < 12; ++month) {
                                 Arrays.fill(basic.getDayMonths(month), true);
                              }

                              for(int month = 0; month < lastMonthToken.mainNumber; ++month) {
                                 Arrays.fill(basic.getDayMonths(month), true);
                              }
                           }
                        } else {
                           array = basic.getDayMonths(firstMonthToken.mainNumber);
                           fillRuleArray(array, pair);
                        }
                     } else if (array != null) {
                        fillRuleArray(array, pair);
                     }

                     int ruleYear = basic.year;
                     if ((ruleYear > 0 || prevYearToken != null) && firstMonthToken != null && lastMonthToken != null) {
                        int length = lastMonthToken.mainNumber > firstMonthToken.mainNumber
                           ? lastMonthToken.mainNumber - firstMonthToken.mainNumber
                           : 12 - firstMonthToken.mainNumber + lastMonthToken.mainNumber;
                        int month = firstMonthToken.mainNumber;
                        int endYear = prevYearToken != null ? prevYearToken.mainNumber : ruleYear;
                        int startYear = ruleYear > 0 ? ruleYear : endYear;
                        int year = startYear;
                        if (basic.firstYearMonths == null) {
                           basic.firstYearMonths = new int[12];
                        }

                        int[] yearMonths = basic.firstYearMonths;

                        for(int k = 0; k <= length; ++k) {
                           yearMonths[month++] = year;
                           if (month > 11) {
                              month = 0;
                              year = endYear;
                              if (basic.lastYearMonths == null) {
                                 basic.lastYearMonths = new int[12];
                              }

                              yearMonths = basic.lastYearMonths;
                           }
                        }

                        if (endYear - startYear > 1) {
                           basic.fullYears = endYear - startYear - 1;
                        }

                        if (endYear > startYear && firstMonthToken.mainNumber >= lastMonthToken.mainNumber) {
                           Arrays.fill(basic.months, true);
                        }
                     }
                  } else if (pair[0] != null) {
                     if (pair[0].type == OpeningHoursParser.TokenType.TOKEN_HOLIDAY) {
                        if (pair[0].mainNumber == 0) {
                           basic.publicHoliday = true;
                        } else if (pair[0].mainNumber == 1) {
                           basic.schoolHoliday = true;
                        } else if (pair[0].mainNumber == 2) {
                           basic.easter = true;
                        }
                     } else if (pair[0].mainNumber >= 0) {
                        OpeningHoursParser.Token firstMonthToken = pair[0].parent;
                        if (tokenDayMonth && firstMonthToken != null) {
                           array = basic.getDayMonths(firstMonthToken.mainNumber);
                        }

                        if (array != null) {
                           array[pair[0].mainNumber] = true;
                           if (prevYearToken != null) {
                              basic.year = prevYearToken.mainNumber;
                           }
                        }
                     }
                  }
               }
            }

            listOfPairs.clear();
            currentPair = new OpeningHoursParser.Token[2];
            indexP = 0;
            listOfPairs.add(currentPair);
            currentPair[indexP++] = t;
            if (t != null) {
               currentParse = t.type;
               currentParseParent = currentParse;
               if (t.type == OpeningHoursParser.TokenType.TOKEN_DAY_MONTH && prevToken != null && prevToken.type == OpeningHoursParser.TokenType.TOKEN_MONTH) {
                  t.parent = prevToken;
                  currentParseParent = prevToken.type;
               }
            }
         } else if (t.type.ord() < currentParseParent.ord() && indexP == 0 && tokens.size() > i) {
            OpeningHoursParser.BasicOpeningHourRule newRule = new OpeningHoursParser.BasicOpeningHourRule(basic.getSequenceIndex());
            newRule.setComment(basic.getComment());
            buildRule(newRule, tokens.subList(i, tokens.size()), rules);
            tokens = tokens.subList(0, i + 1);
         } else if (t.type == OpeningHoursParser.TokenType.TOKEN_COMMA) {
            if (tokens.size() > i + 1 && tokens.get(i + 1) != null && tokens.get(i + 1).type.ord() < currentParseParent.ord()) {
               indexP = 0;
            } else {
               currentPair = new OpeningHoursParser.Token[2];
               indexP = 0;
               listOfPairs.add(currentPair);
            }
         } else if (t.type != OpeningHoursParser.TokenType.TOKEN_DASH) {
            if (t.type == OpeningHoursParser.TokenType.TOKEN_YEAR) {
               prevYearToken = t;
            } else if (t.type.ord() == currentParse.ord() && indexP < 2) {
               currentPair[indexP++] = t;
               if (t.type == OpeningHoursParser.TokenType.TOKEN_DAY_MONTH && prevToken != null && prevToken.type == OpeningHoursParser.TokenType.TOKEN_MONTH) {
                  t.parent = prevToken;
               }
            }
         }

         prevToken = t;
      }

      if (!presentTokens.contains(OpeningHoursParser.TokenType.TOKEN_MONTH)) {
         Arrays.fill(basic.getMonths(), true);
      }

      if (!presentTokens.contains(OpeningHoursParser.TokenType.TOKEN_DAY_WEEK)
         && !presentTokens.contains(OpeningHoursParser.TokenType.TOKEN_HOLIDAY)
         && !presentTokens.contains(OpeningHoursParser.TokenType.TOKEN_DAY_MONTH)) {
         Arrays.fill(basic.getDays(), true);
         basic.hasDays = true;
      } else if (presentTokens.contains(OpeningHoursParser.TokenType.TOKEN_DAY_WEEK) || presentTokens.contains(OpeningHoursParser.TokenType.TOKEN_HOLIDAY)) {
         basic.hasDays = true;
      }

      rules.add(0, basic);
   }

   private static void fillRuleArray(boolean[] array, OpeningHoursParser.Token[] pair) {
      if (pair[0].mainNumber <= pair[1].mainNumber) {
         for(int j = pair[0].mainNumber; j <= pair[1].mainNumber && j >= 0 && j < array.length; ++j) {
            array[j] = true;
         }
      } else {
         for(int j = pair[0].mainNumber; j >= 0 && j < array.length; ++j) {
            array[j] = true;
         }

         for(int j = 0; j <= pair[1].mainNumber && j < array.length; ++j) {
            array[j] = true;
         }
      }
   }

   private static void findInArray(OpeningHoursParser.Token t, String[] list, OpeningHoursParser.TokenType tokenType) {
      for(int i = 0; i < list.length; ++i) {
         if (list[i].equals(t.text)) {
            t.type = tokenType;
            t.mainNumber = i;
            break;
         }
      }
   }

   private static List<List<String>> splitSequences(String format) {
      if (format == null) {
         return null;
      } else {
         List<List<String>> res = new ArrayList<>();
         String[] sequences = format.split("(?= \\|\\| )");

         for(String seq : sequences) {
            seq = seq.trim();
            if (seq.length() != 0) {
               List<String> rules = new ArrayList<>();
               boolean comment = false;
               StringBuilder sb = new StringBuilder();

               for(int i = 0; i < seq.length(); ++i) {
                  char c = seq.charAt(i);
                  if (c == '"') {
                     comment = !comment;
                     sb.append(c);
                  } else if (c != ';' || comment) {
                     sb.append(c);
                  } else if (sb.length() > 0) {
                     String s = sb.toString().trim();
                     if (s.length() > 0) {
                        rules.add(s);
                     }

                     sb.setLength(0);
                  }
               }

               if (sb.length() > 0) {
                  rules.add(sb.toString());
                  sb.setLength(0);
               }

               res.add(rules);
            }
         }

         return res;
      }
   }

   public static void parseRules(String r, int sequenceIndex, List<OpeningHoursParser.OpeningHoursRule> rules) {
      parseRuleV2(r, sequenceIndex, rules);
   }

   public static OpeningHoursParser.OpeningHours parseOpenedHours(String format) {
      if (format == null) {
         return null;
      } else {
         OpeningHoursParser.OpeningHours rs = new OpeningHoursParser.OpeningHours();
         rs.setOriginal(format);
         List<List<String>> sequences = splitSequences(format);

         for(int i = 0; i < sequences.size(); ++i) {
            List<String> rules = sequences.get(i);
            List<OpeningHoursParser.BasicOpeningHourRule> basicRules = new ArrayList<>();

            for(String r : rules) {
               List<OpeningHoursParser.OpeningHoursRule> rList = new ArrayList<>();
               parseRules(r, i, rList);

               for(OpeningHoursParser.OpeningHoursRule rule : rList) {
                  if (rule instanceof OpeningHoursParser.BasicOpeningHourRule) {
                     basicRules.add((OpeningHoursParser.BasicOpeningHourRule)rule);
                  }
               }
            }

            String basicRuleComment = null;
            if (sequences.size() > 1) {
               for(OpeningHoursParser.BasicOpeningHourRule bRule : basicRules) {
                  if (!Algorithms.isEmpty(bRule.getComment())) {
                     basicRuleComment = bRule.getComment();
                     break;
                  }
               }
            }

            if (!Algorithms.isEmpty(basicRuleComment)) {
               for(OpeningHoursParser.BasicOpeningHourRule bRule : basicRules) {
                  bRule.setComment(basicRuleComment);
               }
            }

            rs.addRules(basicRules);
         }

         rs.setSequenceCount(sequences.size());
         return rs.rules.size() > 0 ? rs : null;
      }
   }

   public static OpeningHoursParser.OpeningHours parseOpenedHoursHandleErrors(String format) {
      if (format == null) {
         return null;
      } else {
         OpeningHoursParser.OpeningHours rs = new OpeningHoursParser.OpeningHours();
         rs.setOriginal(format);
         List<List<String>> sequences = splitSequences(format);

         for(int i = sequences.size() - 1; i >= 0; --i) {
            for(String r : sequences.get(i)) {
               r = r.trim();
               if (r.length() != 0) {
                  List<OpeningHoursParser.OpeningHoursRule> rList = new ArrayList<>();
                  parseRules(r, i, rList);
                  rs.addRules(rList);
               }
            }
         }

         rs.setSequenceCount(sequences.size());
         return rs;
      }
   }

   public static List<OpeningHoursParser.OpeningHours.Info> getInfo(String format) {
      OpeningHoursParser.OpeningHours openingHours = parseOpenedHours(format);
      return openingHours == null ? null : openingHours.getInfo();
   }

   private static void formatTimeRange(int startMinute, int endMinute, StringBuilder stringBuilder) {
      int startHour = startMinute / 60 % 24;
      int endHour = endMinute / 60 % 24;
      boolean sameDayPart = Math.max(startHour, endHour) < 12 || Math.min(startHour, endHour) >= 12;
      if (twelveHourFormatting && sameDayPart) {
         boolean amPmOnLeft = isAmPmOnLeft(startMinute);
         formatTime(startMinute, stringBuilder, amPmOnLeft);
         stringBuilder.append("-");
         formatTime(endMinute, stringBuilder, !amPmOnLeft);
      } else {
         formatTime(startMinute, stringBuilder);
         stringBuilder.append("-");
         formatTime(endMinute, stringBuilder);
      }
   }

   private static boolean isAmPmOnLeft(int startMinute) {
      StringBuilder sb = new StringBuilder();
      formatTime(startMinute, sb);
      return !Character.isDigit(sb.charAt(0));
   }

   private static void formatTime(int minutes, StringBuilder sb) {
      formatTime(minutes, sb, true);
   }

   private static void formatTime(int minutes, StringBuilder sb, boolean appendAmPM) {
      int hour = minutes / 60;
      int time = minutes - hour * 60;
      formatTime(hour, time, sb, appendAmPM);
   }

   private static void formatTime(int hours, int minutes, StringBuilder b, boolean appendAmPm) {
      if (twelveHourFormatting) {
         long millis = ((long)hours * 60L + (long)minutes) * 60L * 1000L;
         Date date = new Date(millis);
         String time = appendAmPm ? twelveHourFormatterAmPm.format(date) : twelveHourFormatter.format(date);
         b.append(time);
      } else {
         if (hours < 10) {
            b.append("0");
         }

         b.append(hours).append(":");
         if (minutes < 10) {
            b.append("0");
         }

         b.append(minutes);
      }
   }

   static {
      DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(Locale.US);
      monthsStr = dateFormatSymbols.getShortMonths();
      daysStr = getLettersStringArray(dateFormatSymbols.getShortWeekdays(), 2);
      initLocalStrings();
      additionalStrings.put("off", "off");
      additionalStrings.put("is_open", "Open");
      additionalStrings.put("is_open_24_7", "Open 24/7");
      additionalStrings.put("will_open_at", "Will open at");
      additionalStrings.put("open_from", "Open from");
      additionalStrings.put("will_close_at", "Will close at");
      additionalStrings.put("open_till", "Open till");
      additionalStrings.put("will_open_tomorrow_at", "Will open tomorrow at");
      additionalStrings.put("will_open_on", "Will open on");
   }

   public static class BasicOpeningHourRule implements OpeningHoursParser.OpeningHoursRule {
      private boolean[] days = new boolean[7];
      private boolean hasDays = false;
      private boolean[] months = new boolean[12];
      private int[] firstYearMonths = null;
      private int[] lastYearMonths = null;
      private int fullYears = 0;
      private int year = 0;
      private boolean fallback;
      private boolean[][] dayMonths = null;
      private TIntArrayList startTimes = new TIntArrayList();
      private TIntArrayList endTimes = new TIntArrayList();
      private boolean publicHoliday = false;
      private boolean schoolHoliday = false;
      private boolean easter = false;
      private boolean off = false;
      private String comment;
      private int sequenceIndex;

      public BasicOpeningHourRule() {
         this.sequenceIndex = 0;
      }

      public BasicOpeningHourRule(int sequenceIndex) {
         this.sequenceIndex = sequenceIndex;
      }

      @Override
      public int getSequenceIndex() {
         return this.sequenceIndex;
      }

      @Override
      public boolean isFallbackRule() {
         return this.fallback;
      }

      public boolean[] getDays() {
         return this.days;
      }

      public boolean[] getDayMonths(int month) {
         if (this.dayMonths == null) {
            this.dayMonths = new boolean[12][31];
         }

         return this.dayMonths[month];
      }

      public boolean hasDayMonths() {
         return this.dayMonths != null;
      }

      public boolean[] getMonths() {
         return this.months;
      }

      public boolean appliesToPublicHolidays() {
         return this.publicHoliday;
      }

      public boolean appliesEaster() {
         return this.easter;
      }

      public boolean appliesToSchoolHolidays() {
         return this.schoolHoliday;
      }

      public String getComment() {
         return this.comment;
      }

      public void setComment(String comment) {
         this.comment = comment;
      }

      public void setStartTime(int s) {
         setSingleValueForArrayList(this.startTimes, s);
         if (this.endTimes.size() != 1) {
            setSingleValueForArrayList(this.endTimes, 0);
         }
      }

      public void setEndTime(int e) {
         setSingleValueForArrayList(this.endTimes, e);
         if (this.startTimes.size() != 1) {
            setSingleValueForArrayList(this.startTimes, 0);
         }
      }

      public void setStartTime(int s, int position) {
         if (position == this.startTimes.size()) {
            this.startTimes.add(s);
            this.endTimes.add(0);
         } else {
            this.startTimes.set(position, s);
         }
      }

      public void setEndTime(int s, int position) {
         if (position == this.startTimes.size()) {
            this.endTimes.add(s);
            this.startTimes.add(0);
         } else {
            this.endTimes.set(position, s);
         }
      }

      public int getStartTime() {
         return this.startTimes.size() == 0 ? 0 : this.startTimes.get(0);
      }

      public int getStartTime(int position) {
         return this.startTimes.get(position);
      }

      public int getEndTime() {
         return this.endTimes.size() == 0 ? 0 : this.endTimes.get(0);
      }

      public int getEndTime(int position) {
         return this.endTimes.get(position);
      }

      public TIntArrayList getStartTimes() {
         return new TIntArrayList(this.startTimes);
      }

      public TIntArrayList getEndTimes() {
         return new TIntArrayList(this.endTimes);
      }

      public void setDays(boolean[] days) {
         if (this.days.length == days.length) {
            this.days = days;
         }
      }

      @Override
      public boolean containsDay(Calendar cal) {
         int i = cal.get(7);
         int d = (i + 5) % 7;
         return this.days[d];
      }

      @Override
      public boolean containsNextDay(Calendar cal) {
         int i = cal.get(7);
         int p = (i + 6) % 7;
         return this.days[p];
      }

      @Override
      public boolean containsPreviousDay(Calendar cal) {
         int i = cal.get(7);
         int p = (i + 4) % 7;
         return this.days[p];
      }

      @Override
      public boolean containsMonth(Calendar cal) {
         int month = cal.get(2);
         int year = cal.get(1);
         return this.containsYear(cal) && this.months[month];
      }

      @Override
      public boolean containsYear(Calendar cal) {
         if (this.year == 0 && this.firstYearMonths == null) {
            return true;
         } else {
            int month = cal.get(2);
            int year = cal.get(1);
            if ((this.firstYearMonths == null || this.firstYearMonths[month] != year)
               && (this.lastYearMonths == null || this.lastYearMonths[month] != year)
               && (this.firstYearMonths != null || this.lastYearMonths != null || this.year != year)) {
               if (this.fullYears > 0 && this.year > 0) {
                  for(int i = 1; i <= this.fullYears; ++i) {
                     if (this.year + i == year) {
                        return true;
                     }
                  }
               }

               return false;
            } else {
               return true;
            }
         }
      }

      @Override
      public boolean isOpenedForTime(Calendar cal, boolean checkPrevious) {
         int d = this.getCurrentDay(cal);
         int p = this.getPreviousDay(d);
         int time = this.getCurrentTimeInMinutes(cal);

         for(int i = 0; i < this.startTimes.size(); ++i) {
            int startTime = this.startTimes.get(i);
            int endTime = this.endTimes.get(i);
            if (startTime >= endTime && endTime != -1) {
               if (time >= startTime && this.days[d] && !checkPrevious) {
                  return !this.off;
               }

               if (time < endTime && this.days[p] && checkPrevious) {
                  return !this.off;
               }
            } else if (this.days[d] && !checkPrevious && time >= startTime && (endTime == -1 || time <= endTime)) {
               return !this.off;
            }
         }

         return false;
      }

      private int getCurrentDay(Calendar cal) {
         int i = cal.get(7);
         return (i + 5) % 7;
      }

      private int getPreviousDay(int currentDay) {
         int p = currentDay - 1;
         if (p < 0) {
            p += 7;
         }

         return p;
      }

      private int getNextDay(int currentDay) {
         int n = currentDay + 1;
         if (n > 6) {
            n -= 7;
         }

         return n;
      }

      private int getCurrentTimeInMinutes(Calendar cal) {
         return cal.get(11) * 60 + cal.get(12);
      }

      @Override
      public String toRuleString() {
         return this.toRuleString(false);
      }

      private String toRuleString(boolean useLocalization) {
         String[] dayNames = useLocalization ? OpeningHoursParser.localDaysStr : OpeningHoursParser.daysStr;
         String[] monthNames = useLocalization ? OpeningHoursParser.localMothsStr : OpeningHoursParser.monthsStr;
         String offStr = useLocalization ? OpeningHoursParser.additionalStrings.get("off") : "off";
         StringBuilder b = new StringBuilder(25);
         boolean allMonths = true;

         for(int i = 0; i < this.months.length; ++i) {
            if (!this.months[i]) {
               allMonths = false;
               break;
            }
         }

         boolean allDays = !this.hasDayMonths();
         if (!allDays) {
            boolean dash = false;
            boolean first = true;
            int monthAdded = -1;
            int dayAdded = -1;
            int excludedMonthEnd = -1;
            int excludedDayEnd = -1;
            int excludedMonthStart = -1;
            int excludedDayStart = -1;
            if (this.dayMonths[0][0] && this.dayMonths[11][30]) {
               int prevMonth = 0;
               int prevDay = 0;

               for(int month = 0; month < this.dayMonths.length; ++month) {
                  for(int day = 0; day < this.dayMonths[month].length; prevDay = day++) {
                     if (day == 1) {
                        prevMonth = month;
                     }

                     if (!this.dayMonths[month][day]) {
                        excludedMonthEnd = prevMonth;
                        excludedDayEnd = prevDay;
                        break;
                     }
                  }

                  if (excludedDayEnd != -1) {
                     break;
                  }
               }

               prevMonth = this.dayMonths.length - 1;
               prevDay = this.dayMonths[prevMonth].length - 1;

               for(int month = this.dayMonths.length - 1; month >= 0; --month) {
                  for(int day = this.dayMonths[month].length - 1; day >= 0; prevDay = day--) {
                     if (day == this.dayMonths[month].length - 2) {
                        prevMonth = month;
                     }

                     if (!this.dayMonths[month][day]) {
                        excludedMonthStart = prevMonth;
                        excludedDayStart = prevDay;
                        break;
                     }
                  }

                  if (excludedDayStart != -1) {
                     break;
                  }
               }
            }

            boolean yearAdded = false;

            for(int month = 0; month < this.dayMonths.length; ++month) {
               for(int day = 0; day < this.dayMonths[month].length; ++day) {
                  if ((
                        excludedDayStart == -1
                           || excludedDayEnd == -1
                           || month >= excludedMonthEnd
                              && (month != excludedMonthEnd || day > excludedDayEnd)
                              && month <= excludedMonthStart
                              && (month != excludedMonthStart || day < excludedDayStart)
                     )
                     && this.dayMonths[month][day]
                     && (day != 0 || !dash || !this.dayMonths[month][1])) {
                     if (day <= 0
                        || !this.dayMonths[month][day - 1]
                        || (day >= this.dayMonths[month].length - 1 || !this.dayMonths[month][day + 1])
                           && (day != this.dayMonths[month].length - 1 || month >= this.dayMonths.length - 1 || !this.dayMonths[month + 1][0])) {
                        if (first) {
                           first = false;
                        } else if (!dash) {
                           b.append(", ");
                           monthAdded = -1;
                        }

                        yearAdded = this.appendYearString(b, dash ? this.lastYearMonths : this.firstYearMonths, month);
                        if (monthAdded != month || yearAdded) {
                           b.append(monthNames[month]).append(" ");
                           monthAdded = month;
                        }

                        dayAdded = day + 1;
                        b.append(dayAdded);
                        dash = false;
                     } else if (!dash) {
                        dash = true;
                        if (!first) {
                           b.append("-");
                        }
                     }
                  }
               }
            }

            if (excludedDayStart != -1 && excludedDayEnd != -1) {
               if (first) {
                  first = false;
               } else if (!dash) {
                  b.append(", ");
               }

               this.appendYearString(b, this.firstYearMonths, excludedMonthStart);
               b.append(monthNames[excludedMonthStart]).append(" ").append(excludedDayStart + 1).append("-");
               this.appendYearString(b, this.lastYearMonths, excludedMonthEnd);
               b.append(monthNames[excludedMonthEnd]).append(" ").append(excludedDayEnd + 1);
            } else if (yearAdded && !dash && monthAdded != -1 && this.lastYearMonths != null) {
               b.append("-");
               this.appendYearString(b, this.lastYearMonths, monthAdded);
               b.append(monthNames[monthAdded]);
               if (dayAdded != -1) {
                  b.append(" ").append(dayAdded);
               }
            }

            if (!first) {
               b.append(" ");
            }
         } else if (!allMonths) {
            this.addArray(this.months, monthNames, b);
         }

         this.appendDaysString(b, dayNames);
         if (this.startTimes != null && this.startTimes.size() != 0) {
            if (this.isOpened24_7()) {
               b.setLength(0);
               b.append("24/7");
            } else {
               for(int i = 0; i < this.startTimes.size(); ++i) {
                  int startTime = this.startTimes.get(i);
                  int endTime = this.endTimes.get(i);
                  if (i > 0) {
                     b.append(", ");
                  }

                  OpeningHoursParser.formatTimeRange(startTime, endTime, b);
               }

               if (this.off) {
                  b.append(" ").append(offStr);
               }
            }
         } else {
            if (this.isOpened24_7()) {
               b.setLength(0);
               if (!this.isFallbackRule()) {
                  b.append("24/7 ");
               }
            }

            if (this.off) {
               b.append(offStr);
            }
         }

         if (!Algorithms.isEmpty(this.comment)) {
            if (b.length() > 0) {
               if (b.charAt(b.length() - 1) != ' ') {
                  b.append(" ");
               }

               b.append("- ").append(this.comment);
            } else {
               b.append(this.comment);
            }
         }

         return b.toString();
      }

      private boolean appendYearString(StringBuilder b, int[] yearMonths, int month) {
         if (yearMonths != null && yearMonths[month] > 0) {
            b.append(yearMonths[month]).append(" ");
            return true;
         } else if (this.year > 0) {
            b.append(this.year).append(" ");
            return true;
         } else {
            return false;
         }
      }

      private void addArray(boolean[] array, String[] arrayNames, StringBuilder b) {
         boolean dash = false;
         boolean first = true;

         for(int i = 0; i < array.length; ++i) {
            if (array[i]) {
               if (i > 0 && array[i - 1] && i < array.length - 1 && array[i + 1]) {
                  if (!dash) {
                     dash = true;
                     b.append("-");
                  }
               } else {
                  if (first) {
                     first = false;
                  } else if (!dash) {
                     b.append(", ");
                  }

                  b.append(arrayNames == null ? i + 1 : arrayNames[i]);
                  dash = false;
               }
            }
         }

         if (!first) {
            b.append(" ");
         }
      }

      @Override
      public String toLocalRuleString() {
         return this.toRuleString(true);
      }

      @Override
      public boolean isOpened24_7() {
         boolean opened24_7 = this.isOpenedEveryDay();
         if (opened24_7) {
            if (this.startTimes == null || this.startTimes.size() <= 0) {
               return true;
            }

            for(int i = 0; i < this.startTimes.size(); ++i) {
               int startTime = this.startTimes.get(i);
               int endTime = this.endTimes.get(i);
               if (startTime == 0 && endTime / 60 == 24) {
                  return true;
               }
            }
         }

         return false;
      }

      public boolean isOpenedEveryDay() {
         boolean openedEveryDay = true;

         for(int i = 0; i < 7; ++i) {
            if (!this.days[i]) {
               openedEveryDay = false;
               break;
            }
         }

         return openedEveryDay;
      }

      @Override
      public String getTime(Calendar cal, boolean checkAnotherDay, int limit, boolean opening) {
         StringBuilder sb = new StringBuilder();
         int d = this.getCurrentDay(cal);
         int ad = opening ? this.getNextDay(d) : this.getPreviousDay(d);
         int time = this.getCurrentTimeInMinutes(cal);

         for(int i = 0; i < this.startTimes.size(); ++i) {
            int startTime = this.startTimes.get(i);
            int endTime = this.endTimes.get(i);
            if (opening != this.off) {
               if (startTime >= endTime && endTime != -1) {
                  int diff = -1;
                  if (time <= startTime && this.days[d] && !checkAnotherDay) {
                     diff = startTime - time;
                  } else if (time > endTime && this.days[ad] && checkAnotherDay) {
                     diff = 1440 - endTime + time;
                  }

                  if (limit == -1 || diff != -1 && diff <= limit || limit == -2) {
                     OpeningHoursParser.formatTime(startTime, sb);
                     break;
                  }
               } else if (this.days[d] && !checkAnotherDay) {
                  int diff = startTime - time;
                  if (limit == -1 || time <= startTime && (diff <= limit || limit == -2)) {
                     OpeningHoursParser.formatTime(startTime, sb);
                     break;
                  }
               }
            } else if (startTime >= endTime || endTime == -1) {
               int diff = -1;
               if (time <= endTime && this.days[d] && !checkAnotherDay) {
                  diff = 1440 - time + endTime;
               } else if (time < endTime && this.days[ad] && checkAnotherDay) {
                  diff = endTime - time;
               }

               if (limit == -1 || diff != -1 && diff <= limit) {
                  OpeningHoursParser.formatTime(endTime, sb);
                  break;
               }
            } else if (this.days[d] && !checkAnotherDay) {
               int diff = endTime - time;
               if (limit == -1 && diff >= 0 || time <= endTime && diff <= limit) {
                  OpeningHoursParser.formatTime(endTime, sb);
                  break;
               }
            }
         }

         String res = sb.toString();
         if (res.length() > 0 && !Algorithms.isEmpty(this.comment)) {
            res = res + " - " + this.comment;
         }

         return res;
      }

      @Override
      public String toString() {
         return this.toRuleString();
      }

      public void appendDaysString(StringBuilder builder) {
         this.appendDaysString(builder, OpeningHoursParser.daysStr);
      }

      public void appendDaysString(StringBuilder builder, String[] daysNames) {
         boolean dash = false;
         boolean first = true;

         for(int i = 0; i < 7; ++i) {
            if (this.days[i]) {
               if (i > 0 && this.days[i - 1] && i < 6 && this.days[i + 1]) {
                  if (!dash) {
                     dash = true;
                     builder.append("-");
                  }
               } else {
                  if (first) {
                     first = false;
                  } else if (!dash) {
                     builder.append(", ");
                  }

                  builder.append(daysNames[OpeningHoursParser.getDayIndex(i)]);
                  dash = false;
               }
            }
         }

         if (this.publicHoliday) {
            if (!first) {
               builder.append(", ");
            }

            builder.append("PH");
            first = false;
         }

         if (this.schoolHoliday) {
            if (!first) {
               builder.append(", ");
            }

            builder.append("SH");
            first = false;
         }

         if (this.easter) {
            if (!first) {
               builder.append(", ");
            }

            builder.append("Easter");
            first = false;
         }

         if (!first) {
            builder.append(" ");
         }
      }

      public void addTimeRange(int startTime, int endTime) {
         this.startTimes.add(startTime);
         this.endTimes.add(endTime);
      }

      public int timesSize() {
         return this.startTimes.size();
      }

      public void deleteTimeRange(int position) {
         this.startTimes.removeAt(position);
         this.endTimes.removeAt(position);
      }

      private static void setSingleValueForArrayList(TIntArrayList arrayList, int s) {
         if (arrayList.size() > 0) {
            arrayList.remove(0, arrayList.size());
         }

         arrayList.add(s);
      }

      @Override
      public boolean isOpenedForTime(Calendar cal) {
         int c = this.calculate(cal);
         return c > 0;
      }

      @Override
      public boolean contains(Calendar cal) {
         int c = this.calculate(cal);
         return c != 0;
      }

      @Override
      public boolean hasOverlapTimes() {
         for(int i = 0; i < this.startTimes.size(); ++i) {
            int startTime = this.startTimes.get(i);
            int endTime = this.endTimes.get(i);
            if (startTime >= endTime && endTime != -1) {
               return true;
            }
         }

         return false;
      }

      @Override
      public boolean hasOverlapTimes(Calendar cal, OpeningHoursParser.OpeningHoursRule r, boolean strictOverlap) {
         if (this.off) {
            return true;
         } else {
            if (r != null && r.contains(cal) && r instanceof OpeningHoursParser.BasicOpeningHourRule) {
               OpeningHoursParser.BasicOpeningHourRule rule = (OpeningHoursParser.BasicOpeningHourRule)r;
               if (this.startTimes.size() > 0 && rule.startTimes.size() > 0) {
                  for(int i = 0; i < this.startTimes.size(); ++i) {
                     int startTime = this.startTimes.get(i);
                     int endTime = this.endTimes.get(i);
                     if (endTime == -1) {
                        endTime = 1440;
                     } else if (startTime >= endTime) {
                        endTime += 1440;
                     }

                     for(int k = 0; k < rule.startTimes.size(); ++k) {
                        int rStartTime = rule.startTimes.get(k);
                        int rEndTime = rule.endTimes.get(k);
                        if (rEndTime == -1) {
                           rEndTime = 1440;
                        } else if (rStartTime >= rEndTime) {
                           rEndTime += 1440;
                        }

                        if (rStartTime >= startTime && (strictOverlap ? rStartTime <= endTime : rStartTime < endTime)
                           || startTime >= rStartTime && (strictOverlap ? startTime <= rEndTime : startTime < rEndTime)) {
                           return true;
                        }
                     }
                  }
               }
            }

            return false;
         }
      }

      private int calculate(Calendar cal) {
         int month = cal.get(2);
         if (!this.containsMonth(cal)) {
            return 0;
         } else {
            int dmonth = cal.get(5) - 1;
            int i = cal.get(7);
            int day = (i + 5) % 7;
            int previous = (day + 6) % 7;
            boolean thisDay = this.hasDays || this.hasDayMonths();
            if (thisDay && this.hasDayMonths()) {
               thisDay = this.dayMonths[month][dmonth];
            }

            if (thisDay && this.hasDays) {
               thisDay = this.days[day];
            }

            boolean previousDay = this.hasDays || this.hasDayMonths();
            if (previousDay && this.hasDayMonths() && dmonth > 0) {
               previousDay = this.dayMonths[month][dmonth - 1];
            }

            if (previousDay && this.hasDays) {
               previousDay = this.days[previous];
            }

            if (!thisDay && !previousDay) {
               return 0;
            } else {
               int time = cal.get(11) * 60 + cal.get(12);

               for(int var12 = 0; var12 < this.startTimes.size(); ++var12) {
                  int startTime = this.startTimes.get(var12);
                  int endTime = this.endTimes.get(var12);
                  if (startTime >= endTime && endTime != -1) {
                     if (time >= startTime && thisDay) {
                        return this.off ? -1 : 1;
                     }

                     if (time < endTime && previousDay) {
                        return this.off ? -1 : 1;
                     }
                  } else if (time >= startTime && (endTime == -1 || time <= endTime) && thisDay) {
                     return this.off ? -1 : 1;
                  }
               }

               if (thisDay && (this.startTimes == null || this.startTimes.isEmpty()) && !this.off) {
                  return 1;
               } else {
                  return !thisDay || this.startTimes != null && !this.startTimes.isEmpty() && this.off ? 0 : -1;
               }
            }
         }
      }
   }

   public static class OpeningHours implements Serializable {
      public static final int ALL_SEQUENCES = -1;
      private ArrayList<OpeningHoursParser.OpeningHoursRule> rules;
      private String original;
      private int sequenceCount;

      public OpeningHours(ArrayList<OpeningHoursParser.OpeningHoursRule> rules) {
         this.rules = rules;
      }

      public OpeningHours() {
         this.rules = new ArrayList<>();
      }

      public List<OpeningHoursParser.OpeningHours.Info> getInfo() {
         return this.getInfo(Calendar.getInstance());
      }

      public List<OpeningHoursParser.OpeningHours.Info> getInfo(Calendar cal) {
         List<OpeningHoursParser.OpeningHours.Info> res = new ArrayList<>();

         for(int i = 0; i < this.sequenceCount; ++i) {
            OpeningHoursParser.OpeningHours.Info info = this.getInfo(cal, i);
            res.add(info);
         }

         return res.isEmpty() ? null : res;
      }

      public OpeningHoursParser.OpeningHours.Info getCombinedInfo() {
         return this.getCombinedInfo(Calendar.getInstance());
      }

      public OpeningHoursParser.OpeningHours.Info getCombinedInfo(Calendar cal) {
         return this.getInfo(cal, -1);
      }

      private OpeningHoursParser.OpeningHours.Info getInfo(Calendar cal, int sequenceIndex) {
         OpeningHoursParser.OpeningHours.Info info = new OpeningHoursParser.OpeningHours.Info();
         boolean opened = this.isOpenedForTimeV2(cal, sequenceIndex);
         info.fallback = this.isFallBackRule(sequenceIndex);
         info.opened = opened;
         info.ruleString = this.getCurrentRuleTime(cal, sequenceIndex);
         if (opened) {
            info.opened24_7 = this.isOpened24_7(sequenceIndex);
            info.closingTime = this.getClosingTime(cal, sequenceIndex);
            info.nearToClosingTime = this.getNearToClosingTime(cal, sequenceIndex);
         } else {
            info.openingTime = this.getOpeningTime(cal, sequenceIndex);
            info.nearToOpeningTime = this.getNearToOpeningTime(cal, sequenceIndex);
            info.openingTomorrow = this.getOpeningTomorrow(cal, sequenceIndex);
            info.openingDay = this.getOpeningDay(cal, sequenceIndex);
         }

         return info;
      }

      public void addRule(OpeningHoursParser.OpeningHoursRule r) {
         this.rules.add(r);
      }

      public void addRules(List<? extends OpeningHoursParser.OpeningHoursRule> rules) {
         this.rules.addAll(rules);
      }

      public int getSequenceCount() {
         return this.sequenceCount;
      }

      public void setSequenceCount(int sequenceCount) {
         this.sequenceCount = sequenceCount;
      }

      public ArrayList<OpeningHoursParser.OpeningHoursRule> getRules() {
         return this.rules;
      }

      public ArrayList<OpeningHoursParser.OpeningHoursRule> getRules(int sequenceIndex) {
         if (sequenceIndex == -1) {
            return this.rules;
         } else {
            ArrayList<OpeningHoursParser.OpeningHoursRule> sequenceRules = new ArrayList<>();

            for(OpeningHoursParser.OpeningHoursRule r : this.rules) {
               if (r.getSequenceIndex() == sequenceIndex) {
                  sequenceRules.add(r);
               }
            }

            return sequenceRules;
         }
      }

      public boolean isOpenedForTimeV2(Calendar cal, int sequenceIndex) {
         ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);
         boolean overlap = false;

         for(int i = rules.size() - 1; i >= 0; --i) {
            OpeningHoursParser.OpeningHoursRule r = rules.get(i);
            if (r.hasOverlapTimes()) {
               overlap = true;
               break;
            }
         }

         for(int i = rules.size() - 1; i >= 0; --i) {
            boolean checkNext = false;
            OpeningHoursParser.OpeningHoursRule rule = rules.get(i);
            if (rule.contains(cal)) {
               if (i > 0) {
                  checkNext = !rule.hasOverlapTimes(cal, rules.get(i - 1), false);
               }

               boolean open = rule.isOpenedForTime(cal);
               if (open || !overlap && !checkNext) {
                  return open;
               }
            }
         }

         return false;
      }

      public boolean isOpenedForTime(Calendar cal) {
         return this.isOpenedForTimeV2(cal, -1);
      }

      public boolean isOpenedForTime(Calendar cal, int sequenceIndex) {
         boolean isOpenDay = false;
         ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);

         for(OpeningHoursParser.OpeningHoursRule r : rules) {
            if (r.containsDay(cal) && r.containsMonth(cal)) {
               isOpenDay = r.isOpenedForTime(cal, false);
            }
         }

         boolean isOpenPrevious = false;

         for(OpeningHoursParser.OpeningHoursRule r : rules) {
            if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
               isOpenPrevious = r.isOpenedForTime(cal, true);
            }
         }

         return isOpenDay || isOpenPrevious;
      }

      public boolean isOpened24_7(int sequenceIndex) {
         boolean opened24_7 = false;

         for(OpeningHoursParser.OpeningHoursRule r : this.getRules(sequenceIndex)) {
            opened24_7 = r.isOpened24_7();
         }

         return opened24_7;
      }

      public String getNearToOpeningTime(Calendar cal, int sequenceIndex) {
         return this.getTime(cal, 120, true, sequenceIndex);
      }

      public String getOpeningTime(Calendar cal, int sequenceIndex) {
         return this.getTime(cal, -2, true, sequenceIndex);
      }

      public String getNearToClosingTime(Calendar cal, int sequenceIndex) {
         return this.getTime(cal, 120, false, sequenceIndex);
      }

      public String getClosingTime(Calendar cal, int sequenceIndex) {
         return this.getTime(cal, -1, false, sequenceIndex);
      }

      public String getOpeningTomorrow(Calendar calendar, int sequenceIndex) {
         Calendar cal = (Calendar)calendar.clone();
         String openingTime = "";
         ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);
         cal.add(5, 1);
         Calendar openingTimeCal = null;

         for(OpeningHoursParser.OpeningHoursRule r : rules) {
            if (r.containsDay(cal) && r.containsMonth(cal) && r.containsDay(cal) && r.containsMonth(cal)) {
               String time = r.getTime(cal, false, -1, true);
               if (Algorithms.isEmpty(time) || openingTimeCal == null || cal.before(openingTimeCal)) {
                  openingTime = time;
               }

               openingTimeCal = (Calendar)cal.clone();
            }
         }

         return openingTime;
      }

      public String getOpeningDay(Calendar calendar, int sequenceIndex) {
         Calendar cal = (Calendar)calendar.clone();
         String openingTime = "";
         ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);

         for(int i = 0; i < 7; ++i) {
            cal.add(5, 1);
            Calendar openingTimeCal = null;

            for(OpeningHoursParser.OpeningHoursRule r : rules) {
               if (r.containsDay(cal) && r.containsMonth(cal)) {
                  String time = r.getTime(cal, false, -1, true);
                  if (Algorithms.isEmpty(time) || openingTimeCal == null || cal.before(openingTimeCal)) {
                     openingTime = time;
                  }

                  openingTimeCal = (Calendar)cal.clone();
               }
            }

            if (!Algorithms.isEmpty(openingTime)) {
               openingTime = openingTime + " " + OpeningHoursParser.localDaysStr[cal.get(7)];
               break;
            }
         }

         return openingTime;
      }

      private String getTime(Calendar cal, int limit, boolean opening, int sequenceIndex) {
         String time = this.getTimeDay(cal, limit, opening, sequenceIndex);
         if (Algorithms.isEmpty(time)) {
            time = this.getTimeAnotherDay(cal, limit, opening, sequenceIndex);
         }

         return time;
      }

      private String getTimeDay(Calendar cal, int limit, boolean opening, int sequenceIndex) {
         String atTime = "";
         ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);
         OpeningHoursParser.OpeningHoursRule prevRule = null;

         for(OpeningHoursParser.OpeningHoursRule r : rules) {
            if (r.containsDay(cal) && r.containsMonth(cal)) {
               if (atTime.length() > 0 && prevRule != null && !r.hasOverlapTimes(cal, prevRule, true)) {
                  return atTime;
               }

               atTime = r.getTime(cal, false, limit, opening);
            }

            prevRule = r;
         }

         return atTime;
      }

      private String getTimeAnotherDay(Calendar cal, int limit, boolean opening, int sequenceIndex) {
         String atTime = "";

         for(OpeningHoursParser.OpeningHoursRule r : this.getRules(sequenceIndex)) {
            if ((opening && r.containsPreviousDay(cal) || !opening && r.containsNextDay(cal)) && r.containsMonth(cal)) {
               atTime = r.getTime(cal, true, limit, opening);
            }
         }

         return atTime;
      }

      public String getCurrentRuleTime(Calendar cal) {
         return this.getCurrentRuleTime(cal, -1);
      }

      public boolean isFallBackRule(int sequenceIndex) {
         if (sequenceIndex == -1) {
            return false;
         } else {
            ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);
            return !rules.isEmpty() && rules.get(0).isFallbackRule();
         }
      }

      public String getCurrentRuleTime(Calendar cal, int sequenceIndex) {
         ArrayList<OpeningHoursParser.OpeningHoursRule> rules = this.getRules(sequenceIndex);
         String ruleClosed = null;
         boolean overlap = false;

         for(int i = rules.size() - 1; i >= 0; --i) {
            OpeningHoursParser.OpeningHoursRule r = rules.get(i);
            if (r.hasOverlapTimes()) {
               overlap = true;
               break;
            }
         }

         for(int i = rules.size() - 1; i >= 0; --i) {
            boolean checkNext = false;
            OpeningHoursParser.OpeningHoursRule rule = rules.get(i);
            if (rule.contains(cal)) {
               if (i > 0) {
                  checkNext = !rule.hasOverlapTimes(cal, rules.get(i - 1), false);
               }

               boolean open = rule.isOpenedForTime(cal);
               if (open || !overlap && !checkNext) {
                  return rule.toLocalRuleString();
               }

               ruleClosed = rule.toLocalRuleString();
            }
         }

         return ruleClosed;
      }

      public String getCurrentRuleTimeV1(Calendar cal) {
         String ruleOpen = null;
         String ruleClosed = null;

         for(OpeningHoursParser.OpeningHoursRule r : this.rules) {
            if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
               if (r.isOpenedForTime(cal, true)) {
                  ruleOpen = r.toLocalRuleString();
               } else {
                  ruleClosed = r.toLocalRuleString();
               }
            }
         }

         for(OpeningHoursParser.OpeningHoursRule r : this.rules) {
            if (r.containsDay(cal) && r.containsMonth(cal)) {
               if (r.isOpenedForTime(cal, false)) {
                  ruleOpen = r.toLocalRuleString();
               } else {
                  ruleClosed = r.toLocalRuleString();
               }
            }
         }

         return ruleOpen != null ? ruleOpen : ruleClosed;
      }

      @Override
      public String toString() {
         StringBuilder s = new StringBuilder();
         if (this.rules.isEmpty()) {
            return "";
         } else {
            for(OpeningHoursParser.OpeningHoursRule r : this.rules) {
               s.append(r.toString()).append("; ");
            }

            return s.substring(0, s.length() - 2);
         }
      }

      public String toLocalString() {
         StringBuilder s = new StringBuilder();
         if (this.rules.isEmpty()) {
            return "";
         } else {
            for(OpeningHoursParser.OpeningHoursRule r : this.rules) {
               s.append(r.toLocalRuleString()).append("; ");
            }

            return s.substring(0, s.length() - 2);
         }
      }

      public void setOriginal(String original) {
         this.original = original;
      }

      public String getOriginal() {
         return this.original;
      }

      public static class Info {
         private boolean opened;
         private boolean opened24_7;
         private boolean fallback;
         private String openingTime;
         private String nearToOpeningTime;
         private String closingTime;
         private String nearToClosingTime;
         private String openingTomorrow;
         private String openingDay;
         private String ruleString;

         public boolean isOpened() {
            return this.opened;
         }

         public boolean isOpened24_7() {
            return this.opened24_7;
         }

         public boolean isFallback() {
            return this.fallback;
         }

         public String getInfo() {
            if (this.isOpened24_7()) {
               if (!this.isFallback()) {
                  return !Algorithms.isEmpty(this.ruleString)
                     ? (String)OpeningHoursParser.additionalStrings.get("is_open") + " " + this.ruleString
                     : OpeningHoursParser.additionalStrings.get("is_open_24_7");
               } else {
                  return !Algorithms.isEmpty(this.ruleString) ? this.ruleString : "";
               }
            } else if (!Algorithms.isEmpty(this.nearToOpeningTime)) {
               return (String)OpeningHoursParser.additionalStrings.get("will_open_at") + " " + this.nearToOpeningTime;
            } else if (!Algorithms.isEmpty(this.openingTime)) {
               return (String)OpeningHoursParser.additionalStrings.get("open_from") + " " + this.openingTime;
            } else if (!Algorithms.isEmpty(this.nearToClosingTime)) {
               return (String)OpeningHoursParser.additionalStrings.get("will_close_at") + " " + this.nearToClosingTime;
            } else if (!Algorithms.isEmpty(this.closingTime)) {
               return (String)OpeningHoursParser.additionalStrings.get("open_till") + " " + this.closingTime;
            } else if (!Algorithms.isEmpty(this.openingTomorrow)) {
               return (String)OpeningHoursParser.additionalStrings.get("will_open_tomorrow_at") + " " + this.openingTomorrow;
            } else if (!Algorithms.isEmpty(this.openingDay)) {
               return (String)OpeningHoursParser.additionalStrings.get("will_open_on") + " " + this.openingDay + ".";
            } else {
               return !Algorithms.isEmpty(this.ruleString) ? this.ruleString : "";
            }
         }
      }
   }

   public interface OpeningHoursRule extends Serializable {
      boolean isOpenedForTime(Calendar var1, boolean var2);

      boolean isOpenedForTime(Calendar var1);

      boolean containsPreviousDay(Calendar var1);

      boolean containsDay(Calendar var1);

      boolean containsNextDay(Calendar var1);

      boolean containsMonth(Calendar var1);

      boolean containsYear(Calendar var1);

      boolean hasOverlapTimes();

      boolean hasOverlapTimes(Calendar var1, OpeningHoursParser.OpeningHoursRule var2, boolean var3);

      boolean contains(Calendar var1);

      int getSequenceIndex();

      boolean isFallbackRule();

      String toRuleString();

      String toLocalRuleString();

      boolean isOpened24_7();

      String getTime(Calendar var1, boolean var2, int var3, boolean var4);
   }

   private static class Token {
      int mainNumber = -1;
      OpeningHoursParser.TokenType type;
      String text;
      OpeningHoursParser.Token parent;

      public Token(OpeningHoursParser.TokenType tokenType, String string) {
         this.type = tokenType;
         this.text = string;

         try {
            this.mainNumber = Integer.parseInt(string);
         } catch (NumberFormatException var4) {
         }
      }

      public Token(OpeningHoursParser.TokenType tokenType, int tokenMainNumber) {
         this.type = tokenType;
         this.mainNumber = tokenMainNumber;
         this.text = Integer.toString(this.mainNumber);
      }

      @Override
      public String toString() {
         return this.parent != null
            ? this.parent.text + " [" + this.parent.type + "] (" + this.text + " [" + this.type + "]) "
            : this.text + " [" + this.type + "] ";
      }
   }

   private static enum TokenType {
      TOKEN_UNKNOWN(0),
      TOKEN_COLON(1),
      TOKEN_COMMA(2),
      TOKEN_DASH(3),
      TOKEN_YEAR(4),
      TOKEN_MONTH(5),
      TOKEN_DAY_MONTH(6),
      TOKEN_HOLIDAY(7),
      TOKEN_DAY_WEEK(7),
      TOKEN_HOUR_MINUTES(8),
      TOKEN_OFF_ON(9),
      TOKEN_COMMENT(10);

      public final int ord;

      private TokenType(int ord) {
         this.ord = ord;
      }

      public int ord() {
         return this.ord;
      }
   }

   public static class UnparseableRule implements OpeningHoursParser.OpeningHoursRule {
      private String ruleString;

      public UnparseableRule(String ruleString) {
         this.ruleString = ruleString;
      }

      @Override
      public boolean isOpenedForTime(Calendar cal, boolean checkPrevious) {
         return false;
      }

      @Override
      public boolean containsPreviousDay(Calendar cal) {
         return false;
      }

      @Override
      public boolean hasOverlapTimes() {
         return false;
      }

      @Override
      public boolean hasOverlapTimes(Calendar cal, OpeningHoursParser.OpeningHoursRule r, boolean strictOverlap) {
         return false;
      }

      @Override
      public boolean containsDay(Calendar cal) {
         return false;
      }

      @Override
      public boolean containsNextDay(Calendar cal) {
         return false;
      }

      @Override
      public boolean containsMonth(Calendar cal) {
         return false;
      }

      @Override
      public boolean containsYear(Calendar cal) {
         return false;
      }

      @Override
      public String toRuleString() {
         return this.ruleString;
      }

      @Override
      public String toLocalRuleString() {
         return this.toRuleString();
      }

      @Override
      public boolean isOpened24_7() {
         return false;
      }

      @Override
      public String getTime(Calendar cal, boolean checkAnotherDay, int limit, boolean opening) {
         return "";
      }

      @Override
      public String toString() {
         return this.toRuleString();
      }

      @Override
      public boolean isOpenedForTime(Calendar cal) {
         return false;
      }

      @Override
      public boolean contains(Calendar cal) {
         return false;
      }

      @Override
      public int getSequenceIndex() {
         return 0;
      }

      @Override
      public boolean isFallbackRule() {
         return false;
      }
   }
}
