package com.eaybars.beans.properties;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import static java.util.Arrays.*;
import java.util.Collection;
import java.util.LinkedList;

public class BeanProperty implements Cloneable {

    private Class<?> clazz;
    private PropertyDescriptor propertyDescriptor;

    private BeanProperty parent;
    private Field field;
    private boolean fieldSearched;

    public BeanProperty(Class<?> clazz, PropertyDescriptor propertyDescriptor) {
	super();
	this.clazz = clazz;
	this.propertyDescriptor = propertyDescriptor;
    }

    public Object getValue(Object instance) throws InvocationTargetException {
	if (parent != null) {
	    instance = parent.getValue(instance);
	}
	if (instance.getClass().isArray()) {
	    instance = asList((Object[]) instance);
	}
	if (instance instanceof Collection<?>) {
	    LinkedList<Object> result = new LinkedList<Object>();
	    for (Object o : (Collection<?>) instance) {
		if (o != null) {
                    result.add(getValueFromReadMethod(o));
		}
	    }
	    return result;
	} else {
            return getValueFromReadMethod(instance);
	}
    }
    
    private Object getValueFromReadMethod(Object instance) throws InvocationTargetException {
        if (isWriteOnly()){
            throw new UnsupportedOperationException(toString() + 
                    " is a write only property, value cannot be read from this property");
        }
        try {
            return propertyDescriptor.getReadMethod().invoke(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);// never happens
        }

    }

    public Collection<?> getValueAsCollection(Object instance)
	    throws InvocationTargetException {
	Object value = getValue(instance);
	if (value instanceof Collection<?>) {
	    return (Collection<?>) value;
	} else {
	    return Arrays.asList(value);
	}
    }

    public void setValue(Object instance, Object value)
	    throws InvocationTargetException {
	if (isReadOnly()) {
	    throw new UnsupportedOperationException(
		    propertyDescriptor.getName() + " of "
			    + getClass().getSimpleName() + " is read only");
	}
	try {
	    propertyDescriptor.getWriteMethod().invoke(instance, value);
	} catch (IllegalAccessException e) {
	    throw new RuntimeException(e);// never happens
	}
    }

    public BeanProperty getParent() {
	return parent;
    }

    public PropertyDescriptor getPropertyDescriptor() {
	return propertyDescriptor;
    }

    public Class<?> getBeanClass() {
	return this.clazz;
    }

    public Class<?> getPropertyType() {
	return this.propertyDescriptor.getPropertyType();
    }

    public <K extends Annotation> K getAnnotation(Class<K> annotationClass) {
	K result = null;
	if (isFieldTargeted(annotationClass)) {
	    Field field = getField(isInheritedAnnotation(annotationClass));
	    if (field != null) {
		result = field.getAnnotation(annotationClass);
	    }
	} else {
	    result = getPropertyDescriptor().getReadMethod().getAnnotation(
		    annotationClass);
	}
	return result;
    }

    private boolean isFieldTargeted(Class<? extends Annotation> annotationClass) {
	Target target = annotationClass.getAnnotation(Target.class);
	if (target != null) {
	    for (ElementType e : target.value()) {
		if (ElementType.FIELD.equals(e)) {
		    return true;
		}
	    }
	}
	return false;
    }
    
    private boolean isInheritedAnnotation(Class<? extends Annotation> annotationClass) {
        return annotationClass.getAnnotation(Inherited.class) != null;
    }

    private Field getField(boolean searchSuperClasses) {
	Class<?> currentClass = clazz;
	while (field == null && !fieldSearched) {
	    Class<?> c = clazz;
	    while (c != null && field == null) {
		for (Field f : c.getDeclaredFields()) {
		    if (f.getName().equals(getPropertyDescriptor().getName())
			    && f.getType().equals(getPropertyType())) {
			field = f;
		    }
		}
		c = searchSuperClasses ? c.getSuperclass() : null;
	    }
	    fieldSearched = true;
	}
	return field;
    }

    public boolean isCollectionOrArrayType() {
	return getPropertyType().isArray()
		|| Collection.class.isAssignableFrom(getPropertyType());
    }

    public Class<?> getCollectionOrArrayType() {
	if (getPropertyType().isArray()) {
	    return getPropertyType().getComponentType();
	}
	if (!Collection.class.isAssignableFrom(getPropertyType())) {
	    throw new UnsupportedOperationException(
		    propertyDescriptor.getName() + " of "
			    + getClass().getSimpleName()
			    + " is not a Collection property");
	}
	Type[] typeArgs = getGenericTypeArguments(getGenericType());
	if (typeArgs != null && typeArgs.length > 0) {
	    return (Class<?>) typeArgs[0];
	} else {
	    throw new UnsupportedOperationException(
		    propertyDescriptor.getName() + " of "
			    + getClass().getSimpleName()
			    + " is not a parametrized Collection property");
	}
    }
    
    private Type getGenericType() {
        if (propertyDescriptor.getReadMethod() != null) {
            return propertyDescriptor.getReadMethod().getGenericReturnType();
        } else {
            Type[] types = propertyDescriptor.getWriteMethod().getGenericParameterTypes();
            return types.length > 1 ? types[0] : null;
        }
    }

    private Type[] getGenericTypeArguments(Type type) {
	Type[] typeArgs = null;
	if (type instanceof ParameterizedType) {
	    typeArgs = ((ParameterizedType) type).getActualTypeArguments();
	}
	if (typeArgs != null) {
	    for (Type t : typeArgs) {
		if (!(t instanceof Class<?>))
		    return null;
	    }
	}
	return typeArgs;
    }

    public boolean isReadOnly() {
	return propertyDescriptor.getWriteMethod() == null;
    }
    
    public boolean isWriteOnly() {
        return getPropertyDescriptor().getReadMethod() == null;
    }

    @Override
    public String toString() {
	return parent == null ? propertyDescriptor.getName() : parent
		.toString() + "." + propertyDescriptor.getName();
    }

    @Override
    protected BeanProperty clone() {
	try {
	    return (BeanProperty) super.clone();
	} catch (CloneNotSupportedException e) {
	    throw new RuntimeException(e);// never happens
	}
    }

    protected BeanProperty chain(BeanProperty accessor) {
	BeanProperty clone = accessor.clone();
	clone.parent = this;
	return clone;
    }

}
