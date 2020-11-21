package modules.Covid;

import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;


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
    private final String urlCountries = "https://www.trackcorona.live/api/countries";
    private final String urlCities = "https://www.trackcorona.live/api/cities";
    private ArrayList<String> nameCountry;
    private ArrayList<Integer> casesCountry;
    private ArrayList<Integer> deadCountry;
    private ArrayList<Integer> recoveredCountry;
    private ArrayList<String> latCountry;
    private ArrayList<String> lngCountry;

    private ArrayList<Object> Countries;
    private ArrayList<Object> Cities;

    private ArrayList<String> nameCity;
    private ArrayList<Integer> casesCity;
    private ArrayList<Integer> deadCity;
    private ArrayList<Integer> recoveredCity;
    private ArrayList<String> latCity;
    private ArrayList<String> lngCity;

    CovidInterface main;

    public CovidAPI(CovidInterface caller) {
        nameCountry = new ArrayList<>();
        casesCountry = new ArrayList<>();
        deadCountry = new ArrayList<>();
        recoveredCountry = new ArrayList<>();
        latCountry = new ArrayList<>();
        lngCountry = new ArrayList<>();

        nameCity = new ArrayList<>();
        casesCity = new ArrayList<>();
        deadCity = new ArrayList<>();
        recoveredCity = new ArrayList<>();
        latCity = new ArrayList<>();
        lngCity = new ArrayList<>();

        main = caller;
        Countries = new ArrayList<>();
        Cities = new ArrayList<>();
    }

    public void execute() {
        new getData().execute(urlCountries, urlCities);
    }

    private class getData extends AsyncTask<String, Void, String[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String[] doInBackground(String... params) {
            String linkCountries = params[0];
            String linkCities = params[1];

            String dataCountries;
            String dataCities;
            try {

                URL urlCountries = new URL(linkCountries);
                InputStream isCountries = urlCountries.openConnection().getInputStream();

                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(isCountries));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                dataCountries = buffer.toString();

                URL urlCities = new URL(linkCities);
                InputStream isCities = urlCities.openConnection().getInputStream();

                buffer = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(isCities));

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                dataCities = buffer.toString();
                return new String[] {dataCountries, dataCities};

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(String[] res) {
            try {
                parseJSon(res[0], res[1]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void parseJSon(String dataCountries, String dataCities) throws JSONException {
        if (dataCountries == null || dataCities == null)
            return;
        JSONObject jsonDataCountries = new JSONObject(dataCountries);
        int code = jsonDataCountries.getInt("code");
        if (code == 200) {
            JSONArray statistic = jsonDataCountries.getJSONArray("data");
            for (int i = 0; i < statistic.length(); ++i) {
                JSONObject item = statistic.getJSONObject(i);
                nameCountry.add(item.getString("location"));
                latCountry.add(item.getString("latitude"));
                lngCountry.add(item.getString("longitude"));
                casesCountry.add(item.getInt("confirmed"));
                deadCountry.add(item.getInt("dead"));
                recoveredCountry.add(item.getInt("recovered"));
            }
            Countries.add(nameCountry);
            Countries.add(casesCountry);
            Countries.add(deadCountry);
            Countries.add(recoveredCountry);
            Countries.add(latCountry);
            Countries.add(lngCountry);
        }
        code = 0;

        JSONObject jsonDataCities = new JSONObject(dataCities);
        code = jsonDataCities.getInt("code");
        if (code == 200) {
            JSONArray statistic = jsonDataCities.getJSONArray("data");
            for (int i = 0; i < statistic.length(); ++i) {
                JSONObject item = statistic.getJSONObject(i);
                nameCity.add(item.getString("location"));
                latCity.add(item.getString("latitude"));
                lngCity.add(item.getString("longitude"));
                casesCity.add(item.getInt("confirmed"));


                if (item.get("dead").getClass() == Integer.class) {
                    deadCity.add(item.getInt("dead"));
                }
                else deadCity.add(0);

                if (item.get("recovered").getClass() == Integer.class) {
                    recoveredCity.add(item.getInt("recovered"));
                }
                else recoveredCity.add(0);
            }
            Cities.add(nameCity);
            Cities.add(casesCity);
            Cities.add(deadCity);
            Cities.add(recoveredCity);
            Cities.add(latCity);
            Cities.add(lngCity);
        }
        main.getDataSuccessful(Countries, Cities);
    }
}