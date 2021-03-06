/*
 * Copyright (C) 2018 Philip M. Trenwith
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package LuceneIndexer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Philip M. Trenwith
 */
public class cConfig 
{
  private static volatile cConfig m_oInstance = null;
  private File m_oConfigFile = null;
  private static String sConfigKey = "DriveIndex.Config";
  private static String sConfigKeyValue = "DriveIndex.xml";
  
  private String m_sIndexLocation = "";
  private boolean m_bHashDocuments = false;
  private boolean m_bHashFirstBlockOnly = false;
  private int m_iScanThreads = 10;
  private String m_sDefaultCategory = "";
  private HashMap<String, String> m_oCategoryForFile = new HashMap();
  private HashSet<String> m_oScanDrives = new HashSet();
  private NodeList m_aoNodes = null;
  private int m_iHourOfDay = 0;
  private boolean m_bCountdown = false;
  private boolean m_bEnableSchedular = true;
  
  public static cConfig instance()
  {
    cConfig oInstance = cConfig.m_oInstance;
    if (oInstance == null)
    {
      synchronized (cConfig.class)
      {
        oInstance = cConfig.m_oInstance;
        if (oInstance == null)
        {
          cConfig.m_oInstance = oInstance = new cConfig();
        }
      }
    }
    return oInstance;
  }
  
  private cConfig()
  {
    loadConfig();
  }
  
  private File getConfigFile()
  {
    String sFilename = System.getProperty(sConfigKey, sConfigKeyValue);
    m_oConfigFile = new File(sFilename);
    if (!m_oConfigFile.exists())
    {
      saveConfig();
    }
    return m_oConfigFile;
  }
  
