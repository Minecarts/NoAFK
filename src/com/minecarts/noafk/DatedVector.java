package com.minecarts.noafk;

import java.util.Date;

import org.bukkit.util.Vector;


public class DatedVector {
    protected final Date date;
    protected final Vector vector;
    
    public DatedVector(Vector vector) {
        this(vector, new Date());
    }
    
    public DatedVector(Vector vector, Date date) {
        this.vector = vector;
        this.date = date;
    }
    
    public Vector getVector() {
        return vector.clone();
    }
    
    public long elapsed() {
        return new Date().getTime() - date.getTime();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DatedVector) return vector.equals(((DatedVector) obj).vector);
        return vector.equals(obj);
    }
}
