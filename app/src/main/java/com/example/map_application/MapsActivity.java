package com.example.map_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import modules.DirectionFinder;
import modules.DirectionFinderListener;
import modules.PlaceObject;
import modules.Route;
import modules.DBManager;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener {

    // Request code for Intent
    private final int RQCODE_FOR_PERMISSION = 1;
    private final int RQCODE_FOR_SEARCH = 2;
    private final int RQCODE_FROM_FAVORITE = 3;
    private final int RQCODE_FROM_HISTORY = 4;
    private final int RQCODE_FOR_FINDORI = 5;
    private final int RQCODE_FOR_FINDDES = 6;
    private final int DEFAULT_MAP_HEIGHT = 17;

    private boolean locationPermissionGranted;
    private GoogleMap mMap;
    private Geocoder geocoder;


    //find path
    private EditText edtOrigin, edtDestination;
    private TextView tvDuration, tvDistance;
    private LinearLayout llFindPath;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private Button btnFindPath, btnFindFromCurrent;
    private PopupMenu popupMenu;
    private boolean isFindingPath;
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
    private BottomNavigationView navigation;
    private String menuSelected;

    // Location information
    private LinearLayout informationLocation;
    private TextView nameLocation, phoneLocation, ratingLocation, addressLocation, priceLevel;
    private Button btnAddFav;

    // Data base
    private DBManager dbManager;

    // Intro
    private LinearLayout layoutIntro;

    @RequiresApi(api = Build.VERSION_CODES.Q)
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
        if (ContextCompat.checkSelfPermission(MapsActivity.this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else locationPermissionGranted = false;

        // Tọa độ mặc định của ứng dụng
        defaultLocation = new LatLng(10.8759, 106.7992);

        // Thêm find path
        llFindPath = findViewById(R.id.llFindPath);
        edtOrigin = findViewById(R.id.edtOrigin);
        edtOrigin.setFocusable(false);
        edtDestination = findViewById(R.id.edtDestination);
        edtDestination.setFocusable(false);
        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);
        btnFindPath = findViewById(R.id.btnFindPath);
        btnFindFromCurrent = findViewById(R.id.btnFindFromCurrent);
        isFindingPath = false;

        // Thêm navigation bar
        navigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigation.setSelectedItemId(R.id.home);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        popupMenu = new PopupMenu(MapsActivity.this, navigation);
        popupMenu.getMenuInflater().inflate(R.menu.places_picker_menu, popupMenu.getMenu());
        popupMenu.setForceShowIcon(true);

        // Component để hiển thị lựa chọn kiểu bản đồ
        btnSelectType = findViewById(R.id.floating_button_map_type);
        btnSatellite = findViewById(R.id.map_satellite);
        btnTerrain = findViewById(R.id.map_terrain);
        btnDefault = findViewById(R.id.map_default);
        btnCurLocation = findViewById(R.id.btnCurLocation);
        btnShare = findViewById(R.id.btnShare);

        // Component để hiển thị thông tin của địa điểm được tìm thấy
        informationLocation = findViewById(R.id.infoLayout);
        nameLocation = findViewById(R.id.namePos);
        phoneLocation = findViewById(R.id.phonePos);
        ratingLocation = findViewById(R.id.rating);
        addressLocation = findViewById(R.id.address);
        priceLevel = findViewById(R.id.price_level);
        btnAddFav = informationLocation.findViewById(R.id.btnAddFav);

        layoutIntro = findViewById(R.id.layoutIntro);

        // ẩn ban đầu cho một số view
        selectedMaptype = false;
        btnDefault.hide();
        btnSatellite.hide();
        btnTerrain.hide();
        informationLocation.setVisibility(LinearLayout.GONE);

        dbManager = new DBManager(this);
        navigation.setVisibility(View.GONE);
        btnShare.setVisibility(View.GONE);
        btnSelectType.setVisibility(View.GONE);
        setActionListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFindingPath) navigation.setSelectedItemId(R.id.home);
    }

    // TODO: Sử lý các các tính năng tại đây
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.home:
                    searchLocation.setVisibility(View.VISIBLE);
                    llFindPath.setVisibility(View.GONE);
                    edtOrigin.setText("");
                    edtDestination.setText("");
                    tvDistance.setText(R.string.init_kilometer);
                    tvDuration.setText(R.string.init_second);
                    isFindingPath = false;
                    llFindPath.setVisibility(View.INVISIBLE);
                    btnSelectType.setTranslationY(0);
                    btnDefault.setTranslationY(0);
                    btnSatellite.setTranslationY(0);
                    btnTerrain.setTranslationY(0);
                    return true;
                case R.id.find:
                    //llFindPath.animate().translationY(llFindPath.getHeight());
                    //Hiện thanh tìm kiếm 2 địa điểm
                    llFindPath.setVisibility(View.VISIBLE);
                    //Ẩn thanh search location
                    searchLocation.setVisibility(View.GONE);
                    isFindingPath = true;
                    //Đưa button select map type xuống
                    btnSelectType.setTranslationY(llFindPath.getHeight());
                    btnDefault.setTranslationY(llFindPath.getHeight());
                    btnSatellite.setTranslationY(llFindPath.getHeight());
                    btnTerrain.setTranslationY(llFindPath.getHeight());
                    informationLocation.setVisibility(LinearLayout.GONE);
                    searchLocation.setText("");
                    return true;
                case R.id.place:
                    searchLocation.setVisibility(View.VISIBLE);
                    llFindPath.setVisibility(View.GONE);
                    edtOrigin.setText("");
                    edtDestination.setText("");
                    tvDistance.setText(R.string.init_kilometer);
                    tvDuration.setText(R.string.init_second);
                    isFindingPath = false;
                    popupMenu.show();
                    informationLocation.setVisibility(LinearLayout.GONE);
                    searchLocation.setText("");
                    return true;
                case R.id.favorite:
                    isFindingPath = false;
                    Intent intent_favorite = new Intent(MapsActivity.this, FavoriteActivity.class);
                    startActivityForResult(intent_favorite, RQCODE_FROM_FAVORITE);
                    searchLocation.setText("");
                    return true;
                case R.id.history:
                    isFindingPath = false;
                    Intent intent_history = new Intent(MapsActivity.this, HistoryActivity.class);
                    startActivityForResult(intent_history, RQCODE_FROM_HISTORY);
                    searchLocation.setText("");
                    return true;
            }
            return false;
        }
    };


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
    }

    // Chuyển màn hình đến vị trí hiện tại của thiết bị
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            locationPermissionGranted = true;
                            taskForGetCurLocation();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Đại học Khoa học tự nhiên - ĐHQG TPHCM"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT), new GoogleMap.CancelableCallback() {
                                @Override
                                public void onFinish() {
                                    navigation.setVisibility(View.VISIBLE);
                                    btnShare.setVisibility(View.VISIBLE);
                                    btnSelectType.setVisibility(View.VISIBLE);
                                    layoutIntro.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancel() {

                                }
                            });
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        } else if (!locationPermissionGranted) {
            mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Đại học Khoa học tự nhiên - ĐHQG TPHCM"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    navigation.setVisibility(View.VISIBLE);
                    btnShare.setVisibility(View.VISIBLE);
                    btnSelectType.setVisibility(View.VISIBLE);
                    layoutIntro.setVisibility(View.GONE);
                }

                @Override
                public void onCancel() {

                }
            });
        } else {
            taskForGetCurLocation();
        }
    }

    private void taskForGetCurLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();
        locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    lastLocation = task.getResult();
                    LatLng current = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(current)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location)));

                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(current).radius(80)
                            .strokeWidth(0)
                            .strokeColor(Color.parseColor("#225595EC"))
                            .fillColor(Color.parseColor("#225595EC")));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, DEFAULT_MAP_HEIGHT), new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            navigation.setVisibility(View.VISIBLE);
                            btnShare.setVisibility(View.VISIBLE);
                            btnSelectType.setVisibility(View.VISIBLE);
                            layoutIntro.setVisibility(View.GONE);
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
                }
            }

        });
    }

    private void setActionListener() {

        btnCurLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (informationLocation.getVisibility() == LinearLayout.VISIBLE) {
                    informationLocation.setVisibility(LinearLayout.GONE);
                }
                getCurrentLocation();
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

        edtOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME
                        , Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.PRICE_LEVEL);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_FINDORI);
            }
        });

        edtDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME
                        , Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.PRICE_LEVEL);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_FINDDES);
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity((Activity)v.getContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                locationPermissionGranted = true;
                                Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();
                                locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Location> task) {
                                        if (task.isSuccessful()) {
                                            lastLocation = task.getResult();
                                            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                                            sharingIntent.setType("text/plain");
                                            sharingIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com?q="
                                                    + lastLocation.getLatitude() + "," + lastLocation.getLongitude());
                                            startActivity(Intent.createChooser(sharingIntent, "Share via"));
                                        }
                                    }

                                });
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(getApplicationContext(), "Yêu cầu truy cập vị trí", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }
        });

        //send request
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequest();
            }
        });

        btnFindFromCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity((Activity)v.getContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                locationPermissionGranted = true;
                                Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();
                                locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Location> task) {
                                        if (task.isSuccessful()) {
                                            lastLocation = task.getResult();
                                            String origin = "" + lastLocation.getLatitude() +"," + lastLocation.getLongitude();
                                            String destination = searchLocation.getText().toString();
                                            try {
                                                new DirectionFinder(MapsActivity.this, origin, destination).execute();
                                                informationLocation.setVisibility(View.GONE);
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                });
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(getApplicationContext(), "Yêu cầu truy cập vị trí", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }
        });

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //getCurrentLocation();
                if (locationPermissionGranted && lastLocation != null) {
                    String type="";
                    LatLng current = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(current)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location)));

                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(current).radius(80)
                            .strokeWidth(0)
                            .strokeColor(Color.parseColor("#225595EC"))
                            .fillColor(Color.parseColor("#225595EC")));
                    switch (item.getItemId()) {
                        case R.id.menuATM:
                            type="atm";
                            break;
                        case R.id.menuCafe:
                            type="cafe";
                            break;
                        case R.id.menuGasStation:
                            type="gas_station";
                            break;
                        case R.id.menuGym:
                            type="gym";
                            break;
                        case R.id.menuRestaurant:
                            type="restaurant";
                            break;
                        case R.id.menuSchool:
                            type="school";
                            break;
                    }
                    NearbyLocationSearch searcher = new NearbyLocationSearch(getApplicationContext(),
                            lastLocation.getLatitude(), lastLocation.getLongitude(), type);
                    searcher.execute(mMap);
                    return true;
                }
                return false;
            }
        });
    }

    // Kiểm tra và xin cấp quyền sử dụng vị trí
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(MapsActivity.this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        }
        else {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, RQCODE_FOR_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Xử lí kết quả xin cấp quyền
        locationPermissionGranted = false;

        // requestCode của xin cấp quyền xử dụng vị trí
        if (requestCode == RQCODE_FOR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
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
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            mMap.clear();
            //chuyển camera tới vị trí bắt đầu
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, DEFAULT_MAP_HEIGHT));

            tvDuration.setText(route.duration.text);
            tvDistance.setText(route.distance.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.position_start_32))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(R.color.colorRoadRoute).
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

    private void showPlaceInformation(@NotNull Place place) {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQCODE_FOR_SEARCH && resultCode == RESULT_OK) {
            final Place place = Autocomplete.getPlaceFromIntent(data);

            showPlaceInformation(place);

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
            navigation.setSelectedItemId(R.id.home);
            int pos = data.getIntExtra("position", 0) + 1;
            PlaceObject place = dbManager.FAVORITE_getPlace(pos);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatlong(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatlong()).title(place.getAddress()));
        }

        if (resultCode == RESULT_OK && requestCode == RQCODE_FROM_HISTORY && data != null) {
            navigation.setSelectedItemId(R.id.home);
            int pos = data.getIntExtra("position", 0) + 1;
            PlaceObject place = dbManager.HISTORY_getPlace(pos);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatlong(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatlong()).title(place.getAddress()));
        }

        if (requestCode == RQCODE_FOR_FINDORI && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            edtOrigin.setText(place.getAddress());
        }

        if (requestCode == RQCODE_FOR_FINDDES && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            edtDestination.setText(place.getAddress());
        }
    }
}