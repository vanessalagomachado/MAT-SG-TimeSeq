/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.model;

import java.util.Objects;

/**
 *
 * @author vanes
 */
public class AttributeValue {
    private Object value;
    private SemanticAspect attibute;
    private double numericalValueSD;

    public AttributeValue(Object value, SemanticAspect attibute) {
        this.value = value;
        this.attibute = attibute;
    }

    public AttributeValue(Object value, SemanticAspect attibute, double SD) {
        this.value = value;
        this.attibute = attibute;
        numericalValueSD = SD;
    }
    
    public SemanticAspect getAttibute() {
        return attibute;
    }

    public void setAttibute(SemanticAspect attibute) {
        this.attibute = attibute;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    
    public double getNumericalValueSD() {
        return numericalValueSD;
    }

    public void setNumericalValueSD(double numericalValueSD) {
        this.numericalValueSD = numericalValueSD;
    }
    
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.value);
        hash = 79 * hash + Objects.hashCode(this.attibute);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AttributeValue other = (AttributeValue) obj;
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        if (!Objects.equals(this.attibute, other.attibute)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return attibute.getName() +" -> "+value;
    }

    
    
    
    
    
    
}
