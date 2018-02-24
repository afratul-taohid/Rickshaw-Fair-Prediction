package com.mindork.app.projectdatamining;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.nlopez.smartlocation.OnActivityUpdatedListener;
import io.nlopez.smartlocation.OnGeofencingTransitionListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import static com.mindork.app.projectdatamining.AppConfig.*;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, OnLocationUpdatedListener, OnActivityUpdatedListener,
        OnGeofencingTransitionListener, GoogleMap.OnMarkerDragListener, View.OnClickListener,
        SearchView.OnQueryTextListener {

    private GoogleMap mMap;
    private LocationGooglePlayServicesProvider provider;
    private static final int LOCATION_PERMISSION_ID = 1001;
    private MarkerOptions desMarker;
    private TextView testTV, addTV, desTV, disTV, predictTV;
    private LatLng origin, destination;
    private List<LatLng>  list = new ArrayList<>();
    private List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
    private Logistic mClassifier;
    private EditText fareEdit;
    private String dT;
    private double distance;
    private MarkerOptions sourceMarker;
    private String placeAddress = "";
    private SearchView searchView;
    private ClusterManager<LocationPoints> mClusterManager;
    private LocationPoints points;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_maps);

        //Handling toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        // initializing layout components
        testTV = findViewById(R.id.lat);
        addTV = findViewById(R.id.add_title);
        disTV = findViewById(R.id.travel_dist);
        fareEdit = findViewById(R.id.fare_value);
        Button predictButton = findViewById(R.id.btn_predict);
        predictTV = findViewById(R.id.prediction);
        desTV = findViewById(R.id.add_des);

        disTV.setText("0 KM");

        //listener for predict button
        predictButton.setOnClickListener(this);

        //Load Model
        loadTrainingSetModel();

        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(this, R.layout.autocomplete_list_item);
        adapter.notifyDataSetChanged();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        BottomSheetBehavior.from(findViewById(R.id.bottomView))
                .setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                finish();
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED:
                                setStatusBarDim(false);
                                break;
                            default:
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                }
                                setStatusBarDim(true);
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                    }
                });
    }

    private void setStatusBarDim(boolean dim) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (dim){
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        }
    }

    private void loadTrainingSetModel() {
        try {
            InputStream is = this.getAssets().open("logistic_training_dataset.model");
            ObjectInputStream ois = new ObjectInputStream(is);
            mClassifier = (Logistic) ois.readObject();
            Toast.makeText(this, "Model loaded.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SmartLocation.with(this).location().stop();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setTrafficEnabled(true);
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setMyLocationButtonEnabled(true);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
            return;
        } else {
            startLocation();
        }
        showLast();
    }

    private void startLocation() {

        long mLocTrackingInterval = 1000 * 5; // 5 sec
        float trackingDistance = 0;
        LocationAccuracy trackingAccuracy = LocationAccuracy.HIGH;

        LocationParams.Builder builder = new LocationParams.Builder()
                .setAccuracy(trackingAccuracy)
                .setDistance(trackingDistance)
                .setInterval(mLocTrackingInterval);

        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);
        SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();
        smartLocation.location(provider).continuous().config(builder.build()).start(this);
        smartLocation.activity().start(this);
    }

    private void showLast() {
        Location lastLocation = SmartLocation.with(this).location().getLastLocation();
        if (lastLocation != null) {
            getMapLocation(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
        }

        DetectedActivity detectedActivity = SmartLocation.with(this).activity().getLastActivity();
        if (detectedActivity != null) {
            String status = "" + getNameFromType(detectedActivity);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onActivityUpdated(DetectedActivity detectedActivity) {

    }

    @Override
    public void onGeofenceTransition(TransitionGeofence transitionGeofence) {

    }

    @Override
    public void onLocationUpdated(Location location) {
        origin = new LatLng(location.getLatitude(), location.getLongitude());
        sourceMarker.position(origin);
        try {
            getUpdatedUI(destination, false);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private String getNameFromType(DetectedActivity activityType) {
        switch (activityType.getType()) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            default:
                return "unknown";
        }
    }

    public void getMapLocation(LatLng latlng) {
        //clear the map
        mMap.clear();

        //zoom to current position:
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latlng).zoom(15).bearing(30).build();
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
        sourceMarker = new MarkerOptions();
        sourceMarker.position(latlng);
        sourceMarker.title("Source");
        sourceMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        setDestinationMarker(null,false);
        mMap.addMarker(sourceMarker);
    }

    private void setDestinationMarker(LatLng latLng, boolean isResult){
        desMarker = new MarkerOptions();
        desMarker.title("Destination");
        desMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        desMarker.draggable(true);

        CameraPosition cameraPosition;

        if (isResult){
            try{
                if (distance <= 1){
                    cameraPosition = new CameraPosition.Builder()
                            .target(latLng).zoom(16).bearing(30).build();
                }
                else if (distance < 2){
                    cameraPosition = new CameraPosition.Builder()
                            .target(latLng).zoom(15).bearing(30).build();
                }
                else if (distance < 3){
                    cameraPosition = new CameraPosition.Builder()
                            .target(latLng).zoom(14).bearing(30).build();
                }
                else {
                    cameraPosition = new CameraPosition.Builder()
                            .target(latLng).zoom(12).bearing(30).build();
                }

                mMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(cameraPosition));
                desMarker.position(destination);
                mMap.addMarker(desMarker);
            } catch (Exception ex){

            }
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        getMapLocation(origin);
        destination = marker.getPosition();
        getUpdatedUI(destination, true);
    }

    public LatLng getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(this, Locale.getDefault());
        List<Address> address;
        LatLng latLng = null;
        Address location = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null || address.size()==0) {
                return null;
            }

            if (address.size() > 0) {
                for (int i=0; i<address.size(); i++) {
                    location = address.get(i);
                }
            }

            if ((location != null && location.hasLatitude()) && location.hasLongitude()){
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                latLng = new LatLng(lat, lng);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return latLng;
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();

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

    private void getLocation(String placeAddress){
        String apiUrl = "http://maps.google.com/maps/api/geocode/json?" +
                "address=" + placeAddress;

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
//                    mClusterManager = new ClusterManager<>(MapsActivity.this, mMap);
//                    mMap.setOnCameraIdleListener(mClusterManager);
//                    mMap.setOnMarkerClickListener(mClusterManager);
//                    mMap.setOnInfoWindowClickListener(mClusterManager);

                    if (response.getString(STATUS).equalsIgnoreCase(OK)) {
                        JSONArray jsonArray = response.getJSONArray(RESULTS);
                        for (int i=0; i<jsonArray.length(); i++){
                            JSONObject result = jsonArray.getJSONObject(i);
                            JSONObject geometry = result.getJSONObject(GEOMETRY).getJSONObject(LOCATION);
                            double lat = geometry.getDouble(LAT);
                            double lng = geometry.getDouble(LNG);
                            Toast.makeText(MapsActivity.this, "" + lat, Toast.LENGTH_SHORT).show();
//                            points = new LocationPoints(lat,lng);
//                            mClusterManager.addItem(points);
                        }
                       // mClusterManager.cluster();
                    }
                } catch (JSONException e) {
                    predictTV.setText(e.getMessage());
                }
            }
        };

        JsonObjectRequest request = new JsonObjectRequest(apiUrl, listener
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                predictTV.setText(error.getMessage());
            }
        });
        AppController.getInstance().addToRequestQueue(request);
    }

    private void getAddress(LatLng latLng, final boolean isLabel){

        String placeUrl = "http://maps.googleapis.com/maps/api/geocode/json?"
                + "latlng="+ latLng.latitude
                + "," + latLng.longitude
                + "&sensor=true";

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString(STATUS).equalsIgnoreCase(OK)) {
                        JSONArray jsonArray = response.getJSONArray(RESULTS);
                        JSONObject place = jsonArray.getJSONObject(0);
                        placeAddress = place.getString(FORMATTED_ADDRESS);
                        if (isLabel) {
                            addTV.setText("Source: " + placeAddress);
                        } else {
                            desTV.setText("Destination: " + placeAddress);
                        }
                    }
                } catch (JSONException e) {
                    predictTV.setText(e.getMessage());
                }
            }
        };

        JsonObjectRequest request = new JsonObjectRequest(placeUrl, listener
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                predictTV.setText(error.getMessage());
            }
        });
        AppController.getInstance().addToRequestQueue(request);
    }

    private void getRoutes(LatLng origin, final LatLng destination){
        String apiMapUrl = "http://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + origin.latitude
                + "," + origin.longitude
                + "&destination=" + destination.latitude
                + "," + destination.longitude
                + "&sensor=false"
                + "&units=metric"
                + "&mode=driving"
                + "&alternatives=true"
                + "&region=bn";

        if (routes.size()>0)
            routes.clear();

        JsonObjectRequest request = new JsonObjectRequest(apiMapUrl,

                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        Log.i(TAG, "onResponse: Result= " + result.toString());
                        try {
                            if (result.getString(STATUS).equalsIgnoreCase(OK)) {
                                JSONArray jsonRoutes = result.getJSONArray(ROUTES);
                                for (int i=0; i<jsonRoutes.length(); i++){
                                    List path = new ArrayList<>();
                                    JSONObject overviewPolyline = jsonRoutes.getJSONObject(i).getJSONObject(OVERVIEW_POLYLINE);
                                    String poly = overviewPolyline.getString(POINTS);

                                    if (list.size()>0)
                                        list.clear();

                                    list = decodePoly(poly);

                                    for (int l=0; l<list.size(); l++){
                                        HashMap<String, String> hm = new HashMap<>();
                                        hm.put("lat", Double.toString((list.get(l)).latitude) );
                                        hm.put("lng", Double.toString((list.get(l)).longitude) );
                                        path.add(hm);
                                    }
                                    routes.add(path);
                                }

                                // Traversing through all the routes
                                for (int i = 0; i < routes.size(); i++) {
                                    ArrayList<LatLng> points = new ArrayList<>();

                                    // Fetching i-th route
                                    List<HashMap<String, String>> path = routes.get(i);

                                    // Fetching all the points in i-th route
                                    for (int j = 0; j < path.size(); j++) {
                                        HashMap<String, String> point = path.get(j);

                                        double lat = Double.parseDouble(point.get("lat"));
                                        double lng = Double.parseDouble(point.get("lng"));
                                        LatLng position = new LatLng(lat, lng);

                                        points.add(position);
                                    }

                                    mMap.addPolyline(
                                        new PolylineOptions()
                                            .geodesic(true)
                                            .addAll(points)
                                            .width(10).color(Color.RED)
                                    );
                                }
                            } else if (result.getString(STATUS).equalsIgnoreCase(ZERO_RESULTS)) {

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "parseLocationResult: Error=" + e.getMessage());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: Error= " + error);
                        Log.e(TAG, "onErrorResponse: Error= " + error.getMessage());
                    }
                });

        AppController.getInstance().addToRequestQueue(request);
    }

    public String getDistance(LatLng s_latLng, LatLng d_latLng) {
        Location l1 = new Location("One");
        l1.setLatitude(s_latLng.latitude);
        l1.setLongitude(s_latLng.longitude);

        Location l2 = new Location("Two");
        l2.setLatitude(d_latLng.latitude);
        l2.setLongitude(d_latLng.longitude);

        distance = l1.distanceTo(l2);
        String dist;
        distance = distance / 1000.0f;
        dT = String.format(Locale.getDefault(),"%.1f", distance);
        dist = dT + " KM";
        return dist;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_predict :
                try {
                    predictDoingClassification();
                } catch (Exception ex){

                }
                break;
            default:
                break;
        }
    }

    private void onSearchEnter(String query) {
        try {
            getMapLocation(origin);
            destination = getLocationFromAddress(query + ",Dhaka");
            getUpdatedUI(destination, true);
        } catch (Exception ex){

        }
    }



    private void predictDoingClassification(){
        if(mClassifier==null){
            Toast.makeText(this, "Model not loaded!", Toast.LENGTH_SHORT).show();
            return;
        }

        // we need those for creating new instances later
        // order of attributes/classes needs to be exactly equal to those used for training
        final Attribute attributeSourceLat = new Attribute("source_lat");
        final Attribute attributeSourceLng = new Attribute("source_lng");
        final Attribute attributeDestLat = new Attribute("destination_lat");
        final Attribute attributeDestLng = new Attribute("destination_lng");
        final Attribute attributeDist = new Attribute("distance");
        final Attribute attributeFare = new Attribute("fare");
        final List<String> classes = new ArrayList<String>() {
            {
                add("EXCEED"); // cls nr 1
                add("AVERAGE"); // cls nr 2
                add("NORMAL"); // cls nr 3
            }
        };

        // Instances(...) requires ArrayList<> instead of List<>...
        ArrayList<Attribute> attributeList = new ArrayList<Attribute>(2) {
            {
                add(attributeSourceLat);
                add(attributeSourceLng);
                add(attributeDestLat);
                add(attributeDestLng);
                add(attributeDist);
                add(attributeFare);
                Attribute attributeClass = new Attribute("@@class@@", classes);
                add(attributeClass);
            }
        };

        // unpredicted data sets (reference to sample structure for new instances)
        Instances dataUnpredicted = new Instances("TestInstances",
                attributeList, 1);
        // last feature is target variable
        dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);

        // create new instance
        DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes()) {
            {
                setValue(attributeSourceLat, origin.latitude);
                setValue(attributeSourceLng, origin.longitude);
                setValue(attributeDestLat, destination.latitude);
                setValue(attributeDestLng, destination.longitude);
                setValue(attributeDist, Double.parseDouble(dT));
                setValue(attributeFare, Double.parseDouble(fareEdit.getText().toString()));
            }
        };

        // reference to dataset
        newInstance.setDataset(dataUnpredicted);

        // predict new sample
        try {
            double result = mClassifier.classifyInstance(newInstance);
            String className = classes.get(Double.valueOf(result).intValue());
            String msg = "predicted: " + className;
            predictTV.setText(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Show test data
        String data = "" + origin.toString() + "," +
                destination.toString() + "," +
                getDistance(origin, destination) + ",";
        testTV.setText(data);
    }

    private void predictDoingRegression(){
        try {

            InputStream is = this.getAssets().open("trainning_data_regression.model");
            ObjectInputStream ois = new ObjectInputStream(is);
            LinearRegression model = (LinearRegression) ois.readObject();

            //create instance
            final Attribute attributeSourceLat = new Attribute("source_lat");
            final Attribute attributeSourceLng = new Attribute("source_lng");
            final Attribute attributeDestLat = new Attribute("destination_lat");
            final Attribute attributeDestLng = new Attribute("destination_lng");
            final Attribute attributeDist = new Attribute("distance");
            final Attribute attributeFare = new Attribute("fare");

            // Instances(...) requires ArrayList<> instead of List<>...
            ArrayList<Attribute> attributeList = new ArrayList<Attribute>(2) {
                {
                    add(attributeSourceLat);
                    add(attributeSourceLng);
                    add(attributeDestLat);
                    add(attributeDestLng);
                    add(attributeDist);
                    add(attributeFare);
                }
            };

            // unpredicted data sets (reference to sample structure for new instances)
            Instances dataUnpredicted = new Instances("TestInstances",
                    attributeList, 1);
            // last feature is target variable
            dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);

            // create new instance
            DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes()) {
                {
                    setValue(attributeSourceLat, origin.latitude);
                    setValue(attributeSourceLng, origin.longitude);
                    setValue(attributeDestLat, destination.latitude);
                    setValue(attributeDestLng, destination.longitude);
                    setValue(attributeDist, Double.parseDouble(dT));
                }
            };

            // reference to dataset
            newInstance.setDataset(dataUnpredicted);
            double predictedValue = model.classifyInstance(newInstance);
            String value = String.format(Locale.getDefault(),"%.0f", predictedValue);
            testTV.setTextColor(Color.DKGRAY);
            testTV.setText(value + " TK");
        } catch (Exception e) {
            e.printStackTrace();
            testTV.setText(e.getMessage());
        }
    }

    private LatLng getCenterPoint(){
        try {
            double lat = (origin.latitude + destination.latitude)/2;
            double lng = (origin.longitude + destination.longitude)/2;

            return new LatLng(lat,lng);
        } catch (Exception ex){
            return origin;
        }
    }

    private void getUpdatedUI(LatLng destination, boolean isTrue){
        disTV.setText("" + getDistance(origin, destination));
        getAddress(origin, true);
        getAddress(destination, false);
        if (isTrue){
            //desMarker.position(destination);
            //mMap.addMarker(desMarker);
            setDestinationMarker(getCenterPoint(), true);
            getRoutes(origin, destination);
        }
        predictDoingRegression();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        getLocation(query);
        onSearchEnter(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
//        PlaceAPI placeAPI = new PlaceAPI();
//        ArrayList<String> arrayList = placeAPI.autocomplete(newText);
//        SimpleCursorAdapter suggestionList =
//                new SimpleCursorAdapter(this, android.R.layout.simple_spinner_dropdown_item,
//                        null, arrayList.toArray(new String[arrayList.size()]), new int[]{R.id.autocompleteText},
//                        CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
//        searchView.setSuggestionsAdapter(suggestionList);
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_menu, menu);

//        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
//                getFragmentManager().findFragmentById(R.id.menu_search);
//
//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//                // TODO: Get info about the selected place.
//                Log.i(TAG, "Place: " + place.getName());
//            }
//
//            @Override
//            public void onError(Status status) {
//                // TODO: Handle the error.
//                Log.i(TAG, "An error occurred: " + status);
//            }
//        });


        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()){
            searchView.onActionViewCollapsed();
        } else {
            super.onBackPressed();
        }
    }
}