package com.example.ousmane.afinal;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, FetchNearbyPlacesAsyncTask.Listener {
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int REQUEST_CODE = 1;
    private GoogleApiClient mGoogleApiClient;
    private LatLng mLatLng = null;
    private MapFragment mapFragment = null;
    private GoogleMap mGoogleMap = null;
    private final static String BASE_URL = "https://maps.googleapis.com/maps/api/directions/json?";
    private Location mLocation = null;
    private final String TEMP_FILE_NAME = "temp_cache.txt";
    File tempFile;
    PlaceAutocompleteFragment autocompleteFragment;
    List<Marker> markers = new ArrayList<>();
    private LatLng mDestLatLng = null;
    private Polyline polylines = null;
    private LocationRequest mLocationRequest = null;
    private boolean isZoomed = false;
    RadioGroup radioGroup;
    boolean dialogBoxShown = false;
    String distance = null;
    String duration = null;
    private Marker mDestMarker = null;
    private Marker mStartMarker = null;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = (RadioGroup) findViewById(R.id.rg_modes);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                .findViewById(Integer.parseInt("2"));
        mapFragment.getView().setBackgroundColor(Color.WHITE);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 30);

        File cDir = getBaseContext().getCacheDir();
        tempFile = new File(cDir.getPath() + "/" + TEMP_FILE_NAME) ;

        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText) autocompleteFragment.getView().findViewById(R.id.place_autocomplete_search_input)).setText("");
                view.setVisibility(View.GONE);
                try {
                    new RandomAccessFile(tempFile, "rw").setLength(0);
                    if(polylines != null) {
                        polylines.remove();
                        mDestMarker = null;
                    }
                    mGoogleMap.clear();
                    markers.clear();
                    requestCurrentLocation();
                    displayCurrent();
                    moveToCurrentPosition();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }catch (IOException e){
                }
            }
        });

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(com.google.android.gms.location.places.Place place) {
                try {
                    new RandomAccessFile(tempFile, "rw").setLength(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cleanMapAndReset();
                PlaceItem dest = new PlaceItem(place.getLatLng().latitude,
                        place.getLatLng().longitude,
                        place.getName().toString());
                saveDestination(dest);
                dest = getDestination();
                mDestLatLng = new LatLng(dest.getLat(), dest.getLng());
                displayOnMap(mDestLatLng, place.getName().toString());
                zoomMap();
                dialogBoxShown = false;
                getWayPoints(mLatLng, mDestLatLng, getTravelMode());
                isZoomed = false;
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }

        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(mDestMarker != null){
                    polylines.remove();
                    if(mLatLng != null && mDestLatLng != null) {
                        getWayPoints(mLatLng, mDestLatLng, getTravelMode());
                        zoomMap();
                        showTravelDetails();
                    }
                }
            }
        });
    }

    private void cleanMapAndReset() {
        mGoogleMap.clear();
        markers.clear();
        requestCurrentLocation();
        displayCurrent();
        moveToCurrentPosition();
    }

    private void moveToCurrentPosition(){
        if(mLatLng != null){
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mLatLng, 10);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    private String getTravelMode(){
        int radioButtonID = radioGroup.getCheckedRadioButtonId();
        View radioButton = radioGroup.findViewById(radioButtonID);
        int idx = radioGroup.indexOfChild(radioButton);
        RadioButton r = (RadioButton)  radioGroup.getChildAt(idx);
        String selectedtext = r.getText().toString();
        return selectedtext;
    }

    private void getWayPoints(LatLng origin, LatLng destination, String mode){
        FetchNearbyPlacesAsyncTask task = new FetchNearbyPlacesAsyncTask(this);
        StringBuilder builder = new StringBuilder(BASE_URL);
        builder.append("origin=" + new CustomLatLng(origin.latitude, origin.longitude).toString());
        builder.append("&destination=" + new CustomLatLng(destination.latitude, destination.longitude).toString());
        builder.append("&mode=" + mode.toLowerCase());
        builder.append("&API_KEY="+ Constants.API_KEY.getValue());
        String url = builder.toString();
        Log.d("URL", url);
        task.execute(url);
    }

    private void zoomMap(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        if(!isZoomed) {
            int padding = 0;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mGoogleMap.setMaxZoomPreference(13f);
            mGoogleMap.animateCamera(cu);
            isZoomed = true;
        }
    }

    private void saveDestination(PlaceItem dest){
        try {
            FileOutputStream fout = new FileOutputStream(tempFile);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(dest);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayOnMap(LatLng latLng, String placeName){
        mDestMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(placeName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        markers.clear();
        displayCurrent();
        markers.add(mDestMarker);
    }

    private void displayCurrent(){
        mStartMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(mLatLng)
                .title("You're here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        markers.add(mStartMarker);
    }

    private PlaceItem getDestination(){
        PlaceItem placeItem;
        try{
            FileInputStream fin = new FileInputStream(tempFile);
            ObjectInputStream ois = new ObjectInputStream(fin);
            placeItem = (PlaceItem) ois.readObject();
            ois.close();
            return placeItem;

        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mGoogleMap == null) {
            mGoogleMap = googleMap;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mGoogleMap.setMyLocationEnabled(true);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestCurrentLocation();
    }

    private void requestCurrentLocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(60000);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Google Api Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Google Api Connection failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if(mLocation!= null){
            mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            mGoogleMap.clear();
            displayCurrent();
            PlaceItem dest = getDestination();
            if(dest != null){
                mDestLatLng = new LatLng(dest.getLat(), dest.getLng());
                displayOnMap(mDestLatLng, dest.getName());
                getWayPoints(mLatLng, mDestLatLng, getTravelMode());
                zoomMap();
            }else {
                moveToCurrentPosition();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onComplete(List<List<HashMap<String,String>>> routes) {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;

        for (int i = 0; i < routes.size(); i++) {
            points = new ArrayList<>();
            lineOptions = new PolylineOptions();

            List<HashMap<String, String>> path = routes.get(i);

            distance = path.get(0).get("distance");
            duration = path.get(0).get("duration");

            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);
                points.add(position);
            }

            lineOptions.addAll(points);
            lineOptions.width(15);
            lineOptions.color(Color.RED);
            lineOptions.geodesic(true);

            Log.d("onPostExecute","onPostExecute lineoptions decoded");
        }

        if(lineOptions != null) {
            polylines = mGoogleMap.addPolyline(lineOptions);
            if(!dialogBoxShown) {
                showTravelDetails();
                dialogBoxShown = true;
            }
        }
        else {
            Log.d("onPostExecute","without Polylines drawn");
        }
    }

    private void showTravelDetails() {
        Toast.makeText(this, "Travel Summary " + getTravelMode() + "\n"
                 + "Distance: " + distance + "\n" +
                "Duration: " + duration, Toast.LENGTH_LONG ).show();
    }
}
