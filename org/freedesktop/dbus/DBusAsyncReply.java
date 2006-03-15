package org.freedesktop.dbus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A handle to an asynchronous method call.
 */
public class DBusAsyncReply<ReturnType>
{
   /**
    * Check if any of a set of asynchronous calls have had a reply.
    * @param replies A Collection of handles to replies to check.
    * @return A Collection only containing those calls which have had replies.
    */
   public static Collection<DBusAsyncReply> hasReply(Collection<DBusAsyncReply> replies)
   {
      Collection<DBusAsyncReply> c = new ArrayList(replies);
      Iterator<DBusAsyncReply> i = c.iterator();
      while (i.hasNext())
         if (!i.next().hasReply()) i.remove();
      return c;
   }

   private ReturnType rval = null;
   private DBusExecutionException error = null;
   private MethodCall mc;
   private Method me;
   DBusAsyncReply(MethodCall mc, Method me)
   {
      this.mc = mc;
      this.me = me;
   }
   private synchronized void checkReply()
   {
      if (mc.hasReply()) {
         DBusMessage m = mc.getReply();
         if (m instanceof DBusErrorMessage)
            error = ((DBusErrorMessage) m).getException();
         else if (m instanceof MethodReply) {
            try {
               rval = (ReturnType) RemoteInvocationHandler.convertRV(m.getParameters(), me);
            } catch (DBusExecutionException DBEe) {
               error = DBEe;
            }
         }
      }
   }

   /**
    * Check if we've had a reply.
    * @return True if we have a reply
    */
   public boolean hasReply()
   {
      if (null != rval || null != error) return true;
      checkReply();
      return null != rval || null != error;
   }
   
   /**
    * Get the reply.
    * @return The return value from the method.
    * @throws DBusExecutionException if the reply to the method was an error.
    * @throws NoReply if the method hasn't had a reply yet
    */
   public ReturnType getReply() throws DBusExecutionException
   {
      if (null != rval) return rval;
      else if (null != error) throw error;
      checkReply();
      if (null != rval) return rval;
      else if (null != error) throw error;
      else throw new NoReply("Async call has not had a reply");
   }
}

