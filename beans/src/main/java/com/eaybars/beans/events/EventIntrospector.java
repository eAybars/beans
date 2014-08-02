/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.events;

import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author Ertunc
 */
public class EventIntrospector {
    
    private static final Map<String, BeanEvent<?>> eventMap = new WeakHashMap<String, BeanEvent<?>>();

    private static String getEventTypeyKey(Class<?> clazz,
	    Class<?> listenerClass) {
	return clazz.getName() + "#" + listenerClass.getName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K> BeanEvent<K> getBeanEvent(Class<?> beanClass,
	    Class<K> listener) throws NoSuchEventException {
	BeanEvent event = eventMap.get(getEventTypeyKey(beanClass, listener));
	if (event == null) {
	    event = new EventIntrospector().updateBeanEvents(beanClass, listener);
	}
	if (event.getEventSetDescriptor() == null) {
	    throw new NoSuchEventException(beanClass, listener);
	}
	return event;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <K> BeanEvent<K> updateBeanEvents(Class<?> beanClass,
	    Class<K> listener) {
	BeanEvent result = null;
	try {
	    for (EventSetDescriptor ed : Introspector.getBeanInfo(beanClass)
		    .getEventSetDescriptors()) {
		BeanEvent be = new BeanEvent(ed);
		eventMap.put(getEventTypeyKey(beanClass, ed.getListenerType()),
			be);
		if (ed.getListenerType().equals(listener)) {
		    result = be;
		}
	    }
	} catch (IntrospectionException ex) {
	    throw new RuntimeException(ex);
	}
	if (result == null) {
	    result = new BeanEvent(null);
	    eventMap.put(getEventTypeyKey(beanClass, listener), result);
	}
	return result;
    }
}
