package com.example.souradeep.gps_arduino_android;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class BluetoothHandler {
    // Commands for arduino board
    private final static char L = '1'; // Left
    private final static char R = '2'; // Right
    private final static char F = '3'; // Forward
    private final static char B = '4'; // Back
    private final static char H = '5'; // Halt
    ArrayList<LatLng> path = null;

    public BluetoothHandler() {
    }

    // This function is called every time the robot moves
    // currentHeading is in degrees
    void update(LatLng currentPosition, float currentHeading) {
        if (path != null) {
            // TODO: Implement bluetooth logic
        }
    }

    // This function is called once per request to google maps server
    void setPath(ArrayList<LatLng> path) {
        this.path = path;
        // TODO: Update path and reset state
    }
}
