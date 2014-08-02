package com.eaybars.beans.properties;

public class NoSuchPropertyException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private Class<?> beanClass;
    private String property;

    public NoSuchPropertyException(Class<?> beanClass, String property) {
	super(beanClass.getSimpleName() + " has not such property: " + property);
	this.beanClass = beanClass;
	this.property = property;
    }

    public Class<?> getBeanClass() {
	return beanClass;
    }
    
    public String getProperty() {
	return property;
    }
}
