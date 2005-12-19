package org.freedesktop.dbus;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class IterableNodeList implements Iterable<Node>
{
   private NodeList nl;
   public IterableNodeList(NodeList nl)
   {
      this.nl = nl;
   }
   public Iterator<Node> iterator()
   {
      return new NodeListIterator(nl);
   }
}
