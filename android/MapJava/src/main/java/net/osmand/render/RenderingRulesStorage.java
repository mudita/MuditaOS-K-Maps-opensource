package net.osmand.render;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RenderingRulesStorage {
   private static final Log log = PlatformUtil.getLog(RenderingRulesStorage.class);
   static boolean STORE_ATTRIBUTES = false;
   public static final int MULTY_POLYGON_TYPE = 0;
   public static final int POINT_RULES = 1;
   public static final int LINE_RULES = 2;
   public static final int POLYGON_RULES = 3;
   public static final int TEXT_RULES = 4;
   public static final int ORDER_RULES = 5;
   public static final int LENGTH_RULES = 6;
   private static final int SHIFT_TAG_VAL = 16;
   private static final String SEQ_ATTR_KEY = "seq";
   private static final String SEQ_PLACEHOLDER = "#SEQ";
   List<String> dictionary = new ArrayList<>();
   Map<String, Integer> dictionaryMap = new LinkedHashMap<>();
   public RenderingRuleStorageProperties PROPS = new RenderingRuleStorageProperties();
   public TIntObjectHashMap<RenderingRule>[] tagValueGlobalRules = new TIntObjectHashMap[6];
   protected Map<String, RenderingRule> renderingAttributes = new LinkedHashMap<>();
   protected Map<String, String> renderingConstants = new LinkedHashMap<>();
   protected String renderingName;
   protected String dependsName;
   protected String internalRenderingName;
   protected int internalVersion = 1;

   public String getDependsName() {
      return this.dependsName;
   }

   public RenderingRulesStorage(String name, Map<String, String> renderingConstants) {
      this.getDictionaryValue("");
      this.renderingName = name;
      if (renderingConstants != null) {
         this.renderingConstants.putAll(renderingConstants);
      }
   }

   public RenderingRulesStorage copy() {
      RenderingRulesStorage storage = new RenderingRulesStorage(this.renderingName, this.renderingConstants);
      storage.internalRenderingName = this.internalRenderingName;
      storage.internalVersion = this.internalVersion + 1;
      storage.dictionary = new ArrayList<>(this.dictionary);
      storage.dictionaryMap.putAll(this.dictionaryMap);
      storage.PROPS = new RenderingRuleStorageProperties(this.PROPS);
      storage.tagValueGlobalRules = new TIntObjectHashMap[this.tagValueGlobalRules.length];

      for(int i = 0; i < this.tagValueGlobalRules.length; ++i) {
         TIntObjectHashMap<RenderingRule> rule = this.tagValueGlobalRules[i];
         if (rule != null) {
            TIntObjectHashMap<RenderingRule> newRule = new TIntObjectHashMap();
            newRule.putAll(rule);
            storage.tagValueGlobalRules[i] = newRule;
         }
      }

      storage.renderingAttributes.putAll(this.renderingAttributes);
      return storage;
   }

   public int getInternalVersion() {
      return this.internalVersion;
   }

   public int getDictionaryValue(String val) {
      if (this.dictionaryMap.containsKey(val)) {
         return this.dictionaryMap.get(val);
      } else {
         int nextInd = this.dictionaryMap.size();
         this.dictionaryMap.put(val, nextInd);
         this.dictionary.add(val);
         return nextInd;
      }
   }

   public String getStringValue(int i) {
      return this.dictionary.get(i);
   }

   public String getName() {
      return this.renderingName;
   }

   public String getInternalRenderingName() {
      return this.internalRenderingName;
   }

   public void parseRulesFromXmlInputStream(InputStream is, RenderingRulesStorage.RenderingRulesStorageResolver resolver, boolean addon) throws XmlPullParserException, IOException {
      XmlPullParser parser = PlatformUtil.newXMLPullParser();
      RenderingRulesStorage.RenderingRulesHandler handler = new RenderingRulesStorage.RenderingRulesHandler(parser, resolver, addon);
      handler.parse(is);
      RenderingRulesStorage depends = handler.getDependsStorage();
      if (depends != null) {
         this.dependsName = depends.getName();
         this.mergeDependsOrAddon(depends);
      }
   }

   public void mergeDependsOrAddon(RenderingRulesStorage depends) {
      if (depends != null) {
         for(Entry<String, RenderingRule> e : depends.renderingAttributes.entrySet()) {
            if (this.renderingAttributes.containsKey(e.getKey())) {
               RenderingRule root = this.renderingAttributes.get(e.getKey());

               for(RenderingRule every : e.getValue().getIfElseChildren()) {
                  root.addIfElseChildren(every);
               }

               e.getValue().addToBeginIfElseChildren(root);
            } else {
               this.renderingAttributes.put(e.getKey(), e.getValue());
            }
         }

         for(int i = 0; i < 6; ++i) {
            if (depends.tagValueGlobalRules[i] != null && !depends.tagValueGlobalRules[i].isEmpty()) {
               if (this.tagValueGlobalRules[i] != null) {
                  int[] keys = depends.tagValueGlobalRules[i].keys();

                  for(int j = 0; j < keys.length; ++j) {
                     RenderingRule rule = (RenderingRule)this.tagValueGlobalRules[i].get(keys[j]);
                     RenderingRule dependsRule = (RenderingRule)depends.tagValueGlobalRules[i].get(keys[j]);
                     if (dependsRule != null) {
                        if (rule != null) {
                           RenderingRule toInsert = this.createTagValueRootWrapperRule(keys[j], rule);
                           toInsert.addIfElseChildren(dependsRule);
                           this.tagValueGlobalRules[i].put(keys[j], toInsert);
                        } else {
                           this.tagValueGlobalRules[i].put(keys[j], dependsRule);
                        }
                     }
                  }
               } else {
                  this.tagValueGlobalRules[i] = depends.tagValueGlobalRules[i];
               }
            }
         }
      }
   }

   public static String colorToString(int color) {
      return (0xFF000000 & color) == -16777216 ? "#" + Integer.toHexString(color & 16777215) : "#" + Integer.toHexString(color);
   }

   private void registerGlobalRule(RenderingRule rr, int state, String tagS, String valueS, boolean addToBegin) throws XmlPullParserException {
      if (tagS != null && valueS != null) {
         int key = this.getTagValueKey(tagS, valueS);
         RenderingRule insert = (RenderingRule)this.tagValueGlobalRules[state].get(key);
         if (insert != null) {
            insert = this.createTagValueRootWrapperRule(key, insert);
            if (addToBegin) {
               insert.addToBeginIfElseChildren(rr);
            } else {
               insert.addIfElseChildren(rr);
            }
         } else {
            insert = rr;
         }

         this.tagValueGlobalRules[state].put(key, insert);
      } else {
         throw new XmlPullParserException("Attribute tag should be specified for root filter " + rr.toString());
      }
   }

   private RenderingRule createTagValueRootWrapperRule(int tagValueKey, RenderingRule previous) {
      if (previous.getProperties().length > 0) {
         Map<String, String> m = new HashMap<>();
         RenderingRule toInsert = new RenderingRule(m, true, this);
         toInsert.addIfElseChildren(previous);
         return toInsert;
      } else {
         return previous;
      }
   }

   public void registerTopLevel(RenderingRule renderingRule, List<RenderingRule> applyRules, Map<String, String> attrs, int state, boolean addToBegin) throws XmlPullParserException {
      if (renderingRule.isGroup() && (renderingRule.getIntPropertyValue("tag") == -1 || renderingRule.getIntPropertyValue("value") == -1)) {
         for(RenderingRule ch : renderingRule.getIfElseChildren()) {
            List<RenderingRule> apply = applyRules;
            if (!renderingRule.getIfChildren().isEmpty()) {
               apply = new ArrayList<>(renderingRule.getIfChildren());
               if (applyRules != null) {
                  apply.addAll(applyRules);
               }
            }

            Map<String, String> cattrs = new HashMap<>(attrs);
            cattrs.putAll(renderingRule.getAttributes());
            this.registerTopLevel(ch, apply, cattrs, state, addToBegin);
         }
      } else {
         HashMap<String, String> ns = new HashMap<>(attrs);
         ns.putAll(renderingRule.getAttributes());
         String tg = ns.remove("tag");
         String vl = ns.remove("value");
         renderingRule.init(ns);
         if (STORE_ATTRIBUTES) {
            renderingRule.storeAttributes(ns);
         }

         this.registerGlobalRule(renderingRule, state, tg, vl, addToBegin);
         if (applyRules != null) {
            for(RenderingRule apply : applyRules) {
               renderingRule.addIfChildren(apply);
            }
         }
      }
   }

   public int getTagValueKey(String tag, String value) {
      int itag = this.getDictionaryValue(tag);
      int ivalue = this.getDictionaryValue(value);
      return itag << 16 | ivalue;
   }

   public String getValueString(int tagValueKey) {
      return this.getStringValue(tagValueKey & 65535);
   }

   public String getTagString(int tagValueKey) {
      return this.getStringValue(tagValueKey >> 16);
   }

   protected RenderingRule getRule(int state, int itag, int ivalue) {
      return this.tagValueGlobalRules[state] != null ? (RenderingRule)this.tagValueGlobalRules[state].get(itag << 16 | ivalue) : null;
   }

   public RenderingRule getRule(int state, int key) {
      return this.tagValueGlobalRules[state] != null ? (RenderingRule)this.tagValueGlobalRules[state].get(key) : null;
   }

   public RenderingRule getRenderingAttributeRule(String attribute) {
      return this.renderingAttributes.get(attribute);
   }

   public String[] getRenderingAttributeNames() {
      return this.renderingAttributes.keySet().toArray(new String[0]);
   }

   public RenderingRule[] getRenderingAttributeValues() {
      return this.renderingAttributes.values().toArray(new RenderingRule[0]);
   }

   public RenderingRule[] getRules(int state) {
      return state < this.tagValueGlobalRules.length && this.tagValueGlobalRules[state] != null
         ? (RenderingRule[])this.tagValueGlobalRules[state].values(new RenderingRule[this.tagValueGlobalRules[state].size()])
         : new RenderingRule[0];
   }

   public int getRuleTagValueKey(int state, int ind) {
      return this.tagValueGlobalRules[state].keys()[ind];
   }

   public void printDebug(int state, PrintStream out) {
      for(int key : this.tagValueGlobalRules[state].keys()) {
         RenderingRule rr = (RenderingRule)this.tagValueGlobalRules[state].get(key);
         out.print("\n\n" + this.getTagString(key) + " : " + this.getValueString(key) + "\n ");
         printRenderingRule(" ", rr, out);
      }
   }

   private static void printRenderingRule(String indent, RenderingRule rr, PrintStream out) {
      out.print(rr.toString(indent, new StringBuilder()).toString());
   }

   public static void main(String[] args) throws XmlPullParserException, IOException {
      STORE_ATTRIBUTES = true;
      String loc = "/Users/victorshcherb/osmand/repos/resources/rendering_styles/";
      String defaultFile = "/Users/victorshcherb/osmand/repos/resources/rendering_styles/UniRS.render.xml";
      if (args.length > 0) {
         defaultFile = args[0];
      }

      final Map<String, String> renderingConstants = new LinkedHashMap<>();
      InputStream is = new FileInputStream("/Users/victorshcherb/osmand/repos/resources/rendering_styles/default.render.xml");

      try {
         XmlPullParser parser = PlatformUtil.newXMLPullParser();
         parser.setInput(is, "UTF-8");

         int tok;
         while((tok = parser.next()) != 1) {
            if (tok == 2) {
               String tagName = parser.getName();
               if (tagName.equals("renderingConstant") && !renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
                  renderingConstants.put(parser.getAttributeValue("", "name"), parser.getAttributeValue("", "value"));
               }
            }
         }
      } finally {
         is.close();
      }

      RenderingRulesStorage var12 = new RenderingRulesStorage("default", renderingConstants);
      RenderingRulesStorage.RenderingRulesStorageResolver var13 = new RenderingRulesStorage.RenderingRulesStorageResolver() {
         @Override
         public RenderingRulesStorage resolve(String name, RenderingRulesStorage.RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
            RenderingRulesStorage depends = new RenderingRulesStorage(name, renderingConstants);
            depends.parseRulesFromXmlInputStream(
               new FileInputStream("/Users/victorshcherb/osmand/repos/resources/rendering_styles/" + name + ".render.xml"), ref, false
            );
            return depends;
         }
      };
      is = new FileInputStream(defaultFile);
      var12.parseRulesFromXmlInputStream(is, var13, false);

      for(RenderingRuleProperty p : var12.PROPS.getCustomRules()) {
         System.out.println(p.getCategory() + " " + p.getName() + " " + p.getAttrName());
      }
   }

   protected static void testSearch(RenderingRulesStorage storage) {
      RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(storage);
      searchRequest.setStringFilter(storage.PROPS.R_TAG, "highway");
      searchRequest.setStringFilter(storage.PROPS.R_VALUE, "residential");
      searchRequest.setIntFilter(storage.PROPS.R_MINZOOM, 13);
      searchRequest.setIntFilter(storage.PROPS.R_MAXZOOM, 13);
      boolean res = searchRequest.search(2);
      System.out.println("Result " + res);
      printResult(searchRequest, System.out);
   }

   protected static void printAllRules(RenderingRulesStorage storage) {
      System.out.println("\n\n--------- POINTS ----- ");
      storage.printDebug(1, System.out);
      System.out.println("\n\n--------- POLYGON ----- ");
      storage.printDebug(3, System.out);
      System.out.println("\n\n--------- LINES ----- ");
      storage.printDebug(2, System.out);
      System.out.println("\n\n--------- ORDER ----- ");
      storage.printDebug(5, System.out);
      System.out.println("\n\n--------- TEXT ----- ");
      storage.printDebug(4, System.out);
   }

   private static void printResult(RenderingRuleSearchRequest searchRequest, PrintStream out) {
      if (searchRequest.isFound()) {
         out.print(" Found : ");

         for(RenderingRuleProperty rp : searchRequest.getProperties()) {
            if (rp.isOutputProperty() && searchRequest.isSpecified(rp)) {
               out.print(" " + rp.getAttrName() + "= ");
               if (rp.isString()) {
                  out.print("\"" + searchRequest.getStringPropertyValue(rp) + "\"");
               } else if (rp.isFloat()) {
                  out.print(searchRequest.getFloatPropertyValue(rp));
               } else if (rp.isColor()) {
                  out.print(searchRequest.getColorStringPropertyValue(rp));
               } else if (rp.isIntParse()) {
                  out.print(searchRequest.getIntPropertyValue(rp));
               }
            }
         }
      } else {
         out.println("Not found");
      }
   }

   @Override
   public String toString() {
      return this.getName();
   }

   private class RenderingRulesHandler {
      private final XmlPullParser parser;
      private int state;
      Stack<RenderingRule> stack = new Stack<>();
      private final RenderingRulesStorage.RenderingRulesStorageResolver resolver;
      private final boolean addon;
      private RenderingRulesStorage dependsStorage;

      public RenderingRulesHandler(XmlPullParser parser, RenderingRulesStorage.RenderingRulesStorageResolver resolver, boolean addon) {
         this.parser = parser;
         this.resolver = resolver;
         this.addon = addon;
      }

      public void parse(InputStream is) throws XmlPullParserException, IOException {
         XmlPullParser parser = this.parser;
         Map<String, String> attrsMap = new LinkedHashMap<>();
         parser.setInput(is, "UTF-8");
         RenderingRulesStorage.XmlTreeSequence currentSeqElement = null;

         int tok;
         while((tok = parser.next()) != 1) {
            if (tok == 2) {
               attrsMap.clear();
               this.parseAttributes(parser, attrsMap);
               String name = parser.getName();
               if (Algorithms.isEmpty(parser.getAttributeValue("", "seq")) && currentSeqElement == null) {
                  this.startElement(attrsMap, name);
               } else {
                  RenderingRulesStorage.XmlTreeSequence seq = RenderingRulesStorage.this.new XmlTreeSequence();
                  seq.name = name;
                  seq.attrsMap = new HashMap<>(attrsMap);
                  seq.parent = currentSeqElement;
                  if (currentSeqElement == null) {
                     seq.seqOrder = parser.getAttributeValue("", "seq");
                  } else {
                     currentSeqElement.children.add(seq);
                     seq.seqOrder = currentSeqElement.seqOrder;
                  }

                  currentSeqElement = seq;
               }
            } else if (tok == 3) {
               if (currentSeqElement == null) {
                  this.endElement(parser.getName());
               } else {
                  RenderingRulesStorage.XmlTreeSequence process = currentSeqElement;
                  currentSeqElement = currentSeqElement.parent;
                  if (currentSeqElement == null) {
                     int seqEnd = Integer.parseInt(process.seqOrder.substring(process.seqOrder.indexOf(58) + 1, process.seqOrder.length()));

                     for(int i = 1; i < seqEnd; ++i) {
                        process.process(this, i);
                     }
                  }
               }
            }
         }
      }

      public RenderingRulesStorage getDependsStorage() {
         return this.dependsStorage;
      }

      private boolean isTopCase() {
         for(int i = 0; i < this.stack.size(); ++i) {
            if (!this.stack.get(i).isGroup()) {
               return false;
            }
         }

         return true;
      }

      public void startElement(Map<String, String> attrsMap, String name) throws XmlPullParserException, IOException {
         boolean stateChanged = false;
         boolean isCase = this.isCase(name);
         boolean isSwitch = this.isSwitch(name);
         if (!isCase && !isSwitch) {
            if (this.isApply(name)) {
               RenderingRule renderingRule = new RenderingRule(attrsMap, false, RenderingRulesStorage.this);
               if (RenderingRulesStorage.STORE_ATTRIBUTES) {
                  renderingRule.storeAttributes(attrsMap);
               }

               if (this.stack.size() <= 0 || !(this.stack.peek() instanceof RenderingRule)) {
                  throw new XmlPullParserException("Apply (groupFilter) without parent");
               }

               this.stack.peek().addIfChildren(renderingRule);
               this.stack.push(renderingRule);
            } else if ("order".equals(name)) {
               this.state = 5;
            } else if ("text".equals(name)) {
               this.state = 4;
            } else if ("point".equals(name)) {
               this.state = 1;
            } else if ("line".equals(name)) {
               this.state = 2;
            } else if ("polygon".equals(name)) {
               this.state = 3;
            } else if ("renderingAttribute".equals(name)) {
               String attr = attrsMap.get("name");
               RenderingRule root = new RenderingRule(new HashMap(), false, RenderingRulesStorage.this);
               RenderingRulesStorage.this.renderingAttributes.put(attr, root);
               this.stack.push(root);
            } else if ("renderingProperty".equals(name)) {
               String attr = attrsMap.get("attr");
               String type = attrsMap.get("type");
               RenderingRuleProperty prop;
               if ("boolean".equalsIgnoreCase(type)) {
                  prop = RenderingRuleProperty.createInputBooleanProperty(attr);
               } else if ("string".equalsIgnoreCase(type)) {
                  prop = RenderingRuleProperty.createInputStringProperty(attr);
               } else {
                  prop = RenderingRuleProperty.createInputIntProperty(attr);
               }

               prop.setDescription(attrsMap.get("description"));
               prop.setDefaultValueDescription(attrsMap.get("defaultValueDescription"));
               prop.setCategory(attrsMap.get("category"));
               prop.setName(attrsMap.get("name"));
               if (attrsMap.get("possibleValues") != null) {
                  prop.setPossibleValues(attrsMap.get("possibleValues").split(","));
               }

               RenderingRulesStorage.this.PROPS.registerRule(prop);
            } else if ("renderingConstant".equals(name)) {
               if (!RenderingRulesStorage.this.renderingConstants.containsKey(attrsMap.get("name"))) {
                  RenderingRulesStorage.this.renderingConstants.put(attrsMap.get("name"), attrsMap.get("value"));
               }
            } else if ("renderingStyle".equals(name) && !this.addon) {
               String depends = attrsMap.get("depends");
               if (depends != null && depends.length() > 0) {
                  this.dependsStorage = this.resolver.resolve(depends, this.resolver);
               }

               if (this.dependsStorage != null) {
                  RenderingRulesStorage.this.dictionary = new ArrayList<>(this.dependsStorage.dictionary);
                  RenderingRulesStorage.this.dictionaryMap = new LinkedHashMap<>(this.dependsStorage.dictionaryMap);
                  RenderingRulesStorage.this.PROPS = new RenderingRuleStorageProperties(this.dependsStorage.PROPS);
               }

               RenderingRulesStorage.this.internalRenderingName = attrsMap.get("name");
            } else {
               if ("renderer".equals(name)) {
                  throw new XmlPullParserException("Rendering style is deprecated and no longer supported.");
               }

               RenderingRulesStorage.log.warn("Unknown tag : " + name);
            }
         } else {
            boolean top = this.stack.size() == 0 || this.isTopCase();
            RenderingRule renderingRule = new RenderingRule(attrsMap, isSwitch, RenderingRulesStorage.this);
            if (top || RenderingRulesStorage.STORE_ATTRIBUTES) {
               renderingRule.storeAttributes(attrsMap);
            }

            if (this.stack.size() > 0 && this.stack.peek() instanceof RenderingRule) {
               RenderingRule parent = this.stack.peek();
               parent.addIfElseChildren(renderingRule);
            }

            this.stack.push(renderingRule);
         }

         if (RenderingRulesStorage.this.tagValueGlobalRules[this.state] == null) {
            RenderingRulesStorage.this.tagValueGlobalRules[this.state] = new TIntObjectHashMap();
         }
      }

      protected boolean isCase(String name) {
         return "filter".equals(name) || "case".equals(name);
      }

      protected boolean isApply(String name) {
         return "groupFilter".equals(name) || "apply".equals(name) || "apply_if".equals(name);
      }

      protected boolean isSwitch(String name) {
         return "group".equals(name) || "switch".equals(name);
      }

      private Map<String, String> parseAttributes(XmlPullParser parser, Map<String, String> m) {
         for(int i = 0; i < parser.getAttributeCount(); ++i) {
            String name = parser.getAttributeName(i);
            String vl = parser.getAttributeValue(i);
            if (vl != null && vl.startsWith("$")) {
               String cv = vl.substring(1);
               if (!RenderingRulesStorage.this.renderingConstants.containsKey(cv) && !RenderingRulesStorage.this.renderingAttributes.containsKey(cv)) {
                  throw new IllegalStateException("Rendering constant or attribute '" + cv + "' was not specified.");
               }

               if (RenderingRulesStorage.this.renderingConstants.containsKey(cv)) {
                  vl = RenderingRulesStorage.this.renderingConstants.get(cv);
               }
            }

            m.put(name, vl);
         }

         return m;
      }

      public void endElement(String name) throws XmlPullParserException {
         if (this.isCase(name) || this.isSwitch(name)) {
            RenderingRule renderingRule = this.stack.pop();
            if (this.stack.size() == 0) {
               RenderingRulesStorage.this.registerTopLevel(renderingRule, null, Collections.EMPTY_MAP, this.state, false);
            }
         } else if (this.isApply(name)) {
            this.stack.pop();
         } else if ("renderingAttribute".equals(name)) {
            this.stack.pop();
         }
      }
   }

   public interface RenderingRulesStorageResolver {
      RenderingRulesStorage resolve(String var1, RenderingRulesStorage.RenderingRulesStorageResolver var2) throws XmlPullParserException, IOException;
   }

   private class XmlTreeSequence {
      RenderingRulesStorage.XmlTreeSequence parent;
      String seqOrder;
      Map<String, String> attrsMap = new LinkedHashMap<>();
      String name;
      List<RenderingRulesStorage.XmlTreeSequence> children = new ArrayList<>();

      private XmlTreeSequence() {
      }

      private void process(RenderingRulesStorage.RenderingRulesHandler handler, int el) throws XmlPullParserException, IOException {
         Map<String, String> seqAttrsMap = new HashMap<>(this.attrsMap);
         if (this.attrsMap.containsKey("seq")) {
            this.attrsMap.remove("seq");
         }

         for(Entry<String, String> attr : this.attrsMap.entrySet()) {
            if (attr.getValue().contains("#SEQ")) {
               seqAttrsMap.put(attr.getKey(), attr.getValue().replace("#SEQ", el + ""));
            } else {
               seqAttrsMap.put(attr.getKey(), attr.getValue());
            }
         }

         handler.startElement(seqAttrsMap, this.name);

         for(RenderingRulesStorage.XmlTreeSequence s : this.children) {
            s.process(handler, el);
         }

         handler.endElement(this.name);
      }
   }
}
