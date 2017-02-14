package com.eaybars.beans;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


public class MutableBean {

    private String address;
    private int quantity;
    
    private PropertyChangeSupport support;
    
    public MutableBean() {
	support = new PropertyChangeSupport(this);
    }

    public MutableBean(String address, int quantity) {
        this();
        this.address = address;
        this.quantity = quantity;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
	String old = this.address;
        this.address = address;
        support.firePropertyChange("address", old, address);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
	int old = this.quantity;
        this.quantity = quantity;
        support.firePropertyChange("quantity", old, quantity);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener){
	support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener){
	support.removePropertyChangeListener(listener);
    }
}
