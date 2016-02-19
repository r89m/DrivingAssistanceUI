package uk.ac.kent.rm538.drivingassistanceui;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.MVPBase.BasePresenter;
import uk.ac.kent.rm538.drivingassistanceui.PWC.PWCWebsocketCommsProvider;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.SimpleTimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.TimeTriggerEndReceiver;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.TimeTriggerStartReceiver;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.GeoFenceManager;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileImpl;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager.ProfileAlarmType;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileSerializer;

/**
 * Created by Richard on 11/01/2016.
 */
public class DrivingAssistanceApp extends Application implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, PWCWebsocketCommsProvider.ErrorListener {

    public static int NOTIFICATION_ID_PROFILE_CHANGE = 1;

    public static int REQUEST_ID_PROFILE_START = 5;
    public static int REQUEST_ID_PROFILE_END = 6;
    public static int REQUEST_ID_LOCATION_PREFERENCE = 7;


    public static final String INTENT_EXTRA_PROFILE_NAME = "uk.ac.kent.rm538.drivingassistanceui.intent.PROFILE_NAME";
    public static final String INTENT_EXTRA_LOCATION_LATITUDE = "uk.ac.kent.rm538.drivingassistanceui.intent.LATITUDE";
    public static final String INTENT_EXTRA_LOCATION_LONGITUDE = "uk.ac.kent.rm538.drivingassistanceui.intent.LONGITUDE";

    private static DrivingAssistanceApp instance;
    private static GoogleApiClient googleApiClientInstance;
    private static DrivingAssistanceAppPreferences prefs;

    private static PWCInterface pwcInterface;
    private static PWCWebsocketCommsProvider comms;
    private static PWCWebsocketCommsProvider.ErrorListener socketErrorListener;

    private static List<BasePresenter> presenters = new ArrayList<>();

    private static String FILENAME_PROFILES = "profiles.txt";

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        prefs = new DrivingAssistanceAppPreferences();

        // Setup logging
        Timber.plant(new Timber.DebugTree());

        String socketAddress = DrivingAssistanceApp.getPrefs().getWebSocketAddress();
        comms = new PWCWebsocketCommsProvider(socketAddress, this);
        pwcInterface = new PWCInterface(comms);

        restore();

        UserProfileManager.setPWCInterface(pwcInterface);

        setupAlarmsAndGeofences();

