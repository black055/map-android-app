package com.example.map_application;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private boolean locationPermissionGranted;
    private GoogleMap mMap;
    private Button btnZoomIn, btnZoomOut, btnCurPosition;
    private Location lastLocation;
    private LatLng defaultLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnCurPosition = findViewById(R.id.btnCurPosition);
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
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 20));
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
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 20));
                            mMap.addMarker(new MarkerOptions().position(location).title("Your current location"));
                        } else {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 20));
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
        locationPermissionGranted = false;
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                setCurrentLocation();
            }
        }
    }
}