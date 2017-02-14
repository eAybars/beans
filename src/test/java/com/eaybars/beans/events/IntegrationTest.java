package com.eaybars.beans.events;

import com.eaybars.beans.MutableBean;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import static org.junit.Assert.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

/**
 *
 * @author Ertunc
 */
public class IntegrationTest {

    @Test
    public void test() throws InvocationTargetException {
        BeanEvent<PropertyChangeListener> event = EventIntrospector.getBeanEvent(
                MutableBean.class, PropertyChangeListener.class);
        
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        ArgumentCaptor<PropertyChangeEvent> argumentCaptor = ArgumentCaptor.forClass(PropertyChangeEvent.class);

        MutableBean bean = new MutableBean();

        event.addListener(bean, listener);
	bean.setAddress("some address");

	event.removeListener(bean, listener);
	bean.setQuantity(5);

        verify(listener).propertyChange(argumentCaptor.capture());
        assertEquals("some address", argumentCaptor.getValue().getNewValue());
        assertNull(argumentCaptor.getValue().getOldValue());
        assertEquals("address", argumentCaptor.getValue().getPropertyName());
        

    }
}
