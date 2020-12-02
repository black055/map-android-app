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
import android.app.ProgressDialog;
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
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.map_application.customtextview.CircularTextView;
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
import java.util.Map;

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

import static android.R.layout.simple_list_item_2;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
        , DirectionFinderListener, GetPlaceInterface, CovidInterface, GoogleMap.OnMarkerClickListener, SensorEventListener
        , GoogleMap.OnPolylineClickListener {

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

    // Component cho chức năng tìm đường đi giữa 2 địa điểm
    private EditText edtOrigin, edtDestination;
    private TextView tvDuration, tvDistance;
    private LinearLayout llFindPath;
    private List<Polyline> polylinePaths = new ArrayList<>();
    private boolean isFindingPath;
    private String travelMode;
    private ImageView ivSetOriginByCurrentPosition;
    private Button btnFindPath, btnFindFromCurrent,
            btnDrivingMode, btnWalkingMode, btnTransitMode;
    private boolean routeType, isFindFromCurrentLocation;

    // Component cho chức năng tìm địa điểm gần đây
    private PopupMenu popupMenu;

    private Location lastLocation;  // Vị trị hiện tại được định vị
    private LatLng defaultLocation; // Vị trí mặc định
    private EditText searchLocation;    // EditText tìm địa điểm
    private ImageView btnCurLocation;   // Button định vị vị trí hiện tại
    private FloatingActionButton btnShare;  // Button chia sẻ vị trí hiện tại của bản

    // Component cho chức năng chọn loại bản đồ
    FloatingActionButton btnSelectType, btnSatellite, btnTerrain, btnDefault, btnTraffic;
    private boolean selectedMaptype;
    private boolean isTrafficMode;

    // Navigation Bar
    private BottomNavigationView navigation;

    // Component hiển thị thông tin của địa điểm
    private LinearLayout informationLocation;
    private TextView nameLocation, phoneLocation, ratingLocation, addressLocation, priceLevel;
    private Button btnAddFav;
    private boolean isGoBack;   // biến check khi nhấn back button

    // Data base
    private DBManager dbManager;

    // Giao diện loading
    private LinearLayout layoutIntro;

    // Tìm kiếm địa điểm bằng giọng
    private ImageView searchByVoice;

    // Component cho chức năng theo dõi thông tin Corona
    FloatingActionButton btnCorona;
    boolean isCheckingCorona;
    ProgressDialog progressDialog;  // ProgressDialog hiển thị khi đang tải thông tin
    // Dữ liệu theo cấp Quốc gia
    ArrayList<String> nameCountry;
    ArrayList<Integer> casesCountry;
    ArrayList<Integer> deadCountry;
    ArrayList<Integer> recoveredCountry;
    ArrayList<String> latCountry;
    ArrayList<String> lngCountry;
    // Dữ liệu theo cấp Thành phố
    ArrayList<String> nameCity;
    ArrayList<Integer> casesCity;
    ArrayList<Integer> deadCity;
    ArrayList<Integer> recoveredCity;
    ArrayList<String> latCity;
    ArrayList<String> lngCity;
    HashMap<String, ArrayList<Object>> hashByCountryCode;
    ArrayList<String> countryCode;

    // Marker icons array for nearby location search
    HashMap<String, Integer> markerIcons;

    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    boolean isSetLocationListener;
    boolean moveCamera;
    boolean isHome;

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

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                lastLocation = location;
                if (curLocationMarker != null) {
                    curLocationMarker.setPosition(current);
                } else {
                    curLocationMarker = mMap.addMarker(new MarkerOptions().position(current)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                            .anchor(0.5f, 0.5f));
                }

                if (moveCamera) {
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
                                    btnTraffic.setVisibility(View.VISIBLE);
                                    if(isFindingPath) searchByVoice.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancel() {

                                }
                            });
                    moveCamera = false;
                }
            }
        };

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

        isSetLocationListener = false;
        moveCamera = true;

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
        //route type mặc định là false, nghĩa là tuyến đang chọn là tuyến ngắn nhất
        //nếu = 1 thì tuyến chọn là tuyến dài hơn
        routeType = false;
        isFindFromCurrentLocation = false;

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
        btnTraffic = findViewById(R.id.btnTraffic);

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
        hashByCountryCode = new HashMap<>();
        ArrayList<String> countryCode = new ArrayList<>();

        // ẩn ban đầu cho một số view
        selectedMaptype = false;
        isTrafficMode = false;
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
        btnTraffic.setVisibility(View.GONE);
        isGoBack = false;
        isHome = false;

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

                    if (isFindingPath)
                        searchByVoice.setVisibility(View.VISIBLE);

                    if (isCheckingCorona)
                        isCheckingCorona = false;

                    if (!isHome) {
                        if (mMap != null) {
                        mMap.clear();
                        if (lastLocation != null) {
                            curLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                                    .anchor(0.5f, 0.5f));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), DEFAULT_MAP_HEIGHT));
                        } else {
                            curLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(defaultLocation.latitude, defaultLocation.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                                    .anchor(0.5f, 0.5f));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_MAP_HEIGHT));
                        }
                        }
                        isHome = true;
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
                    isHome = false;
                    //Hiện thanh tìm kiếm 2 địa điểm
                    llFindPath.setVisibility(View.VISIBLE);
                    //Ẩn thanh search location
                    searchLocation.setVisibility(View.GONE);
                    searchByVoice.setVisibility(View.GONE);
                    isFindingPath = true;

                    if (isCheckingCorona) {
                        mMap.clear();
                        isCheckingCorona = false;
                    }

                    //Đưa button select map type xuống
                    btnSelectType.setTranslationY(llFindPath.getHeight());
                    btnDefault.setTranslationY(llFindPath.getHeight());
                    btnSatellite.setTranslationY(llFindPath.getHeight());
                    btnTerrain.setTranslationY(llFindPath.getHeight());
                    informationLocation.setVisibility(LinearLayout.GONE);
                    searchLocation.setText("");
                    return true;
                case R.id.place:
                    isHome = false;
                    searchLocation.setVisibility(View.VISIBLE);
                    searchByVoice.setVisibility(View.VISIBLE);
                    llFindPath.setVisibility(View.GONE);
                    if (isCheckingCorona) {
                        mMap.clear();
                        isCheckingCorona = false;
                    }
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
                    isHome = false;
                    if (isCheckingCorona) {
                        mMap.clear();
                        getCurrentLocation();
                        isCheckingCorona = false;
                    }
                    isFindingPath = false;
                    Intent intent_favorite = new Intent(MapsActivity.this, FavoriteActivity.class);
                    startActivityForResult(intent_favorite, RQCODE_FROM_FAVORITE);
                    searchLocation.setText("");
                    return true;
                case R.id.history:
                    isHome = false;
                    isFindingPath = false;
                    if (isCheckingCorona) {
                        mMap.clear();
                        getCurrentLocation();
                        isCheckingCorona = false;
                    }
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
        isHome = true;
        mMap.setOnPolylineClickListener(this);
        mMap.setOnMarkerClickListener(this);
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

                        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(MapsActivity.this, "Vui lòng mở dịch vụ GPS!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isSetLocationListener) {
            // Kiểm tra xem có vị trí hiện tại sẵn không?
            lastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                
                curLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                            .anchor(0.5f, 0.5f));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), DEFAULT_MAP_HEIGHT),
                        new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                navigation.setVisibility(View.VISIBLE);
                                btnShare.setVisibility(View.VISIBLE);
                                btnSelectType.setVisibility(View.VISIBLE);
                                layoutIntro.setVisibility(View.GONE);
                                searchByVoice.setVisibility(View.VISIBLE);
                                btnCorona.setVisibility(View.VISIBLE);
                                btnTraffic.setVisibility(View.VISIBLE);
                                if(isFindingPath) searchByVoice.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                moveCamera = false;
            }

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , 1000, 0, mLocationListener);
            isSetLocationListener = true;
        } else if (lastLocation != null) {
            if (curLocationMarker == null) {
                curLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                        .anchor(0.5f, 0.5f));
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), DEFAULT_MAP_HEIGHT));
        }
    }

    private void setActionListener() {

        btnCurLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (informationLocation.getVisibility() == LinearLayout.VISIBLE) {
                    informationLocation.setVisibility(LinearLayout.GONE);
                }
                getCurrentLocation();
                searchByVoice.setVisibility(View.VISIBLE);
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
                                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    Toast.makeText(MapsActivity.this, "Vui lòng mở dịch vụ GPS!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (!isSetLocationListener) {
                                    getCurrentLocation();
                                    // Đợi lấy xong vị trí của thiết bị và di chuyển camera đến đây
                                    while (moveCamera == true);
                                }
                                if (lastLocation != null) {
                                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                                    sharingIntent.setType("text/plain");
                                    sharingIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com?q="
                                            + lastLocation.getLatitude() + "," + lastLocation.getLongitude());
                                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
                                }
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(getApplicationContext(), "Yêu cầu cấp quyền truy cập vị trí!", Toast.LENGTH_SHORT).show();
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
                routeType = false;
                if (edtOrigin.getText().toString().equals("Vị trí hiện tại"))
                    isFindFromCurrentLocation = true;
                sendRequest(false);
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
                                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    Toast.makeText(MapsActivity.this, "Vui lòng mở dịch vụ GPS!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (!isSetLocationListener) {
                                    getCurrentLocation();
                                    // Đợi lấy xong vị trí của thiết bị và di chuyển camera đến đây
                                    while (moveCamera == true);
                                }
                                if (lastLocation != null) {
                                    String origin = "Vị trí hiện tại";
                                    String destination = searchLocation.getText().toString();
                                    edtOrigin.setText(origin);
                                    edtDestination.setText(destination);
                                    isFindFromCurrentLocation = true;
                                    btnFindPath.performClick();
                                }
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(getApplicationContext(), "Yêu cầu cấp quyền truy cập vị trí!", Toast.LENGTH_SHORT).show();
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
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(MapsActivity.this, "Vui lòng mở dịch vụ GPS!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (!isSetLocationListener) {
                    getCurrentLocation();
                    // Đợi lấy xong vị trí của thiết bị và di chuyển camera đến đây
                    while (moveCamera == true);
                }
                if (lastLocation != null) {
                    String type = "";
                    LatLng current = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.clear();
                    curLocationMarker = mMap.addMarker(new MarkerOptions().position(current)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                            .anchor(0.5f, 0.5f));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), DEFAULT_MAP_HEIGHT));
                    
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
                        Toast.makeText(MapsActivity.this, "Please turn on the Internet!", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MapsActivity.this, "Không thể xác định vị trí của thiết bị!", Toast.LENGTH_SHORT).show();
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

        btnTraffic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTrafficMode) {
                    mMap.setTrafficEnabled(true);
                    btnTraffic.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_traffic_24));
                } else {
                    mMap.setTrafficEnabled(false);
                    btnTraffic.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_outline_traffic_24));
                }

                isTrafficMode = !isTrafficMode;
            }
        });

        btnCorona.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() != NetworkInfo.State.CONNECTED &&
                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() != NetworkInfo.State.CONNECTED) {
                    Toast.makeText(MapsActivity.this, "Please turn on the Internet!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    progressDialog = new ProgressDialog(MapsActivity.this);
                    progressDialog.show();
                    progressDialog.setContentView(R.layout.progress_dialog);
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
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
                if (!edtOrigin.getText().toString().isEmpty() && !edtDestination.getText().toString().isEmpty()) {
                    btnFindPath.performClick();
                }
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
                if (!edtOrigin.getText().toString().isEmpty() && !edtDestination.getText().toString().isEmpty()) {
                    btnFindPath.performClick();
                }
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
                if (!edtOrigin.getText().toString().isEmpty() && !edtDestination.getText().toString().isEmpty()) {
                    btnFindPath.performClick();
                }
            }
        });

        //Hiển thị tọa độ vị trí hiện tại ở ô edit text origin
        ivSetOriginByCurrentPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(MapsActivity.this, "Vui lòng mở dịch vụ GPS!", Toast.LENGTH_SHORT).show();
                    return;
                }
                edtOrigin.setText("Vị trí hiện tại");
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
    public void onDirectionFinderSuccess(List<Route> routes, boolean isFindingSubRoute) {
        //Cấp phát cho polyline (tập hợp các điểm trên đường đi)
        polylinePaths = new ArrayList<>();

        // Không tìm được routes từ google api cấp
        if (routes.size() == 0) {
            tvDuration.setText("---");
            tvDistance.setText("---");
            Toast.makeText(this, "Không tìm được đường đi!", Toast.LENGTH_SHORT).show();
            return;
        }

        //Xóa các marker trên bản đồ
        mMap.clear();
        //Chuyển camera tới vị trí bắt đầu
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routes.get(0).startLocation, 15));

        //Lấy route chính và phụ trong mảng
        //Đường ngắn nhất là đường chính là route 0 trong routes. Đường phụ dài hơn là route 1
        Route route = !isFindingSubRoute ? routes.get(0) : routes.get(1);

        //Ghi thời gian, khoảng cách vào 2 ô textView duration, distance
        tvDuration.setText(route.duration.text);
        tvDistance.setText(route.distance.text);

        //add marker điểm đầu, điểm cuối lên mMap
        if (isFindFromCurrentLocation){
            curLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                    .anchor(0.5f, 0.5f));
            isFindFromCurrentLocation = false;
        }
        else {
            mMap.addMarker(new MarkerOptions()
                    .icon(bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_position_start))
                    .title(route.startAddress)
                    .position(route.startLocation));
        }
        mMap.addMarker(new MarkerOptions()
                .title(route.endAddress)
                .position(route.endLocation));

        //Xử lý nếu có tuyến phụ
        if (routes.size() > 1) {
            Route subRoute = isFindingSubRoute ? routes.get(0) : routes.get(1);
            //Vẽ các tuyến phụ
            PolylineOptions subPolylineOptions = new PolylineOptions().
                    geodesic(true).
                    clickable(true).
                    color(ContextCompat.getColor(getApplicationContext(), R.color.subRoute)).
                    width(15);
            //Thêm các điểm vào subPolylineOptions
            for (int i = 0; i < subRoute.points.size(); i++)
                subPolylineOptions.add(subRoute.points.get(i));
            //vẽ đường đi giữa 2 điểm
            polylinePaths.add(mMap.addPolyline(subPolylineOptions));
        }
        
        //Tạo polyline để vẽ tuyến chính
        PolylineOptions polylineOptions = new PolylineOptions().
                geodesic(true).
                clickable(true).
                color(ContextCompat.getColor(getApplicationContext(), R.color.colorRoadRoute)).
                width(15);

        //Thêm các điểm vào polylineOptions
        for (int i = 0; i < route.points.size(); i++)
            polylineOptions.add(route.points.get(i));
        //vẽ đường đi giữa 2 điểm
        polylinePaths.add(mMap.addPolyline(polylineOptions));
    }

    private void sendRequest(boolean subRoute) {
        String origin = edtOrigin.getText().toString();
        String destination = edtDestination.getText().toString();
        if (origin.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa điểm xuất phát!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa điểm cần đến!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (isFindFromCurrentLocation) {
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(this, "Vui lòng mở dịch vụ GPS!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isSetLocationListener) {
                    getCurrentLocation();
                    while (moveCamera);
                }
                if (lastLocation != null)   origin = lastLocation.getLatitude() + ", " + lastLocation.getLongitude();
            }
            // Tìm đường dựa vào address điểm đầu, cuối
            new DirectionFinder(this, origin, destination, travelMode, subRoute).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    //-----------------------------------------------------------

    private void showPlaceInformation(@NotNull Place place) {
        searchLocation.setText(place.getAddress());
        nameLocation.setText(place.getName());
        addressLocation.setText(place.getAddress());
        // Hiển thị thông tin số điện thoại của địa
        if (place.getPhoneNumber() != null) {
            phoneLocation.setText("Phone number: " + place.getPhoneNumber());
        } else
            phoneLocation.setVisibility(View.GONE);

        // Hiển thị thông tin về rating
        if (place.getRating() != null) {
            ratingLocation.setText("Rating : " + place.getRating());
        } else
            ratingLocation.setVisibility(View.GONE);


        // Hiển thị thông tin về price
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

            // Button thêm địa điểm vào
            btnAddFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbManager.FAVORITE_addPlace(new PlaceObject(place.getName(), place.getAddress(), place.getLatLng()));
                    Toast.makeText(MapsActivity.this, "Địa điểm đã được thêm vào danh sách", Toast.LENGTH_SHORT).show();
                }
            });

            // Thêm địa điểm đã tìm kiếm vào lịch sử
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
            phoneLocation.setVisibility(View.GONE);
            ratingLocation.setVisibility(View.GONE);
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
            phoneLocation.setVisibility(View.GONE);
            ratingLocation.setVisibility(View.GONE);
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
        phoneLocation.setVisibility(View.GONE);
        if (result[4] != null) {
            ratingLocation.setText("Rating : " + result[4]);
            ratingLocation.setVisibility(View.VISIBLE);
        } else
            ratingLocation.setVisibility(View.GONE);
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
        countryCode = (ArrayList<String>) Countries.get(6);
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

            int num_case = casesCountry.get(i);
            marker = new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(latCountry.get(i)), Double.parseDouble(lngCountry.get(i))))
                    .title(nameCountry.get(i))
                    .alpha(0.8f);
            if (num_case > 5000000) {
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_36));
                non_text.add(marker);
            }
            else if (num_case > 2000000) {
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_32));
                non_text.add(marker);
            }
            else if (num_case > 1000000) {
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_28));
                non_text.add(marker);
            }
            else if (num_case > 500000) {
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_24));
                non_text.add(marker);
            }
            else if (num_case > 200000) {
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_20));
                non_text.add(marker);
            }
            else {
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_16));
                non_text.add(marker);
            }
        }

        boolean isInit = true;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 3.0f));
        mMap.clear();
        for(int i = 0; i < non_text.size(); ++i) {
            mMap.addMarker(non_text.get(i));
        }
        isInit = false;

        hashByCountryCode = new HashMap<>();
        nameCity = (ArrayList<String>) Cities.get(0);
        casesCity = (ArrayList<Integer>) Cities.get(1);
        deadCity = (ArrayList<Integer>) Cities.get(2);
        recoveredCity = (ArrayList<Integer>) Cities.get(3);
        latCity = (ArrayList<String>) Cities.get(4);
        lngCity = (ArrayList<String>) Cities.get(5);
        ArrayList<String> code_country = (ArrayList<String>) Cities.get(6);

        final ArrayList<MarkerOptions> forCities = new ArrayList<>();

        for (int i = 0; i < countryCode.size(); ++i) {
            String code = countryCode.get(i);
            ArrayList<Object> temp = hashByCountryCode.get(code);
            if (temp != null) {
                for(int j = 0; j < nameCity.size(); ++j) {
                    if (code_country.get(j).equals(code)) {
                        ((ArrayList<String>) (hashByCountryCode.get(code)).get(0)).add(nameCity.get(j));
                        ((ArrayList<Integer>) (hashByCountryCode.get(code)).get(1)).add(casesCity.get(j));
                        ((ArrayList<Integer>) (hashByCountryCode.get(code)).get(2)).add(deadCity.get(j));
                        ((ArrayList<Integer>) (hashByCountryCode.get(code)).get(3)).add(recoveredCity.get(j));
                    }
                }
            }
            else {
                ArrayList<Object> arr = new ArrayList<>();
                ArrayList<String> name = new ArrayList<>();
                ArrayList<Integer> cases = new ArrayList<>();
                ArrayList<Integer> dead = new ArrayList<>();
                ArrayList<Integer> recovered = new ArrayList<>();
                for(int j = 0; j < nameCity.size(); ++j) {
                    if (code_country.get(j).equals(code)) {
                        name.add(nameCity.get(j));
                        cases.add(casesCity.get(j));
                        dead.add(deadCity.get(j));
                        recovered.add(recoveredCity.get(j));
                    }
                }

                arr.add(name.clone());
                arr.add(cases.clone());
                arr.add(dead.clone());
                arr.add(recovered.clone());
                hashByCountryCode.put(code, (ArrayList<Object>) arr.clone());
            }
        }

        progressDialog.dismiss();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

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
                }
            }
        });
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
            ListView list_city = bottomSheetView.findViewById(R.id.list_city);
            list_city.setVisibility(View.VISIBLE);
            String Name = marker.getTitle();
            for(int i = 0; i < nameCountry.size(); ++i) {
                if (Name.equals(nameCountry.get(i))) {
                    tvName.setText(nameCountry.get(i));
                    tvCases.setText("Ca nhiễm: " + casesCountry.get(i));
                    tvDead.setText("Tử vong: " + deadCountry.get(i));
                    tvRecovered.setText("Hồi phục: " + recoveredCountry.get(i));
                    String code = countryCode.get(i);
                    ArrayList<Object> info = hashByCountryCode.get(code);
                    Object[] nameObj = (Object[]) ((ArrayList<String>) info.get(0)).toArray();
                    if (nameObj.length == 0) {
                        list_city.setVisibility(View.GONE);
                        break;
                    }
                    Object[] casesObj = (Object[]) ((ArrayList<Integer>) info.get(1)).toArray();
                    Object[] deadObj = (Object[]) ((ArrayList<Integer>) info.get(2)).toArray();
                    Object[] recoveredObj = (Object[]) ((ArrayList<Integer>) info.get(3)).toArray();

                    String[] name = Arrays.copyOf(nameObj, nameObj.length, String[].class);
                    Integer[] cases = Arrays.copyOf(casesObj, casesObj.length, Integer[].class);
                    Integer[] dead = Arrays.copyOf(deadObj, deadObj.length, Integer[].class);
                    Integer[] recovered = Arrays.copyOf(recoveredObj, recoveredObj.length, Integer[].class);

                    List<Map<String, String>> list_item = new ArrayList<>();
                    for (int j = 0; j < name.length; ++j) {
                        Map<String, String> item = new HashMap<>();
                        item.put("name", name[j]);
                        item.put("info", "Ca nhiễm: " + cases[j] + " , Tử vong: " + dead[j] + " , Phục hồi: " + recovered[j]);
                        list_item.add(item);
                    }
                    list_city.setAdapter(new SimpleAdapter(this, list_item, simple_list_item_2,
                            new String[] {"name", "info"}, new int[] {android.R.id.text1, android.R.id.text2}));
                    break;
                }
            }
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
            return true;
        }
        return false;
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

    @Override
    public void onPolylineClick(Polyline polyline) {
        //Kiểm tra có phải "tuyến phụ"
        if (polyline.getColor() == ContextCompat.getColor(getApplicationContext(), R.color.subRoute)) {
            //Nếu tuyến đang chọn là tuyến dài hơn thì đổi tuyến user chọn sang tuyến ngắn hơn
            if (routeType) {
                routeType = false;
                if (edtOrigin.getText().toString().equals("Vị trí hiện tại"))
                    isFindFromCurrentLocation = true;
                sendRequest(false);
            }
            else {
                //Tuyến chọn hiện tại là tuyến ngắn hơn. User cần đổi sang tuyến dài hơn
                routeType = true;
                if (edtOrigin.getText().toString().equals("Vị trí hiện tại"))
                    isFindFromCurrentLocation = true;
                sendRequest(true);
            }
        }
    }
}