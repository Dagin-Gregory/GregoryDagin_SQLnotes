
package com.example.mymapsapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener{

    private GoogleMap mMap;

    private EditText locationSearch;

        private TextView testDegrees;

    private LocationManager locationManager;
    private Location myLocation;
    private boolean gotMyLocationOneTime;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;
    private double latitude, longitude;
    private double previousLatitude, previousLongitude;
    private boolean notTrackingMyLocation = false;
    private int trackMarkerDropCounter = 0;
    private LatLng userLocation = null;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    private static final long MIN_TIME_BW_UPDATES = 1000 * 5;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0.0f;
    private static final int MY_LOC_ZOOM_FACTOR = 17;

    private List<Address> addressList = null;
    private List<Address> addressListZip = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

            // TextView that will tell the user what degree is he heading
            testDegrees = (TextView) findViewById(R.id.testDegrees);

            // initialize your android device sensor capabilities
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
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

        // Add a marker in Sydney and move the camera
        LatLng SanFrancisco = new LatLng(38, -122);
        mMap.addMarker(new MarkerOptions().position(SanFrancisco).title("Born in San Francisco"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SanFrancisco));
        /**
         if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
         // TODO: Consider calling
         //    ActivityCompat#requestPermissions
         // here to request the missing permissions, and then overriding
         //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
         //                                          int[] grantResults)
         // to handle the case where the user grants the permission. See the documentation
         // for ActivityCompat#requestPermissions for more details.
         mMap.setMyLocationEnabled(true);
         }
         */
        locationSearch = findViewById(R.id.editText_addr);

        gotMyLocationOneTime = false;
        getLocation();
    }

            protected void onResume() {
                super.onResume();

                // for the system's orientation sensor registered listeners
                mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                        SensorManager.SENSOR_DELAY_GAME);
            }

    public void changeView(View view) {
        if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL)
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        else mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void dropAMarker(String provider) {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            LatLng userLocation = null;
            myLocation = locationManager.getLastKnownLocation(provider);

            if (myLocation == null) {
                Log.d("MyMapsApp", "myLocation is null");
            } else {
                userLocation = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(userLocation, MY_LOC_ZOOM_FACTOR);
                if (provider.equals(LocationManager.GPS_PROVIDER)) {

                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(userLocation)
                            .radius(10)
                            .strokeColor(Color.RED)
                            .strokeWidth(4)
                            .fillColor(Color.RED));
                }
                else {
                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(userLocation)
                            .radius(10)
                            .strokeColor(Color.BLUE)
                            .strokeWidth(4)
                            .fillColor(Color.BLUE));
                }
                mMap.animateCamera(update);
            }
        }
    }

    public void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            //get GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) Log.d("MyMap", "getLocation: GPS is enabled");

            //get Network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isNetworkEnabled) Log.d("MyMap", "getLocation: Network enabled");

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.d("MyMap", "getLocation: No provider is enabled!");
            } else {
                if (isNetworkEnabled) {
                    Log.d("MyMap", "getLocation: no network is enabled!");
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                locationListenerNetwork);
                    }
                }
                if (isGPSEnabled) {
                    Log.d("MyMap", "getLocation: no GPS is enabled!");
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                locationListenerGPS);
                    }
                }
            }
        } catch (Exception e) {
            Log.d("MyMapsApp", "MapsActivity:getLocation exception");
            e.printStackTrace();
        }
    }

    public void onSearch(View v) {
        String location = locationSearch.getText().toString();

        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = service.getBestProvider(criteria, false);

        Log.d("MyMapsApp", "onSearch: location= " + location);
        Log.d("MyMapsApp", "onSearch: provider= " + provider);

        LatLng userLocation = null;
        try{
            if(locationManager!=null){
                if((myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)) != null){
                    userLocation = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                }
                else if((myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)) != null){
                    userLocation = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                }

            }
        }catch (SecurityException | IllegalArgumentException e){
            Log.d("MyMapsApp", "Sxception getLastKnownLocation");
        }
        if(!location.matches("")){
            Geocoder geocoder = new Geocoder(this, Locale.US);
            try{
                addressList = geocoder.getFromLocationName(location, 10000,
                        userLocation.latitude-(5.0/60.0),
                        userLocation.longitude-(5.0/60.0),
                        userLocation.latitude+(5.0/60.0),
                        userLocation.longitude+(5.0/60.0));
            }
            catch(IOException e){
                e.printStackTrace();
            }
            if(!addressList.isEmpty()){
                for(int i = 0; i<addressList.size(); i++){
                    Address address = addressList.get(i);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    latitude = address.getLatitude();
                    longitude = address.getLongitude();
                    mMap.addMarker(new MarkerOptions().position(latLng).title(i+": " + address.getSubThoroughfare()
                            + " " + address.getThoroughfare()));
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }
        }

    }

    public void clear(View view) {
        mMap.clear();
    }

    public void trackMyLocation(View view) {
        if (notTrackingMyLocation) {
            getLocation();
            notTrackingMyLocation = false;
            Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT);
        } else {
            locationManager.removeUpdates(locationListenerGPS);
            locationManager.removeUpdates(locationListenerNetwork);
            notTrackingMyLocation = true;
            Toast.makeText(this, "Location tracking ended", Toast.LENGTH_SHORT);
        }

    }


    LocationListener locationListenerNetwork = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {

            dropAMarker(LocationManager.NETWORK_PROVIDER);
            if (gotMyLocationOneTime == false) {
                locationManager.removeUpdates(this);
                locationManager.removeUpdates(locationListenerNetwork);
                gotMyLocationOneTime = true;
            } else {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }


                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("MyMapsApp", "locationListenerNetwork: status change");
            switch (status) {
                case LocationProvider.AVAILABLE:
                    String status0 = "AVAILABLE";
                    Log.d("MyMapsApp", "onStatusChanged: status = " + status0 + ", location is updating");
                    Toast.makeText(getApplicationContext(), "onStatusChanged: Location Status = " + status0 + ", updating", Toast.LENGTH_SHORT).show();
                    break;

                case LocationProvider.OUT_OF_SERVICE:
                    String status1 = "OUT_OF_SERVICE";
                    Log.d("MyMapsApp", "onStatusChanged: status = " + status1);
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    String status2 = "TEMPORARILY_UNAVAILABLE";
                    Log.d("MyMapsApp", "onStatusChanged: status = " + status2);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    break;
                default:
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(MapsActivity.this, "GPS onLocationChanged", Toast.LENGTH_SHORT).show();
            dropAMarker(LocationManager.GPS_PROVIDER);
            locationManager.removeUpdates(locationListenerNetwork);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    String status0 = "AVAILABLE";
                    Log.d("MyMapsApp", "onStatusChanged: status = " + status0 + ", location is updating");
                    Toast.makeText(getApplicationContext(), "onStatusChanged: Location Status = " + status + ", updating", Toast.LENGTH_SHORT).show();
                    break;

                case LocationProvider.OUT_OF_SERVICE:
                    String status1 = "OUT_OF_SERVICE";
                    Log.d("MyMapsApp", "onStatusChanged: status = " + status1);
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    String status2 = "TEMPORARILY_UNAVAILABLE";
                    Log.d("MyMapsApp", "onStatusChanged: status = " + status2);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    break;
                default:
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerNetwork);
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

    };

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);

        testDegrees.setText("Degrees: " + Float.toString(degree) + " degrees");

        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            updateCamera(degree);
        }

        /*Polygon polygon1 = mMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(userLocation.latitude, userLocation.longitude),
                        new LatLng(userLocation.latitude + 345, userLocation.longitude + 345),
                        new LatLng(userLocation.latitude - 345, userLocation.longitude - 345)));*/

        updateCamera(degree);

        if( (latitude <= latitude + 5 && latitude >= latitude - 5)  && (longitude <= longitude + 5 && longitude >= longitude - 5 )) {


                LatLng latLng = new LatLng(latitude, longitude);
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(10)
                        .strokeColor(Color.GREEN)
                        .strokeWidth(4)
                        .fillColor(Color.GREEN));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }



    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void updateCamera(float bearing) {
        CameraPosition oldPos = mMap.getCameraPosition();

        CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).zoom(50).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }
}