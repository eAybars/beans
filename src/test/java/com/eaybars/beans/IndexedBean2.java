package com.eaybars.beans;

import com.eaybars.beans.index.Index;
import java.util.Collection;
import java.util.LinkedList;


public class IndexedBean2 {
    
    @Index
    private String name;
    @Index(contents=true)
    private Collection<IndexedBean1> beans;
    public IndexedBean2(String name) {
	super();
	this.name = name;
	beans = new LinkedList<IndexedBean1>();
    }
    
    public IndexedBean2 addBean(IndexedBean1 bean){
	beans.add(bean);
	return this;
    }
    
    public int beanSize(){
	return beans.size();
    }
    
    public boolean containsBean(IndexedBean1 bean){
	return beans.contains(bean);
    }

    public String getName() {
        return name;
    }

    public Collection<IndexedBean1> getBeans() {
        return beans;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((name == null) ? 0 : name.hashCode());
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
	IndexedBean2 other = (IndexedBean2) obj;
	if (name == null) {
	    if (other.name != null)
		return false;
	} else if (!name.equals(other.name))
	    return false;
	return true;
    }
    
    
}
