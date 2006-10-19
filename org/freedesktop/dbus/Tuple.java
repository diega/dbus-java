/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.Arrays;

/**
 * Tuples allow multiple values to be returned from a method.
 * You should create a sub class of this tuple with the correct
 * number of parameters (a 'Pair' or a 'Triplet'). The constructor
 * <b>Must</b> call the super constructor with all the values
 * <i>in order</i>.
 */
public abstract class Tuple
{
   private final Object[] parameters;
   /**
    * Initialise the Tuple.
    */
   public Tuple(Object... parameters)
   {
      this.parameters = parameters;
   }
   /** Get all the values in order. */
   public final Object[] getParameters()
   {
      return parameters;
   }
   /** A String representation of the Tuple. */
   public final String toString()
   {
      String s = getClass().getName()+"<";
      Object[] os = getParameters();
      if (null == os || 0 == os.length) {
         return s+">";
      }
      for (Object o: os)
         s += o+", ";
      return s.replaceAll(", $", ">");
   }
   /** Compare the contents of this tuple with another */
   public boolean equals(Object other)
   {
      if (null == other) return false;
      if (!(other instanceof Tuple)) return false;
      return Arrays.equals(this.parameters, ((Tuple) other).parameters);
   }
}
