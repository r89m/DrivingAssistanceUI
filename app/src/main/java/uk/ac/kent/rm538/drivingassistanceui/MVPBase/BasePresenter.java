package uk.ac.kent.rm538.drivingassistanceui.MVPBase;

import android.view.View;

import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;

/**
 * Created by Richard on 20/01/2016.
 */
public interface BasePresenter {

    public void onResume(BaseView view);
    public void onPause();
    public void onDestroy();
    public boolean viewExists();

    public void onProfileChange(UserProfile newProfile);
    public void onWheelchairConnectionStateChange(boolean newState);
    public void onWebsocketError(Exception ex);
}
