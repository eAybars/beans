/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.events;

import java.beans.EventSetDescriptor;
import java.lang.reflect.InvocationTargetException;
/**
 *
 * @author Ertunc
 */
public class BeanEvent<T> {
    EventSetDescriptor eventSetDescriptor;

    public BeanEvent(EventSetDescriptor eventSetDescriptor) {
        this.eventSetDescriptor = eventSetDescriptor;
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }
    
    public void addListener(Object bean, T listener) 
            throws InvocationTargetException {
        try {
            eventSetDescriptor.getAddListenerMethod().invoke(bean, listener);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);//never happens
        }
    }
    
    public void removeListener(Object bean, T listener) 
            throws InvocationTargetException {
        try {
            eventSetDescriptor.getRemoveListenerMethod().invoke(bean, listener);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);//never happens
        }

    }
    
}
