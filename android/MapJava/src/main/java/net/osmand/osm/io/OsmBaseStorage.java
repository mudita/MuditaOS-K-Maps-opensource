package net.osmand.osm.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Relation;
import net.osmand.osm.edit.Way;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class OsmBaseStorage {
   protected static final String ELEM_OSM = "osm";
   protected static final String ELEM_OSMCHANGE = "osmChange";
   protected static final String ELEM_NODE = "node";
   protected static final String ELEM_TAG = "tag";
   protected static final String ELEM_WAY = "way";
   protected static final String ELEM_ND = "nd";
   protected static final String ELEM_RELATION = "relation";
   protected static final String ELEM_MEMBER = "member";
   protected static final String ELEM_MODIFY = "modify";
   protected static final String ELEM_CREATE = "create";
   protected static final String ELEM_DELETE = "delete";
   protected static final String ATTR_VERSION = "version";
   protected static final String ATTR_ID = "id";
   protected static final String ATTR_LAT = "lat";
   protected static final String ATTR_LON = "lon";
   protected static final String ATTR_TIMESTAMP = "timestamp";
   protected static final String ATTR_UID = "uid";
   protected static final String ATTR_USER = "user";
   protected static final String ATTR_VISIBLE = "visible";
   protected static final String ATTR_CHANGESET = "changeset";
   protected static final String ATTR_K = "k";
   protected static final String ATTR_V = "v";
   protected static final String ATTR_TYPE = "type";
   protected static final String ATTR_REF = "ref";
   protected static final String ATTR_ROLE = "role";
   protected Entity currentParsedEntity = null;
   protected int currentModify = 0;
   protected EntityInfo currentParsedEntityInfo = null;
   protected boolean parseStarted;
   protected Map<Entity.EntityId, Entity> entities = new LinkedHashMap<>();
   protected Map<Entity.EntityId, EntityInfo> entityInfo = new LinkedHashMap<>();
   protected int progressEntity = 0;
   protected IProgress progress;
   protected InputStream inputStream;
   protected InputStream streamForProgress;
   protected List<IOsmStorageFilter> filters = new ArrayList<>();
   protected boolean supressWarnings = true;
   protected boolean convertTagsToLC = true;
   protected boolean parseEntityInfo;
   private boolean osmChange;
   protected static final Set<String> supportedVersions = new HashSet<>();
   protected static final int moduleProgress = 1024;

   public static void main(String[] args) throws IOException, SAXException, XmlPullParserException {
      GZIPInputStream is = new GZIPInputStream(new FileInputStream("/Users/victorshcherb/osmand/temp/m.m001508233.osc.gz"));
      new OsmBaseStorage().parseOSM(is, IProgress.EMPTY_PROGRESS);
   }

   public synchronized void parseOSM(InputStream stream, IProgress progress, InputStream streamForProgress, boolean entityInfo) throws IOException, XmlPullParserException {
      this.inputStream = stream;
      this.progress = progress;
      this.parseEntityInfo = entityInfo;
      if (streamForProgress == null) {
         streamForProgress = this.inputStream;
      }

      this.streamForProgress = streamForProgress;
      this.parseStarted = false;
      this.entities.clear();
      this.entityInfo.clear();
      if (progress != null) {
         progress.startWork(streamForProgress.available());
      }

      XmlPullParser parser = PlatformUtil.newXMLPullParser();
      parser.setInput(stream, "UTF-8");

      int tok;
      while((tok = parser.next()) != 1) {
         if (tok == 2) {
            this.startElement(parser, parser.getName());
         } else if (tok == 3) {
            this.endElement(parser, parser.getName());
         }
      }

      if (progress != null) {
         progress.finishTask();
      }

      this.completeReading();
   }

   public synchronized void parseOSM(InputStream stream, IProgress progress) throws IOException, XmlPullParserException {
      this.parseOSM(stream, progress, null, true);
   }

   public void setConvertTagsToLC(boolean convertTagsToLC) {
      this.convertTagsToLC = convertTagsToLC;
   }

   public boolean isSupressWarnings() {
      return this.supressWarnings;
   }

   public void setSupressWarnings(boolean supressWarnings) {
      this.supressWarnings = supressWarnings;
   }

   public boolean isOsmChange() {
      return this.osmChange;
   }

   protected Long parseId(XmlPullParser parser, String name, long defId) {
      long id = defId;
      String value = parser.getAttributeValue("", name);

      try {
         id = Long.parseLong(value);
      } catch (NumberFormatException var9) {
      }

      return id;
   }

   protected double parseDouble(XmlPullParser parser, String name, double defVal) {
      double ret = defVal;
      String value = parser.getAttributeValue("", name);
      if (value == null) {
         return defVal;
      } else {
         try {
            ret = Double.parseDouble(value);
         } catch (NumberFormatException var9) {
         }

         return ret;
      }
   }

   protected void initRootElement(XmlPullParser parser, String name) throws OsmBaseStorage.OsmVersionNotSupported {
      if (("osm".equals(name) || "osmChange".equals(name)) && supportedVersions.contains(parser.getAttributeValue("", "version"))) {
         this.osmChange = "osmChange".equals(name);
         this.parseStarted = true;
      } else {
         throw new OsmBaseStorage.OsmVersionNotSupported();
      }
   }

   public void startElement(XmlPullParser parser, String name) {
      if (!this.parseStarted) {
         this.initRootElement(parser, name);
      }

      if ("modify".equals(name)) {
         this.currentModify = 1;
      } else if ("create".equals(name)) {
         this.currentModify = 2;
      } else if ("delete".equals(name)) {
         this.currentModify = -1;
      } else if (this.currentParsedEntity == null) {
         ++this.progressEntity;
         if (this.progress != null && this.progressEntity % 1024 == 0 && !this.progress.isIndeterminate() && this.streamForProgress != null) {
            try {
               this.progress.remaining(this.streamForProgress.available());
            } catch (IOException var6) {
               this.progress.startWork(-1);
            }
         }

         if ("node".equals(name)) {
            this.currentParsedEntity = new Node(this.parseDouble(parser, "lat", 0.0), this.parseDouble(parser, "lon", 0.0), this.parseId(parser, "id", -1L));
            this.currentParsedEntity.setVersion(this.parseVersion(parser));
         } else if ("way".equals(name)) {
            this.currentParsedEntity = new Way(this.parseId(parser, "id", -1L));
            this.currentParsedEntity.setVersion(this.parseVersion(parser));
         } else if ("relation".equals(name)) {
            this.currentParsedEntity = new Relation(this.parseId(parser, "id", -1L));
         }

         if (this.currentParsedEntity != null) {
            this.currentParsedEntity.setModify(this.currentModify);
            if (this.parseEntityInfo) {
               this.currentParsedEntityInfo = new EntityInfo();
               this.currentParsedEntityInfo.setChangeset(parser.getAttributeValue("", "changeset"));
               this.currentParsedEntityInfo.setTimestamp(parser.getAttributeValue("", "timestamp"));
               this.currentParsedEntityInfo.setUser(parser.getAttributeValue("", "user"));
               this.currentParsedEntityInfo.setVersion(parser.getAttributeValue("", "version"));
               this.currentParsedEntityInfo.setVisible(parser.getAttributeValue("", "visible"));
               this.currentParsedEntityInfo.setUid(parser.getAttributeValue("", "uid"));
            }
         }
      } else if ("tag".equals(name)) {
         String key = parser.getAttributeValue("", "k");
         if (key != null) {
            if (this.convertTagsToLC) {
               this.currentParsedEntity.putTag(key, parser.getAttributeValue("", "v"));
            } else {
               this.currentParsedEntity.putTagNoLC(key, parser.getAttributeValue("", "v"));
            }
         }
      } else if ("nd".equals(name)) {
         Long id = this.parseId(parser, "ref", -1L);
         if (id != -1L && this.currentParsedEntity instanceof Way) {
            ((Way)this.currentParsedEntity).addNode(id);
         }
      } else if ("member".equals(name)) {
         try {
            Long id = this.parseId(parser, "ref", -1L);
            if (id != -1L && this.currentParsedEntity instanceof Relation) {
               Entity.EntityType type = Entity.EntityType.valueOf(parser.getAttributeValue("", "type").toUpperCase());
               ((Relation)this.currentParsedEntity).addMember(id, type, parser.getAttributeValue("", "role"));
            }
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }
   }

   private int parseVersion(XmlPullParser parser) {
      return parser.getAttributeName(parser.getAttributeCount() - 1).equals("version")
         ? Integer.valueOf(parser.getAttributeValue(parser.getAttributeCount() - 1))
         : 0;
   }

   public void endElement(XmlPullParser parser, String name) {
      Entity.EntityType type = null;
      if ("node".equals(name)) {
         type = Entity.EntityType.NODE;
      } else if ("way".equals(name)) {
         type = Entity.EntityType.WAY;
      } else if ("relation".equals(name)) {
         type = Entity.EntityType.RELATION;
      } else if ("modify".equals(name)) {
         this.currentModify = 0;
      } else if ("create".equals(name)) {
         this.currentModify = 0;
      } else if ("delete".equals(name)) {
         this.currentModify = 0;
      }

      if (type != null && this.currentParsedEntity != null) {
         Entity.EntityId entityId = new Entity.EntityId(type, this.currentParsedEntity.getId());
         if (this.acceptEntityToLoad(entityId, this.currentParsedEntity)) {
            Entity oldEntity = this.entities.put(entityId, this.currentParsedEntity);
            if (this.parseEntityInfo && this.currentParsedEntityInfo != null) {
               this.entityInfo.put(entityId, this.currentParsedEntityInfo);
            }

            if (!this.supressWarnings && oldEntity != null) {
               throw new UnsupportedOperationException("Entity with id=" + oldEntity.getId() + " is duplicated in osm map");
            }
         }

         this.currentParsedEntity = null;
      }
   }

   public void registerEntity(Entity entity, EntityInfo info) {
      this.entities.put(Entity.EntityId.valueOf(entity), entity);
      if (info != null) {
         this.entityInfo.put(Entity.EntityId.valueOf(entity), info);
      }
   }

   protected boolean acceptEntityToLoad(Entity.EntityId entityId, Entity entity) {
      for(IOsmStorageFilter f : this.filters) {
         if (!f.acceptEntityToLoad(this, entityId, entity)) {
            return false;
         }
      }

      return true;
   }

   public void completeReading() {
      for(Entity e : this.entities.values()) {
         e.initializeLinks(this.entities);
      }
   }

   public Map<Entity.EntityId, EntityInfo> getRegisteredEntityInfo() {
      return this.entityInfo;
   }

   public Map<Entity.EntityId, Entity> getRegisteredEntities() {
      return this.entities;
   }

   public List<IOsmStorageFilter> getFilters() {
      return this.filters;
   }

   static {
      supportedVersions.add("0.6");
      supportedVersions.add("0.5");
   }

   public static class OsmVersionNotSupported extends RuntimeException {
      private static final long serialVersionUID = -127558215143984838L;
   }
}
