package uk.ac.kent.rm538.drivingassistanceui.UserLocation;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.service.carrier.CarrierMessagingService;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.Services.GeoFenceIntentService;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;

/**
 * Created by Richard on 28/01/2016.
 */
public class GeoFenceManager {

    private static ArrayList<Geofence> geofences = new ArrayList<>();
    private static PendingIntent pendingIntent;

    public static void registerGeofence(UserProfile profile){

        registerGeofence(null, profile);
    }

    public static void registerGeofence(ResultCallback<Status> callback, UserProfile userProfile){

        UserLocation location = userProfile.getActivationLocation();

        if(location == null || !location.isEnabled()){
            return;
        }

        if(location.getRadius() == 0){
            return;
        }

        GoogleApiClient apiClient = DrivingAssistanceApp.getGoogleApiClient();

        if(!apiClient.isConnected()){
            return;
        }

        Geofence fence = new Geofence.Builder()
                .setRequestId(location.getHash())
                .setCircularRegion(location.getLatitude(), location.getLongitude(), location.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                //.setLoiteringDelay(1000 * 60 * 5)               // Only trigger if device stays in location for 5 minutes
                //.setNotificationResponsiveness(1000 * 60 * 5)   // Only update within 5 minutes of entering location
                .setLoiteringDelay(500)
                .setNotificationResponsiveness(500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        geofences.add(fence);

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(fence);
        GeofencingRequest request = builder.build();

        if(pendingIntent == null) {
            Context ctx = DrivingAssistanceApp.getContext();

            Intent intent = new Intent(ctx, GeoFenceIntentService.class);
            pendingIntent = PendingIntent.getService(ctx,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        try {
            LocationServices.GeofencingApi
                    .addGeofences(apiClient, request, pendingIntent).setResultCallback(callback);
            Timber.i("Added GeoFence: %s (%s)", fence.getRequestId(), userProfile.getName());
        } catch (SecurityException exp){
            Timber.i("Permission denied for adding Geofences: %s", exp.getMessage());
        }

    }

    public static void removeGeofence(UserProfile userProfile){

        UserLocation location = userProfile.getActivationLocation();

        GoogleApiClient apiClient = DrivingAssistanceApp.getGoogleApiClient();
        ArrayList<String> requestIds = new ArrayList<>();

        requestIds.add(location.getHash());

        LocationServices.GeofencingApi.removeGeofences(apiClient, requestIds);
    }

    public static void updateGeofence(ResultCallback<Status> callback, UserProfile userProfile){

        removeGeofence(userProfile);
        registerGeofence(callback, userProfile);
    }

}
