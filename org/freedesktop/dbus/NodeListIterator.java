package org.freedesktop.dbus;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class NodeListIterator implements Iterator<Node>
{
   NodeList nl;
   int i;
   NodeListIterator(NodeList nl)
   {
      this.nl = nl;
      i = 0;
   }
   public boolean hasNext()
   {
      return i < nl.getLength();
   }
   public Node next()
   {
      Node n = nl.item(i);
      i++;      
      return n;
   }
   public void remove() {};
}
