package org.freedesktop.dbus;

public class UInt16 extends Number implements Comparable<UInt16>
{
   public static final int MAX_VALUE = 65536;
   public static final int MIN_VALUE = 0;
   private int value;
   public UInt16(int value)
   {
      if (value < MIN_VALUE || value > MAX_VALUE)
         throw new NumberFormatException(value +" is not between "+ MIN_VALUE +" and "+ MAX_VALUE);
      this.value = value;
   }
   public UInt16(String value)
   {
      this(Integer.parseInt(value));
   }
   public byte byteValue() { return (byte) value; }
   public double doubleValue() { return (double) value; }
   public float floatValue() { return (float) value; }
   public int intValue() { return (int) value; }
   public long longValue() { return (long) value; }
   public short shortValue(){ return (short) value; }
   public boolean equals(Object o)
   {
      return o instanceof UInt16 && ((UInt16) o).value == this.value;
   }
   public int hashCode()
   {
      return (int) value;
   }
   public int compareTo(UInt16 other)
   {
      return (int) (this.value - other.value);
   }
   public String toString()
   {
      return ""+value;
   }
}
