package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class MapRenderingTypes {
   private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
   public static final String[] langs = new String[]{
      "af",
      "als",
      "ar",
      "az",
      "be",
      "bg",
      "bn",
      "bpy",
      "br",
      "bs",
      "ca",
      "ceb",
      "ckb",
      "cs",
      "cy",
      "da",
      "de",
      "el",
      "eo",
      "es",
      "et",
      "eu",
      "fa",
      "fi",
      "fr",
      "fy",
      "ga",
      "gl",
      "he",
      "hi",
      "hsb",
      "hr",
      "ht",
      "hu",
      "hy",
      "id",
      "is",
      "it",
      "ja",
      "ka",
      "kk",
      "kn",
      "ko",
      "ku",
      "la",
      "lb",
      "lo",
      "lt",
      "lv",
      "mk",
      "ml",
      "mr",
      "ms",
      "nds",
      "new",
      "nl",
      "nn",
      "no",
      "nv",
      "oc",
      "os",
      "pl",
      "pms",
      "pt",
      "ro",
      "ru",
      "sat",
      "sc",
      "sh",
      "sk",
      "sl",
      "sq",
      "sr",
      "sv",
      "sw",
      "ta",
      "te",
      "th",
      "tl",
      "tr",
      "uk",
      "vi",
      "vo",
      "zh",
      "zh-hans",
      "zh-hant"
   };
   public static final byte RESTRICTION_NO_RIGHT_TURN = 1;
   public static final byte RESTRICTION_NO_LEFT_TURN = 2;
   public static final byte RESTRICTION_NO_U_TURN = 3;
   public static final byte RESTRICTION_NO_STRAIGHT_ON = 4;
   public static final byte RESTRICTION_ONLY_RIGHT_TURN = 5;
   public static final byte RESTRICTION_ONLY_LEFT_TURN = 6;
   public static final byte RESTRICTION_ONLY_STRAIGHT_ON = 7;
   private static char TAG_DELIMETER = '/';
   private String resourceName = null;
   protected Map<String, MapRenderingTypes.MapRulType> types = null;
   protected List<MapRenderingTypes.MapRulType> typeList = new ArrayList<>();
   protected MapRenderingTypes.MapRulType nameRuleType;
   protected MapRenderingTypes.MapRulType nameEnRuleType;

   public MapRenderingTypes(String fileName) {
      this.resourceName = fileName;
   }

   public Map<String, MapRenderingTypes.MapRulType> getEncodingRuleTypes() {
      this.checkIfInitNeeded();
      return this.types;
   }

   protected void checkIfInitNeeded() {
      if (this.types == null) {
         this.types = new LinkedHashMap<>();
         this.typeList.clear();
         this.nameRuleType = MapRenderingTypes.MapRulType.createText("name");
         this.nameRuleType.order = 40;
         this.registerRuleType(this.nameRuleType);
         this.nameEnRuleType = MapRenderingTypes.MapRulType.createText("name:en");
         this.nameEnRuleType.order = 45;
         this.registerRuleType(this.nameEnRuleType);
         this.init();
      }
   }

   public static Collection<Map<String, String>> splitTagsIntoDifferentObjects(Map<String, String> tags) {
      boolean split = splitIsNeeded(tags);
      return (Collection<Map<String, String>>)(!split ? Collections.singleton(tags) : splitOpenSeaMapsTags(tags));
   }

   protected static boolean splitIsNeeded(Map<String, String> tags) {
      boolean seamark = false;

      for(String s : tags.keySet()) {
         if (s.startsWith("seamark:")) {
            seamark = true;
            break;
         }
      }

      return seamark;
   }

   private static Collection<Map<String, String>> splitOpenSeaMapsTags(Map<String, String> tags) {
      Map<String, Map<String, String>> groupByOpenSeamaps = new HashMap<>();
      Map<String, String> common = new HashMap<>();
      String ATTACHED_KEY = "seamark:attached";
      String type = "";

      for(Entry<String, String> entry : tags.entrySet()) {
         String s = entry.getKey();
         String value = entry.getValue();
         if (s.equals("seamark:type")) {
            type = value;
            common.put(ATTACHED_KEY, openSeaType(value));
         } else if (s.startsWith("seamark:")) {
            String stype = s.substring("seamark:".length());
            int ind = stype.indexOf(58);
            if (ind == -1) {
               common.put(s, value);
            } else {
               String group = openSeaType(stype.substring(0, ind));
               String add = stype.substring(ind + 1);
               if (!groupByOpenSeamaps.containsKey(group)) {
                  groupByOpenSeamaps.put(group, new HashMap());
               }

               groupByOpenSeamaps.get(group).put("seamark:" + add, value);
            }
         } else {
            common.put(s, value);
         }
      }

      List<Map<String, String>> res = new ArrayList<>();

      for(Entry<String, Map<String, String>> g : groupByOpenSeamaps.entrySet()) {
         g.getValue().putAll(common);
         g.getValue().put("seamark", g.getKey());
         if (openSeaType(type).equals(g.getKey())) {
            g.getValue().remove(ATTACHED_KEY);
            g.getValue().put("seamark", type);
            res.add(0, g.getValue());
         } else {
            res.add(g.getValue());
         }
      }

      return res;
   }

   private static String openSeaType(String value) {
      return !value.equals("light_major") && !value.equals("light_minor") ? value : "light";
   }

   public MapRenderingTypes.MapRulType getTypeByInternalId(int id) {
      return this.typeList.get(id);
   }

   private String lc(String a) {
      return a != null ? a.toLowerCase() : a;
   }

   protected MapRenderingTypes.MapRulType checkOrCreateTextRule(String targetTag, MapRenderingTypes.MapRulType ref) {
      MapRenderingTypes.MapRulType mt = this.types.get(constructRuleKey(targetTag, null));
      if (mt == null) {
         MapRenderingTypes.MapRulType ct = MapRenderingTypes.MapRulType.createText(targetTag, ref);
         mt = this.registerRuleType(ct);
      }

      return mt;
   }

   protected MapRenderingTypes.MapRulType checkOrMainRule(String tag, String value, int minzoom) {
      MapRenderingTypes.MapRulType mt = this.types.get(constructRuleKey(tag, value));
      if (mt == null) {
         mt = this.registerRuleType(MapRenderingTypes.MapRulType.createMainEntity(tag, value));
         mt.minzoom = minzoom;
         mt.maxzoom = 21;
      }

      return mt;
   }

   protected MapRenderingTypes.MapRulType checkOrCreateAdditional(String tag, String value, MapRenderingTypes.MapRulType ref) {
      MapRenderingTypes.MapRulType mt = this.types.get(constructRuleKey(tag, value));
      if (mt == null) {
         MapRenderingTypes.MapRulType ct = MapRenderingTypes.MapRulType.createAdditional(tag, value, ref);
         mt = this.registerRuleType(ct);
      }

      return mt;
   }

   protected MapRenderingTypes.MapRulType getRuleType(String tag, String val, boolean poi, boolean map) {
      Map<String, MapRenderingTypes.MapRulType> types = this.getEncodingRuleTypes();
      tag = this.lc(tag);
      val = this.lc(val);
      MapRenderingTypes.MapRulType rType = types.get(constructRuleKey(tag, val));
      if (rType == null || !rType.isPOI() && poi || !rType.isMap() && map) {
         rType = types.get(constructRuleKey(tag, null));
      }

      if (rType != null && (rType.isPOI() || !poi) && (rType.isMap() || !map)) {
         if (rType.isAdditional() && rType.tagValuePattern.value == null) {
            MapRenderingTypes.MapRulType var10 = MapRenderingTypes.MapRulType.createAdditional(tag, val);
            var10.additional = true;
            var10.order = rType.order;
            var10.map = rType.map;
            var10.poi = rType.poi;
            var10.onlyPoint = rType.onlyPoint;
            rType = this.registerRuleType(var10);
         }

         return rType;
      } else {
         return null;
      }
   }

   public MapRenderingTypes.MapRulType getNameRuleType() {
      this.getEncodingRuleTypes();
      return this.nameRuleType;
   }

   public MapRenderingTypes.MapRulType getNameEnRuleType() {
      this.getEncodingRuleTypes();
      return this.nameEnRuleType;
   }

   protected void init() {
      try {
         InputStream is;
         if (this.resourceName == null) {
            is = MapRenderingTypes.class.getResourceAsStream("rendering_types.xml");
         } else {
            is = new FileInputStream(this.resourceName);
         }

         long time = System.currentTimeMillis();
         XmlPullParser parser = PlatformUtil.newXMLPullParser();
         parser.setInput(is, "UTF-8");
         MapRenderingTypes.MapRulType parentCategory = null;

         int tok;
         while((tok = parser.next()) != 1) {
            if (tok == 2) {
               String name = parser.getName();
               if (name.equals("category")) {
                  parentCategory = this.parseCategoryFromXml(parser);
               } else if (name.equals("type")) {
                  this.parseAndRegisterTypeFromXML(parser, parentCategory);
               } else if (name.equals("routing_type")) {
                  this.parseRouteTagFromXML(parser);
               } else if (name.equals("entity_convert")) {
                  this.parseEntityConvertXML(parser);
               }
            }
         }

         log.info("Time to init " + (System.currentTimeMillis() - time));
         is.close();
      } catch (IOException var8) {
         log.error("Unexpected error", var8);
         var8.printStackTrace();
         throw new RuntimeException(var8);
      } catch (RuntimeException var9) {
         log.error("Unexpected error", var9);
         var9.printStackTrace();
         throw var9;
      } catch (XmlPullParserException var10) {
         log.error("Unexpected error", var10);
         var10.printStackTrace();
         throw new RuntimeException(var10);
      }
   }

   protected abstract void parseEntityConvertXML(XmlPullParser var1);

   protected abstract void parseRouteTagFromXML(XmlPullParser var1);

   protected abstract void parseAndRegisterTypeFromXML(XmlPullParser var1, MapRenderingTypes.MapRulType var2);

   protected MapRenderingTypes.MapRulType parseBaseRuleType(XmlPullParser parser, MapRenderingTypes.MapRulType parentCategory, String tag) {
      String value = this.lc(parser.getAttributeValue("", "value"));
      String additional = parser.getAttributeValue("", "additional");
      if (value != null && value.length() == 0) {
         value = null;
      }

      MapRenderingTypes.MapRulType rtype = MapRenderingTypes.MapRulType.createMainEntity(tag, value);
      if ("true".equals(additional)) {
         rtype = MapRenderingTypes.MapRulType.createAdditional(tag, value);
      } else if ("text".equals(additional)) {
         rtype = MapRenderingTypes.MapRulType.createText(tag);
      }

      rtype.map = "true".equals(parser.getAttributeValue("", "map"))
         || "yes".equals(parser.getAttributeValue("", "map"))
         || parser.getAttributeValue("", "map") == null;
      rtype.poi = "true".equals(parser.getAttributeValue("", "poi"))
         || "yes".equals(parser.getAttributeValue("", "poi"))
         || parser.getAttributeValue("", "poi") == null;
      String order = parser.getAttributeValue("", "order");
      if (!Algorithms.isEmpty(order)) {
         rtype.order = Integer.parseInt(order);
      } else if (parentCategory != null) {
         rtype.order = parentCategory.order;
      }

      rtype.category = parentCategory == null ? null : parentCategory.category;
      rtype.onlyPoint = Boolean.parseBoolean(parser.getAttributeValue("", "point"));
      rtype.relation = Boolean.parseBoolean(parser.getAttributeValue("", "relation"));
      rtype.relationGroup = Boolean.parseBoolean(parser.getAttributeValue("", "relationGroup"));
      if (rtype.isMain()) {
         if (rtype.relationGroup) {
            MapRenderingTypes.MapRulType mrt = MapRenderingTypes.MapRulType.createMainEntity(tag + "_" + value, null);
            mrt.order = rtype.order;
            mrt.category = rtype.category;
            mrt.poi = rtype.poi;
            mrt.map = rtype.map;
            this.registerMapRule(parser, mrt);
         }

         String groupSort = parser.getAttributeValue("", "relationGroupSort");
         if (groupSort != null) {
            rtype.relationSortTags = new LinkedHashMap<>();
            String[] ls = groupSort.split(";");

            for(String l : ls) {
               int sp = l.indexOf(61);
               String key = l;
               String[] values = new String[0];
               if (sp >= 0) {
                  key = l.substring(0, sp);
                  values = l.substring(sp + 1).split(",");
               }

               rtype.relationSortTags.put(key, Arrays.asList(values));
            }
         }

         String additionalTags = parser.getAttributeValue("", "additionalTags");
         String additionalPrefix = parser.getAttributeValue("", "additionalPrefix");
         if (additionalTags != null) {
            rtype.additionalTags = new LinkedHashMap<>();

            for(String tg : additionalTags.split(",")) {
               String targetTag = tg;
               if (!Algorithms.isEmpty(additionalPrefix)) {
                  targetTag = additionalPrefix + tg;
               }

               rtype.additionalTags.put(tg, targetTag);
            }
         }

         rtype.relationGroupPrefix = parser.getAttributeValue("", "relationGroupPrefix");
         String relationGroupAdditionalTags = parser.getAttributeValue("", "relationGroupAdditionalTags");
         if (relationGroupAdditionalTags != null) {
            rtype.relationGroupAdditionalTags = new LinkedHashMap<>();

            for(String tg : relationGroupAdditionalTags.split(",")) {
               rtype.relationGroupAdditionalTags.put(tg, tg);
            }
         }

         String nmts = parser.getAttributeValue("", "nameTags");
         if (nmts != null) {
            if (!rtype.relation && !rtype.relationGroup) {
               String[] nameSplit = nmts.split(",");

               for(String nameTag : nameSplit) {
                  this.checkOrCreateTextRule(nameTag, null);
               }
            } else {
               String namePrefix = parser.getAttributeValue("", "namePrefix");
               if (namePrefix == null) {
                  namePrefix = "";
               }

               rtype.relationNames = new LinkedHashMap<>();
               this.putNameTags(nmts, rtype.relationNames, namePrefix);
            }
         }

         String rnmts = parser.getAttributeValue("", "relationGroupNameTags");
         if (rnmts != null) {
            rtype.relationGroupNameTags = new LinkedHashMap<>();
            this.putNameTags(rnmts, rtype.relationGroupNameTags, "");
         }
      }

      return rtype;
   }

   private void putNameTags(String namesList, Map<String, String> names, String namePrefix) {
      if (namesList != null) {
         String[] nameSplit = namesList.split(",");

         for(int i = 0; i < nameSplit.length; ++i) {
            String tagName = nameSplit[i];
            String tagTargetName = tagName;
            if (namePrefix.length() > 0) {
               tagTargetName = namePrefix + tagName;
            }

            names.put(tagName, tagTargetName);

            for(String lng : langs) {
               names.put(tagName + ":" + lng, tagTargetName + ":" + lng);
            }
         }
      }
   }

   protected void registerMapRule(XmlPullParser parser, MapRenderingTypes.MapRulType rtype) {
      String val = parser.getAttributeValue("", "minzoom");
      if (rtype.isMain()) {
         rtype.minzoom = 15;
      }

      if (val != null) {
         rtype.minzoom = Integer.parseInt(val);
      }

      val = parser.getAttributeValue("", "maxzoom");
      rtype.maxzoom = 31;
      if (val != null) {
         rtype.maxzoom = Integer.parseInt(val);
      }

      this.registerRuleType(rtype);
   }

   protected MapRenderingTypes.MapRulType registerRuleType(MapRenderingTypes.MapRulType rt) {
      String tag = rt.tagValuePattern.tag;
      String val = rt.tagValuePattern.value;
      String keyVal = constructRuleKey(tag, val);
      if (this.types.containsKey(keyVal)) {
         MapRenderingTypes.MapRulType mapRulType = this.types.get(keyVal);
         if (!mapRulType.isAdditional() && !mapRulType.isText()) {
            throw new RuntimeException("Duplicate " + keyVal);
         } else {
            rt.id = mapRulType.id;
            if (rt.isMain()) {
               mapRulType.main = true;
               mapRulType.order = rt.order;
               if (rt.minzoom != 0) {
                  mapRulType.minzoom = Math.max(rt.minzoom, mapRulType.minzoom);
               }

               if (rt.maxzoom != 0) {
                  mapRulType.maxzoom = Math.min(rt.maxzoom, mapRulType.maxzoom);
               }
            }

            return mapRulType;
         }
      } else {
         rt.id = this.types.size();
         this.types.put(keyVal, rt);
         this.typeList.add(rt);
         return rt;
      }
   }

   protected MapRenderingTypes.MapRulType parseCategoryFromXml(XmlPullParser parser) {
      MapRenderingTypes.MapRulType rtype = new MapRenderingTypes.MapRulType();
      rtype.category = parser.getAttributeValue("", "name");
      if (!Algorithms.isEmpty(parser.getAttributeValue("", "order"))) {
         rtype.order = Integer.parseInt(parser.getAttributeValue("", "order"));
      }

      return rtype;
   }

   protected static String constructRuleKey(String tag, String val) {
      return val != null && val.length() != 0 ? tag + TAG_DELIMETER + val : tag;
   }

   protected static String getTagKey(String tagValue) {
      int i = tagValue.indexOf(TAG_DELIMETER);
      return i >= 0 ? tagValue.substring(0, i) : tagValue;
   }

   protected static String getValueKey(String tagValue) {
      int i = tagValue.indexOf(TAG_DELIMETER);
      return i >= 0 ? tagValue.substring(i + 1) : null;
   }

   public static String getRestrictionValue(int i) {
      switch(i) {
         case 1:
            return "NO_RIGHT_TURN".toLowerCase();
         case 2:
            return "NO_LEFT_TURN".toLowerCase();
         case 3:
            return "NO_U_TURN".toLowerCase();
         case 4:
            return "NO_STRAIGHT_ON".toLowerCase();
         case 5:
            return "ONLY_RIGHT_TURN".toLowerCase();
         case 6:
            return "ONLY_LEFT_TURN".toLowerCase();
         case 7:
            return "ONLY_STRAIGHT_ON".toLowerCase();
         default:
            return "unkonwn";
      }
   }

   public static class MapRulType {
      protected Map<String, String> relationNames;
      protected Map<String, String> additionalTags;
      protected Map<String, List<String>> relationSortTags;
      protected String relationGroupPrefix;
      protected Map<String, String> relationGroupNameTags;
      protected Map<String, String> relationGroupAdditionalTags;
      protected MapRenderingTypes.TagValuePattern tagValuePattern;
      protected boolean additional;
      protected boolean additionalText;
      protected boolean main;
      protected int order = 50;
      protected String category = null;
      protected boolean relation;
      protected boolean relationGroup;
      protected boolean map = true;
      protected boolean poi = true;
      protected int minzoom;
      protected int maxzoom;
      protected boolean onlyPoint;
      protected int id = -1;
      protected int freq;
      protected int targetId;
      protected int targetPoiId = -1;

      private MapRulType() {
      }

      private void copyMetadata(MapRenderingTypes.MapRulType ref) {
         this.minzoom = ref.minzoom;
         this.maxzoom = ref.maxzoom;
         this.order = ref.order;
         this.category = ref.category;
         this.onlyPoint = ref.onlyPoint;
      }

      public boolean isPOI() {
         return this.poi;
      }

      public boolean isMap() {
         return this.map;
      }

      public int getOrder() {
         return this.order;
      }

      public static MapRenderingTypes.MapRulType createMainEntity(String tag, String value) {
         MapRenderingTypes.MapRulType rt = new MapRenderingTypes.MapRulType();
         rt.tagValuePattern = new MapRenderingTypes.TagValuePattern(tag, value);
         rt.main = true;
         return rt;
      }

      public static MapRenderingTypes.MapRulType createText(String tag, MapRenderingTypes.MapRulType ref) {
         MapRenderingTypes.MapRulType rt = new MapRenderingTypes.MapRulType();
         rt.minzoom = 2;
         rt.maxzoom = 31;
         if (ref != null) {
            rt.copyMetadata(ref);
         }

         rt.additionalText = true;
         rt.tagValuePattern = new MapRenderingTypes.TagValuePattern(tag, null);
         return rt;
      }

      public static MapRenderingTypes.MapRulType createAdditional(String tag, String value, MapRenderingTypes.MapRulType ref) {
         MapRenderingTypes.MapRulType rt = new MapRenderingTypes.MapRulType();
         rt.minzoom = 2;
         rt.maxzoom = 31;
         if (ref != null) {
            rt.copyMetadata(ref);
         }

         rt.additional = true;
         rt.tagValuePattern = new MapRenderingTypes.TagValuePattern(tag, value);
         return rt;
      }

      public static MapRenderingTypes.MapRulType createText(String tag) {
         return createText(tag, null);
      }

      public static MapRenderingTypes.MapRulType createAdditional(String tag, String value) {
         return createAdditional(tag, value, null);
      }

      public String getTag() {
         return this.tagValuePattern.tag;
      }

      public int getTargetId() {
         return this.targetId;
      }

      public int getTargetPoiId() {
         return this.targetPoiId;
      }

      public void setTargetPoiId(int catId, int valueId) {
         if (catId <= 31) {
            this.targetPoiId = valueId << 6 | catId << 1;
         } else {
            if (catId > 32768) {
               throw new IllegalArgumentException("Refer source code");
            }

            this.targetPoiId = valueId << 16 | catId << 1 | 1;
         }
      }

      public int getInternalId() {
         return this.id;
      }

      public void setTargetId(int targetId) {
         this.targetId = targetId;
      }

      public String getValue() {
         return this.tagValuePattern.value;
      }

      public int getMinzoom() {
         return this.minzoom;
      }

      public boolean isAdditional() {
         return this.additional;
      }

      public boolean isAdditionalOrText() {
         return this.additional || this.additionalText;
      }

      public boolean isMain() {
         return this.main;
      }

      public boolean isText() {
         return this.additionalText;
      }

      public boolean isOnlyPoint() {
         return this.onlyPoint;
      }

      public int getFreq() {
         return this.freq;
      }

      public int updateFreq() {
         return ++this.freq;
      }

      @Override
      public String toString() {
         return this.getTag() + " " + this.getValue();
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         return 31 * result + this.id;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            MapRenderingTypes.MapRulType other = (MapRenderingTypes.MapRulType)obj;
            return this.id == other.id && this.id >= 0;
         }
      }
   }

   protected static class TagValuePattern {
      protected String tag;
      protected String value;
      protected String tagPrefix;
      protected int substrSt = 0;
      protected int substrEnd = 0;

      protected TagValuePattern(String t, String v) {
         this.tag = t;
         this.value = v;
         if (this.tag == null && this.value == null) {
            throw new IllegalStateException("Tag/value null should be handled differently");
         } else if (this.tag == null) {
            throw new UnsupportedOperationException();
         }
      }

      public boolean isApplicable(Map<String, String> e) {
         if (this.value == null) {
            return e.get(this.tag) != null;
         } else {
            return this.value.equals(e.get(this.tag));
         }
      }

      @Override
      public String toString() {
         return "tag=" + this.tag + " val=" + this.value;
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + (this.tag == null ? 0 : this.tag.hashCode());
         return 31 * result + (this.value == null ? 0 : this.value.hashCode());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            MapRenderingTypes.TagValuePattern other = (MapRenderingTypes.TagValuePattern)obj;
            if (this.tag == null) {
               if (other.tag != null) {
                  return false;
               }
            } else if (!this.tag.equals(other.tag)) {
               return false;
            }

            if (this.value == null) {
               if (other.value != null) {
                  return false;
               }
            } else if (!this.value.equals(other.value)) {
               return false;
            }

            return true;
         }
      }
   }
}
