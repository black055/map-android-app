package com.example.map_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.map_application.customtextview.CircularTextView;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import modules.FindPath.DirectionFinder;
import modules.FindPath.DirectionFinderListener;
import modules.FindPath.Route;
import modules.LocationByName.GetPlaceFromText;
import modules.LocationByName.GetPlaceInterface;
import modules.LocationByName.PlaceObject;
import modules.NearBySearch.NearbyLocationSearch;
import modules.StoreData.DBManager;
import modules.Covid.CovidAPI;
import modules.Covid.CovidInterface;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
        , DirectionFinderListener, GetPlaceInterface, CovidInterface, GoogleMap.OnMarkerClickListener, SensorEventListener {

    // Request code for Intent
    private final int RQCODE_FOR_SEARCH = 2;
    private final int RQCODE_FROM_FAVORITE = 3;
    private final int RQCODE_FROM_HISTORY = 4;
    private final int RQCODE_FOR_FINDORI = 5;
    private final int RQCODE_FOR_FINDDES = 6;
    private final int RQCODE_FOR_SEARCH_VIA_VOICE = 7;
    private final int DEFAULT_MAP_HEIGHT = 17;

    private GoogleMap mMap;
    ConnectivityManager connectivityManager;
    private Marker curLocationMarker;

    private SensorManager sensorManager;

    //find path
    private EditText edtOrigin, edtDestination;
    private TextView tvDuration, tvDistance;
    private LinearLayout llFindPath;
    private List<Polyline> polylinePaths = new ArrayList<>();
    private Button btnFindPath, btnFindFromCurrent,
            btnDrivingMode, btnWalkingMode, btnTransitMode;
    private ImageView ivSetOriginByCurrentPosition;
    private PopupMenu popupMenu;
    private boolean isFindingPath;
    private String travelMode;
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

    // Location information
    private LinearLayout informationLocation;
    private TextView nameLocation, phoneLocation, ratingLocation, addressLocation, priceLevel;
    private Button btnAddFav;
    private boolean isGoBack;

    // Data base
    private DBManager dbManager;

    // Intro
    private LinearLayout layoutIntro;

    // Search via voice
    private ImageView searchByVoice;

    // Corona virus
    FloatingActionButton btnCorona;
    boolean isCheckingCorona;
    ArrayList<String> nameCountry;
    ArrayList<Integer> casesCountry;
    ArrayList<Integer> deadCountry;
    ArrayList<Integer> recoveredCountry;
    ArrayList<String> latCountry;
    ArrayList<String> lngCountry;

    ArrayList<String> nameCity;
    ArrayList<Integer> casesCity;
    ArrayList<Integer> deadCity;
    ArrayList<Integer> recoveredCity;
    ArrayList<String> latCity;
    ArrayList<String> lngCity;

    // Marker icons array for nearby location search
    HashMap<String, Integer> markerIcons;

    // Convert a view to bitmap
    public static Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));

        searchLocation = findViewById(R.id.searchLocation);
        searchByVoice = findViewById(R.id.searchVoice);
        searchLocation.setFocusable(false);

        // Tọa độ mặc định của ứng dụng
        defaultLocation = new LatLng(10.8759, 106.7992);
        curLocationMarker = null;

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

        //Chọn phương thức di chuyển
        travelMode = "driving";
        btnDrivingMode = findViewById(R.id.btnDrivingMode);
        btnWalkingMode = findViewById(R.id.btnWalkingMode);
        btnTransitMode = findViewById(R.id.btnTransitMode);
        ivSetOriginByCurrentPosition = findViewById(R.id.ivSetOriginByCurrentPosition);

        // Thêm navigation bar
        navigation = findViewById(R.id.bottom_navigation);
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
        // Corona virus
        btnCorona = findViewById(R.id.btnCorona);
        isCheckingCorona = false;
        nameCountry = new ArrayList<>();
        casesCountry = new ArrayList<>();
        deadCountry = new ArrayList<>() ;
        recoveredCountry = new ArrayList<>();
        latCountry = new ArrayList<>();
        lngCountry = new ArrayList<>();

        nameCity = new ArrayList<>();
        casesCity = new ArrayList<>();
        deadCity = new ArrayList<>() ;
        recoveredCity = new ArrayList<>();
        latCity = new ArrayList<>();
        lngCity = new ArrayList<>();

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
        searchByVoice.setVisibility(View.GONE);
        btnCorona.setVisibility(View.GONE);
        isGoBack = false;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        markerIcons = new HashMap<>();
        markerIcons.put("atm", R.drawable.atm_marker);
        markerIcons.put("cafe", R.drawable.cafe_marker);
        markerIcons.put("gas_station", R.drawable.gas_station_marker);
        markerIcons.put("gym", R.drawable.fitness_center_marker);
        markerIcons.put("restaurant", R.drawable.restaurant_marker);
        markerIcons.put("school", R.drawable.school_marker);

        setActionListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFindingPath) navigation.setSelectedItemId(R.id.home);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.home:
                    searchLocation.setVisibility(View.VISIBLE);

                    if (isGoBack) {
                        informationLocation.setVisibility(View.VISIBLE);
                        isGoBack = false;
                    }

                    if (isFindingPath) {
                        searchByVoice.setVisibility(View.VISIBLE);
                    }

                    llFindPath.setVisibility(View.GONE);
                    edtOrigin.setText("");
                    edtDestination.setText("");
                    tvDistance.setText(R.string.init_kilometer);
                    tvDuration.setText(R.string.init_second);
                    isFindingPath = false;
                    travelMode = "driving";
                    btnDrivingMode.performClick();
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
                    isCheckingCorona = false;
                    //Ẩn thanh search location
                    searchLocation.setVisibility(View.GONE);
                    searchByVoice.setVisibility(View.GONE);
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
                    searchByVoice.setVisibility(View.VISIBLE);
                    llFindPath.setVisibility(View.GONE);
                    isCheckingCorona = false;
                    edtOrigin.setText("");
                    edtDestination.setText("");
                    tvDistance.setText(R.string.init_kilometer);
                    tvDuration.setText(R.string.init_second);
                    if (isFindingPath) {
                        btnSelectType.setTranslationY(0);
                        btnDefault.setTranslationY(0);
                        btnSatellite.setTranslationY(0);
                        btnTerrain.setTranslationY(0);
                        isFindingPath = false;
                    }
                    popupMenu.show();
                    informationLocation.setVisibility(LinearLayout.GONE);
                    searchLocation.setText("");
                    return true;
                case R.id.favorite:
                    isFindingPath = false;
                    isCheckingCorona = false;
                    Intent intent_favorite = new Intent(MapsActivity.this, FavoriteActivity.class);
                    startActivityForResult(intent_favorite, RQCODE_FROM_FAVORITE);
                    searchLocation.setText("");
                    return true;
                case R.id.history:
                    isFindingPath = false;
                    isCheckingCorona = false;
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
        mMap.getUiSettings().setCompassEnabled(false);
        searchByVoice.setVisibility(View.GONE);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        getCurrentLocation();
    }

    // Chuyển màn hình đến vị trí hiện tại của thiết bị
    private void getCurrentLocation() {
        Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        searchByVoice.setVisibility(View.GONE);
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        taskForGetCurLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        searchByVoice.setVisibility(View.GONE);
                        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Đại học Khoa học tự nhiên - ĐHQG TPHCM"));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT)
                                , new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                navigation.setVisibility(View.VISIBLE);
                                btnShare.setVisibility(View.VISIBLE);
                                btnSelectType.setVisibility(View.VISIBLE);
                                layoutIntro.setVisibility(View.GONE);
                                searchByVoice.setVisibility(View.VISIBLE);
                                if(isFindingPath) searchByVoice.setVisibility(View.GONE);
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
                    curLocationMarker = mMap.addMarker(new MarkerOptions().position(current)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                            .anchor(0.5f, 0.5f));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, DEFAULT_MAP_HEIGHT),
                            new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            navigation.setVisibility(View.VISIBLE);
                            btnShare.setVisibility(View.VISIBLE);
                            btnSelectType.setVisibility(View.VISIBLE);
                            layoutIntro.setVisibility(View.GONE);
                            searchByVoice.setVisibility(View.VISIBLE);
                            btnCorona.setVisibility(View.VISIBLE);
                            if(isFindingPath) searchByVoice.setVisibility(View.GONE);
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
                isCheckingCorona = false;
            }
        });

        searchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME
                        , Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.PRICE_LEVEL);
                isCheckingCorona = false;
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fieldList)
                        .build(MapsActivity.this);
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
                } else {
                    btnTerrain.show();
                    btnSatellite.show();
                    btnDefault.show();
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
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fieldList)
                        .build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_FINDORI);
            }
        });

        edtDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME
                        , Place.Field.RATING, Place.Field.PHONE_NUMBER, Place.Field.PRICE_LEVEL);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fieldList)
                        .build(MapsActivity.this);
                startActivityForResult(intent, RQCODE_FOR_FINDDES);
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity((Activity) v.getContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();
                                locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Location> task) {
                                        if (task.isSuccessful() && task.getResult() != null) {
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
                Dexter.withActivity((Activity) v.getContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                Task<Location> locationResult = LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation();
                                locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Location> task) {
                                        if (task.isSuccessful() && task.getResult() != null) {
                                            lastLocation = task.getResult();
                                            String origin = "" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
                                            String destination = searchLocation.getText().toString();
                                            try {
                                                new DirectionFinder(MapsActivity.this, origin, destination, travelMode).execute();
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
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (lastLocation != null) {
                    String type = "";
                    LatLng current = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.clear();
                    curLocationMarker = mMap.addMarker(new MarkerOptions().position(current)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                            .anchor(0.5f, 0.5f));
                    
                    switch (item.getItemId()) {
                        case R.id.menuATM:
                            type = "atm";
                            break;
                        case R.id.menuCafe:
                            type = "cafe";
                            break;
                        case R.id.menuGasStation:
                            type = "gas_station";
                            break;
                        case R.id.menuGym:
                            type = "gym";
                            break;
                        case R.id.menuRestaurant:
                            type = "restaurant";
                            break;
                        case R.id.menuSchool:
                            type = "school";
                            break;
                    }

                    connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                    if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() != NetworkInfo.State.CONNECTED &&
                            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() != NetworkInfo.State.CONNECTED) {
                        Toast.makeText(MapsActivity.this, "Please turn on the Internet !", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    else
                    {
                        NearbyLocationSearch searcher = new NearbyLocationSearch(getApplicationContext(),
                                lastLocation.getLatitude(), lastLocation.getLongitude(), type,
                                bitmapDescriptorFromVector(MapsActivity.this, markerIcons.get(type)));
                        searcher.execute(mMap);
                    }
                    return true;
                }
                return false;
            }
        });

        searchByVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
                intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
                intent.putExtra("android.speech.extra.PROMPT", "Speak Now");
                startActivityForResult(intent, RQCODE_FOR_SEARCH_VIA_VOICE);
            }
        });

        btnCorona.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() != NetworkInfo.State.CONNECTED &&
                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() != NetworkInfo.State.CONNECTED) {
                    Toast.makeText(MapsActivity.this, "Please turn on the Internet !", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    new CovidAPI(MapsActivity.this).execute();
                }
            }
        });

        //Event click thay đổi travel mode (mặc định là tìm đường đi theo xe)
        btnDrivingMode.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v) {
                tvDistance.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_car_travelmode_clicked, 0, 0,0);
                btnDrivingMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_car_travelmode_clicked, 0, 0, 0);
                btnWalkingMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_walk_24, 0, 0,0);
                btnTransitMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_bus_24, 0, 0,0);
                btnDrivingMode.setTextColor(Color.parseColor("#FF6D00"));
                btnWalkingMode.setTextColor(Color.parseColor("#8a000000"));
                btnTransitMode.setTextColor(Color.parseColor("#8a000000"));

                //Nếu đang là driving thì thoát
                if (travelMode.equals("driving")) return;
                travelMode = "driving";
                if (!edtOrigin.getText().toString().isEmpty() && !edtDestination.getText().toString().isEmpty()) sendRequest();
            }
        });

        btnWalkingMode.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v) {
                tvDistance.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_walk_24_clicked, 0, 0,0);
                btnDrivingMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_car_travelmode_24, 0, 0, 0);
                btnWalkingMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_walk_24_clicked, 0, 0,0);
                btnTransitMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_bus_24, 0, 0,0);
                btnDrivingMode.setTextColor(Color.parseColor("#8a000000"));
                btnWalkingMode.setTextColor(Color.parseColor("#FF6D00"));
                btnTransitMode.setTextColor(Color.parseColor("#8a000000"));

                if (travelMode.equals("walking")) return;
                travelMode = "walking";
                if (!edtOrigin.getText().toString().isEmpty() && !edtDestination.getText().toString().isEmpty()) sendRequest();
            }
        });

        btnTransitMode.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v) {
                tvDistance.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_bus_24_clicked, 0, 0,0);
                btnDrivingMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_car_travelmode_24, 0, 0, 0);
                btnWalkingMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_walk_24, 0, 0,0);
                btnTransitMode.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_directions_bus_24_clicked, 0, 0,0);
                btnDrivingMode.setTextColor(Color.parseColor("#8a000000"));
                btnWalkingMode.setTextColor(Color.parseColor("#8a000000"));
                btnTransitMode.setTextColor(Color.parseColor("#FF6D00"));

                if (travelMode.equals("transit")) return;
                travelMode = "transit";
                if (!edtOrigin.getText().toString().isEmpty() && !edtDestination.getText().toString().isEmpty()) sendRequest();
            }
        });

        //Hiển thị tọa độ vị trí hiện tại ở ô edit text origin
        ivSetOriginByCurrentPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                String temp = String.valueOf(lastLocation.getLatitude()) + ", "
                        + String.valueOf(lastLocation.getLongitude());
                edtOrigin.setText(temp);
                searchByVoice.setVisibility(View.GONE);
            }
        });
    }

    //đổi icon từ định dạng vector sang bitmap
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    //Find path-----------
    @Override
    public void onDirectionFinderStart() {
        //Xóa polyline cũ
        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        //Cấp phát cho polyline (tập hợp các điểm trên đường đi)
        polylinePaths = new ArrayList<>();

        // Không tìm được routes từ google cấp
        if (routes.size() == 0) return;

        for (Route route : routes) {
            //Xóa các marker trên bản đồ
            mMap.clear();
            //Chuyển camera tới vị trí bắt đầu
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, DEFAULT_MAP_HEIGHT));

            //Ghi thời gian, khoảng cách vào 2 ô textview duration, distance
            tvDuration.setText(route.duration.text);
            tvDistance.setText(route.distance.text);

            //add marker điểm đầu, điểm cuối lên mMap
            mMap.addMarker(new MarkerOptions()
                    .icon(bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_position_start))
                    .title(route.startAddress)
                    .position(route.startLocation));
            mMap.addMarker(new MarkerOptions()
                    .title(route.endAddress)
                    .position(route.endLocation));

            //Tạo 1 polyline
            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(ContextCompat.getColor(getApplicationContext(), R.color.colorRoadRoute)).
                    width(15);

            //Thêm các điểm vào polylineOptions
            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));
            //vẽ đường đi giữa 2 điểm
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
            // Tìm đường dựa vào address điểm đầu, cuối
            new DirectionFinder(this, origin, destination, travelMode).execute();
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
        } else
            phoneLocation.setVisibility(View.GONE);

        // check rating
        if (place.getRating() != null) {
            ratingLocation.setText("Rating : " + place.getRating());
        } else
            ratingLocation.setVisibility(View.GONE);


        // check price level
        if (place.getPriceLevel() != null) {
            priceLevel.setText("Price level: " + place.getPriceLevel());
        } else
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
                    Toast.makeText(MapsActivity.this, "Địa điểm đã được thêm vào danh sách", Toast.LENGTH_SHORT).show();
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
            final PlaceObject place = dbManager.FAVORITE_getPlace(pos);
            btnAddFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbManager.FAVORITE_addPlace(place);
                    Toast.makeText(MapsActivity.this, "Địa điểm đã được thêm vào danh sách", Toast.LENGTH_SHORT).show();
                }
            });
            searchLocation.setText(place.getName());
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatlong(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatlong()).title(place.getAddress()));
            nameLocation.setText(place.getName());
            addressLocation.setText(place.getAddress());
            // check phone number
            phoneLocation.setVisibility(View.GONE);

            // check rating
            ratingLocation.setVisibility(View.GONE);

            // check price level
            priceLevel.setVisibility(View.GONE);
            informationLocation.setVisibility(LinearLayout.VISIBLE);
            isGoBack = true;
        }

        if (resultCode == RESULT_OK && requestCode == RQCODE_FROM_HISTORY && data != null) {
            navigation.setSelectedItemId(R.id.home);
            int pos = data.getIntExtra("position", 0) + 1;
            final PlaceObject place = dbManager.HISTORY_getPlace(pos);
            btnAddFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbManager.FAVORITE_addPlace(place);
                    Toast.makeText(MapsActivity.this, "Địa điểm đã được thêm vào danh sách", Toast.LENGTH_SHORT).show();
                }
            });
            searchLocation.setText(place.getName());
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatlong(), DEFAULT_MAP_HEIGHT));
            mMap.addMarker(new MarkerOptions().position(place.getLatlong()).title(place.getAddress()));
            nameLocation.setText(place.getName());
            addressLocation.setText(place.getAddress());
            // check phone number
            phoneLocation.setVisibility(View.GONE);

            // check rating
            ratingLocation.setVisibility(View.GONE);

            // check price level
            priceLevel.setVisibility(View.GONE);
            informationLocation.setVisibility(LinearLayout.VISIBLE);
            isGoBack = true;
        }

        if (requestCode == RQCODE_FOR_FINDORI && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            edtOrigin.setText(place.getAddress());
        }

        if (requestCode == RQCODE_FOR_FINDDES && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            edtDestination.setText(place.getAddress());
        }

        if (requestCode == RQCODE_FOR_SEARCH_VIA_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra("android.speech.extra.RESULTS");
            String result = matches.get(0);
            GetPlaceFromText getPlace = new GetPlaceFromText(MapsActivity.this, result);
            getPlace.execute();
        }
    }

    @Override
    public void getPlaceSuccess(final String[] result) {
        if (result[0].equals("") && result[1].equals("") && result[4] == null) {
            Toast.makeText(MapsActivity.this, "Không tìm thấy kết quả phù hợp", Toast.LENGTH_SHORT).show();
            return;
        }
        searchLocation.setText(result[0]);
        nameLocation.setText(result[0]);
        addressLocation.setText(result[1]);
        // check phone number
        phoneLocation.setVisibility(View.GONE);

        // check rating
        if (result[4] != null) {
            ratingLocation.setText("Rating : " + result[4]);
            ratingLocation.setVisibility(View.VISIBLE);
        } else
            ratingLocation.setVisibility(View.GONE);

        // check price level
        priceLevel.setVisibility(View.GONE);
        informationLocation.setVisibility(LinearLayout.VISIBLE);
        btnAddFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbManager.FAVORITE_addPlace(new PlaceObject(result[0], result[1], new LatLng(Double.parseDouble(result[2]), Double.parseDouble(result[3]))));
                Toast.makeText(MapsActivity.this, "Địa điểm đã được thêm vào danh sách", Toast.LENGTH_SHORT).show();
            }
        });
        dbManager.HISTORY_addPlace(new PlaceObject(result[0], result[1], new LatLng(Double.parseDouble(result[2]), Double.parseDouble(result[3]))));
        mMap.clear();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(result[2]), Double.parseDouble(result[3])), DEFAULT_MAP_HEIGHT));
        mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(result[2]), Double.parseDouble(result[3]))).title(result[1]));

    }

    @Override
    public void getDataSuccessful(ArrayList<Object> Countries, ArrayList<Object> Cities) {
        nameCountry = (ArrayList<String>) Countries.get(0);
        casesCountry = (ArrayList<Integer>) Countries.get(1);
        deadCountry = (ArrayList<Integer>) Countries.get(2);
        recoveredCountry = (ArrayList<Integer>) Countries.get(3);
        latCountry = (ArrayList<String>) Countries.get(4);
        lngCountry = (ArrayList<String>) Countries.get(5);
        isCheckingCorona = true;
        mMap.clear();
        final ArrayList<MarkerOptions> non_text = new ArrayList<>();
        final ArrayList<MarkerOptions> with_text = new ArrayList<>();

        View markerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.covid_area_marker, null);
        CircularTextView numTxt = markerView.findViewById(R.id.circularTextView);
        MarkerOptions marker;
        for (int i = 0; i < nameCountry.size(); ++i) {
            numTxt.setText(String.valueOf(casesCountry.get(i)));
            marker = new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(latCountry.get(i)), Double.parseDouble(lngCountry.get(i))))
                    .title(nameCountry.get(i))
                    .icon(BitmapDescriptorFactory.fromBitmap(MapsActivity.createDrawableFromView(this, markerView)));

            with_text.add(marker);

            numTxt.setText("");
            marker = new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(latCountry.get(i)), Double.parseDouble(lngCountry.get(i))))
                    .title(nameCountry.get(i))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_16))
                    .alpha(0.8f);
            non_text.add(marker);
        }

        boolean isInit = true;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), -DEFAULT_MAP_HEIGHT));
        mMap.clear();
        for(int i = 0; i < non_text.size(); ++i) {
            mMap.addMarker(non_text.get(i));
        }
        isInit = false;

        nameCity = (ArrayList<String>) Cities.get(0);
        casesCity = (ArrayList<Integer>) Cities.get(1);
        deadCity = (ArrayList<Integer>) Cities.get(2);
        recoveredCity = (ArrayList<Integer>) Cities.get(3);
        latCity = (ArrayList<String>) Cities.get(4);
        lngCity = (ArrayList<String>) Cities.get(5);

        final ArrayList<MarkerOptions> forCities = new ArrayList<>();

        for (int i = 0; i < nameCity.size(); ++i) {
            numTxt.setText(String.valueOf(casesCity.get(i)));
            marker = new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(latCity.get(i)), Double.parseDouble(lngCity.get(i))))
                    .title(nameCity.get(i))
                    .icon(BitmapDescriptorFactory.fromBitmap(MapsActivity.createDrawableFromView(this, markerView)));

            forCities.add(marker);
        }

        final boolean finalIsInit = isInit;
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            boolean changeType = false;
            @Override
            public void onCameraMove() {
                if (!finalIsInit) {
                    CameraPosition cameraPosition = mMap.getCameraPosition();
                    if(cameraPosition.zoom >= 3.5 && cameraPosition.zoom < 6 && changeType && isCheckingCorona) {
                        mMap.clear();
                        for (int i = 0; i < with_text.size(); ++i) {
                            mMap.addMarker(with_text.get(i));
                        }
                        changeType = false;
                    }
                    else if (cameraPosition.zoom < 3.5 && !changeType && isCheckingCorona) {
                        mMap.clear();
                        for (int i = 0; i < non_text.size(); ++i) {
                            mMap.addMarker(non_text.get(i));
                        }
                        changeType = true;
                    }
                    else if (cameraPosition.zoom >= 6 && !changeType && isCheckingCorona) {
                        mMap.clear();
                        for (int i = 0; i < forCities.size(); ++i) {
                            mMap.addMarker(forCities.get(i));
                        }
                        changeType = true;
                    }
                }
            }
        });

        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(isCheckingCorona) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MapsActivity.this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.show_corona_info, null);
            TextView tvName, tvCases, tvDead, tvRecovered;
            tvName = bottomSheetView.findViewById(R.id.tvNameCountry);
            tvCases = bottomSheetView.findViewById(R.id.tvCases);
            tvDead = bottomSheetView.findViewById(R.id.tvDead);
            tvRecovered = bottomSheetView.findViewById(R.id.tvRecovered);

            String Name = marker.getTitle();
            boolean isFound = false;
            for(int i = 0; i < nameCountry.size(); ++i) {
                if (Name.equals(nameCountry.get(i))) {
                    tvName.setText(nameCountry.get(i));
                    tvCases.setText("Ca nhiễm: " + casesCountry.get(i));
                    tvDead.setText("Ca tử vong: " + deadCountry.get(i));
                    tvRecovered.setText("Hồi phục: " + recoveredCountry.get(i));
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                for(int i = 0; i < nameCity.size(); ++i) {
                    if (Name.equals(nameCity.get(i))) {
                        tvName.setText(nameCity.get(i));
                        tvCases.setText("Ca nhiễm: " + casesCity.get(i));
                        tvDead.setText("Ca tử vong: " + deadCity.get(i));
                        tvRecovered.setText("Hồi phục: " + recoveredCity.get(i));
                        isFound = true;
                        break;
                    }
                }
            }
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
            return true;
        }
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (curLocationMarker != null && mMap.getCameraPosition() != null) {
            if (event.values[0] >= mMap.getCameraPosition().bearing)
                curLocationMarker.setRotation(event.values[0] - mMap.getCameraPosition().bearing);
            else curLocationMarker.setRotation(360.0f + event.values[0] - mMap.getCameraPosition().bearing);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}