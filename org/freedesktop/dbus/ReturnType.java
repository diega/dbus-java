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
 * Return Type Annotation. 
 * Declares the D-Bus return signature of methods which return
 * Tuple.
 * This should be used to annotate all methods returning Tuple and
 * should be given an array containing the types of each element:
 * <tt>@ReturnType({"ai", "s", "b"}) public Tuple returnsStuff();</tt>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReturnType
{
   /** The D-Bus type signature. */
   String[] value();
}
