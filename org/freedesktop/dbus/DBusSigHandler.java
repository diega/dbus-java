package org.freedesktop.dbus;
/** Handle a signal on DBus.
 *  All Signal handlers are run in their own Thread. 
 *  Application writers are responsible for managing any concurrency issues.
 */
public interface DBusSigHandler
{
   /**
    * Handle a signal.
    * @param s The signal to handle. If such a class exists, the
    * signal will be an instance of the class with the correct type signature.
    * Otherwise it will be an instance of DBusSignal
    */
   public void handle(DBusSignal s);
}
