package org.freedesktop.dbus.test;

import java.lang.reflect.Type;

import java.util.List;
import java.util.Vector;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusSerializable;

public class TestSerializable<A> implements DBusSerializable
{
   private int a;
   private String b;
   private Vector<Integer> c;
   public TestSerializable(int a, A b, Vector<Integer> c)
   {
      this.a = a;
      this.b = b.toString();
      this.c = c;
   }
   public TestSerializable() {}
   public void deserialize(int a, String b, List<Integer> c)
   {
      this.a = a;
      this.b = b;
      this.c = new Vector<Integer>(c);
   }
   public Object[] serialize() throws DBusException
   {
      return DBusConnection.convertParameters(new Object[] { a, b, c }, new Type[] { TestSerializable.class });
   }
   public int getInt() { return a; }
   public String getString() { return b; }
   public Vector<Integer> getVector() { return c; }
   public String toString()
   {
      return "TestSerializable{"+a+","+b+","+c+"}";
   }
}
