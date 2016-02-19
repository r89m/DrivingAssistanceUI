package uk.ac.kent.rm538.drivingassistanceui.UserProfiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.NotificationCompat;

import java.util.List;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.R;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.SimpleTimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.TimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.GeoFenceManager;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.UserLocation;

/**
 * Created by Richard on 01/02/2016.
 */
public class UserProfileManager {

    private static UserProfile currentUserProfile;
    public static List<UserProfile> userProfiles;
    private static UserProfile currentProfile = null;
    private static PWCInterface pwcInterface;

    public static final String PREFERENCES_KEY_NAME = "profile_name";
    public static final String PREFERENCES_KEY_VISUAL_ENABLED = "profile_visual_enabled";
    public static final String PREFERENCES_KEY_VISUAL_DISTANCE = "profile_visual_distance";
    public static final String PREFERENCES_KEY_AUDIBLE_ENABLED = "profile_audible_enabled";
    public static final String PREFERENCES_KEY_AUDIBLE_VOLUME = "profile_audible_volume";
    public static final String PREFERENCES_KEY_HAPTIC_ENABLED = "profile_haptic_enabled";
    public static final String PREFERENCES_KEY_HAPTIC_INTENSITY = "profile_haptic_intensity";
    public static final String PREFERENCES_KEY_TIMER_ENABLED = "profile_timer_enabled";
    public static final String PREFERENCES_KEY_TIMER_START = "profile_timer_start";
    public static final String PREFERENCES_KEY_TIMER_END = "profile_timer_end";
    public static final String PREFERENCES_KEY_LOCATION_ENABLED = "profile_location_enabled";
    public static final String PREFERENCES_KEY_LOCATION_LATITUDE = "profile_location_latitude";
    public static final String PREFERENCES_KEY_LOCATION_LONGITUDE = "profile_location_longitude";
    public static final String PREFERENCES_KEY_LOCATION_RADIUS = "profile_location_radius";

    public enum ProfileChangeReason {TIMER_START, TIMER_END, LOCATION_ENTER, LOCATION_LEAVE, SELECTED, DEFAULT, APP_LAUNCH}
    public enum ProfileAlarmType {START, END}

    public static void setCurrentProfile(UserProfile profile) {

        setCurrentProfile(profile, null);
    }

    public static void setCurrentProfile(UserProfile profile, ProfileChangeReason changeReason){

        UserProfile oldProfile = currentProfile;
        currentProfile = profile;

        // If the profile has changed...
        if(currentProfile != oldProfile) {

            // Update the driving characteristics of the wheelchair
            if (pwcInterface != null) {
                try {
                    /*
                    pwcInterface.setSensitivity(currentProfile.getForwardSensitivity(),
                            currentProfile.getBackwardSensitivity(),
                            currentProfile.getSidewaysSensitivity());
                    */
                    Timber.i("Set sensitivity: %d %d %d", currentProfile.getForwardSensitivity(), currentProfile.getBackwardSensitivity(), currentProfile.getSidewaysSensitivity());
                } catch (Exception e){
                    Timber.e(e.getMessage());
                }
            }

            // Update any presenters
            DrivingAssistanceApp.presenterEventOnProfileChange(currentProfile);

            DrivingAssistanceApp app = DrivingAssistanceApp.getContext();
            NotificationManager manager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);

            Timber.i("Profile changed. New profile is: %s", currentProfile.getName());

            // Only display a notification if the profile was not manually selected or the app opening
            if (!(changeReason == ProfileChangeReason.SELECTED || changeReason == ProfileChangeReason.APP_LAUNCH)) {

                String messageText = "";
                if (changeReason != null) {
                    String msgName = String.format("profile_change_reason_%s", changeReason.name().toLowerCase());
                    messageText = "\n" + app.getStringResourceByName(msgName);
                }

                String notificationMessage = String.format("Profile changed to %s%s",
                        currentProfile.getName(), messageText);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(app)
                        .setSmallIcon(R.drawable.ic_account_circle_white_24dp)
                        .setContentTitle("Profile changed")
                        .setContentText(notificationMessage)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage));

                manager.notify(DrivingAssistanceApp.NOTIFICATION_ID_PROFILE_CHANGE, notificationBuilder.build());