        UserProfileManager.setAppropriateProfile(null, UserProfileManager.ProfileChangeReason.APP_LAUNCH);
        Timber.i("Application Launched");
    }

    public static DrivingAssistanceApp getContext(){

        return instance;
    }

    public static GoogleApiClient getGoogleApiClient(){

        if(googleApiClientInstance == null){

            DrivingAssistanceApp app = getContext();

            googleApiClientInstance = new GoogleApiClient.Builder(app)
                    .addConnectionCallbacks(app)
                    .addOnConnectionFailedListener(app)
                    .addApi(LocationServices.API)
                    .build();

            if(!googleApiClientInstance.isConnecting()) {
                googleApiClientInstance.connect();
            }
        }

        return googleApiClientInstance;
    }

    public static void setupAlarmsAndGeofences() {

        Timber.i("Setting up timers and fences");

        setupAlarms();
        setupGeofences();
    }

    public static void setupAlarms(){

        Timber.i("Setup up timers");

        // Clear all alarams
        Context context = getContext();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getAlarmPendingIntent(ProfileAlarmType.START, ""));
        am.cancel(getAlarmPendingIntent(ProfileAlarmType.END, ""));

        // Iterate over all user profiles and set alarms or geofences for any triggers
        List<UserProfile> userProfiles = UserProfileManager.getUserProfiles();

        long currentNearestStartAlarmTime = Long.MAX_VALUE;
        long currentNearestEndAlarmTime = Long.MAX_VALUE;

        UserProfile nearestStartAlarmProfile = null;
        UserProfile nearestEndAlarmProfile = null;

        for(UserProfile userProfile : userProfiles){
            // If a time trigger has been configured, find the next alarm time and set it
            SimpleTimeTrigger timeTrigger = (SimpleTimeTrigger) userProfile.getTimeTrigger();
            //Timber.i("Alarm check for profile %s: %b", userProfile.getName(), timeTrigger.isEnabled());
            if(timeTrigger != null && timeTrigger.isEnabled()){

                long startTimestamp = timeTrigger.getNextStartTriggerTime().getMilliseconds(SimpleTimeTrigger.TIMEZONE_GMT);
                long endTimestamp = timeTrigger.getNextEndTriggerTime().getMilliseconds(SimpleTimeTrigger.TIMEZONE_GMT);

                if(startTimestamp < currentNearestStartAlarmTime){
                    currentNearestStartAlarmTime = startTimestamp;
                    nearestStartAlarmProfile = userProfile;
                }

                if(endTimestamp < currentNearestEndAlarmTime){
                    currentNearestEndAlarmTime = endTimestamp;
                    nearestEndAlarmProfile = userProfile;
                }

            }
        }

        if(currentNearestStartAlarmTime < currentNearestEndAlarmTime) {
            if (nearestStartAlarmProfile != null) {
                Timber.i("Alarm set for profile to start: %s at [%d]", nearestStartAlarmProfile.getName(), currentNearestStartAlarmTime);
                am.setWindow(AlarmManager.RTC_WAKEUP, currentNearestStartAlarmTime, 5000, getAlarmPendingIntent(ProfileAlarmType.START, nearestStartAlarmProfile.getName()));
            }
        } else {
            if (nearestEndAlarmProfile != null) {
                Timber.i("Alarm set for profile to end: %s at [%d]", nearestEndAlarmProfile.getName(), currentNearestEndAlarmTime);
                am.setWindow(AlarmManager.RTC_WAKEUP, currentNearestEndAlarmTime, 5000, getAlarmPendingIntent(ProfileAlarmType.END, nearestEndAlarmProfile.getName()));
            }
        }
    }

    public static void setupGeofences(){

        // Cause the Google API client to start.
        // Once it has started it will register any required Geofences
        getGoogleApiClient();

        for(UserProfile userProfile : UserProfileManager.userProfiles){
            GeoFenceManager.registerGeofence(getContext(), userProfile);
        }
    }

    public static PendingIntent getAlarmPendingIntent(ProfileAlarmType type, String name){

        Context context = getContext();

        Intent alarmIntent;
        int requestId;
        if(type == ProfileAlarmType.START){
            alarmIntent = new Intent(context, TimeTriggerStartReceiver.class);
            requestId = REQUEST_ID_PROFILE_START;
        } else {
            alarmIntent = new Intent(context, TimeTriggerEndReceiver.class);
            requestId = REQUEST_ID_PROFILE_END;
        }

        alarmIntent.putExtra(DrivingAssistanceApp.INTENT_EXTRA_PROFILE_NAME, name);
        return PendingIntent.getBroadcast(context, requestId, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public static void save(){

        Context ctx = getContext();




        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UserProfileImpl.class, new UserProfileSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();

        final String outputJson = gson.toJson(UserProfileManager.userProfiles);

        try {
            boolean writeProfileSuccess = writeFile(ctx.openFileOutput("profiles.txt", MODE_PRIVATE), outputJson);
            Timber.i("Writing profiles to file %s", (writeProfileSuccess ? "succeeded" : "failed"));
        } catch (IOException ex){
            Timber.e("Writing file error: %s", ex.getMessage());
        }
    }

    public  static void restore(){

        Context ctx = getContext();

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UserProfileImpl.class, new UserProfileSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();

        try {
            String inputJson = readFile(ctx.openFileInput("profiles.txt"));
            //inputJson = "[{name=\"Default\"}]";
            Type listType = new TypeToken<ArrayList<UserProfileImpl>>(){}.getType();
            UserProfileManager.userProfiles = gson.fromJson(inputJson, listType);
            Timber.i("User Profiles Test: %d", UserProfileManager.userProfiles.size());
        } catch (IOException ex){
            Timber.e("Reading file error: %s", ex.getMessage());
        } catch (Exception ex2){
            ex2.printStackTrace();
        }

        if(UserProfileManager.userProfiles == null) {
            UserProfileManager.userProfiles = new ArrayList<>();

            // Add some dummy ones for now
            UserProfileManager.userProfiles.add(new UserProfileImpl("Profile 1"));
            UserProfileManager.userProfiles.add(new UserProfileImpl("Profile 2"));
            UserProfileManager.userProfiles.add(new UserProfileImpl("Profile 3"));
            UserProfileManager.userProfiles.add(new UserProfileImpl("Profile 4"));
            UserProfileManager.userProfiles.add(new UserProfileImpl("Profile 5"));
        }


        /*
        UserProfile profile1 = UserProfileManager.getUserProfileByIndex(0);
        profile1.setName("K&C Hospital");
        profile1.getActivationLocation().setEnabled(true);
        profile1.getActivationLocation().setLocation(51.264967, 1.086329);
        profile1.getActivationLocation().setRadius(200);

        UserProfile profile2 = UserProfileManager.getUserProfileByIndex(1);
        profile2.setName("Kent University");
        profile2.getActivationLocation().setEnabled(false);
        profile2.getActivationLocation().setLocation(51.297872, 1.069843);
        profile2.getActivationLocation().setRadius(200);
        profile2.getTimeTrigger().setEnabled(false);

        SimpleTimeTrigger timeTrigger2 = (SimpleTimeTrigger) profile2.getTimeTrigger();
        timeTrigger2.setEnabled(true);
        timeTrigger2.setStartTime(21, 45);
        timeTrigger2.setEndTime(22, 0);

        UserProfile profile3 = UserProfileManager.getUserProfileByIndex(2);
        profile3.setName("Guys Hospital");
        profile3.getActivationLocation().setEnabled(true);
        profile3.getActivationLocation().setLocation(51.503671, -0.087628);
        profile3.getActivationLocation().setRadius(200);

        UserProfile profile4 = UserProfileManager.getUserProfileByIndex(3);
        profile4.setName("Home");
        profile4.getActivationLocation().setEnabled(false);
        profile4.getActivationLocation().setLocation(51.272581, -0.210162);
        profile4.getActivationLocation().setRadius(200);

        SimpleTimeTrigger timeTrigger4 = (SimpleTimeTrigger) profile4.getTimeTrigger();
        timeTrigger4.setEnabled(true);
        timeTrigger4.setStartTime(21, 17);
        timeTrigger4.setEndTime(21, 40);

        UserProfile profile5 = UserProfileManager.getUserProfileByIndex(4);
        profile5.setName("Default");
        profile5.getActivationLocation().setEnabled(false);
        profile5.getFeedbackConfiguration().setVisualFeedbackDistance(140);
        */
    }

    private static boolean writeFile(FileOutputStream outputSteam, String data){

        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputSteam);
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            return true;
        } catch (IOException ex){
            Timber.e("Writing file error: %s", ex.getMessage());
            return false;
        }
    }

    private static String readFile(FileInputStream inputStream){

        String outputData = null;
        if ( inputStream != null ) {
            try {

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                outputData = stringBuilder.toString();
            } catch (IOException ex){
                Timber.e("Reading file error: %s", ex.getMessage());
            }
        }

        return outputData;
    }

    public String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        Timber.i("Get string resource: %s - %s", aString, packageName);
        int resId = getResources().getIdentifier(aString, "string", packageName);
        return getString(resId);
    }

    @Override
    public void onConnected(Bundle bundle) {

        Timber.i("Connected to Google API");

        setupGeofences();
    }

    public static void registerPresenter(BasePresenter presenter){

        synchronized (presenters) {
            presenters.add(presenter);
        }
    }

    public static void unregisterPresenter(BasePresenter presenter){

        synchronized (presenters) {
            presenters.remove(presenter);
        }
    }

    public static PWCInterface getPWCInterface() {

        return pwcInterface;
    }

    public static void connectComms(){

        comms.connect(getPrefs().getPort());
    }

    public static void disconnectComms(){

        comms.disconnect();
    }

    public static void presenterEventOnProfileChange(final UserProfile newProfile){

        // Use a handler to ensure that onPWCInterfaceEvent is called on the UI thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for(BasePresenter presenter : presenters){
                    if(presenter != null){
                        presenter.onProfileChange(newProfile);
                    }
                }
            }
        };
        mainHandler.post(runnable);
    }

    @Override
    public void onConnectionSuspended(int i) {

        Timber.i("Disconnected from Google API");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Timber.w("Connection to Google API failed: [%d] %s", connectionResult.getErrorCode(), connectionResult.getErrorMessage());
    }

    @Override
    public void onResult(Status status) {

        if(status.isSuccess()){
            Timber.i("GeoFence Created: %s", status.toString());
        } else {
            Timber.e(status.toString());
        }
    }

    public static DrivingAssistanceAppPreferences getPrefs(){

        return prefs;
    }

    @Override
    public void onError(final Exception ex) {

        Timber.e(ex.getMessage());

        // Use a handler to ensure that onWebsocketError is called on the UI thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for(BasePresenter presenter : presenters){
                    if(presenter != null){
                        presenter.onWebsocketError(ex);
                    }
                }
            }
        };
        mainHandler.post(runnable);
    }

    public class DrivingAssistanceAppPreferences{

        private String webSocketAddress = "ws://192.168.30.1:8989/ws";
        private String port = "/dev/ttyACM0";
//        private String webSocketAddress = "ws://192.168.43.97:8989/ws";
//        private String port = "com4";

        public String getWebSocketAddress(){

            return webSocketAddress;
        }

        public String getPort(){

            return port;
        }
    }
}
