/*************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *  the BSD license.
 *  
 *  Copyright (c) 2003, 2009 by Sun Microsystems, Inc.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of Sun Microsystems, Inc. nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *     
 *************************************************************************/

package org.openoffice.inspector;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.MethodConcept;
import com.sun.star.beans.Property;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XIntrospection;
import com.sun.star.beans.XIntrospectionAccess;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XHierarchicalNameAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.reflection.TypeDescriptionSearchDepth;
import com.sun.star.reflection.XConstantTypeDescription;
import com.sun.star.reflection.XConstantsTypeDescription;
import com.sun.star.reflection.XIdlClass;
import com.sun.star.reflection.XIdlField;
import com.sun.star.reflection.XIdlMethod;
import com.sun.star.reflection.XIdlReflection;
import com.sun.star.reflection.XIndirectTypeDescription;
import com.sun.star.reflection.XInterfaceTypeDescription;
import com.sun.star.reflection.XPropertyTypeDescription;
import com.sun.star.reflection.XServiceTypeDescription;
import com.sun.star.reflection.XTypeDescription;
import com.sun.star.reflection.XTypeDescriptionEnumeration;
import com.sun.star.reflection.XTypeDescriptionEnumerationAccess;
import com.sun.star.ucb.XSimpleFileAccess;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Type;
import com.sun.star.uno.TypeClass;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.URL;
import java.util.List;
import java.util.Vector;

public class Introspector extends WeakBase
{

  private static volatile Introspector instance = null;
  
  private XIntrospection          xIntrospection;
  private XMultiComponentFactory  xMultiComponentFactory;
  private XComponentContext       xComponentContext;
  private XTypeDescriptionEnumerationAccess xTDEnumerationAccess;
  private static XComponentContext xOfficeComponentContext;
  private XIdlReflection xIdlReflection;
  private URL openHyperlink;
  private XSimpleFileAccess xSimpleFileAccess = null;

  public static Introspector getIntrospector()
  {
    return instance;
  }

  public static synchronized Introspector getIntrospector(XComponentContext xComponentContext)
  {
    if (instance == null)
    {
      Introspector.instance = new Introspector(xComponentContext);
    }
    return Introspector.instance;
  }

  /** 
   * Creates a new instance of Introspection.
   */
  private Introspector(XComponentContext xComponentContext)
  {
    try
    {
      this.xComponentContext      = xComponentContext;
      this.xMultiComponentFactory = xComponentContext.getServiceManager();
      Object obj = xMultiComponentFactory.createInstanceWithContext(
        "com.sun.star.beans.Introspection", xComponentContext);
      this.xIntrospection = (XIntrospection)
        UnoRuntime.queryInterface(XIntrospection.class, obj);
      Object oCoreReflection = getXMultiComponentFactory().
        createInstanceWithContext("com.sun.star.reflection.CoreReflection", getXComponentContext());
      this.xIdlReflection = (XIdlReflection)
        UnoRuntime.queryInterface(XIdlReflection.class, oCoreReflection);
      initTypeDescriptionManager();
    }
    catch (Exception exception)
    {
      exception.printStackTrace();
    }
  }

  public XComponentContext getXComponentContext()
  {
    return this.xComponentContext;
  }

  protected XMultiComponentFactory getXMultiComponentFactory()
  {
    return this.xMultiComponentFactory;
  }

  public XIntrospectionAccess getXIntrospectionAccess(Object unoComponent)
  {
    return this.xIntrospection.inspect(unoComponent);
  }

