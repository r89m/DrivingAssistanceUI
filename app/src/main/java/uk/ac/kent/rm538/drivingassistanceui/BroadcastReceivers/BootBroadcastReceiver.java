package uk.ac.kent.rm538.drivingassistanceui.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;

/**
 * Created by Richard on 21/01/2016.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // Check that this was called due to the device booting up
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETE")){

            Timber.i("Boot event");

            DrivingAssistanceApp.setupAlarmsAndGeofences();
        }
    }
}
