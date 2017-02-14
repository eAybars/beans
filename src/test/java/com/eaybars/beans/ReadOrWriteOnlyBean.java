/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eaybars.beans;

import com.eaybars.beans.index.Index;

@Index
public class ReadOrWriteOnlyBean {
    
    private String name;
    private int age;
    @Index(comparator = ReverseComparator.class)
    private int score;

    public ReadOrWriteOnlyBean(String name, int age, int score) {
        this.name = name;
        this.age = age;
        this.score = score;
    }

    public ReadOrWriteOnlyBean(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return this.name + "-"+age;
    }

    public int getScore() {
        return score;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 19 * hash + this.score;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReadOrWriteOnlyBean other = (ReadOrWriteOnlyBean) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.score != other.score) {
            return false;
        }
        return true;
    }


    
}
