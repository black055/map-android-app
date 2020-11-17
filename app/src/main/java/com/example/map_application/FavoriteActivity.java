package com.example.map_application;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modules.StoreData.DBManager;
import modules.LocationByName.PlaceObject;

import static android.R.layout.simple_list_item_2;

public class FavoriteActivity extends ListActivity {
    ArrayList<PlaceObject> listFav;
    List<Map<String, String>> list_favorite;
    DBManager dbManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        list_favorite = new ArrayList<>();
        dbManager = new DBManager(this);
        listFav = dbManager.FAVORITE_getAll();
        for (PlaceObject place : listFav) {
            Map<String, String> item = new HashMap<>();
            item.put("name", place.getName());
            item.put("address", place.getAddress());
            list_favorite.add(item);
        }
        setListAdapter(new SimpleAdapter(this, list_favorite, simple_list_item_2,
                new String[] {"name", "address"}, new int[] {android.R.id.text1, android.R.id.text2}));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent result = new Intent();
        result.putExtra("position", position);
        setResult(Activity.RESULT_OK, result);
        this.finish();
    }
}