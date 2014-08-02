/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.eaybars.beans.IndexedBean1;
import com.eaybars.beans.IndexedBean2;
import com.eaybars.beans.MutableBean;
import com.eaybars.beans.index.Index;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;

/**
 *
 * @author Ertunc
 */
public class BeanPropertyTest {
    
    private BeanProperty getAddressProperty() throws NoSuchMethodException {
	PropertyDescriptor descriptor = mock(PropertyDescriptor.class);
	when(descriptor.getReadMethod()).thenReturn(
		MutableBean.class.getMethod("getAddress"));
	when(descriptor.getWriteMethod()).thenReturn(
		MutableBean.class.getMethod("setAddress", String.class));
	when(descriptor.getName()).thenReturn("address");
	return new BeanProperty(MutableBean.class, descriptor);
    }

    private BeanProperty getNameProperty() throws NoSuchMethodException {
	PropertyDescriptor descriptor = mock(PropertyDescriptor.class);
	when(descriptor.getReadMethod()).thenReturn(
		IndexedBean1.class.getMethod("getName"));
	when(descriptor.getName()).thenReturn("name");
	return new BeanProperty(IndexedBean1.class, descriptor);
    }

    private BeanProperty getBeansProperty() throws NoSuchMethodException {
	PropertyDescriptor descriptor = mock(PropertyDescriptor.class);
	when(descriptor.getReadMethod()).thenReturn(
		IndexedBean2.class.getMethod("getBeans"));
	when(descriptor.getName()).thenReturn("beans");
        when(descriptor.getPropertyType()).thenReturn((Class)Collection.class);
	return new BeanProperty(IndexedBean2.class, descriptor);
    }

    @Test
    public void simplePropertyTest() throws NoSuchMethodException,
	    InvocationTargetException {
	BeanProperty property = getAddressProperty();
        assertEquals(MutableBean.class, property.getBeanClass());
        assertNull(property.getAnnotation(Override.class));
        assertFalse(property.isReadOnly());
        assertFalse(property.isWriteOnly());

	MutableBean bean = new MutableBean();

	property.setValue(bean, "Some Address");

	assertEquals("Some Address", bean.getAddress());
	assertEquals("Some Address", property.getValue(bean));
        assertEquals(Arrays.asList("Some Address"), 
                property.getValueAsCollection(bean));

	bean.setAddress("Some other address");
	assertEquals("Some other address", property.getValue(bean));
    }
    
    @Test
    public void findAnnotationTest() throws NoSuchMethodException {
        BeanProperty property = getBeansProperty();
        Index index = property.getAnnotation(Index.class);
        
        assertNotNull(index);
        assertTrue(index.contents());
    }

    @Test
    public void chainedPropertyTest() throws NoSuchMethodException,
	    InvocationTargetException {
	BeanProperty chainedProperty = getBeansProperty().chain(
		getNameProperty());

	IndexedBean2 bean = new IndexedBean2("Bean-2")
		.addBean(new IndexedBean1("abc", 1));

	Collection<String> col = new LinkedList<String>();
	col.add("abc");
	
	assertEquals(col, chainedProperty.getValue(bean));
	assertEquals(col, chainedProperty.getValueAsCollection(bean));
	
	bean.addBean(new IndexedBean1("xyz", 2));
	col.add("xyz");

	assertEquals(col, chainedProperty.getValue(bean));
    }
}
