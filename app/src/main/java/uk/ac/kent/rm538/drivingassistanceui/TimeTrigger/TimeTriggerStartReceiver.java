package uk.ac.kent.rm538.drivingassistanceui.TimeTrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.widget.Toast;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;

/**
 * Created by Richard on 25/01/2016.
 */
public class TimeTriggerStartReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        String intentName = intent.getStringExtra(DrivingAssistanceApp.INTENT_EXTRA_PROFILE_NAME);
        Timber.i("Alarm receiver fired: START. New profile: %s", intentName);

        // Set the correct profile
        UserProfile intentProfile = UserProfileManager.getUserProfileByName(intentName);
        UserProfileManager.setAppropriateProfile(intentProfile, UserProfileManager.ProfileChangeReason.TIMER_START);

        // Prepare for the next alarm
        DrivingAssistanceApp.setupAlarms();
    }
}
