package org.freedesktop.dbus;
class SignalTuple
{
   String type;
   String name;
   String object;
   String source;
   public SignalTuple(String type, String name, String object, String source)
   {
      this.type = type;
      this.name = name;
      this.object = object;
      this.source = source;
   }
   public boolean equals(Object o)
   {
      if (!(o instanceof SignalTuple)) return false;
      SignalTuple other = (SignalTuple) o;
      if (null == this.type && null != other.type) return false;
      if (null != this.type && !this.type.equals(other.type)) return false;
      if (null == this.name && null != other.name) return false;
      if (null != this.name && !this.name.equals(other.name)) return false;
      if (null == this.object && null != other.object) return false;
      if (null != this.object && !this.object.equals(other.object)) return false;
      if (null == this.source && null != other.source) return false;
      if (null != this.source && !this.source.equals(other.source)) return false;
      return true;
   }
   public int hashCode()
   {
      return (null == type ? 0 : type.hashCode())
         +   (null == name ? 0 : name.hashCode())
         +   (null == source ? 0 : source.hashCode())
         +   (null == object ? 0 : object.hashCode());
   }
   public String toString()
   {
      return "SignalTuple("+type+","+name+","+object+","+source+")";
   }
}

