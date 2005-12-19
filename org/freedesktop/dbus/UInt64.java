package org.freedesktop.dbus;

public class UInt64 extends Number implements Comparable<UInt64>
{
   public static final long MAX_VALUE = Long.MAX_VALUE;
   public static final long MIN_VALUE = 0;
   private long value;
   public UInt64(long value)
   {
      if (value < MIN_VALUE || value > MAX_VALUE)
         throw new NumberFormatException(value +" is not between "+ MIN_VALUE +" and "+ MAX_VALUE);
      this.value = value;
   }
   public UInt64(String value)
   {
      this(Long.parseLong(value));
   }
   public byte byteValue() { return (byte) value; }
   public double doubleValue() { return (double) value; }
   public float floatValue() { return (float) value; }
   public int intValue() { return (int) value; }
   public long longValue() { return (long) value; }
   public short shortValue(){ return (short) value; }
   public boolean equals(Object o)
   {
      return o instanceof UInt64 && ((UInt64) o).value == this.value;
   }
   public int hashCode()
   {
      return (int) value;
   }
   public int compareTo(UInt64 other)
   {
      return (int) (this.value - other.value);
   }
   public String toString()
   {
      return ""+value;
   }
}

