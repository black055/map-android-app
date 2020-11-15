package modules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataParser {
    public List<HashMap<String, String>> parse(String data) throws JSONException {
        List<HashMap<String, String>> result = new ArrayList<>();

        JSONArray array = null;
        JSONObject object;

        try {
            object = new JSONObject((String) data);
            array = object.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < array.length(); i++) {
            HashMap<String, String> place = new HashMap<String, String>();
            JSONObject jsonPlace = array.getJSONObject(i);
            // Nếu có name thì thêm vào, không thì thêm vào N/A
            if (!jsonPlace.isNull("name"))
                place.put("name", jsonPlace.getString("name"));
            else place.put("name", "N/A");
            // Nếu có vicinity thì thêm vào, không thì thêm vào N/A
            if (!jsonPlace.isNull("vicinity"))
                place.put("vicinity", jsonPlace.getString("vicinity"));
            else place.put("vicinity", "N/A");
            // Lấy thông tin vị trí
            place.put("lat", jsonPlace.getJSONObject("geometry").getJSONObject("location").getString("lat"));
            place.put("lng", jsonPlace.getJSONObject("geometry").getJSONObject("location").getString("lng"));

            result.add(place);
        }

        return result;
    }
}
