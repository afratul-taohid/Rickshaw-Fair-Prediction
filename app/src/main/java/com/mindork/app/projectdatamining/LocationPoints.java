package com.mindork.app.projectdatamining;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by IMRAN on 1/4/2018.
 */

public class LocationPoints implements ClusterItem{
    private double latitude;
    private double longitude;
    private final LatLng mPosition;

    LocationPoints(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        mPosition = new LatLng(latitude, longitude);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }
}
