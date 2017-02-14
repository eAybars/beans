package com.eaybars.beans.events;

import com.eaybars.beans.MutableBean;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Ertunc
 */
public class EventIntrospectorTest {

    @Test
    public void findEventTest() {
        BeanEvent<PropertyChangeListener> event = EventIntrospector.getBeanEvent(
                MutableBean.class, PropertyChangeListener.class);

        assertNotNull(event);

        assertEquals(PropertyChangeListener.class, event.getEventSetDescriptor().getListenerType());
        assertEquals("addPropertyChangeListener", event.getEventSetDescriptor().getAddListenerMethod().getName());
        assertEquals("removePropertyChangeListener", event.getEventSetDescriptor().getRemoveListenerMethod().getName());
        
        assertSame(event, EventIntrospector.getBeanEvent(
                MutableBean.class, PropertyChangeListener.class));
    }
    
    @Test(expected = NoSuchEventException.class)
    public void nonExistingListenerTest() {
        try{
            EventIntrospector.getBeanEvent(MutableBean.class, ActionListener.class);
        } catch (NoSuchEventException e) {
            assertEquals(MutableBean.class, e.getBeanClass());
            assertEquals(ActionListener.class, e.getListenerType());
            throw e;
        }
    }
    
}
