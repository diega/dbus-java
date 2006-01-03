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

   public class PropertyModified<A extends String, B extends Boolean, C extends Boolean> extends DBusSignal
   {
      public final A key;
      public final B added;
      public final C removed;
      public PropertyModified(String path, A key, B added, C removed) throws DBusException
      {
         super(path, key, added, removed);
         this.key = key;
         this.added = added; 
         this.removed = removed;
      }
   }

   public class Condition<A extends String, V extends Variant> extends DBusSignal
   {
      public final A condition;
      public final V value;
      public Condition(String path, A condition, V value) throws DBusException
      {
         super(path, condition, value);
         this.condition = condition;
         this.value = value;
      }
   }
}
