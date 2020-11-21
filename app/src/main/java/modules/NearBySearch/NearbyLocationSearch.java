package modules.NearBySearch;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

import com.example.map_application.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class NearbyLocationSearch extends AsyncTask<Object, String, String> {
    private final int RADIUS = 2000;

    private Context context;
    private double lat, lng;
    private String type;
    private BitmapDescriptor markerIcon;

    private String googlePlacesData;
    private GoogleMap mMap;

    public NearbyLocationSearch(Context context, double lat, double lng, String type, BitmapDescriptor markerIcon) {
        this.context = context;
        this.lat = lat;
        this.lng = lng;
        this.type = type;
        this.markerIcon = markerIcon;
    }

    String getURL(double lat, double lng, String type) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                lat + "," + lng + "&radius=" + RADIUS
                + "&type=" + type + "&key=" + context.getResources().getString(R.string.google_maps_key);
        return url;
    }

    private String downloadURL(String string) throws IOException {
        URL url = new URL(string);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = "";
        while ((line = br.readLine()) != null) {
            builder.append(line);
        }
        br.close();
        return builder.toString();
    }

    @Override
    protected String doInBackground(Object... objects) {
        mMap = (GoogleMap) objects[0];
        try {
            googlePlacesData = downloadURL(getURL(lat, lng, type));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return googlePlacesData;
    }

    @Override
    protected void onPostExecute(String s) {
        List<HashMap<String, String>> nearbyPlacesList = null;
        DataParser dataParser = new DataParser();
        try {
            nearbyPlacesList = dataParser.parse(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        showNearbyPlaceOnMap(nearbyPlacesList);
    }

    private void showNearbyPlaceOnMap(List<HashMap<String, String>> places) {
        mMap.addCircle(new CircleOptions()
                .center(new LatLng(lat, lng)).radius(RADIUS)
                .strokeWidth(0)
                .strokeColor(Color.parseColor("#225595EC"))
                .fillColor(Color.parseColor("#225595EC")));

        for (int i = 0; i < places.size(); i++) {
            HashMap<String, String> place = places.get(i);
            String name = place.get("name"), vicinity = place.get("vicinity");
            double lat = Double.parseDouble(place.get("lat")), lng = Double.parseDouble(place.get("lng"));

            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))
                                                .title(name + " : " + vicinity)
                                                .icon(markerIcon));
        }
    }
}