  private void loadConfig()
  {
    try
    {
      m_oConfigFile = getConfigFile();
   
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document oDocument = dBuilder.parse(m_oConfigFile);      
      Element oDocumentElement = oDocument.getDocumentElement();
      m_aoNodes = oDocumentElement.getChildNodes();
      
      Element oIndexElement = getElement("Index");
      m_sIndexLocation = getChildElement(oIndexElement, "Location").getTextContent();
      m_bHashDocuments = Boolean.parseBoolean(getChildElement(oIndexElement, "HashDocuments").getTextContent());
      m_bHashFirstBlockOnly = Boolean.parseBoolean(getChildElement(oIndexElement, "HashFirstBlockOnly").getTextContent());
      m_iScanThreads = Integer.parseInt(getChildElement(oIndexElement, "ScanThreads").getTextContent());
      Element oDrivesElement = getElement("Drives");
      ArrayList<Element> lsDriveTypes = getChildElements(oDrivesElement, "Type");
      for (Element oDriveType :lsDriveTypes)
      {
        boolean bScan = Boolean.parseBoolean(""+oDriveType.getAttribute("Scan"));
        if (bScan)
        {
          m_oScanDrives.add(oDriveType.getTextContent());
        }
      }
      
      Element oCategoriesElement = getElement("Categories");
      m_sDefaultCategory = oCategoriesElement.getAttribute("Default");
      ArrayList<Element> lsCategories = getChildElements(oCategoriesElement, "Category");
      for (Element oCategory :lsCategories)
      {
        String sName = oCategory.getAttribute("Name");
        String sExtensions = oCategory.getTextContent();
        String[] lsExtensions = sExtensions.split(",");
        for (String sExtension: lsExtensions)
        {
          m_oCategoryForFile.put(sExtension, sName);
        }
      }
      
      Element oScheduleElement = getElement("Schedule");
      try
      {
        m_iHourOfDay = Integer.parseInt(getChildElement(oScheduleElement, "HourOfDay").getTextContent());
      }
      catch (Exception ex)
      {
        m_iHourOfDay = 23;
      }
      m_bCountdown = Boolean.parseBoolean(getChildElement(oScheduleElement, "Countdown").getTextContent());
      m_bEnableSchedular = Boolean.parseBoolean(getChildElement(oScheduleElement, "Enabled").getTextContent());
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void saveConfig()
  {
    try
    {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      // root elements
      Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement("LuceneIndex");
      doc.appendChild(rootElement);

      Element oIndex = doc.createElement("Index");
      rootElement.appendChild(oIndex);
      Element oLocation = doc.createElement("Location");
      oIndex.appendChild(oLocation);
      oLocation.setTextContent("index");
      Element oHashDocuments = doc.createElement("HashDocuments");
      oIndex.appendChild(oHashDocuments);
      oHashDocuments.setTextContent("true");
      Element oHashFirstBlockOnly = doc.createElement("HashFirstBlockOnly");
      oIndex.appendChild(oHashFirstBlockOnly);
      oHashFirstBlockOnly.setTextContent("true");
      Element oScanThreads = doc.createElement("ScanThreads");
      oIndex.appendChild(oScanThreads);
      oScanThreads.setTextContent("10");
      
      Element oDrives = doc.createElement("Drives");
      rootElement.appendChild(oDrives);
      Element oType = doc.createElement("Type");
      oDrives.appendChild(oType);
      oType.setAttribute("Scan", "true");
      oType.setTextContent("Local Disk");
      Element oType1 = doc.createElement("Type");
      oDrives.appendChild(oType1);
      oType1.setAttribute("Scan", "true");
      oType1.setTextContent("Network Drive");
      
      Element oCategories = doc.createElement("Categories");
      oCategories.setAttribute("Default", "File");
      rootElement.appendChild(oCategories);
      Element oCategory = doc.createElement("Category");
      oCategories.appendChild(oCategory);
      oCategory.setAttribute("Name", "Document");
      oCategory.setTextContent("txt,doc,docx,pdf,xls,xlsx");
      Element oCategory1 = doc.createElement("Category");
      oCategories.appendChild(oCategory1);
      oCategory1.setAttribute("Name", "Video");
      oCategory1.setTextContent("avi,mp4,mkv,flv,wmv,mov");
      Element oCategory2 = doc.createElement("Category");
      oCategories.appendChild(oCategory2);
      oCategory2.setAttribute("Name", "Image");
      oCategory2.setTextContent("jpeg,jpg,png,tiff,bmp");
      Element oCategory3 = doc.createElement("Category");
      oCategories.appendChild(oCategory3);
      oCategory3.setAttribute("Name", "Audio");
      oCategory3.setTextContent("mp3,flac,m4a,wma,gp3,wav");
      
      Element oSchedule = doc.createElement("Schedule");
      Element oScheduleEnabled = doc.createElement("Enabled");
      oSchedule.appendChild(oScheduleEnabled);
      oScheduleEnabled.setTextContent("true");
      Element oDisplayNextRunLabel = doc.createElement("Countdown");
      oSchedule.appendChild(oDisplayNextRunLabel);
      oDisplayNextRunLabel.setTextContent("true");
      Element oHourOfDay = doc.createElement("HourOfDay");
      oSchedule.appendChild(oHourOfDay);
      oHourOfDay.setTextContent("21");
      rootElement.appendChild(oSchedule);
      
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(m_oConfigFile);
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(source, result);
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public Element getElement(String sTagName)
  {
    for (int i = 0; i < m_aoNodes.getLength(); ++i)
    {
      Node oNode = m_aoNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oChild = (Element) oNode;
        if(oChild.getTagName().equals(sTagName))
        {
          return oChild;
        }
      }
    }
    return null;
  }
  
  public ArrayList<Element> getElements(String sTagName)
  {
    ArrayList<Element> lsResults = new ArrayList();
    
    for (int i = 0; i < m_aoNodes.getLength(); ++i)
    {
      Node oNode = m_aoNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oElement = (Element) oNode;
        if(oElement.getTagName().equals(sTagName))
        {
          lsResults.add(oElement);
        }
      }
    } 
    return lsResults;
  }
  
  public Element getChildElement(Element oParentElement, String sTagName)
  {
    NodeList oChildNodes = oParentElement.getChildNodes();
    for (int i = 0; i < oChildNodes.getLength(); ++i)
    {
      Node oNode = oChildNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oChild = (Element) oNode;
        if(oChild.getTagName().equals(sTagName))
        {
          return oChild;
        }
      }
    }
    return null;
  }
  
  public ArrayList<Element> getChildElements(Element oParentElement, String sTagName)
  {
    ArrayList<Element> lsResults = new ArrayList();
    NodeList oChildNodes = oParentElement.getChildNodes();
    for (int i = 0; i < oChildNodes.getLength(); ++i)
    {
      Node oNode = oChildNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oElement = (Element) oNode;
        if(oElement.getTagName().equals(sTagName))
        {
          lsResults.add(oElement);
        }
      }
    } 
    return lsResults;
  }
  
  public String getIndexLocation()
  {
    return m_sIndexLocation;
  }
  
  public boolean getHashDocuments()
  {
    return m_bHashDocuments;
  }
  
  public boolean getHashFirstBlockOnly()
  {
    return m_bHashFirstBlockOnly;
  }
  
  public int getScanThreads()
  {
    return m_iScanThreads;
  }
  
  public String getCategory(File oFile)
  {
    String ext = FilenameUtils.getExtension(oFile.getAbsolutePath());
    if (m_oCategoryForFile.containsKey(ext))
    {
      return m_oCategoryForFile.get(ext);
    }
    else
    {
      return m_sDefaultCategory;
    }
  }
  
  public boolean getScanDriveType(String sType)
  {
    return m_oScanDrives.contains(sType);
  }
  
  public int getHourOfDay()
  {
    return m_iHourOfDay;
  }

  public boolean getCountdown()
  {
    return m_bCountdown;
  }
  
  public boolean getEnableScheduler()
  {
    return m_bEnableSchedular;
  }
}
