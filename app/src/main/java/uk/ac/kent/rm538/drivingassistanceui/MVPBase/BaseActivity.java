package uk.ac.kent.rm538.drivingassistanceui.MVPBase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.R;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;
import uk.ac.kent.rm538.drivingassistanceui.UserSettings.SettingsActivity;

/**
 * Created by Richard on 14/01/2016.
 */
public class BaseActivity extends AppCompatActivity implements BaseView {

    protected BasePresenter presenter;

    protected void goToSettings() {

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    protected void editProfile() {

        // Create an intent to launch the profile settings
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.ProfilePreferenceFragement.class.getName());
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, R.string.pref_header_profiles);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE, R.string.pref_subheader_profiles);

        // Copy all the current profile's parameters to the relevant preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        UserProfileManager.copyCurrentProfileToPrefs(settings);

        startActivity(intent);
    }

    @Override
    public void onResume() {
        if(presenter != null){
            presenter.onResume((BaseView)this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if(presenter != null){
            presenter.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if(presenter != null) {
            presenter.onDestroy();
        }
        super.onDestroy();
    }

    protected BasePresenter getPresenter(){

        return presenter;
    }

    protected void setPresenter(BasePresenter presenter){

        this.presenter = presenter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch(id){
            case R.id.action_settings:
                goToSettings();
                return true;

            case R.id.action_edit_current_profile:
                editProfile();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
