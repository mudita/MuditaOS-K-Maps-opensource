package net.osmand.osm.edit;

public class EntityInfo {
   String timestamp;
   String uid;
   String user;
   String visible;
   String version;
   String changeset;
   String action;

   public EntityInfo() {
   }

   public EntityInfo(String version) {
      this.version = version;
   }

   public String getAction() {
      return this.action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public String getTimestamp() {
      return this.timestamp;
   }

   public void setTimestamp(String timestamp) {
      this.timestamp = timestamp;
   }

   public String getUid() {
      return this.uid;
   }

   public void setUid(String uid) {
      this.uid = uid;
   }

   public String getUser() {
      return this.user;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public String getVisible() {
      return this.visible;
   }

   public void setVisible(String visible) {
      this.visible = visible;
   }

   public String getVersion() {
      return this.version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getChangeset() {
      return this.changeset;
   }

   public void setChangeset(String changeset) {
      this.changeset = changeset;
   }
}
