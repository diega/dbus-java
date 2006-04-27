package org.freedesktop.dbus;

/**
 * Class to represent unsigned 64-bit numbers.
 * Warning. Java has no native unsigned 64-bit type, 
 * only signed 64-bit types. Therefore it is not possible 
 * to store all unsigned 64-bit numbers in this type,
 * only ones which would fit in a signed number. This
 * is not a problem since if you could there would
 * be no way of getting the number out again anyway.
 */
@SuppressWarnings("serial")
public class UInt64 extends Number implements Comparable<UInt64>
{
   /** Maximum allowed value */
   public static final long MAX_VALUE = Long.MAX_VALUE;
   /** Minimum allowed value */
   public static final long MIN_VALUE = 0;
   private long value;
   /** Create a UInt64 from a long.
    * @param value Must be a valid integer within MIN_VALUE&ndash;MAX_VALUE 
    * @throws NumberFormatException if value is not between MIN_VALUE and MAX_VALUE
    */
   public UInt64(long value)
   {
      if (value < MIN_VALUE || value > MAX_VALUE)
         throw new NumberFormatException(value +" is not between "+ MIN_VALUE +" and "+ MAX_VALUE);
      this.value = value;
   }
   /** Create a UInt64 from a String.
    * @param value Must parse to a valid integer within MIN_VALUE&ndash;MAX_VALUE 
    * @throws NumberFormatException if value is not an integer between MIN_VALUE and MAX_VALUE
    */
   public UInt64(String value)
   {
      this(Long.parseLong(value));
   }
   /** The value of this as a byte. */
   public byte byteValue() { return (byte) value; }
   /** The value of this as a double. */
   public double doubleValue() { return (double) value; }
   /** The value of this as a float. */
   public float floatValue() { return (float) value; }
   /** The value of this as a int. */
   public int intValue() { return (int) value; }
   /** The value of this as a long. */
   public long longValue() { return (long) value; }
   /** The value of this as a short. */
   public short shortValue(){ return (short) value; }
   /** Test two UInt64s for equality. */
   public boolean equals(Object o)
   {
      return o instanceof UInt64 && ((UInt64) o).value == this.value;
   }
   public int hashCode()
   {
      return (int) value;
   }
   /** Compare two UInt32s. 
    * @return 0 if equal, -ve or +ve if they are different. 
    */
   public int compareTo(UInt64 other)
   {
      return (int) (this.value - other.value);
   }
   /** The value of this as a string. */
   public String toString()
   {
      return ""+value;
   }
}

