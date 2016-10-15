package com.example.souradeep.gps_arduino_android;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    float[] mGravity;
    float[] mGeomagnetic;
    private ArrayList<LatLng> path = null;
    private GoogleMap mMap;
    private BluetoothHandler bluetoothHandler = null;
    private LatLng currentPosition = null;
    private Button searchButton;
    private EditText searchBar;
    private Polyline line;
    private float currentHeading; // Degrees
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Initialize variables
        bluetoothHandler = new BluetoothHandler();
        ArrayList<LatLng> path = new ArrayList<>();
        searchBar = (EditText) findViewById(R.id.etSearchBar);
        searchButton = (Button) findViewById(R.id.bSearchButton);
        currentHeading = 0;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runQuery(searchBar.getText().toString());
            }
        });

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                bluetoothHandler.update(currentPosition, currentHeading);
                Log.i("MapsActivity", "Position: " + currentPosition + " Heading: " + currentHeading);
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.i("MapsActivity", "Location permission denied by user");
                    }
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
                Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
            }
        };

        // Register the listener with the Location Manager to receive location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void runQuery(String destination) {
        if (mMap == null || currentPosition == null) {
            Toast.makeText(getApplicationContext(), "Please wait", Toast.LENGTH_SHORT).show();
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            URL url = null;
            try {
                url = new URL(makeURL(currentPosition, destination));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            new UrlToPathTask().execute(url);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public String makeURL(LatLng gps, String destination) {
        return "http://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + gps.latitude + "," + gps.longitude
                + "&destination=" + destination
                + "&mode=driving&alternatives=false"
                ;//+ "&key=" + R.string.google_maps_key;
    }

    public String getJSON(URL url) {
        StringBuilder jsonString = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
        return jsonString.toString();
    }

    public ArrayList<LatLng> getPath(String jsonString) {
        ArrayList<LatLng> path = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");

            path = decodePoly(encodedString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return path;
    }

    private ArrayList<LatLng> decodePoly(String encoded) {

        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = orientation[0];
                currentHeading = -azimuth * 360 / (2 * 3.14159f);
                Log.i("MapsActivity", azimuth + " " + currentHeading);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class UrlToPathTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            int count = urls.length;
            if (count == 0) return null;
            if (isCancelled()) return null;
            Log.i("MapsActivity", "URL = " + urls[0]);
            return getJSON(urls[0]);
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(String jsonString) {
            Log.i("MapsActivity", "Json string length = " + jsonString.length());
            path = getPath(jsonString);
            Log.i("MapsActivity", "Path size = " + path.size());
            bluetoothHandler.setPath(path);
            if (line != null)
                line.remove();
            line = mMap.addPolyline(new PolylineOptions()
                            .addAll(path)
                            .width(12)
                            .color(Color.parseColor("#05b1fb"))//Google maps blue color
                            .geodesic(true)
            );

        }
    }
}
