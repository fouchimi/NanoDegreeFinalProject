package com.example.ousmane.afinal;

import java.io.Serializable;

/**
 * Created by ousma on 10/4/2016.
 */

public class PlaceItem implements Serializable {

    private double lat;
    private double lng;
    private String name;

    public PlaceItem(double lat, double lng, String name){
        this.lat = lat;
        this.lng = lng;
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
