/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.Hal;

import java.util.Map;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;

public interface Device extends DBusInterface
{
   <T> void SetProperty(String key, T value);
   void SetPropertyString(String key, String value);
   void SetPropertyInteger(String key, int value);
   void SetPropertyBoolean(String key, boolean value);
   void SetPropertyDouble(String key, double value);

   <T> T GetProperty(String key);
   String GetPropertyString(String key);
   int GetPropertyInteger(String key);
   boolean GetPropertyBoolean(String key);
   double GetPropertyDouble(String key);

   Map<String, Variant> GetAllProperties();

   void RemoveProperty(String key);

   int GetPropertyType(String key);

   boolean PropertyExists(String key);

   void AddCapability(String capability);

   boolean QueryCapability(String capability);

   void Lock(String reason);

   void Unlock();

   public class PropertyModified extends DBusSignal
   {
      public final String key;
      public final Boolean added;
      public final Boolean removed;
      public PropertyModified(String path, String key, Boolean added, Boolean removed) throws DBusException
      {
         super(path, key, added, removed);
         this.key = key;
         this.added = added; 
         this.removed = removed;
      }
   }

   public class Condition extends DBusSignal
   {
      public final String condition;
      public final Variant value;
      public Condition(String path, String condition, Variant value) throws DBusException
      {
         super(path, condition, value);
         this.condition = condition;
         this.value = value;
      }
   }
}
