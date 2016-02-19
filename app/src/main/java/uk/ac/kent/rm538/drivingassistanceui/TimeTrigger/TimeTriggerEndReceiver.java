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
public class TimeTriggerEndReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        Timber.i("Alarm receiver fired: END. Old profile: %s", intent.getStringExtra(DrivingAssistanceApp.INTENT_EXTRA_PROFILE_NAME));

        // Set the new profile
        UserProfileManager.setAppropriateProfile(null, UserProfileManager.ProfileChangeReason.TIMER_END);
        // Prepare for the next alarm
        DrivingAssistanceApp.setupAlarms();
    }
}
