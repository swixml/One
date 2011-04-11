/*--
 Copyright (C) 2003-2007 Wolf Paulus.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
 notice, this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions, and the disclaimer that follows
 these conditions in the documentation and/or other materials provided
 with the distribution.

 3. The end-user documentation included with the redistribution,
 if any, must include the following acknowledgment:
        "This product includes software developed by the
         SWIXML Project (http://www.swixml.org/)."
 Alternately, this acknowledgment may appear in the software itself,
 if and wherever such third-party acknowledgments normally appear.

 4. The name "Swixml" must not be used to endorse or promote products
 derived from this software without prior written permission. For
 written permission, please contact <info_AT_swixml_DOT_org>

 5. Products derived from this software may not be called "Swixml",
 nor may "Swixml" appear in their name, without prior written
 permission from the Swixml Project Management.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE SWIXML PROJECT OR ITS
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.
 ====================================================================

 This software consists of voluntary contributions made by many
 individuals on behalf of the Swixml Project and was originally
 created by Wolf Paulus <wolf_AT_swixml_DOT_org>. For more information
 on the Swixml Project, please see <http://www.swixml.org/>.
*/
package org.swixml;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Swixml XML Schema Generator
 *
 * @author Wolf Paulus
 */
public class SchemaGenerator {
  private static final Namespace XSNS = Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema");

  /**
   * Generate the Swixml XML Schema and writes it into a file.
   *
   * @param uri  <code> taget Namespace, e.g. http://www.swixml.org/2007/SwixmlTags
   * @param file <code>File</code> - target
   * @throws IOException - if schema cannot be printed into the given file.
   */
  public static void print(URI uri, File file) throws IOException {
    Element root = new Element("schema");
    Document schema = new Document(root);

    root.setAttribute("targetNamespace", uri.toString());
    root.setNamespace(XSNS);
    root.addNamespaceDeclaration(XSNS);
    root.setAttribute("elementFormDefault", "qualified");

    addSwixmlTags(root);

    Format format = Format.getPrettyFormat();
    format.setIndent("  ");
    XMLOutputter xout = new XMLOutputter(format);
    xout.output(schema, new FileWriter(file));
  }


  public static void print() throws IOException, URISyntaxException {
    Element root = new Element("schema");
    Document schema = new Document(root);

    root.setAttribute("targetNamespace", new URI("http://www.swixml.org/2007/Swixml").toString());
    root.setNamespace(XSNS);
    root.addNamespaceDeclaration(XSNS);
    root.setAttribute("elementFormDefault", "qualified");

    addSwixmlTags(root);

    Format format = Format.getPrettyFormat();
    format.setIndent("  ");
    XMLOutputter xout = new XMLOutputter(format);
    xout.output(schema, System.out);
  }
  /**
   * Loops through all tags in Swixml's yag lib.
   *
   * @param root <code>Element</code> - node tags will be inserted in
   * @return <code>Element</code> - passed in root.
   */
  private static Element addSwixmlTags(Element root) {
    TagLibrary taglib = new SwingEngine().getTaglib();
    for (Object name : new TreeSet(taglib.getTagClasses().keySet())) {
      Element elem = new Element("element", XSNS);
      elem.setAttribute("name", name.toString());
      elem.setContent(addSwixmlAttr((Factory) taglib.getTagClasses().get(name)));
      root.addContent(elem);
    }
    return root;
  }
  /*
  private static Element addCustomTags(Element root) {
    Map<String,LayoutConverter> lcs = LayoutConverterLibrary.getInstance().getLayoutConverters();
    for (LayoutConverter lc : lcs.values()) {
      Element elem = new Element("element", XSNS);
      elem.setAttribute("name", "layout");
      elem.setAttribute("name", name.toString());
      elem.setContent(addSwixmlAttr((Factory) taglib.getTagClasses().get(name)));
      root.addContent(elem);
    }
    return root;
  }
  */


  /**
   * Loops through all the available setters in the facktory and adds the attributes.
   *
   * @param factory <code>Factory</code.
   * @return <code>Element</code> - the complex type element, container for the attribute tags.
   */
  private static Element addSwixmlAttr(Factory factory) {
    Set<String> set = new HashSet<String>();
    Element elem = new Element("complexType", XSNS);
    elem.setAttribute("mixed", "true");
    Element sequence = new Element("sequence", XSNS).setContent(new Element("any", XSNS));
    sequence.setAttribute("minOccurs", "0");
    sequence.setAttribute("maxOccurs", "unbounded");
    elem.setContent(sequence);
    for (Object obj : factory.getSetters()) {
      Element e = new Element("attribute", XSNS);
      Method m = (Method) obj;
      String s = m.getName();
      if (s.startsWith(Factory.SETTER_ID))
        s = s.substring(Factory.SETTER_ID.length()).toLowerCase();
      if (s.startsWith(Factory.ADDER_ID))
        s = s.substring(Factory.ADDER_ID.length()).toLowerCase();
      boolean b = boolean.class.equals(m.getParameterTypes()[0]);
      if (!set.contains(s)) {
        e.setAttribute("name", s);
        e.setAttribute("type", b ? XSNS.getPrefix() + ":boolean" : XSNS.getPrefix() + ":string");
        elem.addContent(e);
        set.add(s);
      }
    }
    //
    // add custom swixml atributes
    //
    for (Field field : Parser.class.getFields()) {
      if (field.getName().startsWith("ATTR_") && !field.getName().endsWith("PREFIX") && Modifier.isFinal(field.getModifiers())) {
        Element e = new Element("attribute", XSNS);
        try {
          String s = field.get(Parser.class).toString().toLowerCase();
          if (!set.contains(s)) {
            e.setAttribute("name", s);
            e.setAttribute("type", XSNS.getPrefix() + ":string");
            elem.addContent(e);
            set.add(s);
          }
        } catch (IllegalAccessException e1) {
          e1.printStackTrace();
        }
      }
    }
    return elem;
  }

  /**
   * Writes teh schema into the given file. defaults to userhome/swixml.xsd
   *
   * @param args <code>String[]<code> file name.
   */
  public static void main(String[] args) {

    try {
      SchemaGenerator.print();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (URISyntaxException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    File file;
    try {
      if (args != null && args[0]!=null && 0 < args[0].length()) {
        file = new File(args[0]);
        SchemaGenerator.print(new URI("http://www.swixml.org/2007/Swixml"), file);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
