/*
 * Copyright 2011, 2012 Institut Pasteur.
 * 
 * This file is part of MiceProfiler.
 * 
 * MiceProfiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MiceProfiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MiceProfiler. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.MiceProfiler.Utilities;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLTools {

    public static Document loadDocument(File XMLFile) {
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            return docBuilder.parse(XMLFile);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveDocument(Document XMLDocument, File XMLFile) {
        try {
            XMLDocument.normalizeDocument();
            TransformerFactory transfac = TransformerFactory.newInstance();
            transfac.setAttribute("indent-number", 4);
            Transformer trans = transfac.newTransformer();

            trans.setOutputProperty(OutputKeys.METHOD, "xml");
            trans.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            trans.transform(new DOMSource(XMLDocument), new StreamResult(XMLFile));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
