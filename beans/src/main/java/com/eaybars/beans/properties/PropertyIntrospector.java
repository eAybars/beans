/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.properties;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author Ertunc
 */
public class PropertyIntrospector {
    
    private final static Map<String, BeanProperty> propertyMap = new WeakHashMap<String, BeanProperty>();

    public static BeanProperty getProperty(Class<?> clazz, String property)
	    throws NoSuchPropertyException {
	String key = getPropertyKey(clazz, property);
	BeanProperty bProperty = propertyMap.get(key);
	if (bProperty == null) {
	    bProperty = new PropertyIntrospector().findProperty(clazz, property);
	    propertyMap.put(key, bProperty);
	}
	return bProperty;
    }

    public static Collection<BeanProperty> getAllProperties(Class<?> clazz) {
	Collection<BeanProperty> result = new LinkedList<BeanProperty>();
	try {
	    BeanInfo bi = Introspector.getBeanInfo(clazz);
	    for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
		if (!"class".equals(pd.getName())) {
		    result.add(getProperty(clazz, pd.getName()));
		}
	    }
	} catch (IntrospectionException e) {
	    throw new RuntimeException(e);
	}
	return result;
    }

    private BeanProperty findProperty(Class<?> clazz, String property)
	    throws NoSuchPropertyException {
	int index = property.lastIndexOf(".");
	if (index < 0) {
	    return findImmediateProperty(clazz, property);
	} else {
	    BeanProperty parent = getProperty(clazz,
		    property.substring(0, index));
	    return parent.chain(getProperty(getParentType(parent),
		    property.substring(index + 1)));
	}
    }

    private Class<?> getParentType(BeanProperty parent) {
	return parent.isCollectionOrArrayType() ? parent
		.getCollectionOrArrayType() : parent.getPropertyType();
    }

    private BeanProperty findImmediateProperty(Class<?> clazz, String property)
	    throws NoSuchPropertyException {
	BeanProperty result = null;
	try {
	    BeanInfo bi = Introspector.getBeanInfo(clazz);
	    for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
		if (!"class".equals(pd.getName())) {
		    BeanProperty bProperty = new BeanProperty(clazz, pd);
		    propertyMap.put(getPropertyKey(clazz, pd.getName()),
			    bProperty);
		    if (pd.getName().equals(property)) {
			result = bProperty;
		    }
		}
	    }
	} catch (IntrospectionException e) {
	    throw new RuntimeException(e);
	}
	if (result == null) {
	    throw new NoSuchPropertyException(clazz, property);
	}
	return result;
    }

    private static String getPropertyKey(Class<?> clazz, String property) {
	return clazz.getName() + "#" + property;
    }

}
