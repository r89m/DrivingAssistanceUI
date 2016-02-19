package uk.ac.kent.rm538.drivingassistanceui.UserFeedback;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import uk.ac.kent.rm538.drivingassistanceui.CustomViews.ObstacleView;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.MVPBase.BaseActivity;
import uk.ac.kent.rm538.drivingassistanceui.R;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;

public class UserFeedbackActivity extends BaseActivity implements UserFeedbackView {

    @Bind(R.id.view) ObstacleView obstacleView;
    @Bind(R.id.profile_info) TextView profileInfoView;

    private UserFeedbackPresenter presenterU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        setPresenter(new UserFeedbackPresenterImpl(this));

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected UserFeedbackPresenter getPresenter(){

        return (UserFeedbackPresenter) presenter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        SubMenu profileMenu = menu.findItem(R.id.action_choose_profile).getSubMenu();
        profileMenu.clear();

        int index = 0;

        for(UserProfile profile : UserProfileManager.getUserProfiles()){
            profileMenu.add(R.id.action_choose_profile, index, index, profile.getName());
            index++;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        int groupId = item.getGroupId();

        switch (groupId){

            case R.id.action_choose_profile:
                getPresenter().selectProfile(item.getItemId());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void displayLastMessage(String lastMessage){

    }

    @Override
    public void hideAllObstacles() {

        obstacleView.clearObstacles();
    }

    @Override
    public void showToastShort(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showToastLong(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void addObstacle(int arcStartAngle, int arcEndAngle, int distance, int xOffset, int yOffset, int colour) {

        obstacleView.addObstacles(arcStartAngle, arcEndAngle, distance, xOffset, yOffset, colour);
    }

    @Override
    public void drawObstacles() {

        obstacleView.drawObstacles();
    }

    @Override
    public void displayCurrentProfileInfo(String infoString) {

        profileInfoView.setText(infoString);
    }

    @Override
    public int getColourFromSeverity(FeedbackSeverity severity) {

        int resourceId;

        switch (severity){

            case SEVERE:
                resourceId = R.color.colourObstacleSevere;
                break;

            case MODERATE:
                resourceId = R.color.colourObstacleModerate;
                break;

            case MILD:
                resourceId = R.color.colourObstacleMild;
                break;

            case LOW:
                resourceId = R.color.colourObstacleLow;
                break;

            case NONE:
            default:
                resourceId = R.color.colourObstacleNone;
                break;
        }

        return ContextCompat.getColor(this, resourceId);
    }
}
