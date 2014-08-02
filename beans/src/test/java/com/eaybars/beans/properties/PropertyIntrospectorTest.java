/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans.properties;

import com.eaybars.beans.IndexedBean1;
import com.eaybars.beans.IndexedBean2;
import java.util.Collection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Ertunc
 */
public class PropertyIntrospectorTest {
    @Test
    public void getSimplePropertyTest() {
	BeanProperty property = PropertyIntrospector.getProperty(IndexedBean1.class, "name");
	assertNotNull(property);
	assertEquals("name", property.toString());

	BeanProperty sameProperty = PropertyIntrospector.getProperty(IndexedBean1.class, "name");
	assertSame(property, sameProperty);
    }

    @Test
    public void getChainedPropertyTest() {
	BeanProperty beansProperty = PropertyIntrospector.getProperty(IndexedBean2.class, "beans");
	BeanProperty numberProperty = PropertyIntrospector.getProperty(IndexedBean1.class, "number");

	BeanProperty property = PropertyIntrospector.getProperty(IndexedBean2.class, "beans.number");
	assertNotNull(property);
	assertEquals("beans.number", property.toString());
	assertSame(property, PropertyIntrospector.getProperty(IndexedBean2.class, "beans.number"));

	assertSame(beansProperty, property.getParent());
	assertNotSame(beansProperty, numberProperty);
    }

    @Test
    public void getAllPropertiesTest() {
	Collection<BeanProperty> properties = PropertyIntrospector.getAllProperties(IndexedBean1.class);
	assertEquals(2, properties.size());
	assertTrue(properties.contains(PropertyIntrospector.getProperty(IndexedBean1.class, "name")));
	assertTrue(properties
		.contains(PropertyIntrospector.getProperty(IndexedBean1.class, "number")));
    }
    
    @Test(expected=NoSuchPropertyException.class)
    public void getNonExistingPropertyTest() {
        try {
            PropertyIntrospector.getProperty(IndexedBean1.class, "property");
        } catch (NoSuchPropertyException e) {
            assertEquals(IndexedBean1.class, e.getBeanClass());
            assertEquals("property", e.getProperty());
            throw e;
        }
    }
    
}
