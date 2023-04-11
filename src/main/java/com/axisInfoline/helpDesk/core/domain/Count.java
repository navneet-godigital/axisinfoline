package com.axisInfoline.helpDesk.core.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Count {

    @Id
    String name;
    double count;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "Count{" +
                "name='" + name + '\'' +
                ", count=" + count +
                '}';
    }
}
