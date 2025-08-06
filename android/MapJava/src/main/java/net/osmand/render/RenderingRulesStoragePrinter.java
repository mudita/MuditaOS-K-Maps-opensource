package net.osmand.render;

import gnu.trove.iterator.TIntObjectIterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.PlatformUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RenderingRulesStoragePrinter {
   public static void main(String[] args) throws XmlPullParserException, IOException {
      RenderingRulesStorage.STORE_ATTRIBUTES = true;
      String defaultFile = "/Users/victorshcherb/osmand/repos/resources/rendering_styles/default.render.xml";
      if (args.length > 0) {
         defaultFile = args[0];
      }

      String outputPath = ".";
      if (args.length > 1) {
         outputPath = args[1];
      }

      String name = "Style";
      Map<String, String> renderingConstants = new LinkedHashMap<>();
      InputStream is = new FileInputStream(defaultFile);

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

      is = new FileInputStream(defaultFile);
      RenderingRulesStorage var13 = new RenderingRulesStorage("default", renderingConstants);
      RenderingRulesStorage.RenderingRulesStorageResolver var14 = new RenderingRulesStorage.RenderingRulesStorageResolver() {
         @Override
         public RenderingRulesStorage resolve(String name, RenderingRulesStorage.RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
            RenderingRulesStorage depends = new RenderingRulesStorage(name, null);
            depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name + ".render.xml"), ref, false);
            return depends;
         }
      };
      var13.parseRulesFromXmlInputStream(is, var14, false);
      new RenderingRulesStoragePrinter().printJavaFile(outputPath, name, var13);
   }

   protected void printJavaFile(String path, String name, RenderingRulesStorage storage) throws IOException {
      PrintStream out = System.out;
      out = new PrintStream(new File(path, name + "RenderingRulesStorage.java"));
      out.println("\n\npackage net.osmand.render;\n\npublic class " + name + "RenderingRulesStorage {");
      String defindent = "\t";
      out.println("" + defindent + defindent + "RenderingRulesStorage storage;");
      out.println(
         "\tprivate java.util.Map<String, String> createMap(int... attrs) {\n\t\tjava.util.Map<String, String> mp = new java.util.HashMap<String, String>();\n\t\t\tfor(int i = 0; i< attrs.length; i+=2) {\n\t\t\t\tmp.put(storage.getStringValue(attrs[i]), storage.getStringValue(attrs[i+1]));\n\t\t\t}\n\t\treturn mp;\n\t}"
      );
      out.println(
         "\tprivate java.util.Map<String, String> createMap(String... attrs) {\n\t\tjava.util.Map<String, String> mp = new java.util.HashMap<String, String>();\n\t\t\tfor(int i = 0; i< attrs.length; i+=2) {\n\t\t\t\tmp.put(attrs[i], attrs[i+1]);\n\t\t\t}\n\t\treturn mp;\n\t}"
      );
      out.println("\n" + defindent + "public void createStyle(RenderingRulesStorage storage) {");
      out.println("" + defindent + defindent + "this.storage=storage;");
      out.println("" + defindent + defindent + "storage.renderingName=" + this.javaString(storage.renderingName) + ";");
      out.println("" + defindent + defindent + "storage.internalRenderingName=" + this.javaString(storage.internalRenderingName) + ";");
      out.println("" + defindent + defindent + "initDictionary();");
      out.println("" + defindent + defindent + "initProperties();");
      out.println("" + defindent + defindent + "initConstants();");
      out.println("" + defindent + defindent + "initAttributes();");
      out.println("" + defindent + defindent + "initRules();");
      out.println("" + defindent + "}");
      this.printJavaInitConstants(storage, out, defindent, defindent);
      this.printJavaInitProperties(storage, out, defindent, defindent);
      this.printJavaInitRules(storage, out, defindent, defindent);
      this.printJavaInitAttributes(storage, out, defindent, defindent);
      this.printJavaInitDictionary(storage, out, defindent, defindent);
      out.println("\n\n}");
   }

   private String javaString(String s) {
      return "\"" + s + "\"";
   }

   private void printJavaInitDictionary(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
      out.println("\n" + indent + "public void initDictionary() {");
      int i = 0;

      for(String s : storage.dictionary) {
         out.println("" + indent + ti + "storage.getDictionaryValue(" + this.javaString(s) + ");  // " + i++);
      }

      out.println("" + indent + "}");
   }

   private void printJavaInitProperties(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
      out.println("\n" + indent + "public void initProperties() {");
      out.println("" + indent + ti + "RenderingRuleProperty prop = null;");

      for(RenderingRuleProperty p : storage.PROPS.customRules) {
         out.println("" + indent + ti + "prop = new RenderingRuleProperty(" + this.javaString(p.attrName) + "," + p.type + ", " + p.input + ");");
         out.println("" + indent + ti + "prop.setDescription(" + this.javaString(p.description) + ");");
         out.println("" + indent + ti + "prop.setCategory(" + this.javaString(p.category) + ");");
         out.println("" + indent + ti + "prop.setName(" + this.javaString(p.name) + ");");
         if (p.possibleValues != null && !p.isBoolean()) {
            String mp = "";

            for(String s : p.possibleValues) {
               if (mp.length() > 0) {
                  mp = mp + ", ";
               }

               mp = mp + this.javaString(s);
            }

            out.println("" + indent + ti + "prop.setPossibleValues(new String[]{" + mp + "});");
         }

         out.println("" + indent + ti + "storage.PROPS.registerRule(prop);");
      }

      out.println("" + indent + "}");
   }

   private void printJavaInitAttributes(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
      out.println("\n" + indent + "public void initAttributes() {");

      for(int i = 0; i < 15; ++i) {
         out.println("" + indent + ti + "RenderingRule rule" + i + " = null;");
      }

      for(Entry<String, RenderingRule> entry : storage.renderingAttributes.entrySet()) {
         this.generateRenderingRule(storage, out, indent + ti, "rule", 0, entry.getValue());
         out.println("" + indent + ti + "storage.renderingAttributes.put(" + this.javaString(entry.getKey()) + ", rule0);");
      }

      out.println("" + indent + "}");
   }

   private void printJavaInitRules(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
      int javaFunctions = 0;
      boolean initNewSection = true;

      for(int rulesSection = 0; rulesSection < 6; ++rulesSection) {
         initNewSection = true;
         if (storage.tagValueGlobalRules[rulesSection] != null) {
            TIntObjectIterator<RenderingRule> iterator = storage.tagValueGlobalRules[rulesSection].iterator();
            int rulesInSection = 0;

            while(iterator.hasNext()) {
               iterator.advance();
               if (initNewSection) {
                  if (javaFunctions > 0) {
                     out.println("" + indent + "}\n");
                  }

                  out.println("\n" + indent + "public void initRules" + javaFunctions + "() {");

                  for(int k = 0; k < 15; ++k) {
                     out.println("" + indent + ti + "RenderingRule rule" + k + " = null;");
                  }

                  initNewSection = false;
                  ++javaFunctions;
               }

               if (rulesInSection > 50) {
                  rulesInSection = 0;
                  initNewSection = true;
               }

               rulesInSection += this.generateRenderingRule(storage, out, indent + ti, "rule", 0, (RenderingRule)iterator.value());
               out.println("" + indent + ti + "storage.tagValueGlobalRules[" + rulesSection + "].put(" + iterator.key() + ", rule0);");
            }
         }
      }

      if (javaFunctions > 0) {
         out.println("" + indent + "}\n");
      }

      out.println("\n" + indent + "public void initRules() {");

      for(int k = 0; k < 6; ++k) {
         if (storage.tagValueGlobalRules[k] != null) {
            out.println("" + indent + ti + "storage.tagValueGlobalRules[" + k + "] = new gnu.trove.map.hash.TIntObjectHashMap();");
         }
      }

      for(int i = 0; i < javaFunctions; ++i) {
         out.println("" + indent + ti + "initRules" + i + "();");
      }

      out.println("" + indent + "}");
   }

   private int generateRenderingRule(RenderingRulesStorage storage, PrintStream out, String indent, String name, int ind, RenderingRule key) {
      int cnt = 1;
      String mp = "";
      Iterator<Entry<String, String>> it = key.getAttributes().entrySet().iterator();

      while(it.hasNext()) {
         Entry<String, String> e = it.next();
         int kk = storage.getDictionaryValue(e.getKey());
         int vv = storage.getDictionaryValue(e.getValue());
         mp = mp + kk + ", " + vv;
         if (it.hasNext()) {
            mp = mp + ", ";
         }
      }

      if (mp.isEmpty()) {
         mp = "java.util.Collections.EMPTY_MAP";
      } else {
         mp = "createMap(" + mp + ")";
      }

      out.println("" + indent + name + ind + " = new RenderingRule(" + mp + ", " + key.isGroup() + ",  storage);");

      for(RenderingRule k : key.getIfChildren()) {
         this.generateRenderingRule(storage, out, indent + "\t", name, ind + 1, k);
         out.println("" + indent + name + ind + ".addIfChildren(" + name + (ind + 1) + ");");
         ++cnt;
      }

      for(RenderingRule k : key.getIfElseChildren()) {
         this.generateRenderingRule(storage, out, indent + "\t", name, ind + 1, k);
         out.println("" + indent + name + ind + ".addIfElseChildren(" + name + (ind + 1) + ");");
         ++cnt;
      }

      return cnt;
   }

   private void printJavaInitConstants(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
      out.println("\n" + indent + "public void initConstants() {");

      for(Entry<String, String> entry : storage.renderingConstants.entrySet()) {
         out.println("" + indent + ti + "storage.renderingConstants.put(" + this.javaString(entry.getKey()) + ", " + this.javaString(entry.getValue()) + ");");
      }

      out.println("" + indent + "}");
   }
}
