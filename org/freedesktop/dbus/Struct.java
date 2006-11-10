/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import org.freedesktop.dbus.Position;

/**
 * This class should be extended to create Structs.
 * Any such class may be sent over DBus to a method which takes a Struct.
 * All fields in the Struct which you wish to be serialized and sent to the
 * remote method should be annotated with the org.freedesktop.dbus.Position
 * annotation, in the order they should appear in to Struct to DBus.
 */
public abstract class Struct extends Container
{
   public Struct() {}
}
