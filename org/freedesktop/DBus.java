package org.freedesktop;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;

public interface DBus extends DBusInterface
{
   public static final int DBUS_NAME_FLAG_ALLOW_REPLACEMENT = 0x01;
   public static final int DBUS_NAME_FLAG_REPLACE_EXISTING = 0x02;
   public static final int DBUS_NAME_FLAG_DO_NOT_QUEUE = 0x04;
   public static final int DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER = 1;
   public static final int DBUS_REQUEST_NAME_REPLY_IN_QUEUE = 2;
   public static final int DBUS_REQUEST_NAME_REPLY_EXISTS = 3;
   public static final int DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER = 4;
   public static final int DBUS_RELEASE_NAME_REPLY_RELEASED = 1;
   public static final int DBUS_RELEASE_NAME_REPLY_NON_EXISTANT = 2;
   public static final int DBUS_RELEASE_NAME_REPLY_NOT_OWNER = 3;
   public static final int DBUS_START_REPLY_SUCCESS = 1;
   public static final int DBUS_START_REPLY_ALREADY_RUNNING = 2;
   /**
    * All DBus Applications should respond to the Ping method on this interface
    */
   public interface Peer extends DBusInterface
   {
      public void Ping();
   }
   /**
    * Objects can provide introspection data via this interface and method.
    * See the <a href="http://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format">Introspection Format</a>.
    */
   public interface Introspectable extends DBusInterface
   {
      /**
       * @return The XML introspection data for this object
       */
      public String Introspect();
   }
   /**
    * A standard properties interface.
    */
   public interface Properties extends DBusInterface
   {
      /**
       * Get the value for the given property.
       * @param interface_name The interface this property is associated with.
       * @param property_name The name of the property.
       * @return The value of the property (may be any valid DBus type).
       */
      public <A> A Get (String interface_name, String property_name);
      /**
       * Set the value for the given property.
       * @param interface_name The interface this property is associated with.
       * @param property_name The name of the property.
       * @param value The new value of the property (may be any valid DBus type).
       */
      public <A> void Set (String interface_name, String property_name, A value);
   }
   /**
    * Messages generated locally in the application.
    */
   public interface Local extends DBusInterface
   {
      public class Disconnected extends DBusSignal
      {
         public Disconnected(String path) throws DBusException
         {
            super(path);
         }
      }
   }
   
   /**
    * Initial message to register ourselves on the Bus.
    * @return The unique name of this connection to the Bus.
    */
   public String Hello();
   /**
    * Lists all connected names on the Bus.
    * @return An array of all connected names.
    */
   public String[] ListNames();
   /**
    * Determine if a name has an owner.
    * @param name The name to query.
    * @return true if the name has an owner.
    */
   public boolean NameHasOwner(String name);
   /**
    * Get the connection unique name that owns the given name.
    * @param name The name to query.
    * @return The connection which owns the name.
    */
   public String GetNameOwner(String name);
   /**
    * Get the Unix UID that owns a connection name.
    * @param connection_name The connection name.
    * @return The Unix UID that owns it.
    */
   public UInt32 GetConnectionUnixUser(String connection_name);
   /**
    * Start a service. If the given service is not provided
    * by any application, it will be started according to the .service file
    * for that service.
    * @param name The service name to start.
    * @param flags Unused.
    * @return DBUS_START_REPLY constants.
    */
   public UInt32 StartServiceByName(String name, UInt32 flags);
   /**
    * Request a name on the bus.
    * @param name The name to request.
    * @param flags DBUS_NAME flags.
    * @return DBUS_REQUEST_NAME_REPLY constants.
    */
   public UInt32 RequestName(String name, UInt32 flags);
   /**
    * Release a name on the bus.
    * @param name The name to release.
    * @return DBUS_RELEASE_NAME_REPLY constants.
    */
   public UInt32 ReleaseName(String name);

   /**
    * Add a match rule.
    * Will cause you to receive messages that aren't directed to you which 
    * match this rule.
    * @param matchrule The Match rule as a string. Format Undocumented.
    */
   public void AddMatch(String matchrule);

   /**
    * Remove a match rule.
    * Will cause you to stop receiving messages that aren't directed to you which 
    * match this rule.
    * @param matchrule The Match rule as a string. Format Undocumented.
    */
   public void RemoveMatch(String matchrule);

   /**
    * List the connections currently queued for a name.
    * @param name The name to query
    * @return A list of unique connection IDs.
    */
   public String[] ListQueuedOwners(String name);

   /**
    * Returns the proccess ID associated with a connection.
    * @param connection_name The name of the connection
    * @return The PID of the connection.
    */
   public UInt32 GetConnectionUnixProcessID(String a);

   /**
    * Does something undocumented.
    */
   public Byte[] GetConnectionSELinuxSecurityContext(String a);

   /**
    * Does something undocumented.
    */
   public void ReloadConfig();

         
   public class NameOwnerChanged extends DBusSignal
   {
      public final String name;
      public final String old_owner;
      public final String new_owner;
      public NameOwnerChanged(String path, String name, String old_owner, String new_owner) throws DBusException
      {
         super(path, name, old_owner, new_owner);
         this.name = name;
         this.old_owner = old_owner;
         this.new_owner = new_owner;
      }
   }
   public class NameLost extends DBusSignal
   {
      public final String name;
      public NameLost(String path, String name) throws DBusException
      {
         super(path, name);
         this.name = name;
      }
   }
   public class NameAquired extends DBusSignal
   {
      public final String name;
      public NameAquired(String path, String name) throws DBusException
      {
         super(path, name);
         this.name = name;
      }
   }
}