                Timber.i("Show notification");

            } else {
                // Hide the notification
                manager.cancel(DrivingAssistanceApp.NOTIFICATION_ID_PROFILE_CHANGE);
                Timber.i("Notification hidden");
            }
        }
    }

    public  static UserProfile getDefaultProfile(){

        return getUserProfileByName("Default");
    }

    public static void setAppropriateProfile(UserProfile suggestedProfile, ProfileChangeReason changeReason){

        UserProfile newProfile = null;

        if(changeReason == ProfileChangeReason.LOCATION_ENTER
                || changeReason == ProfileChangeReason.TIMER_START){
            newProfile = suggestedProfile;
        }

        // Use the most appropriate profile
        if(newProfile == null){
            newProfile = determineMostAppropriateProfile();
        }

        // If a profile still hasn't been found, use the default
        if(newProfile == null){
            newProfile = getDefaultProfile();
        }

        setCurrentProfile(newProfile, changeReason);
    }

    private static UserProfile determineMostAppropriateProfile(){

        DrivingAssistanceApp app = DrivingAssistanceApp.getContext();
        LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ex){
            // Permission denied
            Timber.e(ex.getMessage());
        }

        Timber.i("Determine most appropriate profile...");

        for(UserProfile profile : userProfiles){
            TimeTrigger time = profile.getTimeTrigger();
            Timber.i(profile.getName());
            if(time.isEnabled()){
                boolean inTimeWindow = time.currentlyWithinTimeWindow();
                Timber.i("In time window? %b", inTimeWindow);
                if(inTimeWindow){
                    Timber.i("Use this profile based on time window: %s", profile.getName());
                    return profile;
                }
            }
            UserLocation location = profile.getActivationLocation();
            if(location.isEnabled()){
                if(location.isWithinRadius(lastKnownLocation)){
                    return profile;
                }
            }
        }

        return null;
    }

    public static void setPWCInterface(PWCInterface newPwcInterface){

        pwcInterface = newPwcInterface;
    }

    public static List<UserProfile> getUserProfiles(){

        return userProfiles;
    }

    public static UserProfile getUserProfileByName(String userProfileName){

        if(userProfileName != null) {
            for (UserProfile profile : userProfiles) {
//                Timber.i("Checking profile: %s vs. %s", userProfileName, profile.getName());
                if (userProfileName.equals(profile.getName())) {
//                    Timber.i("Matched!");
                    return profile;
                }
            }
        }
        return null;
    }

    public static UserProfile getUserProfileByLocationId(String requestId) {

        Timber.i("Get user profile by location id: %s", requestId);

        if(requestId != null){
            Timber.i("Scan profiles");
            for(UserProfile profile : userProfiles){
                Timber.i("Checking profile %s: %s vs. %s",
                        profile.getName(),
                        requestId,
                        profile.getActivationLocation().getHash());
                if(requestId.equals(profile.getActivationLocation().getHash())){
                    Timber.i("Matched!");
                    return profile;
                }
            }
        }
        return null;
    }

    public static UserProfile getUserProfileByIndex(int index){

        return userProfiles.get(index);
    }

    public static UserProfile getCurrentProfile(){

        if(currentProfile == null){
            currentProfile = userProfiles.get(0);
        }
        return currentProfile;
    }

    public static void copyCurrentProfileToPrefs(SharedPreferences settings) {

        SharedPreferences.Editor editor = settings.edit();

        UserProfile profile = UserProfileManager.getCurrentProfile();
        FeedbackConfiguration config = profile.getFeedbackConfiguration();
        SimpleTimeTrigger timeTrigger = (SimpleTimeTrigger) profile.getTimeTrigger();
        UserLocation activationLocation = profile.getActivationLocation();

        editor.putString(UserProfileManager.PREFERENCES_KEY_NAME, profile.getName());
        editor.putBoolean(UserProfileManager.PREFERENCES_KEY_VISUAL_ENABLED, config.isVisualFeedbackEnabled());
        editor.putInt(UserProfileManager.PREFERENCES_KEY_VISUAL_DISTANCE, config.getVisualFeedbackDistance());
        editor.putBoolean(UserProfileManager.PREFERENCES_KEY_AUDIBLE_ENABLED, config.isAudibleFeedbackEnabled());
        editor.putInt(UserProfileManager.PREFERENCES_KEY_AUDIBLE_VOLUME, config.getAudibleFeedbackVolume());
        editor.putBoolean(UserProfileManager.PREFERENCES_KEY_HAPTIC_ENABLED, config.isHapticFeedbackEnabled());
        editor.putInt(UserProfileManager.PREFERENCES_KEY_HAPTIC_INTENSITY, config.getHapticFeedbackIntensity());
        editor.putBoolean(UserProfileManager.PREFERENCES_KEY_TIMER_ENABLED, timeTrigger.isEnabled());
        editor.putInt(UserProfileManager.PREFERENCES_KEY_TIMER_START, timeTrigger.getStartTime());
        editor.putInt(UserProfileManager.PREFERENCES_KEY_TIMER_END, timeTrigger.getEndTime());
        editor.putBoolean(UserProfileManager.PREFERENCES_KEY_LOCATION_ENABLED, activationLocation.isEnabled());
        editor.putLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LATITUDE, Double.doubleToRawLongBits(activationLocation.getLatitude()));
        editor.putLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LONGITUDE, Double.doubleToRawLongBits(activationLocation.getLongitude()));
        editor.putString(UserProfileManager.PREFERENCES_KEY_LOCATION_RADIUS, String.valueOf(activationLocation.getRadius()));
        editor.commit();
    }

    public static void copyPrefsToCurrentProfile(SharedPreferences settings){

        UserProfile profile = getCurrentProfile();
        FeedbackConfiguration config = profile.getFeedbackConfiguration();
        SimpleTimeTrigger timeTrigger = (SimpleTimeTrigger) profile.getTimeTrigger();
        UserLocation activationLocation = profile.getActivationLocation();

        profile.setName(settings.getString(UserProfileManager.PREFERENCES_KEY_NAME, "Name Failed"));
        config.setVisualFeedbackEnabled(settings.getBoolean(UserProfileManager.PREFERENCES_KEY_VISUAL_ENABLED, true));
        config.setVisualFeedbackDistance(settings.getInt(UserProfileManager.PREFERENCES_KEY_VISUAL_DISTANCE, 120));
        config.setAudibleFeedbackEnabled(settings.getBoolean(UserProfileManager.PREFERENCES_KEY_AUDIBLE_ENABLED, true));
        config.setAudibleFeedbackVolume(settings.getInt(UserProfileManager.PREFERENCES_KEY_AUDIBLE_VOLUME, 100));
        config.setHapticFeedbackEnabled(settings.getBoolean(UserProfileManager.PREFERENCES_KEY_HAPTIC_ENABLED, true));
        config.setHapticFeedbackIntensity(settings.getInt(UserProfileManager.PREFERENCES_KEY_HAPTIC_INTENSITY, 100));

        timeTrigger.setEnabled(settings.getBoolean(UserProfileManager.PREFERENCES_KEY_TIMER_ENABLED, false));
        timeTrigger.setStartTime(settings.getInt(UserProfileManager.PREFERENCES_KEY_TIMER_START, 0));
        timeTrigger.setEndTime(settings.getInt(UserProfileManager.PREFERENCES_KEY_TIMER_END, 0));

        // Get the new geofence states
        boolean new_enabled = settings.getBoolean(UserProfileManager.PREFERENCES_KEY_LOCATION_ENABLED, false);
        double new_latitude = Double.longBitsToDouble(settings.getLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LATITUDE, 0));
        double new_longitude = Double.longBitsToDouble(settings.getLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LONGITUDE, 0));

        // Check if they have changed, and recreate if so
        if((new_enabled != activationLocation.isEnabled()) || (new_latitude != activationLocation.getLatitude()) || (new_longitude != activationLocation.getLongitude())){
            GeoFenceManager.removeGeofence(profile);
            GeoFenceManager.registerGeofence(profile);
        }

        activationLocation.setEnabled(new_enabled);
        activationLocation.setLocation(new_latitude, new_longitude);
        activationLocation.setRadius(Float.parseFloat(settings.getString(UserProfileManager.PREFERENCES_KEY_LOCATION_RADIUS, "0")));
    }
}
