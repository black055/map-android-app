package com.example.map_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int DEFAULT_MAP_HEIGHT = 17;
    private boolean locationPermissionGranted;
    private GoogleMap mMap;
    private Geocoder geocoder;
    private Button btnZoomIn, btnZoomOut;
    private ImageView btnCurPosition;
    private Location lastLocation;
    private LatLng defaultLocation;
    private SearchView searchLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geocoder = new Geocoder(MapsActivity.this);

        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnCurPosition = findViewById(R.id.btnCurPosition);
        searchLocation = findViewById(R.id.searchLocation);

        // Kiểm tra đã cấp quyền truy cập vào vị trí chưa
        if (ContextCompat.checkSelfPermission(MapsActivity.this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else locationPermissionGranted = false;
        // Tọa độ mặc định của ứng dụng
        defaultLocation = new LatLng(10.8759, 106.7992);

        setActionListener();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Đại học Khoa học tự nhiên - ĐHQG TPHCM"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT));
    }

    // Chuyển màn hình đến vị trí hiện tại của thiết bị
    private void setCurrentLocation() {
        if (locationPermissionGranted) {
            Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();

            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        lastLocation = task.getResult();
                        if (lastLocation != null) {
                            LatLng location = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_MAP_HEIGHT));
                            Address address = null;
                            try {
                                address = geocoder.getFromLocation(lastLocation.getLatitude(),lastLocation.getLongitude(), 1).get(0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (address != null && address.getAddressLine(0) != null)
                                mMap.addMarker(new MarkerOptions().position(location).title(address.getAddressLine(0)));
                            else
                                mMap.addMarker(new MarkerOptions().position(location).title("Your current location"));
                        } else {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT));
                        }
                    }
                }
            });
        } else getLocationPermission();
    }

    private void setActionListener() {
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        btnCurPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentLocation();
            }
        });


        searchLocation.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.equals("")) {
                    List<Address> addressList = null;
                    try {
                        addressList = geocoder.getFromLocationName(query, 10);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (addressList.size() > 0) {
                        LatLng location = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_MAP_HEIGHT));
                        if (addressList.get(0).getAddressLine(0) != null) {
                            mMap.addMarker(new MarkerOptions().position(location).title(addressList.get(0).getAddressLine(0)));
                        } else mMap.addMarker(new MarkerOptions().position(location));
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    // Kiểm tra và xin cấp quyền sử dụng vị trí
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(MapsActivity.this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            setCurrentLocation();
        }
        else ActivityCompat.requestPermissions(MapsActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Xử lí kết quả xin cấp quyền
        locationPermissionGranted = false;

        // requestCode của xin cấp quyền xử dụng vị trí
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                setCurrentLocation();
            }
        }
    }
}