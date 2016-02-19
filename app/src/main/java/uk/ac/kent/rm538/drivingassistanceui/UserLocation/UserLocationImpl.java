package uk.ac.kent.rm538.drivingassistanceui.UserLocation;

import android.location.Location;

/**
 * Created by Richard on 20/01/2016.
 */
public class UserLocationImpl implements UserLocation {

    private double longitude = 0;
    private double latitude = 0;
    private float radius = 200F;
    private boolean enabled = false;

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public float getRadius() {
        return radius;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setLocation(double latitude, double longitude) {

        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public void setRadius(float radius) {

        this.radius = radius;
    }

    @Override
    public void setEnabled(boolean enabled) {

        this.enabled = enabled;
    }

    @Override
    public boolean isWithinRadius(Location testLocation) {

        if(testLocation == null){
            return false;
        }

        Location fenceLocation = new Location("temp");
        fenceLocation.setLongitude(getLongitude());
        fenceLocation.setLatitude(getLatitude());

        return (fenceLocation.distanceTo(testLocation) < getRadius());
    }

    @Override
    public String getHash() {
        return String.format("%f:%f:%f", latitude, longitude, radius);
    }
}
