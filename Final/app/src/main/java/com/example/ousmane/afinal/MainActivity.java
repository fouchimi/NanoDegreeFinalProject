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
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
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
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    if(polylines != null) polylines.remove();
                    mGoogleMap.clear();
                    markers.clear();
                    if(mLatLng != null) {
                        displayCurrent();
                    }else {
                        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
                            return;
                        }
                        if(mLocation!= null) {
                            mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                            displayCurrent();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }catch (IOException e){

                }
            }
        });

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(com.google.android.gms.location.places.Place place) {
                if(markers.size() >= 2) {
                    markers.get(1).remove();
                    if(polylines != null) polylines.remove();
                    mGoogleMap.clear();
                    displayCurrent();
                }
                PlaceItem dest = new PlaceItem(place.getLatLng().latitude,
                        place.getLatLng().longitude,
                        place.getName().toString());
                saveDestination(dest);
                dest = getDestination();
                mDestLatLng = new LatLng(dest.getLat(), dest.getLng());
                displayOnMap(mDestLatLng, place.getName().toString());
                zoomMap();
                getWayPoints(mLatLng, mDestLatLng);
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }

        });
    }

    private void moveToCurrentPosition(){
        if(mLatLng != null){
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mLatLng, 15);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    private void getWayPoints(LatLng origin, LatLng destination){
        FetchNearbyPlacesAsyncTask task = new FetchNearbyPlacesAsyncTask(this);
        StringBuilder builder = new StringBuilder(BASE_URL);
        builder.append("origin=" + new CustomLatLng(origin.latitude, origin.longitude).toString());
        builder.append("&destination=" + new CustomLatLng(destination.latitude, destination.longitude).toString());
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
        int padding = 15;
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mGoogleMap.animateCamera(cu);
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
        Marker destinationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(placeName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        markers.add(destinationMarker);
    }

    private void displayCurrent(){
        Marker start = mGoogleMap.addMarker(new MarkerOptions()
                .position(mLatLng)
                .title("You're here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        markers.add(start);
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
        /*MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView)
                MenuItemCompat.getActionView(searchItem);
        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(MainActivity.this, query, Toast.LENGTH_LONG).show();
                Log.d(TAG, query);
                searched = true;
                FetchNearbyPlacesAsyncTask fetchNearbyPlacesAsyncTask = new FetchNearbyPlacesAsyncTask(MainActivity.this);
                query = query.trim();
                String fullUrlQuery = BASE_URL + "query=" + query;
                fullUrlQuery += "&location=" + String.valueOf(mLatLng.latitude).toString() + "," + String.valueOf(mLatLng.longitude).toString();
                fullUrlQuery += "&key=" + Constants.API_KEY.getValue().toString();
                Log.d(TAG, fullUrlQuery);
                fetchNearbyPlacesAsyncTask.execute(fullUrlQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    searched = false;
                }
                return searched;
            }
        }); */

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
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLocation!= null){
            mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            mGoogleMap.clear();
            displayCurrent();
            moveToCurrentPosition();
            PlaceItem dest = getDestination();
            if(dest != null){
                mDestLatLng = new LatLng(dest.getLat(), dest.getLng());
                displayOnMap(mDestLatLng, dest.getName());
                getWayPoints(mLatLng, mDestLatLng);
                zoomMap();
            }
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
            return;
        }

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
        Log.d(TAG, "Location changed");
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

            Log.d("onPostExecute","onPostExecute lineoptions decoded");

        }

        if(lineOptions != null) {
            polylines = mGoogleMap.addPolyline(lineOptions);
        }
        else {
            Log.d("onPostExecute","without Polylines drawn");
        }

    }
}
