/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

/**
 * <code>TemplateSet</code> manages a collection of templates and makes them
 * persistent.
 */
public class TemplateSet {

	private static final String NAME_ATTRIBUTE= "name"; //$NON-NLS-1$
	private static final String DESCRIPTION_ATTRIBUTE= "description"; //$NON-NLS-1$
	private static final String CONTEXT_ATTRIBUTE= "context"; //$NON-NLS-1$
	private static final String ENABLED_ATTRIBUTE= "enabled"; //$NON-NLS-1$

	private List fTemplates= new ArrayList();
	private String fTemplateTag;
	
	public TemplateSet(String templateTag) {
		fTemplateTag= templateTag;
	}
	
	/**
	 * Convenience method for reading templates from a file.
	 * 
	 * @see #addFromStream(InputStream, boolean)
	 */
	public void addFromFile(File file, boolean allowDuplicates) throws CoreException {
		InputStream stream= null;

		try {
			stream= new FileInputStream(file);
			addFromStream(stream, allowDuplicates, false);

		} catch (IOException e) {
			throwReadException(e);

		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {}
		}		
	}
	
	public String getTemplateTag() {
		return fTemplateTag;
	}
	

	/**
	 * Reads templates from a XML stream and adds them to the templates
	 */	
	public void addFromStream(InputStream stream, boolean allowDuplicates, boolean doTranslations) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder parser= factory.newDocumentBuilder();		
			Document document= parser.parse(new InputSource(stream));
			
			NodeList elements= document.getElementsByTagName(getTemplateTag());
			
			int count= elements.getLength();
			for (int i= 0; i != count; i++) {
				Node node= elements.item(i);					
				NamedNodeMap attributes= node.getAttributes();

				if (attributes == null)
					continue;

				String name= getAttributeValue(attributes, NAME_ATTRIBUTE);
				String description= getAttributeValue(attributes, DESCRIPTION_ATTRIBUTE);
				if (doTranslations) {
					description= translateString(description);
				} 
				String context= getAttributeValue(attributes, CONTEXT_ATTRIBUTE);
				Node enabledNode= attributes.getNamedItem(ENABLED_ATTRIBUTE);

				if (name == null || description == null || context == null)
					throw new SAXException(TemplateMessages.getString("TemplateSet.error.missing.attribute")); //$NON-NLS-1$

				boolean enabled= (enabledNode == null) || (enabledNode.getNodeValue().equals("true")); //$NON-NLS-1$

				StringBuffer buffer= new StringBuffer();
				NodeList children= node.getChildNodes();
				for (int j= 0; j != children.getLength(); j++) {
					String value= children.item(j).getNodeValue();
					if (value != null)
						buffer.append(value);
				}
				String pattern= buffer.toString().trim();
				if (doTranslations) {
					pattern= translateString(pattern);
				}				

				Template template= new Template(name, description, context, pattern);
				
				String message= validateTemplate(template);
				if (message == null) {
					template.setEnabled(enabled);
					if (!allowDuplicates) {
						Template[] templates= getTemplates(name);
						for (int k= 0; k < templates.length; k++) {
							remove(templates[k]);
						}
					}
					add(template);					
				} else {
					JavaPlugin.logErrorMessage("Template " + name + " not added: " + message);  //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (ParserConfigurationException e) {
			throwReadException(e);
		} catch (IOException e) {
			throwReadException(e);
		} catch (SAXException e) {
			throwReadException(e);
		}
	}
	
	private String translateString(String str) {
		int idx= str.indexOf('%');
		if (idx == -1) {
			return str;
		}
		StringBuffer buf= new StringBuffer();
		int k= 0;
		while (idx != -1) {
			buf.append(str.substring(k, idx));
			for (k= idx + 1; k < str.length() && !Character.isWhitespace(str.charAt(k)); k++) {
				// loop
			}
			String key= str.substring(idx + 1, k);
			buf.append(TemplateMessages.getString(key));
			idx= str.indexOf('%', k);
		}
		buf.append(str.substring(k));
		return buf.toString();
	}
	
	protected String validateTemplate(Template template) throws CoreException {
		ContextType type= ContextTypeRegistry.getInstance().getContextType(template.getContextTypeName());
		if (type == null) {
			return "Unknown context type: " + template.getContextTypeName(); //$NON-NLS-1$
		}
		return type.validate(template.getPattern());
	}
	
	private String getAttributeValue(NamedNodeMap attributes, String name) {
		Node node= attributes.getNamedItem(name);

		return node == null
			? null
			: node.getNodeValue();
	}

