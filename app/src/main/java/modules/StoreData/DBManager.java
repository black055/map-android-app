package modules.StoreData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import modules.LocationByName.PlaceObject;

public class DBManager extends SQLiteOpenHelper {

    // database name
    public static final String DB_NAME = "place_list";

    // table name
    private static final String TABLE_FAVORITE_PLACE = "table_favorite_place";
    private static final String TABLE_HISTORY_PLACE = "table_history_place";

    // property name
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


    // create database nếu chưa có
    @Override
    public void onCreate(SQLiteDatabase db) {
        String FAVORITE_sqlCreate = "CREATE TABLE "+ TABLE_FAVORITE_PLACE +" (" +
                ID +" integer primary key, "+
                NAME + " TEXT, "+
                ADDRESS +" TEXT, "+
                LATITUDE+" REAL," +
                LONGITUDE +" REAL)";

        String HISTORY_sqlCreate = "CREATE TABLE "+ TABLE_HISTORY_PLACE +" (" +
                ID +" integer primary key, "+
                NAME + " TEXT, "+
                ADDRESS +" TEXT, "+
                LATITUDE+" REAL," +
                LONGITUDE +" REAL)";
        db.execSQL(FAVORITE_sqlCreate);
        db.execSQL(HISTORY_sqlCreate);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_FAVORITE_PLACE);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_HISTORY_PLACE);
        onCreate(db);
    }

    // thêm 1 địa điểm vào bản dữ liệu
    public void FAVORITE_addPlace(PlaceObject place) {
        if (FAVORITE_isDuplicated(place))
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

    public void HISTORY_addPlace(PlaceObject place) {
        if (HISTORY_isDuplicated(place))
            return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NAME, place.getName());
        values.put(ADDRESS, place.getAddress());
        values.put(LATITUDE, place.getLatlong().latitude);
        values.put(LONGITUDE, place.getLatlong().longitude);

        db.insert(TABLE_HISTORY_PLACE, null, values);
        db.close();
    }

    // Lấy ra một địa điểm cụ thể nào đó thông qua id
    public PlaceObject FAVORITE_getPlace(int id) {
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

    public PlaceObject HISTORY_getPlace(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY_PLACE, new String[] {ID, NAME, ADDRESS, LATITUDE, LONGITUDE}
                , ID+"=?", new String[] {String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        PlaceObject place = new PlaceObject(cursor.getString(1),cursor.getString(2),
                new LatLng(cursor.getDouble(3), cursor.getDouble(4)));
        cursor.close();
        db.close();
        return place;
    }

    // Lấy số lượng địa điểm từ bản dữ liệu
    public int FAVORITE_getPlaceCount() {
        String countQuery = "SELECT  * FROM " + TABLE_FAVORITE_PLACE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
        return cursor.getCount();
    }

    public int HISTORY_getPlaceCount() {
        String countQuery = "SELECT  * FROM " + TABLE_HISTORY_PLACE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
        return cursor.getCount();
    }

    // Lấy ra toàn bộ địa điểm đã lưu trong bảng dữ liệu
    public ArrayList<PlaceObject> FAVORITE_getAll() {
        ArrayList<PlaceObject> arr = new ArrayList<PlaceObject>();
        String selectQuery = "SELECT  * FROM " + TABLE_FAVORITE_PLACE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                PlaceObject place = new PlaceObject(cursor.getInt(0),cursor.getString(1)
                        , cursor.getString(2), new LatLng(cursor.getDouble(3)
                        , cursor.getDouble(4)));
                arr.add(place);
            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return arr;
    }

    public ArrayList<PlaceObject> HISTORY_getAll() {
        ArrayList<PlaceObject> arr = new ArrayList<PlaceObject>();
        String selectQuery = "SELECT  * FROM " + TABLE_HISTORY_PLACE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                PlaceObject place = new PlaceObject(cursor.getInt(0),cursor.getString(1)
                        , cursor.getString(2), new LatLng(cursor.getDouble(3)
                        , cursor.getDouble(4)));
                arr.add(place);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return arr;
    }


    // Kiểm tra 1 địa điểm đã tồn tại trong csdl hay chưa
    public boolean FAVORITE_isDuplicated (PlaceObject place) {
        ArrayList<PlaceObject> arr = new ArrayList<PlaceObject>();
        arr = FAVORITE_getAll();
        for (PlaceObject pl : arr) {
            if (pl.getLatlong().latitude == place.getLatlong().latitude &&
            pl.getLatlong().longitude == place.getLatlong().longitude)
                return true;
        }
        return false;
    }

    public boolean HISTORY_isDuplicated (PlaceObject place) {
        ArrayList<PlaceObject> arr = new ArrayList<PlaceObject>();
        arr = HISTORY_getAll();
        for (PlaceObject pl : arr) {
            if (pl.getLatlong().latitude == place.getLatlong().latitude &&
                    pl.getLatlong().longitude == place.getLatlong().longitude)
                return true;
        }
        return false;
    }
}
