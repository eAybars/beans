/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.properties;

import com.eaybars.beans.IndexedBean1;
import com.eaybars.beans.IndexedBean2;
import com.eaybars.beans.MutableBean;
import com.eaybars.beans.index.Index;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Ertunc
 */
public class IntegrationTest {
    
    @Test
    public void simplePropertyTest() throws NoSuchMethodException,
	    InvocationTargetException {
	BeanProperty property = PropertyIntrospector.getProperty(MutableBean.class, "address");

	MutableBean bean = new MutableBean();

	property.setValue(bean, "Some Address");

	assertEquals("Some Address", bean.getAddress());
	assertEquals("Some Address", property.getValue(bean));

	bean.setAddress("Some other address");
	assertEquals("Some other address", property.getValue(bean));
    }

    @Test
    public void chainedPropertyTest() throws NoSuchMethodException,
	    InvocationTargetException {
	BeanProperty chainedProperty = PropertyIntrospector.getProperty(IndexedBean2.class, "beans.name");

	IndexedBean2 bean = new IndexedBean2("Bean-2")
		.addBean(new IndexedBean1("abc", 1));

	Collection<String> col = new LinkedList<String>();
	col.add("abc");
	
	assertEquals(col, chainedProperty.getValue(bean));
	
	bean.addBean(new IndexedBean1("xyz", 2));
	col.add("xyz");

	assertEquals(col, chainedProperty.getValue(bean));
    }
    
    @Test
    public void findAnnotationTest() {
        BeanProperty property = PropertyIntrospector.getProperty(IndexedBean2.class, "name");
        Index i = property.getAnnotation(Index.class);
        assertNotNull(i);
        assertEquals(false, i.contents());
        assertFalse(i.sorted());
    }
}
