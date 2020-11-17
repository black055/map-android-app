package modules.LocationByName;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class GetPlaceFromText {
    private static final String PLACE_URL_API = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input=";
    private static final String GOOGLE_API_KEY = "AIzaSyBBvvBoeUtjWBb9jnXyxYkJRfuF_PdCCYM";
    private String address;
    private String name;
    private String rating;
    private String lat;
    private String lng;
    private String[] arrInfor;

    private GetPlaceInterface map;

    private String nameInput;

    public GetPlaceFromText(GetPlaceInterface caller, String input) {
        this.nameInput = input;
        map = caller;
        address = "";
        name = "";
        rating = null;
        lat = "";
        lng = "";
    }

    public void execute() {
        new getData().execute(PLACE_URL_API + nameInput +
                "&inputtype=textquery&fields=name,geometry,formatted_address,rating&key=" + GOOGLE_API_KEY);
    }

    private class getData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                parseJSon(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseJSon(String data) throws JSONException {
        if (data == null)
            return;
        JSONObject jsonData = new JSONObject(data);
        JSONArray candidates = jsonData.getJSONArray("candidates");
        if(candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            this.address = candidate.getString("formatted_address");
            this.name = candidate.getString("name");
            if (candidate.has("rating"))
            {
                this.rating = candidate.getString("rating");
            }
            JSONObject geometry = candidate.getJSONObject("geometry");
            JSONObject location = geometry.getJSONObject("location");
            this.lat = location.getString("lat");
            this.lng = location.getString("lng");
        }
        arrInfor = new String[]{name, address, lat, lng, rating};
        map.getPlaceSuccess(arrInfor);
    }
}
