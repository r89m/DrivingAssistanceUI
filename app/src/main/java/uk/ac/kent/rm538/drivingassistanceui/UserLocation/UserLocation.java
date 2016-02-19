package uk.ac.kent.rm538.drivingassistanceui.UserLocation;

import android.location.Location;

/**
 * Created by Richard on 20/01/2016.
 */
public interface UserLocation {

    public double getLongitude();
    public double getLatitude();
    public float getRadius();
    public boolean isEnabled();

    public void setLocation(double latitude, double longitude);
    public void setRadius(float radius);
    public void setEnabled(boolean enabled);
    public boolean isWithinRadius(Location testLocation);

    public String getHash();
}
