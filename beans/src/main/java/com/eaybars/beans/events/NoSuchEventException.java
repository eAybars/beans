/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.events;

/**
 *
 * @author Ertunc
 */
public class NoSuchEventException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private Class<?> beanClass;
    private Class<?> listenerType;

    public NoSuchEventException(Class<?> beanClass, Class<?> listenerType) {
	super(beanClass.getSimpleName() + " has not such event listener type: " 
                + listenerType.getName());
        this.beanClass = beanClass;
        this.listenerType = listenerType;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Class<?> getListenerType() {
        return listenerType;
    }

    
}
