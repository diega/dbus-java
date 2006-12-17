package org.freedesktop.dbus;
import java.util.Map;
import java.util.HashMap;

public class BusAddress
{
   private String type;
   private Map<String,String> parameters;
   public BusAddress(String address)
   {
      String[] ss = address.split(":", 2);
      type = ss[0];
      String[] ps = ss[1].split(",");
      parameters = new HashMap<String,String>();
      for (String p: ps) {
         String[] kv = p.split("=", 2);
         parameters.put(kv[0], kv[1]);
      }
   }
   public String getType() { return type; }
   public String getParameter(String key) { return parameters.get(key); }
   public String toString() { return type+": "+parameters; }
}
