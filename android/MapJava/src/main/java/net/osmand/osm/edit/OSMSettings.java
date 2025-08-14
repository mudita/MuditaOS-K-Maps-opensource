package net.osmand.osm.edit;

public class OSMSettings {
   public static boolean wayForCar(String tagHighway) {
      if (tagHighway != null) {
         String[] cars = new String[]{
            "trunk",
            "motorway",
            "primary",
            "secondary",
            "tertiary",
            "service",
            "residential",
            "trunk_link",
            "motorway_link",
            "primary_link",
            "secondary_link",
            "residential_link",
            "tertiary_link",
            "track",
            "unclassified"
         };

         for(String c : cars) {
            if (c.equals(tagHighway)) {
               return true;
            }
         }
      }

      return false;
   }

   public static enum OSMHighwayTypes {
      TRUNK,
      MOTORWAY,
      PRIMARY,
      SECONDARY,
      RESIDENTIAL,
      TERTIARY,
      SERVICE,
      TRACK,
      TRUNK_LINK,
      MOTORWAY_LINK,
      PRIMARY_LINK,
      SECONDARY_LINK,
      RESIDENTIAL_LINK,
      TERTIARY_LINK,
      SERVICE_LINK,
      TRACK_LINK;
   }

   public static enum OSMTagKey {
      NAME("name"),
      NAME_EN("name:en"),
      LOCK_NAME("lock_name"),
      HIGHWAY("highway"),
      BUILDING("building"),
      BOUNDARY("boundary"),
      POSTAL_CODE("postal_code"),
      RAILWAY("railway"),
      STATION("subway"),
      ONEWAY("oneway"),
      LAYER("layer"),
      BRIDGE("bridge"),
      TUNNEL("tunnel"),
      TOLL("toll"),
      JUNCTION("junction"),
      ROUTE("route"),
      ROUTE_MASTER("route_master"),
      BRAND("brand"),
      OPERATOR("operator"),
      REF("ref"),
      RCN_REF("rcn_ref"),
      RWN_REF("rwn_ref"),
      PLACE("place"),
      ADDR_HOUSE_NUMBER("addr:housenumber"),
      ADDR2_HOUSE_NUMBER("addr2:housenumber"),
      ADDR_HOUSE_NAME("addr:housename"),
      ADDR_STREET("addr:street"),
      ADDR_STREET2("addr:street2"),
      ADDR2_STREET("addr2:street"),
      ADDR_CITY("addr:city"),
      ADDR_PLACE("addr:place"),
      ADDR_POSTCODE("addr:postcode"),
      ADDR_INTERPOLATION("addr:interpolation"),
      ADDRESS_TYPE("address:type"),
      ADDRESS_HOUSE("address:house"),
      TYPE("type"),
      IS_IN("is_in"),
      LOCALITY("locality"),
      AMENITY("amenity"),
      SHOP("shop"),
      LANDUSE("landuse"),
      OFFICE("office"),
      EMERGENCY("emergency"),
      MILITARY("military"),
      ADMINISTRATIVE("administrative"),
      MAN_MADE("man_made"),
      BARRIER("barrier"),
      LEISURE("leisure"),
      TOURISM("tourism"),
      SPORT("sport"),
      HISTORIC("historic"),
      NATURAL("natural"),
      INTERNET_ACCESS("internet_access"),
      CONTACT_WEBSITE("contact:website"),
      CONTACT_PHONE("contact:phone"),
      OPENING_HOURS("opening_hours"),
      PHONE("phone"),
      DESCRIPTION("description"),
      WEBSITE("website"),
      URL("url"),
      WIKIPEDIA("wikipedia"),
      ADMIN_LEVEL("admin_level"),
      PUBLIC_TRANSPORT("public_transport"),
      ENTRANCE("entrance"),
      COLOUR("colour");

      private final String value;

      private OSMTagKey(String value) {
         this.value = value;
      }

      public String getValue() {
         return this.value;
      }
   }
}
