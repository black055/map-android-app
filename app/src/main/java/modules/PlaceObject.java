package modules;

import com.google.android.gms.maps.model.LatLng;

public class PlaceObject {
    private String name;
    private String address;
    LatLng latlong;
    private int ID;

    public PlaceObject(int ID, String name, String adress, LatLng latlong) {
        this.name = name;
        this.address = adress;
        this.latlong = latlong;
        this.ID = ID;
    }

    public PlaceObject(String name, String address, LatLng latlong) {
        this.name = name;
        this.address = address;
        this.latlong = latlong;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String adress) {
        this.address = adress;
    }

    public LatLng getLatlong() {
        return latlong;
    }

    public void setLatlong(LatLng latlong) {
        this.latlong = latlong;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }
}
