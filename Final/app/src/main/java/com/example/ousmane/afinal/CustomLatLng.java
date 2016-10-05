package com.example.ousmane.afinal;

/**
 * Created by ousma on 10/5/2016.
 */

public class CustomLatLng {
    private double latitude;
    private double longitude;

    public CustomLatLng(double lat, double lng){
        this.latitude = lat;
        this.longitude = lng;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return latitude + "," + longitude;
    }
}
