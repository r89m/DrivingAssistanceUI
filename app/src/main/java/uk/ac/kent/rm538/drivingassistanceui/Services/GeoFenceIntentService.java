package uk.ac.kent.rm538.drivingassistanceui.Services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager.ProfileChangeReason;

/**
 * Created by Richard on 28/01/2016.
 */
public class GeoFenceIntentService extends IntentService {

    public GeoFenceIntentService() {
        super("GeoFenceIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Timber.i("Intent Service launched");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Timber.e(String.valueOf(geofencingEvent.getErrorCode()));
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String transitionType = "Unknown";
            switch(geofenceTransition){
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    transitionType = "Entered";
                    break;

                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    transitionType = "Left";
                    break;

                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    transitionType = "Dwell";
                    break;
            }

            // Get the transition details as a String.
            String geofenceTransitionDetails = String.format("%s: ", transitionType);

            UserProfile suggestedProfile = null;

            for(Geofence fence : triggeringGeofences){
                geofenceTransitionDetails += String.format("[%s] ", fence.getRequestId());
                suggestedProfile = UserProfileManager.getUserProfileByLocationId(fence.getRequestId());
                // exit loop once a suitable profile has been found.
                if(suggestedProfile != null){
                    break;
                }
            }

            ProfileChangeReason changeReason = null;

            switch (geofenceTransition){

                case Geofence.GEOFENCE_TRANSITION_ENTER:
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    changeReason = ProfileChangeReason.LOCATION_ENTER;
                    break;

                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    changeReason = ProfileChangeReason.LOCATION_LEAVE;
                    break;
            }

            UserProfileManager.setAppropriateProfile(suggestedProfile, changeReason);

            Timber.i(geofenceTransitionDetails);
            if(suggestedProfile != null) {
                Timber.i("Suggested profile: %s", suggestedProfile.getName());
            }
        } else {
            // Log the error.
            Timber.e("Geofence Transition Invalid Type: %s", geofenceTransition);
        }
    }
}
