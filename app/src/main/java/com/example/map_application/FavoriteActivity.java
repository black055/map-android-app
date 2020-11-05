package com.example.map_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modules.DBManager;
import modules.PlaceObject;

import static android.R.layout.simple_list_item_2;

public class FavoriteActivity extends ListActivity {
    ArrayList<String> name;
    ArrayList<PlaceObject> listFav;
    List<Map<String, String>> list2item;
    DBManager dbManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        name = new ArrayList<String>();
        list2item = new ArrayList<>();
        dbManager = new DBManager(this);
        listFav = dbManager.getAll();
        for (PlaceObject place : listFav) {
            Map<String, String> item = new HashMap<>();
            item.put("name", place.getName());
            item.put("address", place.getAddress());
            list2item.add(item);
        }
        setListAdapter(new SimpleAdapter(this, list2item, simple_list_item_2,
                new String[] {"name", "address"}, new int[] {android.R.id.text1, android.R.id.text2}));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        //Toast.makeText(this, "Item" + position +"selected", Toast.LENGTH_SHORT).show();
    }
}