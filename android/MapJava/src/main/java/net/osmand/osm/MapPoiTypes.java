package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.osmand.PlatformUtil;
import net.osmand.StringMatcher;
import net.osmand.data.Amenity;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MapPoiTypes {
   private static final String OTHER_MAP_CATEGORY = "Other";
   private static MapPoiTypes DEFAULT_INSTANCE = null;
   private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
   private String resourceName;
   private List<PoiCategory> categories = new ArrayList<>();
   private Set<String> forbiddenTypes = new HashSet<>();
   private PoiCategory otherCategory;
   public static final String WIKI_LANG = "wiki_lang";
   public static final String WIKI_PLACE = "wiki_place";
   public static final String OSM_WIKI_CATEGORY = "osmwiki";
   public static final String SPEED_CAMERA = "speed_camera";
   public static final String ROUTES = "routes";
   public static final String ROUTE_ARTICLE = "route_article";
   public static final String ROUTE_ARTICLE_POINT = "route_article_point";
   public static final String CATEGORY = "category";
   public static final String ROUTE_TRACK = "route_track";
   public static final String ROUTE_TRACK_POINT = "route_track_point";
   private MapPoiTypes.PoiTranslator poiTranslator = null;
   private boolean init;
   Map<String, PoiType> poiTypesByTag = new LinkedHashMap<>();
   Map<String, String> deprecatedTags = new LinkedHashMap<>();
   Map<String, String> poiAdditionalCategoryIconNames = new LinkedHashMap<>();
   List<PoiType> textPoiAdditionals = new ArrayList<>();

   public static final String TOP_INDEX_ADDITIONAL_PREFIX = "top_index_";

   public MapPoiTypes(String fileName) {
      this.resourceName = fileName;
   }

   public static MapPoiTypes getDefaultNoInit() {
      if (DEFAULT_INSTANCE == null) {
         DEFAULT_INSTANCE = new MapPoiTypes(null);
      }

      return DEFAULT_INSTANCE;
   }

   public static void setDefault(MapPoiTypes types) {
      DEFAULT_INSTANCE = types;
      DEFAULT_INSTANCE.init();
   }

   public static MapPoiTypes getDefault() {
      if (DEFAULT_INSTANCE == null) {
         DEFAULT_INSTANCE = new MapPoiTypes(null);
         DEFAULT_INSTANCE.init();
      }

      return DEFAULT_INSTANCE;
   }

   public boolean isInit() {
      return this.init;
   }

   public PoiCategory getOtherPoiCategory() {
      return this.otherCategory;
   }

   public PoiCategory getOtherMapCategory() {
      return this.getPoiCategoryByName("Other", true);
   }

   public String getPoiAdditionalCategoryIconName(String category) {
      return this.poiAdditionalCategoryIconNames.get(category);
   }

   public List<PoiType> getTextPoiAdditionals() {
      return this.textPoiAdditionals;
   }

   public List<AbstractPoiType> getTopVisibleFilters() {
      List<AbstractPoiType> lf = new ArrayList<>();

      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory pc = this.categories.get(i);
         if (pc.isTopVisible()) {
            lf.add(pc);
         }

         for(PoiFilter p : pc.getPoiFilters()) {
            if (p.isTopVisible()) {
               lf.add(p);
            }
         }

         for(PoiType p : pc.getPoiTypes()) {
            if (p.isTopVisible()) {
               lf.add(p);
            }
         }
      }

      this.sortList(lf);
      return lf;
   }

   public PoiCategory getOsmwiki() {
      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory category = this.categories.get(i);
         if (category.isWiki()) {
            return category;
         }
      }

      return null;
   }

   public PoiCategory getRoutes() {
      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory category = this.categories.get(i);
         if (category.isRoutes()) {
            return category;
         }
      }

      return null;
   }

   public List<String> getAllAvailableWikiLocales() {
      List<String> availableWikiLocales = new ArrayList<>();

      for(PoiType type : this.getOsmwiki().getPoiTypeByKeyName("wiki_place").getPoiAdditionals()) {
         String name = type.getKeyName();
         String wikiLang = "wiki_lang:";
         if (name != null && name.startsWith(wikiLang)) {
            String locale = name.substring(wikiLang.length());
            availableWikiLocales.add(locale);
         }
      }

      return availableWikiLocales;
   }

   private void sortList(List<? extends AbstractPoiType> lf) {
      final Collator instance = Collator.getInstance();
      Collections.sort(lf, new Comparator<AbstractPoiType>() {
         public int compare(AbstractPoiType object1, AbstractPoiType object2) {
            return instance.compare(object1.getTranslation(), object2.getTranslation());
         }
      });
   }

   public PoiCategory getUserDefinedCategory() {
      return this.otherCategory;
   }

   public PoiType getPoiTypeByKey(String name) {
      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory pc = this.categories.get(i);
         PoiType pt = pc.getPoiTypeByKeyName(name);
         if (pt != null && !pt.isReference()) {
            return pt;
         }
      }

      return null;
   }

   public PoiType getPoiTypeByKeyInCategory(PoiCategory category, String keyName) {
      return category != null ? category.getPoiTypeByKeyName(keyName) : null;
   }

   public AbstractPoiType getAnyPoiTypeByKey(String name) {
      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory pc = this.categories.get(i);
         if (pc.getKeyName().equals(name)) {
            return pc;
         }

         for(PoiFilter pf : pc.getPoiFilters()) {
            if (pf.getKeyName().equals(name)) {
               return pf;
            }
         }

         PoiType pt = pc.getPoiTypeByKeyName(name);
         if (pt != null && !pt.isReference()) {
            return pt;
         }
      }

      return null;
   }

   public Map<String, PoiType> getAllTranslatedNames(boolean skipNonEditable) {
      Map<String, PoiType> translation = new HashMap<>();

      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory pc = this.categories.get(i);
         if (!skipNonEditable || !pc.isNotEditableOsm()) {
            this.addPoiTypesTranslation(skipNonEditable, translation, pc);
         }
      }

      return translation;
   }

   private void addPoiTypesTranslation(boolean skipNonEditable, Map<String, PoiType> translation, PoiFilter pf) {
      for(PoiType pt : pf.getPoiTypes()) {
         if (!pt.isReference() && pt.getBaseLangType() == null && (!skipNonEditable || !pt.isNotEditableOsm())) {
            translation.put(pt.getKeyName().replace('_', ' ').toLowerCase(), pt);
            translation.put(pt.getTranslation().toLowerCase(), pt);
         }
      }
   }

   public List<AbstractPoiType> getAllTypesTranslatedNames(StringMatcher matcher) {
      List<AbstractPoiType> tm = new ArrayList<>();

      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory pc = this.categories.get(i);
         if (pc != this.getOtherMapCategory()) {
            this.addIf(tm, pc, matcher);

            for(PoiFilter pt : pc.getPoiFilters()) {
               this.addIf(tm, pt, matcher);
            }

            for(PoiType pt : pc.getPoiTypes()) {
               if (!pt.isReference()) {
                  this.addIf(tm, pt, matcher);
               }
            }
         }
      }

      return tm;
   }

   private void addIf(List<AbstractPoiType> tm, AbstractPoiType pc, StringMatcher matcher) {
      if (matcher.matches(pc.getTranslation()) || matcher.matches(pc.getKeyName().replace('_', ' '))) {
         tm.add(pc);
      }

      List<PoiType> additionals = pc.getPoiAdditionals();
      if (additionals != null) {
         for(PoiType a : additionals) {
            this.addIf(tm, a, matcher);
         }
      }
   }

   public Map<String, PoiType> getAllTranslatedNames(PoiCategory pc, boolean onlyTranslation) {
      Map<String, PoiType> translation = new TreeMap<>();

      for(PoiType pt : pc.getPoiTypes()) {
         translation.put(pt.getTranslation(), pt);
         if (!onlyTranslation) {
            translation.put(Algorithms.capitalizeFirstLetterAndLowercase(pt.getKeyName().replace('_', ' ')), pt);
         }
      }

      return translation;
   }

   public PoiCategory getPoiCategoryByName(String name) {
      return this.getPoiCategoryByName(name, false);
   }

   public PoiCategory getPoiCategoryByName(String name, boolean create) {
      if (name.equals("leisure") && !create) {
         name = "entertainment";
      }

      if (name.equals("historic") && !create) {
         name = "tourism";
      }

      for(PoiCategory p : this.categories) {
         if (p.getKeyName().equalsIgnoreCase(name)) {
            return p;
         }
      }

      if (create) {
         PoiCategory lastCategory = new PoiCategory(this, name, this.categories.size());
         if (!lastCategory.getKeyName().equals("Other")) {
            lastCategory.setTopVisible(true);
         }

         this.addCategory(lastCategory);
         return lastCategory;
      } else {
         return this.otherCategory;
      }
   }

   private void addCategory(PoiCategory category) {
      List<PoiCategory> categories = new ArrayList<>(this.categories);
      categories.add(category);
      this.categories = categories;
   }

   public MapPoiTypes.PoiTranslator getPoiTranslator() {
      return this.poiTranslator;
   }

   public void setPoiTranslator(MapPoiTypes.PoiTranslator poiTranslator) {
      this.poiTranslator = poiTranslator;
      List<PoiCategory> categories = new ArrayList<>(this.categories);
      this.sortList(categories);
      this.categories = categories;
   }

   public void init() {
      this.init(null);
   }

   public void init(String resourceName) {
      System.out.println("Initializing poi types: " + resourceName);
      if (resourceName != null) {
         this.resourceName = resourceName;
      }

      try {
         InputStream is;
         if (this.resourceName == null) {
            is = MapPoiTypes.class.getResourceAsStream("poi_types.xml");
         } else {
            is = new FileInputStream(this.resourceName);
         }

         System.out.println("Initializing poi types: " + is);
         this.initFromInputStream(is);
      } catch (IOException var3) {
         log.error("Unexpected error", var3);
         var3.printStackTrace();
         throw new RuntimeException(var3);
      } catch (RuntimeException var4) {
         log.error("Unexpected error", var4);
         var4.printStackTrace();
         throw var4;
      }
   }

   public void initFromInputStream(InputStream is) {
      long time = System.currentTimeMillis();
      List<PoiType> referenceTypes = new ArrayList<>();
      Map<String, PoiType> allTypes = new LinkedHashMap<>();
      Map<String, List<PoiType>> categoryPoiAdditionalMap = new LinkedHashMap<>();
      Map<AbstractPoiType, Set<String>> abstractTypeAdditionalCategories = new LinkedHashMap<>();
      Map<String, PoiType> poiTypesByTag = new LinkedHashMap<>();
      Map<String, String> deprecatedTags = new LinkedHashMap<>();
      Map<String, String> poiAdditionalCategoryIconNames = new LinkedHashMap<>();
      List<PoiType> textPoiAdditionals = new ArrayList<>();
      List<PoiCategory> categoriesList = new ArrayList<>();

      try {
         XmlPullParser parser = PlatformUtil.newXMLPullParser();
         parser.setInput(is, "UTF-8");
         PoiCategory lastCategory = null;
         Set<String> lastCategoryPoiAdditionalsCategories = new TreeSet<>();
         PoiFilter lastFilter = null;
         Set<String> lastFilterPoiAdditionalsCategories = new TreeSet<>();
         PoiType lastType = null;
         Set<String> lastTypePoiAdditionalsCategories = new TreeSet<>();
         String lastPoiAdditionalCategory = null;
         PoiCategory localOtherMapCategory = new PoiCategory(this, "Other", categoriesList.size());
         categoriesList.add(localOtherMapCategory);

         int tok;
         while((tok = parser.next()) != 1) {
            if (tok == 2) {
               String name = parser.getName();
               if (name.equals("poi_category")) {
                  lastCategory = new PoiCategory(this, parser.getAttributeValue("", "name"), categoriesList.size());
                  lastCategory.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
                  lastCategory.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
                  lastCategory.setDefaultTag(parser.getAttributeValue("", "default_tag"));
                  if (!Algorithms.isEmpty(parser.getAttributeValue("", "poi_additional_category"))) {
                     Collections.addAll(lastCategoryPoiAdditionalsCategories, parser.getAttributeValue("", "poi_additional_category").split(","));
                  }

                  if (!Algorithms.isEmpty(parser.getAttributeValue("", "excluded_poi_additional_category"))) {
                     lastCategory.addExcludedPoiAdditionalCategories(parser.getAttributeValue("", "excluded_poi_additional_category").split(","));
                     lastCategoryPoiAdditionalsCategories.removeAll(lastCategory.getExcludedPoiAdditionalCategories());
                  }

                  categoriesList.add(lastCategory);
               } else if (name.equals("poi_filter")) {
                  PoiFilter tp = new PoiFilter(this, lastCategory, parser.getAttributeValue("", "name"));
                  tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
                  lastFilter = tp;
                  lastFilterPoiAdditionalsCategories.addAll(lastCategoryPoiAdditionalsCategories);
                  if (!Algorithms.isEmpty(parser.getAttributeValue("", "poi_additional_category"))) {
                     Collections.addAll(lastFilterPoiAdditionalsCategories, parser.getAttributeValue("", "poi_additional_category").split(","));
                  }

                  if (!Algorithms.isEmpty(parser.getAttributeValue("", "excluded_poi_additional_category"))) {
                     tp.addExcludedPoiAdditionalCategories(parser.getAttributeValue("", "excluded_poi_additional_category").split(","));
                     lastFilterPoiAdditionalsCategories.removeAll(tp.getExcludedPoiAdditionalCategories());
                  }

                  lastCategory.addPoiType(tp);
               } else if (name.equals("poi_reference")) {
                  PoiType tp = new PoiType(this, lastCategory, lastFilter, parser.getAttributeValue("", "name"));
                  referenceTypes.add(tp);
                  tp.setReferenceType(tp);
                  if (lastFilter != null) {
                     lastFilter.addPoiType(tp);
                  }

                  lastCategory.addPoiType(tp);
               } else if (name.equals("poi_additional")) {
                  if (lastCategory == null) {
                     lastCategory = localOtherMapCategory;
                  }

                  PoiType baseType = this.parsePoiAdditional(
                     parser, lastCategory, lastFilter, lastType, null, null, lastPoiAdditionalCategory, textPoiAdditionals
                  );
                  if ("true".equals(parser.getAttributeValue("", "lang"))) {
                     for(String lng : MapRenderingTypes.langs) {
                        this.parsePoiAdditional(parser, lastCategory, lastFilter, lastType, lng, baseType, lastPoiAdditionalCategory, textPoiAdditionals);
                     }

                     this.parsePoiAdditional(parser, lastCategory, lastFilter, lastType, "en", baseType, lastPoiAdditionalCategory, textPoiAdditionals);
                  }

                  if (lastPoiAdditionalCategory != null) {
                     List<PoiType> categoryAdditionals = categoryPoiAdditionalMap.get(lastPoiAdditionalCategory);
                     if (categoryAdditionals == null) {
                        categoryAdditionals = new ArrayList<>();
                        categoryPoiAdditionalMap.put(lastPoiAdditionalCategory, categoryAdditionals);
                     }

                     categoryAdditionals.add(baseType);
                  }
               } else if (name.equals("poi_additional_category")) {
                  if (lastPoiAdditionalCategory == null) {
                     lastPoiAdditionalCategory = parser.getAttributeValue("", "name");
                     String icon = parser.getAttributeValue("", "icon");
                     if (!Algorithms.isEmpty(icon)) {
                        poiAdditionalCategoryIconNames.put(lastPoiAdditionalCategory, icon);
                     }
                  }
               } else if (name.equals("poi_type")) {
                  if (lastCategory == null) {
                     lastCategory = localOtherMapCategory;
                  }

                  if (!Algorithms.isEmpty(parser.getAttributeValue("", "deprecated_of"))) {
                     String vl = parser.getAttributeValue("", "name");
                     String target = parser.getAttributeValue("", "deprecated_of");
                     deprecatedTags.put(vl, target);
                  } else {
                     lastType = this.parsePoiType(allTypes, parser, lastCategory, lastFilter, null, null);
                     if ("true".equals(parser.getAttributeValue("", "lang"))) {
                        for(String lng : MapRenderingTypes.langs) {
                           this.parsePoiType(allTypes, parser, lastCategory, lastFilter, lng, lastType);
                        }
                     }

                     lastTypePoiAdditionalsCategories.addAll(lastCategoryPoiAdditionalsCategories);
                     lastTypePoiAdditionalsCategories.addAll(lastFilterPoiAdditionalsCategories);
                     if (!Algorithms.isEmpty(parser.getAttributeValue("", "poi_additional_category"))) {
                        Collections.addAll(lastTypePoiAdditionalsCategories, parser.getAttributeValue("", "poi_additional_category").split(","));
                     }

                     if (!Algorithms.isEmpty(parser.getAttributeValue("", "excluded_poi_additional_category"))) {
                        lastType.addExcludedPoiAdditionalCategories(parser.getAttributeValue("", "excluded_poi_additional_category").split(","));
                        lastTypePoiAdditionalsCategories.removeAll(lastType.getExcludedPoiAdditionalCategories());
                     }
                  }
               }
            } else if (tok == 3) {
               String name = parser.getName();
               if (name.equals("poi_filter")) {
                  if (lastFilterPoiAdditionalsCategories.size() > 0) {
                     abstractTypeAdditionalCategories.put(lastFilter, lastFilterPoiAdditionalsCategories);
                     lastFilterPoiAdditionalsCategories = new TreeSet<>();
                  }

                  lastFilter = null;
               } else if (name.equals("poi_type")) {
                  if (lastTypePoiAdditionalsCategories.size() > 0) {
                     abstractTypeAdditionalCategories.put(lastType, lastTypePoiAdditionalsCategories);
                     lastTypePoiAdditionalsCategories = new TreeSet<>();
                  }

                  lastType = null;
               } else if (name.equals("poi_category")) {
                  if (lastCategoryPoiAdditionalsCategories.size() > 0) {
                     abstractTypeAdditionalCategories.put(lastCategory, lastCategoryPoiAdditionalsCategories);
                     lastCategoryPoiAdditionalsCategories = new TreeSet<>();
                  }

                  lastCategory = null;
               } else if (name.equals("poi_additional_category")) {
                  lastPoiAdditionalCategory = null;
               }
            }
         }

         is.close();
      } catch (IOException var29) {
         log.error("Unexpected error", var29);
         var29.printStackTrace();
         throw new RuntimeException(var29);
      } catch (RuntimeException var30) {
         log.error("Unexpected error", var30);
         var30.printStackTrace();
         throw var30;
      } catch (XmlPullParserException var31) {
         log.error("Unexpected error", var31);
         var31.printStackTrace();
         throw new RuntimeException(var31);
      }

      for(PoiType gt : referenceTypes) {
         PoiType pt = allTypes.get(gt.getKeyName());
         if (pt == null || pt.getOsmTag() == null) {
            throw new IllegalStateException("Can't find poi type for poi reference '" + gt.keyName + "'");
         }

         gt.setReferenceType(pt);
      }

      for(Entry<AbstractPoiType, Set<String>> entry : abstractTypeAdditionalCategories.entrySet()) {
         for(String category : entry.getValue()) {
            List<PoiType> poiAdditionals = categoryPoiAdditionalMap.get(category);
            if (poiAdditionals != null) {
               for(PoiType poiType : poiAdditionals) {
                  this.buildPoiAdditionalReference(poiType, entry.getKey(), textPoiAdditionals);
               }
            }
         }
      }

      this.categories = categoriesList;
      this.poiTypesByTag = poiTypesByTag;
      this.deprecatedTags = deprecatedTags;
      this.poiAdditionalCategoryIconNames = poiAdditionalCategoryIconNames;
      this.textPoiAdditionals = textPoiAdditionals;
      this.otherCategory = this.getPoiCategoryByName("user_defined_other");
      if (this.otherCategory == null) {
         throw new IllegalArgumentException("No poi category other");
      } else {
         this.init = true;
         log.info("Time to init poi types " + (System.currentTimeMillis() - time));
      }
   }

   private PoiType buildPoiAdditionalReference(PoiType poiAdditional, AbstractPoiType parent, List<PoiType> textPoiAdditionals) {
      PoiCategory lastCategory = null;
      PoiFilter lastFilter = null;
      PoiType lastType = null;
      PoiType ref = null;
      if (parent instanceof PoiCategory) {
         lastCategory = (PoiCategory)parent;
         ref = new PoiType(this, lastCategory, null, poiAdditional.getKeyName());
      } else if (parent instanceof PoiFilter) {
         lastFilter = (PoiFilter)parent;
         ref = new PoiType(this, lastFilter.getPoiCategory(), lastFilter, poiAdditional.getKeyName());
      } else if (parent instanceof PoiType) {
         lastType = (PoiType)parent;
         ref = new PoiType(this, lastType.getCategory(), lastType.getFilter(), poiAdditional.getKeyName());
      }

      if (ref == null) {
         return null;
      } else {
         if (poiAdditional.isReference()) {
            ref.setReferenceType(poiAdditional.getReferenceType());
         } else {
            ref.setReferenceType(poiAdditional);
         }

         ref.setBaseLangType(poiAdditional.getBaseLangType());
         ref.setLang(poiAdditional.getLang());
         ref.setAdditional((AbstractPoiType)(lastType != null ? lastType : (lastFilter != null ? lastFilter : lastCategory)));
         ref.setTopVisible(poiAdditional.isTopVisible());
         ref.setText(poiAdditional.isText());
         ref.setOrder(poiAdditional.getOrder());
         ref.setOsmTag(poiAdditional.getOsmTag());
         ref.setNotEditableOsm(poiAdditional.isNotEditableOsm());
         ref.setOsmValue(poiAdditional.getOsmValue());
         ref.setOsmTag2(poiAdditional.getOsmTag2());
         ref.setOsmValue2(poiAdditional.getOsmValue2());
         ref.setPoiAdditionalCategory(poiAdditional.getPoiAdditionalCategory());
         ref.setFilterOnly(poiAdditional.isFilterOnly());
         if (lastType != null) {
            lastType.addPoiAdditional(ref);
         } else if (lastFilter != null) {
            lastFilter.addPoiAdditional(ref);
         } else if (lastCategory != null) {
            lastCategory.addPoiAdditional(ref);
         }

         if (ref.isText()) {
            textPoiAdditionals.add(ref);
         }

         return ref;
      }
   }

   private PoiType parsePoiAdditional(
      XmlPullParser parser,
      PoiCategory lastCategory,
      PoiFilter lastFilter,
      PoiType lastType,
      String lang,
      PoiType langBaseType,
      String poiAdditionalCategory,
      List<PoiType> textPoiAdditionals
   ) {
      String oname = parser.getAttributeValue("", "name");
      if (lang != null) {
         oname = oname + ":" + lang;
      }

      String otag = parser.getAttributeValue("", "tag");
      if (lang != null) {
         otag = otag + ":" + lang;
      }

      PoiType tp = new PoiType(this, lastCategory, lastFilter, oname);
      tp.setBaseLangType(langBaseType);
      tp.setLang(lang);
      tp.setAdditional((AbstractPoiType)(lastType != null ? lastType : (lastFilter != null ? lastFilter : lastCategory)));
      tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
      tp.setText("text".equals(parser.getAttributeValue("", "type")));
      String orderStr = parser.getAttributeValue("", "order");
      if (!Algorithms.isEmpty(orderStr)) {
         tp.setOrder(Integer.parseInt(orderStr));
      }

      tp.setOsmTag(otag);
      tp.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
      tp.setOsmValue(parser.getAttributeValue("", "value"));
      tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
      tp.setOsmValue2(parser.getAttributeValue("", "value2"));
      tp.setPoiAdditionalCategory(poiAdditionalCategory);
      tp.setFilterOnly(Boolean.parseBoolean(parser.getAttributeValue("", "filter_only")));
      if (lastType != null) {
         lastType.addPoiAdditional(tp);
      } else if (lastFilter != null) {
         lastFilter.addPoiAdditional(tp);
      } else if (lastCategory != null) {
         lastCategory.addPoiAdditional(tp);
      }

      if (tp.isText()) {
         textPoiAdditionals.add(tp);
      }

      return tp;
   }

   private PoiType parsePoiType(
      Map<String, PoiType> allTypes, XmlPullParser parser, PoiCategory lastCategory, PoiFilter lastFilter, String lang, PoiType langBaseType
   ) {
      String oname = parser.getAttributeValue("", "name");
      if (lang != null) {
         oname = oname + ":" + lang;
      }

      PoiType tp = new PoiType(this, lastCategory, lastFilter, oname);
      String otag = parser.getAttributeValue("", "tag");
      if (lang != null) {
         otag = otag + ":" + lang;
      }

      tp.setBaseLangType(langBaseType);
      tp.setLang(lang);
      tp.setOsmTag(otag);
      tp.setOsmValue(parser.getAttributeValue("", "value"));
      tp.setOsmEditTagValue(parser.getAttributeValue("", "edit_tag"), parser.getAttributeValue("", "edit_value"));
      tp.setOsmEditTagValue2(parser.getAttributeValue("", "edit_tag2"), parser.getAttributeValue("", "edit_value2"));
      tp.setOsmTag2(parser.getAttributeValue("", "tag2"));
      tp.setOsmValue2(parser.getAttributeValue("", "value2"));
      tp.setText("text".equals(parser.getAttributeValue("", "type")));
      String orderStr = parser.getAttributeValue("", "order");
      if (!Algorithms.isEmpty(orderStr)) {
         tp.setOrder(Integer.parseInt(orderStr));
      }

      tp.setNameOnly("true".equals(parser.getAttributeValue("", "name_only")));
      tp.setNameTag(parser.getAttributeValue("", "name_tag"));
      tp.setRelation("true".equals(parser.getAttributeValue("", "relation")));
      tp.setNotEditableOsm("true".equals(parser.getAttributeValue("", "no_edit")));
      tp.setTopVisible(Boolean.parseBoolean(parser.getAttributeValue("", "top")));
      if (lastFilter != null) {
         lastFilter.addPoiType(tp);
      }

      allTypes.put(tp.getKeyName(), tp);
      lastCategory.addPoiType(tp);
      if ("true".equals(parser.getAttributeValue("", "basemap"))) {
         lastCategory.addBasemapPoi(tp);
      }

      return tp;
   }

   public List<PoiCategory> getCategories(boolean includeMapCategory) {
      ArrayList<PoiCategory> lst = new ArrayList<>(this.categories);
      if (!includeMapCategory) {
         lst.remove(this.getOtherMapCategory());
      }

      return lst;
   }

   private static void print(MapPoiTypes df) {
      for(PoiCategory p : df.getCategories(true)) {
         System.out.println("Category " + p.getKeyName());

         for(PoiFilter f : p.getPoiFilters()) {
            System.out.println(" Filter " + f.getKeyName());
            print("  ", f);
         }

         print(" ", p);
      }
   }

   private PoiType getPoiAdditionalByKey(AbstractPoiType p, String name) {
      List<PoiType> pp = p.getPoiAdditionals();
      if (pp != null) {
         for(PoiType pt : pp) {
            if (pt.getKeyName().equals(name)) {
               return pt;
            }
         }
      }

      return null;
   }

   public PoiType getTextPoiAdditionalByKey(String name) {
      for(PoiType pt : this.textPoiAdditionals) {
         if (pt.getKeyName().equals(name)) {
            return pt;
         }
      }

      return null;
   }

   public AbstractPoiType getAnyPoiAdditionalTypeByKey(String name) {
      PoiType add = null;

      for(int i = 0; i < this.categories.size(); ++i) {
         PoiCategory pc = this.categories.get(i);
         add = this.getPoiAdditionalByKey(pc, name);
         if (add != null) {
            return add;
         }

         for(PoiFilter pf : pc.getPoiFilters()) {
            add = this.getPoiAdditionalByKey(pf, name);
            if (add != null) {
               return add;
            }
         }

         for(PoiType p : pc.getPoiTypes()) {
            add = this.getPoiAdditionalByKey(p, name);
            if (add != null) {
               return add;
            }
         }
      }

      return null;
   }

   private static void print(String indent, PoiFilter f) {
      for(PoiType pt : f.getPoiTypes()) {
         System.out.println(indent + " Type " + pt.getKeyName() + (pt.isReference() ? " -> " + pt.getReferenceType().getCategory().getKeyName() : ""));
      }
   }

   public static void main(String[] args) {
      DEFAULT_INSTANCE = new MapPoiTypes("/Users/victorshcherb/osmand/repos/resources/poi/poi_types.xml");
      DEFAULT_INSTANCE.init();

      for(AbstractPoiType l : DEFAULT_INSTANCE.getTopVisibleFilters()) {
         System.out.println("----------------- " + l.getKeyName());
         Map var4 = l.putTypes(new LinkedHashMap());
      }
   }

   public String getSynonyms(AbstractPoiType abstractPoiType) {
      if (this.poiTranslator != null) {
         String translation = this.poiTranslator.getSynonyms(abstractPoiType);
         if (!Algorithms.isEmpty(translation)) {
            return translation;
         }
      }

      return "";
   }

   public String getEnTranslation(AbstractPoiType abstractPoiType) {
      if (this.poiTranslator != null) {
         String translation = this.poiTranslator.getEnTranslation(abstractPoiType);
         if (!Algorithms.isEmpty(translation)) {
            return translation;
         }
      }

      return this.getBasePoiName(abstractPoiType);
   }

   public String getTranslation(AbstractPoiType abstractPoiType) {
      if (this.poiTranslator != null) {
         String translation = this.poiTranslator.getTranslation(abstractPoiType);
         if (!Algorithms.isEmpty(translation)) {
            return translation;
         }
      }

      return this.getBasePoiName(abstractPoiType);
   }

   public String getTranslationLanguage() {
      if (poiTranslator == null) {
         return null;
      } else {
         return poiTranslator.getTranslatorLanguage();
      }
   }

   public String getAllLanguagesTranslationSuffix() {
      return this.poiTranslator != null ? this.poiTranslator.getAllLanguagesTranslationSuffix() : "all languages";
   }

   public String getBasePoiName(AbstractPoiType abstractPoiType) {
      String name = abstractPoiType.getKeyName();
      if (name.startsWith("osmand_")) {
         name = name.substring("osmand_".length());
      }

      if (name.startsWith("amenity_")) {
         name = name.substring("amenity_".length());
      }

      name = name.replace('_', ' ');
      return Algorithms.capitalizeFirstLetterAndLowercase(name);
   }

   public String getPoiTranslation(String keyName) {
      if (this.poiTranslator != null) {
         String translation = this.poiTranslator.getTranslation(keyName);
         if (!Algorithms.isEmpty(translation)) {
            return translation;
         }
      }

      String var3 = keyName.replace('_', ' ');
      return Algorithms.capitalizeFirstLetter(var3);
   }

   public boolean isRegisteredType(PoiCategory t) {
      return this.getPoiCategoryByName(t.getKeyName()) != this.otherCategory;
   }

   public void initPoiTypesByTag() {
      if (this.poiTypesByTag.isEmpty()) {
         for(int i = 0; i < this.categories.size(); ++i) {
            PoiCategory poic = this.categories.get(i);

            for(PoiType p : poic.getPoiTypes()) {
               this.initPoiType(p);

               for(PoiType pts : p.getPoiAdditionals()) {
                  this.initPoiType(pts);
               }
            }

            for(PoiType p : poic.getPoiAdditionals()) {
               this.initPoiType(p);
            }
         }
      }
   }

   private void initPoiType(PoiType p) {
      if (!p.isReference()) {
         String key = null;
         if (p.isAdditional()) {
            key = p.isText() ? p.getRawOsmTag() : p.getRawOsmTag() + "/" + p.getOsmValue();
         } else {
            key = p.getRawOsmTag() + "/" + p.getOsmValue();
         }

         if (this.poiTypesByTag.containsKey(key)) {
            throw new UnsupportedOperationException("!! Duplicate poi type " + key);
         }

         this.poiTypesByTag.put(key, p);
      }
   }

   public String replaceDeprecatedSubtype(PoiCategory type, String subtype) {
      return this.deprecatedTags.containsKey(subtype) ? this.deprecatedTags.get(subtype) : subtype;
   }

   public Amenity parseAmenity(String tag, String val, boolean relation, Map<String, String> otherTags) {
      this.initPoiTypesByTag();
      PoiType pt = this.poiTypesByTag.get(tag + "/" + val);
      if (pt == null) {
         pt = this.poiTypesByTag.get(tag);
      }

      if (pt != null && !pt.isAdditional()) {
         if (!Algorithms.isEmpty(pt.getOsmTag2()) && !Algorithms.objectEquals(otherTags.get(pt.getOsmTag2()), pt.getOsmValue2())) {
            return null;
         } else if (pt.getCategory() == this.getOtherMapCategory()) {
            return null;
         } else {
            String nameValue = otherTags.get("name");
            if (pt.getNameTag() != null) {
               nameValue = otherTags.get(pt.getNameTag());
            }

            boolean hasName = !Algorithms.isEmpty(nameValue);
            if (!hasName && pt.isNameOnly()) {
               return null;
            } else if (relation && !pt.isRelation()) {
               return null;
            } else {
               Amenity a = new Amenity();
               a.setType(pt.getCategory());
               a.setSubType(pt.getKeyName());
               if (pt.getNameTag() != null) {
                  a.setName(nameValue);
               }

               for(Entry<String, String> e : otherTags.entrySet()) {
                  String otag = e.getKey();
                  if (!otag.equals(tag) && !otag.equals("name")) {
                     PoiType pat = this.poiTypesByTag.get(otag + "/" + (String)e.getValue());
                     if (pat == null) {
                        for(String splValue : e.getValue().split(";")) {
                           PoiType ps = this.poiTypesByTag.get(otag + "/" + splValue.trim());
                           if (ps != null) {
                              a.setAdditionalInfo(ps.getKeyName(), splValue.trim());
                           }
                        }

                        pat = this.poiTypesByTag.get(otag);
                     }

                     if (pat != null && pat.isAdditional()) {
                        a.setAdditionalInfo(pat.getKeyName(), e.getValue());
                     }
                  }
               }

               return a;
            }
         }
      } else {
         return null;
      }
   }

   public boolean isTextAdditionalInfo(String key, String value) {
      if (!key.startsWith("name:") && !key.equals("name")) {
         PoiType pat = (PoiType)this.getAnyPoiAdditionalTypeByKey(key);
         return pat == null ? true : pat.isText();
      } else {
         return true;
      }
   }

   public void setForbiddenTypes(Set<String> forbiddenTypes) {
      this.forbiddenTypes = forbiddenTypes;
   }

   public boolean isTypeForbidden(String typeName) {
      return this.forbiddenTypes.contains(typeName);
   }

   public interface PoiTranslator {
      String getTranslation(AbstractPoiType var1);

      String getTranslation(String var1);

      String getEnTranslation(AbstractPoiType var1);

      String getEnTranslation(String var1);

      String getSynonyms(AbstractPoiType var1);

      String getSynonyms(String var1);

      String getAllLanguagesTranslationSuffix();

      String getTranslatorLanguage();
   }
}
