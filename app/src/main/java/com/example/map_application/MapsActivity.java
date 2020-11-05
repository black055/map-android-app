package com.example.map_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import modules.DirectionFinder;
import modules.DirectionFinderListener;
import modules.PlaceObject;
import modules.Route;
import modules.DBManager;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener {

    private final int RQCODE_FOR_PERMISSION = 1;
    private final int RQCODE_FOR_SEARCH = 2;
    private final int RQCODE_FROM_FAVORITE = 3;
    private final int RQCODE_FROM_HISTORY = 4;
    private final int DEFAULT_MAP_HEIGHT = 17;

    private boolean locationPermissionGranted;
    private GoogleMap mMap;
    private Geocoder geocoder;private Button btnZoomIn, btnZoomOut, btnFindPathBack, btnFindPath;

    //find path
    private EditText edtOrigin, edtDestination;
    private TextView tvDuration, tvDistance;
    private LinearLayout llFindPath;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    //---------
    private Location lastLocation;
    private LatLng defaultLocation;
    private EditText searchLocation;
    private ImageView btnCurLocation;
    private FloatingActionButton btnShare;

    // MapType Option
    FloatingActionButton btnSelectType, btnSatellite, btnTerrain, btnDefault;
    boolean selectedMaptype;

    // Navigation Bar
    public BottomNavigationView navigation;

    // Location information
    LinearLayout informationLocation;
    TextView nameLocation, phoneLocation, ratingLocation, addressLocation, priceLevel;
    DBManager dbManager;
    Button btnAddFav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));

        geocoder = new Geocoder(MapsActivity.this);
        searchLocation = findViewById(R.id.searchLocation);
        searchLocation.setFocusable(false);
        // Kiểm tra đã cấp quyền truy cập vào vị trí chưa
        if (ContextCompat.checkSelfPermission(MapsActivity.this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else locationPermissionGranted = false;

        // Tọa độ mặc định của ứng dụng
        defaultLocation = new LatLng(10.8759, 106.7992);

        // Thêm find path
        llFindPath = findViewById(R.id.llFindPath);
        edtOrigin = findViewById(R.id.edtOrigin);
        edtDestination = findViewById(R.id.edtDestination);
        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);
        btnFindPathBack = findViewById(R.id.btnFindPathBack);
        btnFindPath = findViewById(R.id.btnFindPath);


        //send request
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequest();
            }
        });

        // Thêm navigation bar
        navigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigation.setSelectedItemId(R.id.invisible);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Thêm Chọn Mape Type
        btnSelectType = findViewById(R.id.floating_button_map_type);
        btnSatellite = findViewById(R.id.map_satellite);
        btnTerrain = findViewById(R.id.map_terrain);
        btnDefault = findViewById(R.id.map_default);
        btnCurLocation = findViewById(R.id.btnCurLocation);
        btnShare = findViewById(R.id.btnShare);

        informationLocation = findViewById(R.id.infoLayout);
        nameLocation = findViewById(R.id.namePos);
        phoneLocation = findViewById(R.id.phonePos);
        ratingLocation = findViewById(R.id.rating);
        addressLocation = findViewById(R.id.address);
        priceLevel = findViewById(R.id.price_level);

        btnAddFav = informationLocation.findViewById(R.id.btnAddFav);

        // ẩn ban đầu cho một số view
        selectedMaptype = false;
        btnDefault.hide();
        btnSatellite.hide();
        btnTerrain.hide();
        informationLocation.setVisibility(LinearLayout.GONE);

        dbManager = new DBManager(this);
        setActionListener();
    }

    // TODO: Sử lý các các tính năng tại đây
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.find:
                    //Ẩn thanh search location
                    searchLocation.setVisibility(View.GONE);
                    //Hiện thanh tìm kiếm 2 địa điểm
                    llFindPath.setVisibility(View.VISIBLE);
                    //Xử lí nút back quay về giao diện chính
                    btnFindPathBack.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            llFindPath.setVisibility(View.GONE);
                            searchLocation.setVisibility(View.VISIBLE);
                            edtOrigin.setText("");
                            edtDestination.setText("");
                            tvDistance.setText(R.string.init_kilometer);
                            tvDuration.setText(R.string.init_second);
                        }
                    });
                    informationLocation.setVisibility(LinearLayout.GONE);
                    return true;
                case R.id.place:
                    getCurrentLocation();
                    if (locationPermissionGranted && lastLocation != null) {
                        moveMapToCurrentLocation();
                        NearbyLocationSearch searcher = new NearbyLocationSearch(getApplicationContext(),
                                lastLocation.getLatitude(), lastLocation.getLongitude(), "atm");
                        searcher.execute(mMap);
                    }
                    informationLocation.setVisibility(LinearLayout.GONE);
                    return true;
                case R.id.favorite:
                    Intent intent_favorite = new Intent(MapsActivity.this, FavoriteActivity.class);
                    startActivityForResult(intent_favorite, RQCODE_FROM_FAVORITE);
                    informationLocation.setVisibility(LinearLayout.GONE);
                    return true;
                case R.id.history:
                    Intent intent_history = new Intent(MapsActivity.this, HistoryActivity.class);
                    startActivityForResult(intent_history, RQCODE_FROM_HISTORY);
                    informationLocation.setVisibility(LinearLayout.GONE);
                    return true;
            }
            return false;
        }
    };


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setPadding(0, 1600, 0, 140);
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Đại học Khoa học tự nhiên - ĐHQG TPHCM"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT));
    }

    // Chuyển màn hình đến vị trí hiện tại của thiết bị
    private void getCurrentLocation() {
        if (locationPermissionGranted) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();

            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        lastLocation = task.getResult();
                    }
                }
            });
        } else getLocationPermission();
    }

    private void moveMapToCurrentLocation() {
        LatLng location = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_MAP_HEIGHT));
        Address address = null;
        try {
            address = geocoder.getFromLocation(lastLocation.getLatitude(),lastLocation.getLongitude(), 1).get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Nếu lấy được địa chỉ chi tiết thì hiển thị trên marker, không thì hiển thị "Your current location"
        if (address != null && address.getAddressLine(0) != null)
            mMap.addMarker(new MarkerOptions().position(location).title(address.getAddressLine(0)));
        else
            mMap.addMarker(new MarkerOptions().position(location).title("Your current location"));
    }

    private void setActionListener() {

        btnCurLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (informationLocation.getVisibility() == LinearLayout.VISIBLE) {
                    informationLocation.setVisibility(LinearLayout.GONE);
                }
                getCurrentLocation();
                // Nếu lấy được vị trí hiện tại thì chuyển camera đến vị trí hiện tại, nếu không thì chuyển đến vị trí mặc định
                if (lastLocation != null) {
                    moveMapToCurrentLocation();
                } else {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT));
                }
            }
        });

        searchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME
                        , Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.PRICE_LEVEL);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_SEARCH);
            }
        });

        // Thay đổi MapType
        btnSelectType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedMaptype) {
                    btnDefault.hide();
                    btnSatellite.hide();
                    btnTerrain.hide();
                    selectedMaptype = false;
                }

                else if (!selectedMaptype) {
                    btnTerrain.show();
                    btnSatellite.show();
                    btnDefault.show();;
                    selectedMaptype = true;
                }
            }
        });

        btnDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });

        btnSatellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_SATELLITE) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
            }
        });

        btnTerrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_TERRAIN) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }
            }
        });

        /*edtOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_SEARCH);
            }
        });

        edtDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_SEARCH);
            }
        });*/

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                if (lastLocation != null) {
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com?q="
                    + lastLocation.getLatitude() + "," + lastLocation.getLongitude());
                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
                }
            }
        });
    }

    // Kiểm tra và xin cấp quyền sử dụng vị trí
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(MapsActivity.this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getCurrentLocation();
        }
        else ActivityCompat.requestPermissions(MapsActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, RQCODE_FOR_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Xử lí kết quả xin cấp quyền
        locationPermissionGranted = false;

        // requestCode của xin cấp quyền xử dụng vị trí
        if (requestCode == RQCODE_FOR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                getCurrentLocation();
            }
        }
    }

    //Find path-----------
    @Override
    public void onDirectionFinderStart() {
        /*progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);*/

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        if (routes.size() == 0) return;

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            tvDuration.setText(route.duration.text);
            tvDistance.setText(route.distance.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_start_32))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.red_end_32))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(R.color.quantum_lightblue).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    private void sendRequest() {
        String origin = edtOrigin.getText().toString();
        String destination = edtDestination.getText().toString();
        if (origin.isEmpty()) {
            Toast.makeText(this, "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    //-----------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQCODE_FOR_SEARCH && resultCode == RESULT_OK) {
            final Place place = Autocomplete.getPlaceFromIntent(data);
            searchLocation.setText(place.getAddress());
            nameLocation.setText(place.getName());
            addressLocation.setText(place.getAddress());
            // check phone number
            if (place.getPhoneNumber() != null) {
                phoneLocation.setText("Phone number: " + place.getPhoneNumber());
            }
            else
                phoneLocation.setVisibility(View.GONE);

            // check rating
            if (place.getRating() != null) {
                ratingLocation.setText("Rating : " + place.getRating());
            }
            else
                ratingLocation.setVisibility(View.GONE);


            // check price level
            if (place.getPriceLevel() != null) {
                priceLevel.setText("Price level: " + place.getPriceLevel());
            }
            else
                priceLevel.setVisibility(View.GONE);

            informationLocation.setVisibility(LinearLayout.VISIBLE);

            // listener cho button add favorite
            btnAddFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbManager.FAVORITE_addPlace(new PlaceObject(place.getName(), place.getAddress(), place.getLatLng()));
                }
            });

            // thêm vào lịch sử tìm kiếm
            dbManager.HISTORY_addPlace(new PlaceObject(place.getName(), place.getAddress(), place.getLatLng()));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getAddress()));
        }

        if (resultCode == RESULT_OK && requestCode == RQCODE_FROM_FAVORITE && data != null) {
            int pos = data.getIntExtra("position", 0) + 1;
            PlaceObject place = dbManager.FAVORITE_getPlace(pos);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatlong(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatlong()).title(place.getAddress()));
        }

        if (resultCode == RESULT_OK && requestCode == RQCODE_FROM_HISTORY && data != null) {
            int pos = data.getIntExtra("position", 0) + 1;
            PlaceObject place = dbManager.HISTORY_getPlace(pos);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatlong(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatlong()).title(place.getAddress()));
        }
    }
}