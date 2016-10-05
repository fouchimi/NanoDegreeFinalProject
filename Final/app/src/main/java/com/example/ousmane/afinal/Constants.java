package com.example.ousmane.afinal;

/**
 * Created by ousma on 9/28/2016.
 */

public enum Constants {
    API_KEY("AIzaSyARduirvMCQNNA99S8MpoatM4yCLgJeUVI");

    private String value;

    private Constants(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
