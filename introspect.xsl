<?xml version="1.0" encoding="ISO-8859-15"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" xmlns:str="http://xsltsl.org/string" >

<!-- 
 Copyright (C) 2005 Lennart Poettering.

 Licensed under the Academic Free License version 2.1

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
-->

<!-- $Id: introspect.xsl,v 1.1 2005/08/18 04:04:57 johnp Exp $ -->

<!-- xsl:import href="http://xsltsl.sourceforge.net/modules/stdlib.xsl"/ -->
<xsl:import href="xsltsl-1.2.1/stdlib.xsl"/>

<xsl:output method="xml" version="1.0" encoding="iso-8859-15" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" indent="yes" />

<xsl:template match="/">
  <html>
    <head>
      <title>DBUS Introspection data</title>
      <style type="text/css">
      body { color: black; background-color: white } 
      h1,th,h3 { font-family: serif; background-color: #CCF; border: 1px solid #999; }
      th,h3 { font-size: 125%; }
      tr.sub th { font-size: 1em; padding: 5px; }
      td ul { list-style-type: none; margin: 0px;  }
      .keyword { font-style: normal }
      .type { font-weight: bold }
      .symbol { font-style: italic }
      .interface, .package { margin: 10px; padding: 10px; }
      table, td { border: 1px solid #999; }
      table { border-collapse: collapse; width: 80%; }
      td { padding: 10px; }
      </style>
    </head>
    <body>
    <h1>DBUS Introspection Data</h1>
    <h2><span class="keyword">object</span> <span class="symbol"><xsl:value-of select="node/@name" /></span></h2>
    <table class="package">
       <tr><th colspan="3">Interfaces</th></tr>
        <xsl:for-each select="node/interface">
          <tr><td><a href="#{@name}">
          <xsl:call-template name="str:substring-after-last">
             <xsl:with-param name="text" select="@name" />  
             <xsl:with-param name="chars">.</xsl:with-param>  
          </xsl:call-template>
          </a></td><td><xsl:call-template name="str:substring-before-last">
             <xsl:with-param name="text" select="@name" />  
             <xsl:with-param name="chars">.</xsl:with-param>  
          </xsl:call-template>
          </td><td>
          <xsl:value-of select="annotation[@name='org.freedesktop.DBus.Description']/@value" />
          </td></tr>  
         </xsl:for-each>
    </table>
          
    <table class="package">
       <tr><th colspan="3">Signals</th></tr>
        <xsl:for-each select="node/interface">
           <xsl:variable name="iface_name" select="@name"/>
           <xsl:for-each select="signal">
             <tr><td>
                <a href="#{$iface_name}.{@name}"><xsl:value-of select="@name"/></a>
             </td><td>
                <xsl:value-of select="$iface_name"/> 
             </td><td>
                <xsl:value-of select="annotation[@name='org.freedesktop.DBus.Description']/@value" />
            </td></tr> 
            </xsl:for-each>
         </xsl:for-each>
    </table>
    
      <xsl:for-each select="node/interface">
        <div class="interface">
          <h2 id="{@name}">
            <span class="keyword">interface</span><xsl:text> </xsl:text>
            <span class="symbol"><xsl:value-of select="@name"/></span>
          </h2>   

          <p>
             <xsl:value-of select="annotation[@name='org.freedesktop.DBus.Description']/@value" />
          </p>
          
           <ul>
             <xsl:apply-templates select="annotation[@name!='org.freedesktop.DBus.Description']"/> 
           </ul>

          <xsl:variable name="iface_name" select="@name"/>

         <xsl:if test="property">
         <table class="package">
            <tr><th colspan="4">Properties</th></tr>
            <xsl:for-each select="property">
            <tr>
            <td id="{$iface_name}.{@name}">
               <span class="keyword">property</span><xsl:text
               disable-output-escaping='yes'>&amp;</xsl:text>nbsp;<span
                class="symbol"><xsl:value-of select="@name"/></span>
            </td>
            <td><span class="type"><xsl:value-of select="@type"/></span></td>
            <td><span class="keyword"><xsl:value-of select="@access"/></span></td>
            <td>
             <xsl:value-of select="annotation[@name='org.freedesktop.DBus.Description']/@value" />
            </td>
            </tr>
            <xsl:apply-templates select="annotation[@name!='org.freedesktop.DBus.Description']"/> 
            </xsl:for-each>
         </table>
        </xsl:if>

         <xsl:if test="method">
            <table class="package">
            <tr><th colspan="4">Methods</th></tr>
            <tr class="sub"><th>Name</th><th>In</th><th>Out</th><th>Description</th></tr>
            <xsl:for-each select="method">
            <tr>
            <td id="{$iface_name}.{@name}"><span class="keyword">method</span><xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;<span class="symbol"><xsl:value-of select="@name"/></span></td>
            <td>
               <ul>
                  <xsl:for-each select="arg[@direction='in']">
                  <li><span class="type"><xsl:value-of select="@type"/></span><xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;<span class="symbol"><xsl:value-of select="@name"/></span> </li>
                  </xsl:for-each>
               </ul>
            </td>
            <td>
               <ul>
                  <xsl:for-each select="arg[@direction='out']">
                  <li><span class="type"><xsl:value-of select="@type"/></span><xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;<span class="symbol"><xsl:value-of select="@name"/></span> </li>
                  </xsl:for-each>
               </ul>
            </td>
            <td>
             <xsl:value-of select="annotation[@name='org.freedesktop.DBus.Description']/@value" />
            </td>
            </tr>
            <xsl:apply-templates select="annotation[@name!='org.freedesktop.DBus.Description']"/> 
            </xsl:for-each>
         </table>
        </xsl:if>

         <xsl:if test="signal">
         <table class="package">
            <tr><th colspan="3">Signals</th></tr>
            <tr class="sub"><th>Name</th><th>Out</th><th>Description</th></tr>
            <xsl:for-each select="signal">
            <tr>
            <td id="{$iface_name}.{@name}"><span class="keyword">signal</span><xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;<span class="symbol"><xsl:value-of select="@name"/></span></td>
            <td>
               <ul>
                  <xsl:for-each select="arg">
                  <li><span class="type"><xsl:value-of select="@type"/></span><xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;<span class="symbol"><xsl:value-of select="@name"/></span> </li>
                  </xsl:for-each>
               </ul>
            </td>
            <td>
             <xsl:value-of select="annotation[@name='org.freedesktop.DBus.Description']/@value" />
            </td>
            </tr>
            <xsl:apply-templates select="annotation[@name!='org.freedesktop.DBus.Description']"/> 
            </xsl:for-each>
         </table>
        </xsl:if>

        </div>
      </xsl:for-each>
    </body>
  </html>
</xsl:template>

<xsl:template match="interface/annotation"> 
  <li>
    <span class="keyword">annotation</span>
    <xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;
    <code><xsl:value-of select="@name"/></code><xsl:text> = </xsl:text>
    <code><xsl:value-of select="@value"/></code>
  </li>
</xsl:template>

<xsl:template match="method/annotation|property/annotation"> 
  <tr><td colspan="4">
    <span class="keyword">annotation</span>
    <xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;
    <code><xsl:value-of select="@name"/></code><xsl:text> = </xsl:text>
    <code><xsl:value-of select="@value"/></code>
  </td></tr>
</xsl:template>

<xsl:template match="signal/annotation"> 
  <tr><td colspan="3">
    <span class="keyword">annotation</span>
    <xsl:text disable-output-escaping='yes'>&amp;</xsl:text>nbsp;
    <code><xsl:value-of select="@name"/></code><xsl:text> = </xsl:text>
    <code><xsl:value-of select="@value"/></code>
  </td></tr>
</xsl:template>

</xsl:stylesheet>
