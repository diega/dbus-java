package org.freedesktop.dbus;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class DBusMap<K, V> implements Map<K, V>
{
   private K[] keys;
   private V[] values;
   public DBusMap(K[] keys, V[] values)
   {
      this.keys=keys;
      this.values=values;
   }
   class Entry implements Map.Entry<K,V>
   {
      private int entry;
      public Entry(int i)
      {
         this.entry = i;
      }
      public boolean  equals(Object o)
      {
         if (null == o) return false;
         if (!(o instanceof DBusMap.Entry)) return false;
         return this.entry == ((Entry) o).entry;
      }
      public K getKey()
      {
         return keys[entry];
      }
      public V getValue()
      {
         return values[entry];
      }
      public int hashCode()
      {
         return keys[entry].hashCode();
      }
      public V setValue(V value)
      {
         throw new UnsupportedOperationException();
      }
   }

   public void clear()
   {
      throw new UnsupportedOperationException();
   }
   public boolean  containsKey(Object key)
   {
      for (int i = 0; i < keys.length; i++)
         if (key == keys[i] || (key != null && key.equals(keys[i])))
            return true;
      return false;
   }
   public boolean   containsValue(Object value)
   {
      for (int i = 0; i < values.length; i++)
         if (value == values[i] || (value != null && value.equals(values[i])))
            return true;
      return false;
   }
   public Set<Map.Entry<K,V>>    entrySet()
   {
      Set<Map.Entry<K,V>> s = new TreeSet<Map.Entry<K,V>>();
      for (int i = 0; i < keys.length; i++)
         s.add(new Entry(i));
      return s;
   }
   public V   get(Object key)
   {
      for (int i = 0; i < keys.length; i++)
         if (key == keys[i] || (key != null && key.equals(keys[i])))
            return values[i];
      return null;
   }
   public boolean  isEmpty() 
   { 
      return keys.length == 0;
   }
   public Set<K>    keySet()
   {
      Set<K> s = new TreeSet<K>();
      s.addAll(Arrays.asList(keys));
      return s;
   }
   public V    put(K key, V value)
   {
      throw new UnsupportedOperationException();
   }
   public void  putAll(Map<? extends K,? extends V> t)
   {
      throw new UnsupportedOperationException();
   }
   public V   remove(Object key)
   {
      throw new UnsupportedOperationException();
   }
   public int  size()
   {
      return keys.length;
   }
   public Collection<V>  values()
   {
      return Arrays.asList(values);
   }
   public int  hashCode() 
   {
      return Arrays.hashCode(keys) + Arrays.hashCode(values);
   }
   public boolean equals(Object o) 
   {
      if (null == o) return false;
      if (!(o instanceof DBusMap)) return false;
      return Arrays.equals(((DBusMap) o).keys, this.keys) && Arrays.equals(((DBusMap) o).values, this.values);
   }
}