	/**
	 * Convenience method for saving to a file.
	 * 
	 * @see #saveToStream(OutputStream)
	 */
	public void saveToFile(File file) throws CoreException {
		OutputStream stream= null;

		try {
			stream= new FileOutputStream(file);
			saveToStream(stream);

		} catch (IOException e) {
			throwWriteException(e);

		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {}
		}
	}
		
	/**
	 * Saves the template set as XML.
	 */
	public void saveToStream(OutputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();

			Node root= document.createElement("templates"); //$NON-NLS-1$
			document.appendChild(root);
			
			for (int i= 0; i != fTemplates.size(); i++) {
				Template template= (Template) fTemplates.get(i);
				
				Node node= document.createElement(getTemplateTag());
				root.appendChild(node);
				
				NamedNodeMap attributes= node.getAttributes();
				
				Attr name= document.createAttribute(NAME_ATTRIBUTE);
				name.setValue(template.getName());
				attributes.setNamedItem(name);
	
				Attr description= document.createAttribute(DESCRIPTION_ATTRIBUTE);
				description.setValue(template.getDescription());
				attributes.setNamedItem(description);
	
				Attr context= document.createAttribute(CONTEXT_ATTRIBUTE);
				context.setValue(template.getContextTypeName());
				attributes.setNamedItem(context);			

				Attr enabled= document.createAttribute(ENABLED_ATTRIBUTE);
				enabled.setValue(template.isEnabled() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
				attributes.setNamedItem(enabled);
				
				Text pattern= document.createTextNode(template.getPattern());
				node.appendChild(pattern);			
			}		
			
			OutputFormat format = new OutputFormat();
			format.setPreserveSpace(true);
			Serializer serializer = SerializerFactory.getSerializerFactory("xml").makeSerializer(stream, format); //$NON-NLS-1$
			serializer.asDOMSerializer().serialize(document);

		} catch (ParserConfigurationException e) {
			throwWriteException(e);
		} catch (IOException e) {
			throwWriteException(e);
		}		
	}

	private static void throwReadException(Throwable t) throws CoreException {
		int code;
		if (t instanceof SAXException)
			code= IJavaStatusConstants.TEMPLATE_PARSE_EXCEPTION;
		else
			code= IJavaStatusConstants.TEMPLATE_IO_EXCEPTION;
		IStatus status= JavaUIStatus.createError(code, TemplateMessages.getString("TemplateSet.error.read"), t); //$NON-NLS-1$
		throw new JavaUIException(status);
	}
	
	private static void throwWriteException(Throwable t) throws CoreException {
		IStatus status= JavaUIStatus.createError(IJavaStatusConstants.TEMPLATE_IO_EXCEPTION,
			TemplateMessages.getString("TemplateSet.error.write"), t); //$NON-NLS-1$
		throw new JavaUIException(status);
	}

	/**
	 * Adds a template to the set.
	 */
	public void add(Template template) {
		if (exists(template))
			return; // ignore duplicate
		
		fTemplates.add(template);
	}

	private boolean exists(Template template) {
		for (Iterator iterator = fTemplates.iterator(); iterator.hasNext();) {
			Template anotherTemplate = (Template) iterator.next();

			if (template.equals(anotherTemplate))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a template to the set.
	 */	
	public void remove(Template template) {
		fTemplates.remove(template);
	}

	/**
	 * Empties the set.
	 */		
	public void clear() {
		fTemplates.clear();
	}
	
	/**
	 * Returns all templates.
	 */
	public Template[] getTemplates() {
		return (Template[]) fTemplates.toArray(new Template[fTemplates.size()]);
	}
	
	/**
	 * Returns all templates with a given name.
	 */
	public Template[] getTemplates(String name) {
		ArrayList res= new ArrayList();
		for (Iterator iterator= fTemplates.iterator(); iterator.hasNext();) {
			Template curr= (Template) iterator.next();
			if (curr.getName().equals(name)) {
				res.add(curr);
			}
		}
		return (Template[]) res.toArray(new Template[res.size()]);
	}
	
	/**
	 * Returns the first templates with the given name.
	 */
	public Template getFirstTemplate(String name) {
		for (Iterator iterator= fTemplates.iterator(); iterator.hasNext();) {
			Template curr= (Template) iterator.next();
			if (curr.getName().equals(name)) {
				return curr;
			}
		}
		return null;
	}	
	
}

