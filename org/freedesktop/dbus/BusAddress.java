/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
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
