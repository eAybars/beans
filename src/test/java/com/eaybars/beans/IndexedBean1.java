package com.eaybars.beans;

import com.eaybars.beans.index.Index;



@Index
public class IndexedBean1 {
    
    private String name;
    @Index(sorted=true)
    private int number;
    
    public IndexedBean1(String name, int number) {
	super();
	this.name = name;
	this.number = number;
    }
    public String getName() {
        return name;
    }
    public int getNumber() {
        return number;
    }
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	result = prime * result + number;
	return result;
    }
    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	IndexedBean1 other = (IndexedBean1) obj;
	if (name == null) {
	    if (other.name != null)
		return false;
	} else if (!name.equals(other.name))
	    return false;
	if (number != other.number)
	    return false;
	return true;
    }
    
    @Override
    public String toString() {
        return name+"-"+number;
    }
    
}
