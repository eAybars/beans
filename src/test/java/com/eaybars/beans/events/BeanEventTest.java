/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eaybars.beans.events;

import static org.mockito.Mockito.*;

import com.eaybars.beans.MutableBean;
import java.beans.EventSetDescriptor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

/**
 *
 * @author Ertunc
 */
public class BeanEventTest {

    private EventSetDescriptor createEventDescriptor() throws NoSuchMethodException {
        EventSetDescriptor ed = mock(EventSetDescriptor.class);

        when(ed.getAddListenerMethod()).thenReturn(MutableBean.class.getMethod(
                "addPropertyChangeListener", PropertyChangeListener.class));
        when(ed.getRemoveListenerMethod()).thenReturn(MutableBean.class.getMethod(
                "removePropertyChangeListener", PropertyChangeListener.class));

        return ed;
    }

    @Test
    public void addRemovetest() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        BeanEvent<PropertyChangeListener> event = new BeanEvent<PropertyChangeListener>(createEventDescriptor());

        PropertyChangeListener listener = mock(PropertyChangeListener.class);

        MutableBean bean = new MutableBean();

        event.addListener(bean, listener);
	bean.setAddress("address");

	event.removeListener(bean, listener);
	bean.setQuantity(5);

	verify(listener, times(1)).propertyChange(
		any(PropertyChangeEvent.class));

    }

}
