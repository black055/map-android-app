package modules;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class DBManager extends SQLiteOpenHelper {

    public static final String DB_NAME = "place_list";
    private static final String TABLE_FAVORITE_PLACE = "table_favorite_place";
    private static final String TABLE_HISTORY_PLACE = "table_history_place";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String ADDRESS = "address";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";

    private Context context;

    public DBManager(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;
    }

    public DBManager(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version, @Nullable DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
        this.context = context;
    }

    public DBManager(Context context) {
        super(context, DB_NAME, null, 1);
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlQuery = "CREATE TABLE "+ TABLE_FAVORITE_PLACE +" (" +
                ID +" integer primary key, "+
                NAME + " TEXT, "+
                ADDRESS +" TEXT, "+
                LATITUDE+" REAL," +
                LONGITUDE +" REAL)";
        db.execSQL(sqlQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_FAVORITE_PLACE);
        onCreate(db);
    }

    public void addPlaceFavorite(PlaceObject place) {
        if (isDuplicated(place))
            return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NAME, place.getName());
        values.put(ADDRESS, place.getAddress());
        values.put(LATITUDE, place.getLatlong().latitude);
        values.put(LONGITUDE, place.getLatlong().longitude);

        db.insert(TABLE_FAVORITE_PLACE, null, values);
        db.close();
    }

    public PlaceObject getPlaceFavorite(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITE_PLACE, new String[] {ID, NAME, ADDRESS, LATITUDE, LONGITUDE}
                , ID+"=?", new String[] {String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        PlaceObject place = new PlaceObject(cursor.getString(1),cursor.getString(2),
                new LatLng(cursor.getDouble(3), cursor.getDouble(4)));
        cursor.close();
        db.close();
        return place;
    }

    public int getPlaceFavoriteCount() {
        String countQuery = "SELECT  * FROM " + TABLE_FAVORITE_PLACE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
        return cursor.getCount();
    }

    public ArrayList<PlaceObject> getAll() {
        ArrayList<PlaceObject> arr = new ArrayList<PlaceObject>();
        String selectQuery = "SELECT  * FROM " + TABLE_FAVORITE_PLACE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                PlaceObject place = new PlaceObject(cursor.getInt(0),cursor.getString(1)
                        , cursor.getString(2), new LatLng(cursor.getDouble(3), cursor.getDouble(4)));
                arr.add(place);
            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return arr;
    }

    public boolean isDuplicated (PlaceObject place) {
        ArrayList<PlaceObject> arr = new ArrayList<PlaceObject>();
        arr = getAll();
        for (PlaceObject pl : arr) {
            if (pl.getLatlong().latitude == place.getLatlong().latitude &&
            pl.getLatlong().longitude == place.getLatlong().longitude)
                return true;
        }
        return false;
    }
}
