package modules;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class CovidAPI {
    private final String urlTotal = "https://www.trackcorona.live/api/countries";

    private ArrayList<String> nameCountry;
    private ArrayList<Integer> cases;
    private ArrayList<Integer> dead;
    private ArrayList<Integer> recovered;
    private ArrayList<String> lat;
    private ArrayList<String> lng;

    CovidInterface main;

    public CovidAPI(CovidInterface caller) {
        nameCountry = new ArrayList<>();
        cases = new ArrayList<>();
        dead = new ArrayList<>();
        recovered = new ArrayList<>();
        lat = new ArrayList<>();
        lng = new ArrayList<>();
        main = caller;
    }

    public void execute() {
        new getData().execute(urlTotal);
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
        Integer code = jsonData.getInt("code");
        if (code == 200) {
            JSONArray statistic = jsonData.getJSONArray("data");
            for (int i = 0; i < statistic.length(); ++i) {
                JSONObject item = statistic.getJSONObject(i);
                nameCountry.add(item.getString("location"));
                lat.add(item.getString("latitude"));
                lng.add(item.getString("longitude"));
                cases.add(item.getInt("confirmed"));
                dead.add(item.getInt("dead"));
                recovered.add(item.getInt("recovered"));
            }

            main.getDataSuccessful(nameCountry, cases, dead, recovered, lat, lng);
        }
        else
            return;
    }
}
