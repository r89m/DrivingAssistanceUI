package uk.ac.kent.rm538.drivingassistanceui.UserSettings;


import android.annotation.TargetApi;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.drm.DrmInfo;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.AudibleFeedbackProvider;
import uk.ac.kent.rm538.drivingassistanceui.PWC.PWCHapticFeedback;
import uk.ac.kent.rm538.drivingassistanceui.R;
import uk.ac.kent.rm538.drivingassistanceui.UserFeedback.AudibleFeedbackRunnable;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.GeoFenceManager;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;
import uk.ac.kent.rm538.drivingassistanceui.UserSettings.Preferences.LocationPreference;
import uk.ac.kent.rm538.drivingassistanceui.UserSettings.Preferences.SeekBarPreference;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                index = Math.min(index, listPreference.getEntries().length - 1);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sBindTimePreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            long timestamp = Long.parseLong(String.valueOf(newValue));
            int hrs = (int) TimeUnit.SECONDS.toHours(timestamp);
            int mins = (int) TimeUnit.SECONDS.toMinutes(timestamp - (hrs * 3600));

            Timber.i("Preference on change: %02d:%02d", hrs, mins);

            return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, String.format("%02d:%02d", hrs, mins));
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindTimePreferenceSummaryToValue(Preference preference){

        preference.setOnPreferenceChangeListener(sBindTimePreferenceSummaryToValueListener);

        sBindTimePreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getInt(preference.getKey(), 0));
    }

    private static void bindIntPreferenceSummaryToValue(Preference preference){

        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getInt(preference.getKey(), 0));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || ProfilePreferenceFragement.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ProfilePreferenceFragement extends PreferenceFragment implements Preference.OnPreferenceClickListener, TimePickerDialog.OnTimeSetListener, SeekBarPreference.OnSeekBarPreferenceChangeListener, AudibleFeedbackProvider, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

        private Preference currentTimePreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_profile);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("profile_name"));
            bindIntPreferenceSummaryToValue(findPreference("profile_visual_distance"));
            bindTimePreferenceSummaryToValue(findPreference("profile_timer_start"));
            bindIntPreferenceSummaryToValue(findPreference("profile_audible_volume"));
            bindIntPreferenceSummaryToValue(findPreference("profile_haptic_intensity"));
            bindTimePreferenceSummaryToValue(findPreference("profile_timer_end"));
            bindPreferenceSummaryToValue(findPreference("profile_location_radius"));

            findPreference("profile_audible_volume").setOnPreferenceClickListener(this);
            findPreference("profile_haptic_intensity").setOnPreferenceClickListener(this);

            findPreference("profile_timer_start").setOnPreferenceClickListener(this);
            findPreference("profile_timer_end").setOnPreferenceClickListener(this);

            findPreference("profile_location_position").setOnPreferenceClickListener(this);

            DrivingAssistanceApp.connectComms();
        }

        @Override
        public void onPause() {
            super.onPause();

            DrivingAssistanceApp.disconnectComms();
            UserProfileManager.copyPrefsToCurrentProfile(PreferenceManager.getDefaultSharedPreferences(getActivity()));
            DrivingAssistanceApp.save();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item){
            int id = item.getItemId();
            if(id == android.R.id.home){
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        public void showTimeDialog(Preference preference, int defaultTime){

            currentTimePreference = preference;

            int hr = (int) TimeUnit.SECONDS.toHours(defaultTime);
            int minute = (int) TimeUnit.SECONDS.toMinutes(defaultTime - (hr * 3600));

            new TimePickerDialog(getActivity(), this, hr, minute, true).show();
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            if(currentTimePreference != null){
                int timestamp = (hourOfDay * 3600) + (minute * 60);
                PreferenceManager.getDefaultSharedPreferences(currentTimePreference.getContext())
                        .edit().putInt(currentTimePreference.getKey(), timestamp).commit();
                sBindTimePreferenceSummaryToValueListener.onPreferenceChange(currentTimePreference, timestamp);
            }
        }

        public void showMapPreference(Preference preference, double latitude, double longitude){

            Intent mapIntent = new Intent(preference.getContext(), LocationPreference.class);
            mapIntent.putExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LATITUDE, latitude);
            mapIntent.putExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LONGITUDE, longitude);
            Timber.i("Starting activity..");
            startActivityForResult(mapIntent, DrivingAssistanceApp.REQUEST_ID_LOCATION_PREFERENCE);
            Timber.i("Started");
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

            if(requestCode == DrivingAssistanceApp.REQUEST_ID_LOCATION_PREFERENCE){
                if(resultCode == RESULT_OK){
                    Double latitude = data.getDoubleExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LATITUDE, 0);
                    Double longitude = data.getDoubleExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LONGITUDE, 0);

                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .edit()
                            .putLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LATITUDE, Double.doubleToRawLongBits(latitude))
                            .putLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LONGITUDE, Double.doubleToRawLongBits(longitude))
                            .commit();
                }
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {

            switch(preference.getTitleRes()){

                case R.string.pref_title_profile_visual_distance:
                case R.string.pref_title_profile_audible_volume:
                case R.string.pref_title_profile_haptic_intensity:
                    SeekBarPreference seekPref = (SeekBarPreference) preference;
                    seekPref.setOnChangeListener(this);
                    seekPref.setOnCancelListener(this);
                    seekPref.setOnDismissListener(this);
                    return true;

                case R.string.pref_title_profile_timer_start:
                    showTimeDialog(preference, preference.getSharedPreferences().getInt(UserProfileManager.PREFERENCES_KEY_TIMER_START, 0));
                    return true;

                case R.string.pref_title_profile_timer_end:
                    showTimeDialog(preference, preference.getSharedPreferences().getInt(UserProfileManager.PREFERENCES_KEY_TIMER_END, 0));
                    return true;

                case R.string.pref_title_location_position:
                    SharedPreferences settings = preference.getSharedPreferences();
                    double longitude = Double.longBitsToDouble(settings.getLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LONGITUDE, 0));
                    double latitude = Double.longBitsToDouble(settings.getLong(UserProfileManager.PREFERENCES_KEY_LOCATION_LATITUDE, 0));

                    // If no location has been saved, use the current location
                    if(longitude == 0 && latitude == 0){
                        LocationManager lm = (LocationManager) DrivingAssistanceApp.getContext().getSystemService(Context.LOCATION_SERVICE);
                        Location lastKnownLocation = null;
                        try {
                            lastKnownLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            longitude = lastKnownLocation.getLongitude();
                            latitude = lastKnownLocation.getLatitude();
                        } catch (SecurityException ex){
                            // Permission denied
                            Timber.e(ex.getMessage());
                        }
                    }

                    showMapPreference(preference, latitude, longitude);
                    return true;

            }
            return false;
        }

        @Override
        public void onProgressChange(SeekBarPreference pref, int value) {

            Timber.i("Progress change");
            Timber.i("%d vs. %d", pref.getTitleRes(), R.string.pref_title_profile_audible_volume);

            switch(pref.getTitleRes()){

                case R.string.pref_title_profile_audible_volume:
                    provideAudibleFeedback(value, FeedbackSeverity.SEVERE);
                    break;

                case R.string.pref_title_profile_haptic_intensity:
                    try {
                        PWCHapticFeedback.getInstance().provideHapticFeedback(value, FeedbackSeverity.SEVERE);
                    } catch (Exception ex){
                        ex.printStackTrace();
                        Toast.makeText(pref.getContext(), "Could not demonstrate haptic feedback", Toast.LENGTH_SHORT).show();
                    }
                    break;

            }
        }

        @Override
        public void provideAudibleFeedback(final int volume, final FeedbackSeverity severity) {

            AudibleFeedbackRunnable.provideAudibleFeedback(volume, severity, 3000);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

            stopFeedback();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {

            stopFeedback();
        }

        private void stopFeedback(){

            Timber.i("Stop feedback");
            AudibleFeedbackRunnable.stopAudibleFeedback();
            PWCHapticFeedback.getInstance().provideHapticFeedback(0, FeedbackSeverity.NONE);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
//            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("connection_mode"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
