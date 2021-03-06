/*
 * Arthur Edelstein, UCSF, 2011
 */

package org.micromanager.clojureeditor;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class ClojureEditorPlugin implements MMPlugin {
   public static final String menuName = "Clojure editor";
   public static final String tooltipDescription =
      "Clojure script editor and REPL";
   
   public void dispose() {
      // do nothing
   }

   public void setApp(ScriptInterface app) {
      // do nothing.
   }

   public void show() {
      clooj.core.show();
   }

   public void configurationChanged() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public String getDescription() {
      return tooltipDescription;
   }

   public String getInfo() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public String getVersion() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public String getCopyright() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

}