  public boolean isContainer(Object _oUnoObject)
  {
    boolean bIsContainer = false;
    try
    {
      XIntrospectionAccess xIntrospectionAccessObject = getXIntrospectionAccess(_oUnoObject);
      if (xIntrospectionAccessObject != null)
      {
        XEnumerationAccess xEnumerationAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xIntrospectionAccessObject.queryAdapter(new Type(XEnumerationAccess.class)));
        if (xEnumerationAccess != null)
        {
          XEnumeration xEnumeration = xEnumerationAccess.createEnumeration();
          bIsContainer = xEnumeration.hasMoreElements();
        }
        if (!bIsContainer)
        {
          XIndexAccess xIndexAccess = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xIntrospectionAccessObject.queryAdapter(new Type(XIndexAccess.class)));
          if (xIndexAccess != null)
          {
            bIsContainer = (xIndexAccess.getCount() > 0);
          }
        }
      }
    }
    catch (IllegalTypeException ex)
    {
      ex.printStackTrace();
    }
    return bIsContainer;
  }

  //  add all containers for the given object to the tree under the node
  //  parent
  public Object[] getUnoObjectsOfContainer(Object _oUnoParentObject)
  {
    Object[] oRetComponents = null;
    try
    {
      Vector oRetComponentsVector = new Vector();
      XIntrospectionAccess xIntrospectionAccessObject = getXIntrospectionAccess(_oUnoParentObject);
      if (xIntrospectionAccessObject != null)
      {
        XEnumerationAccess xEnumerationAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xIntrospectionAccessObject.queryAdapter(new Type(XEnumerationAccess.class)));
        if (xEnumerationAccess != null)
        {
          XEnumeration xEnumeration = xEnumerationAccess.createEnumeration();
          while (xEnumeration.hasMoreElements())
          {
            oRetComponentsVector.add(xEnumeration.nextElement());
          }
        }
        XIndexAccess xIndexAccess = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xIntrospectionAccessObject.queryAdapter(new Type(XIndexAccess.class)));
        if (xIndexAccess != null)
        {
          XIdlMethod mMethod = xIntrospectionAccessObject.getMethod("getByIndex", com.sun.star.beans.MethodConcept.INDEXCONTAINER);
          for (int i = 0; i < xIndexAccess.getCount(); i++)
          {
            Object[][] aParamInfo = new Object[1][1];
            aParamInfo[0] = new Integer[]{new Integer(i)};
            oRetComponentsVector.add(mMethod.invoke(_oUnoParentObject, aParamInfo));
          }
        }
      }
      if (oRetComponentsVector != null)
      {
        oRetComponents = new Object[oRetComponentsVector.size()];
        oRetComponentsVector.toArray(oRetComponents);
      }
    }
    catch (Exception exception)
    {
      System.err.println(exception);
    }
    return oRetComponents;
  }

  public XIdlMethod[] getMethodsOfInterface(Type type)
  {
    try
    {
      XIdlClass xIdlClass = this.xIdlReflection.forName(type.getTypeName());
      return xIdlClass.getMethods();
    }
    catch (Exception e)
    {
      System.err.println(e);
      return null;
    }
  }

  public XIdlField[] getFieldsOfType(Type type)
  {
    try
    {
      XIdlClass xIdlClass = this.xIdlReflection.forName(type.getTypeName());
      return xIdlClass.getFields();
    }
    catch (Exception e)
    {
      System.err.println(e);
      return null;
    }
  }

  public boolean hasMethods(Object unoObject)
  {
    boolean bHasMethods = (getMethods(unoObject).length > 0);
    return bHasMethods;
  }

  //  add all methods for the given object to the tree under the node parent
  public XIdlMethod[] getMethods(Object _oUnoParentObject)
  {
    try
    {
      XIntrospectionAccess xIntrospectionAccess = getXIntrospectionAccess(_oUnoParentObject);
      if (xIntrospectionAccess != null)
      {
        XIdlMethod[] xIdlMethods = xIntrospectionAccess.getMethods(MethodConcept.ALL - MethodConcept.DANGEROUS);
        return xIdlMethods;
      }
    }
    catch (Exception e)
    {
      System.err.println(e);
    }
    return null;
  }

  public boolean hasProperties(Object _oUnoObject)
  {
    boolean bHasProperties = (getProperties(_oUnoObject).length > 0);
    return bHasProperties;
  }

  public Property[] getProperties(Object unoParentObject)
  {
    try
    {
      XIntrospectionAccess xIntrospectionAccess = getXIntrospectionAccess(unoParentObject);
      if (xIntrospectionAccess != null)
      {
        Property[] aProperties = xIntrospectionAccess.getProperties(com.sun.star.beans.PropertyConcept.ATTRIBUTES + com.sun.star.beans.PropertyConcept.PROPERTYSET);
        return aProperties;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  public static Object getPropertyValue(Property prop, Object unoObject)
  {
    try
    {
      XPropertySet ps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, unoObject);

      if(ps != null)
        return ps.getPropertyValue(prop.Name);
      
      return null;
    }
    catch(UnknownPropertyException ex)
    {
      System.err.println("Caught UnknownPropertyException!");
      System.err.println("prop=" + prop.Name);
      System.err.println("unoObject=" + unoObject);
    }
    catch(WrappedTargetException ex)
    {
      ex.printStackTrace();
    }
    return null;
  }

  public Property[] getProperties(Object _oUnoObject, String _sServiceName)
  {
    Property[] aProperties = getProperties(_oUnoObject);
    List aListOfProperties = java.util.Arrays.asList(aProperties);
    Vector aPropertiesVector = new Vector(aListOfProperties);
    if (aProperties != null)
    {
      XPropertyTypeDescription[] xPropertyTypeDescriptions = getPropertyDescriptionsOfService(_sServiceName);
      for (int i = aProperties.length - 1; i >= 0; i--)
      {
        if (!hasByName(xPropertyTypeDescriptions, _sServiceName + "." + aProperties[i].Name))
        {
          aPropertiesVector.remove(i);
        }
      }
    }
    Property[] aRetProperties = new Property[aPropertiesVector.size()];
    aPropertiesVector.toArray(aRetProperties);
    return aRetProperties;
  }

  public Type[] getInterfaces(Object _oUnoObject, String _sServiceName)
  {
    Type[] aTypes = getInterfaces(_oUnoObject);
    List aListOfTypes = java.util.Arrays.asList(aTypes);
    Vector aTypesVector = new Vector(aListOfTypes);
    if (aTypes != null)
    {
      XInterfaceTypeDescription[] xInterfaceTypeDescriptions = getInterfaceDescriptionsOfService(_sServiceName);
      for (int i = aTypes.length - 1; i >= 0; i--)
      {
        if (!hasByName(xInterfaceTypeDescriptions, aTypes[i].getTypeName()))
        {
          aTypesVector.remove(i);
        }
      }
    }
    Type[] aRetTypes = new Type[aTypesVector.size()];
    aTypesVector.toArray(aRetTypes);
    return aRetTypes;
  }

  public boolean hasInterfaces(Object _oUnoObject)
  {
    return (getInterfaces(_oUnoObject).length > 0);
  }

  public Type[] getInterfaces(Object _oUnoParentObject)
  {
    Type[] aTypes = new Type[]{};
    XTypeProvider xTypeProvider = (XTypeProvider) UnoRuntime.queryInterface(XTypeProvider.class, _oUnoParentObject);
    if (xTypeProvider != null)
    {
      aTypes = xTypeProvider.getTypes();
    }
    return aTypes;
  }
  
  public static String[] getSupportedServiceNames(Object unoObject)
  {
    if(unoObject == null)
      return null;
    
    try
    {
      XServiceInfo xServiceInfo = 
        (XServiceInfo)UnoRuntime.queryInterface(XServiceInfo.class, unoObject);
    
      return xServiceInfo.getSupportedServiceNames();
    }
    catch(Exception ex)
    {
      System.err.println(ex.getLocalizedMessage());
      return null;
    }
  }

  public static boolean isObjectSequence(Object _oUnoObject)
  {
    Type aType = AnyConverter.getType(_oUnoObject);
    return aType.getTypeClass().getValue() == TypeClass.SEQUENCE_value;
  }

  public static boolean isObjectPrimitive(Object _oUnoObject)
  {
    boolean breturn = false;
    if (_oUnoObject != null)
    {
      Type aType = AnyConverter.getType(_oUnoObject);
      breturn = isObjectPrimitive(_oUnoObject.getClass(), aType.getTypeClass());
    }
    return breturn;
  }

  public static boolean isPrimitive(TypeClass _typeClass)
  {
    return ((_typeClass == TypeClass.BOOLEAN) || (_typeClass == TypeClass.BYTE) || (_typeClass == TypeClass.CHAR) || (_typeClass == TypeClass.DOUBLE) || (_typeClass == TypeClass.ENUM) || (_typeClass == TypeClass.FLOAT) || (_typeClass == TypeClass.HYPER) || (_typeClass == TypeClass.LONG) || (_typeClass == TypeClass.SHORT) || (_typeClass == TypeClass.STRING) || (_typeClass == TypeClass.UNSIGNED_HYPER) || (_typeClass == TypeClass.UNSIGNED_LONG) || (_typeClass == TypeClass.UNSIGNED_SHORT));
  }

  public static boolean isObjectPrimitive(Class _oUnoClass, TypeClass _typeClass)
  {
    return !((!_oUnoClass.isPrimitive()) && (_typeClass != TypeClass.ARRAY) && (_typeClass != TypeClass.BOOLEAN) && (_typeClass != TypeClass.BYTE) && (_typeClass != TypeClass.CHAR) && (_typeClass != TypeClass.DOUBLE) && (_typeClass != TypeClass.ENUM) && (_typeClass != TypeClass.FLOAT) && (_typeClass != TypeClass.HYPER) && (_typeClass != TypeClass.LONG) && (_typeClass != TypeClass.SHORT) && (_typeClass != TypeClass.STRING) && (_typeClass != TypeClass.UNSIGNED_HYPER) && (_typeClass != TypeClass.UNSIGNED_LONG) && (_typeClass != TypeClass.UNSIGNED_SHORT));
  }

  protected void initTypeDescriptionManager()
  {
    try
    {
      Object oTypeDescriptionManager = getXComponentContext().getValueByName("/singletons/com.sun.star.reflection.theTypeDescriptionManager");
      this.xTDEnumerationAccess = (XTypeDescriptionEnumerationAccess) UnoRuntime.queryInterface(XTypeDescriptionEnumerationAccess.class, oTypeDescriptionManager);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  public XTypeDescriptionEnumerationAccess getXTypeDescriptionEnumerationAccess()
  {
    return this.xTDEnumerationAccess;
  }

  public XConstantTypeDescription[] getFieldsOfConstantGroup(String typeClass)
  {
    XConstantTypeDescription[] xConstantTypeDescriptions = null;
    try
    {
      TypeClass[] eTypeClasses = new com.sun.star.uno.TypeClass[1];
      eTypeClasses[0] = com.sun.star.uno.TypeClass.CONSTANTS;
      XTypeDescriptionEnumeration xTDEnumeration = 
        this.xTDEnumerationAccess.createTypeDescriptionEnumeration(
          getModuleName(typeClass), 
          eTypeClasses, TypeDescriptionSearchDepth.INFINITE);
      while (xTDEnumeration.hasMoreElements())
      {
        XTypeDescription xTD = xTDEnumeration.nextTypeDescription();
        if (xTD.getName().equals(typeClass))
        {
          XConstantsTypeDescription xConstantsTypeDescription = (XConstantsTypeDescription) UnoRuntime.queryInterface(XConstantsTypeDescription.class, xTD);
          xConstantTypeDescriptions = xConstantsTypeDescription.getConstants();
        }
        String sName = xTD.getName();
      }
      return xConstantTypeDescriptions;
    }
    catch (java.lang.Exception e)
    {
      System.out.println(System.out);
    }
    return null;
  }

  private XServiceTypeDescription getServiceTypeDescription(String _sServiceName, TypeClass _eTypeClass)
  {
    try
    {
      if (_sServiceName.length() > 0)
      {
        TypeClass[] eTypeClasses = new com.sun.star.uno.TypeClass[2];
        eTypeClasses[0] = com.sun.star.uno.TypeClass.SERVICE;
        eTypeClasses[1] = _eTypeClass;
        XTypeDescriptionEnumeration xTDEnumeration = getXTypeDescriptionEnumerationAccess().createTypeDescriptionEnumeration(Introspector.getModuleName(_sServiceName), eTypeClasses, TypeDescriptionSearchDepth.INFINITE);
        while (xTDEnumeration.hasMoreElements())
        {
          XTypeDescription xTD = xTDEnumeration.nextTypeDescription();
          if (xTD.getName().equals(_sServiceName))
          {
            XServiceTypeDescription xServiceTypeDescription = (XServiceTypeDescription) UnoRuntime.queryInterface(XServiceTypeDescription.class, xTD);
            return xServiceTypeDescription;
          }
        }
      }
      return null;
    }
    catch (Exception ex)
    {
      ex.printStackTrace(System.out);
      return null;
    }
  }

  public XPropertyTypeDescription[] getPropertyDescriptionsOfService(String _sServiceName)
  {
    try
    {
      XServiceTypeDescription xServiceTypeDescription = getServiceTypeDescription(_sServiceName, com.sun.star.uno.TypeClass.PROPERTY);
      if (xServiceTypeDescription != null)
      {
        XPropertyTypeDescription[] xPropertyTypeDescriptions = xServiceTypeDescription.getProperties();
        return xPropertyTypeDescriptions;
      }
    }
    catch (java.lang.Exception e)
    {
      System.out.println(System.out);
    }
    return new XPropertyTypeDescription[]{};
  }

  public XTypeDescription getReferencedType(String _sTypeName)
  {
    XTypeDescription xTypeDescription = null;
    try
    {
      XHierarchicalNameAccess xHierarchicalNameAccess = 
        (XHierarchicalNameAccess) UnoRuntime.queryInterface(
          XHierarchicalNameAccess.class, this.xTDEnumerationAccess);
      if (xHierarchicalNameAccess != null)
      {
        if (xHierarchicalNameAccess.hasByHierarchicalName(_sTypeName))
        {
          XIndirectTypeDescription xIndirectTypeDescription = (XIndirectTypeDescription) UnoRuntime.queryInterface(XIndirectTypeDescription.class, xHierarchicalNameAccess.getByHierarchicalName(_sTypeName));
          if (xIndirectTypeDescription != null)
          {
            xTypeDescription = xIndirectTypeDescription.getReferencedType();
          }
        }
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace(System.out);
    }
    return xTypeDescription;
  }

  public XInterfaceTypeDescription[] getInterfaceDescriptionsOfService(String _sServiceName)
  {
    try
    {
      XServiceTypeDescription xServiceTypeDescription = getServiceTypeDescription(_sServiceName, com.sun.star.uno.TypeClass.INTERFACE);
      if (xServiceTypeDescription != null)
      {
        XInterfaceTypeDescription[] xInterfaceTypeDescriptions = xServiceTypeDescription.getMandatoryInterfaces();
        return xInterfaceTypeDescriptions;
      }
    }
    catch (java.lang.Exception e)
    {
      System.out.println(e);
    }
    return new XInterfaceTypeDescription[]{};
  }

  static boolean hasByName(XTypeDescription[] _xTypeDescriptions, String _sTypeName)
  {
    for (int i = 0; i < _xTypeDescriptions.length; i++)
    {
      if (_xTypeDescriptions[i].getName().equals(_sTypeName))
      {
        return true;
      }
    }
    return false;
  }

  public static String getModuleName(String _sTypeClass)
  {
    int nlastindex = _sTypeClass.lastIndexOf(".");
    if (nlastindex > -1)
    {
      return _sTypeClass.substring(0, nlastindex);
    }
    else
    {
      return "";
    }
  }

  public static String getShortClassName(String _sClassName)
  {
    String sShortClassName = _sClassName;
    int nindex = _sClassName.lastIndexOf(".");
    if ((nindex < _sClassName.length()) && nindex > -1)
    {
      sShortClassName = _sClassName.substring(nindex + 1);
    }
    return sShortClassName;
  }

  public static boolean isUnoTypeObject(Object _oUnoObject)
  {
    return isOfUnoType(_oUnoObject, "com.sun.star.uno.Type");
  }

  public static boolean isUnoPropertyTypeObject(Object _oUnoObject)
  {
    return isOfUnoType(_oUnoObject, "com.sun.star.beans.Property");
  }

  public static boolean isUnoPropertyValueTypeObject(Object _oUnoObject)
  {
    return isOfUnoType(_oUnoObject, "com.sun.star.beans.PropertyValue");
  }

  public static boolean isOfUnoType(Object _oUnoObject, String _sTypeName)
  {
    boolean bIsUnoObject = false;
    if (_oUnoObject != null)
    {
      if (_oUnoObject.getClass().isArray())
      {
        if (!_oUnoObject.getClass().getComponentType().isPrimitive())
        {
          Object[] oUnoArray = (Object[]) _oUnoObject;
          if (oUnoArray.length > 0)
          {
            bIsUnoObject = (oUnoArray[0].getClass().getName().equals(_sTypeName));
          }
        }
      }
    }
    else
    {
      bIsUnoObject = (_oUnoObject.getClass().getName().equals(_sTypeName));
    }
    return bIsUnoObject;
  }

  public String getConstantDisplayString(int _nValue, XConstantTypeDescription[] _xConstantTypeDescription, String _sDisplayString)
  {
    String sPrefix = "";
    int[] nbits = new int[_xConstantTypeDescription.length];
    for (int i = 0; i < _xConstantTypeDescription.length; i++)
    {
      short nConstantValue = ((Short) _xConstantTypeDescription[i].getConstantValue()).shortValue();
      nbits[i] = _nValue & nConstantValue;
      if (nbits[i] > 0)
      {
        _sDisplayString += sPrefix + _xConstantTypeDescription[i].getName();
        sPrefix = " + ";
      }
    }
    return _sDisplayString;
  }

  public static boolean isArray(Object obj)
  {
    return obj.getClass().isArray();
  }

  public boolean hasSupportedServices(Object _oUnoObject)
  {
    boolean bHasSupportedServices = false;
    XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, _oUnoObject);
    if (xServiceInfo != null)
    {
      String[] sSupportedServiceNames = xServiceInfo.getSupportedServiceNames();
      bHasSupportedServices = sSupportedServiceNames.length > 0;
    }
    return bHasSupportedServices;
  }

  public Object getValueOfText(TypeClass aTypeClass, String sText)
  {
    Object oReturn = null;
    switch (aTypeClass.getValue())
    {
      case TypeClass.CHAR_value:
        break;
      case TypeClass.DOUBLE_value:
        oReturn = Double.valueOf(sText);
        break;
      case TypeClass.ENUM_value:
        break;
      case TypeClass.FLOAT_value:
        oReturn = Float.valueOf(sText);
        break;
      case TypeClass.HYPER_value:
        oReturn = Long.valueOf(sText);
        break;
      case TypeClass.LONG_value:
        oReturn = Integer.valueOf(sText);
        break;
      case TypeClass.SHORT_value:
        oReturn = Byte.valueOf(sText);
        break;
      case TypeClass.STRING_value:
        oReturn = sText;
        break;
      case TypeClass.UNSIGNED_HYPER_value:
        oReturn = Long.valueOf(sText);
        break;
      case TypeClass.UNSIGNED_LONG_value:
        oReturn = Integer.valueOf(sText);
        break;
      case TypeClass.UNSIGNED_SHORT_value:
        oReturn = Byte.valueOf(sText);
        break;
      default:
    }
    return oReturn;
  }

}
