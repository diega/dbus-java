/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type Annotation. Declares the DBus return signature of methods which return
 * Tuple
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TupleParameters
{
   /** The order of this field in the Struct. */
   String[] value();
}
