package uk.ac.kent.rm538.drivingassistanceui.UserFeedback;


import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.Hardware.Zone;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEvent;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceListener;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadObstacle;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackController.DefaultFeedbackController;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.AudibleFeedbackProvider;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.VisualFeedbackProvider;
import uk.ac.kent.rm538.PWCInterface.Hardware.Obstacle;
import uk.ac.kent.rm538.drivingassistanceui.MVPBase.BaseView;
import uk.ac.kent.rm538.drivingassistanceui.PWC.PWCHapticFeedback;
import uk.ac.kent.rm538.drivingassistanceui.PWC.PWCWebsocketCommsProvider;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager.ProfileChangeReason;

/**
 * Created by Richard on 23/11/2015.
 */
public class UserFeedbackPresenterImpl implements UserFeedbackPresenter, AudibleFeedbackProvider, VisualFeedbackProvider, PWCInterfaceListener {

    private UserFeedbackView view;
    private UserFeedbackInteractor interactor;
    private List<Obstacle> obstacles = new ArrayList<>();

    private FeedbackSeverity lastSeverity;
    long lastSeverityTimestamp;

    public UserFeedbackPresenterImpl(UserFeedbackView view){

        this.view = view;
        interactor = new UserFeedbackInteractorImpl(this);
        DrivingAssistanceApp.registerPresenter(this);
    }

    @Override
    public void onResume(BaseView inView) {

        this.view = (UserFeedbackView) inView;

        interactor.connect();

        /*
        Node node1 = DrivingAssistanceApp.getPWCInterface().getNode(1);
        Zone zone_F_L_F = new Zone(node1, 1, Zone.Position.FRONT_LEFT_CORNER, Zone.Orientation.BACK);
        Zone zone_F_L_L = new Zone(node1, 2, Zone.Position.FRONT_LEFT_CORNER, Zone.Orientation.LEFT);
        Zone zone_F_L_LF = new Zone(node1, 3, Zone.Position.FRONT_LEFT_CORNER, Zone.Orientation.BACK_LEFT);

        node1.setZone(1, zone_F_L_F);
        node1.setZone(2, zone_F_L_L);
        node1.setZone(3, zone_F_L_LF);

        Node node2 = DrivingAssistanceApp.getPWCInterface().getNode(2);
        Zone zone_F_R_F = new Zone(node2, 1, Zone.Position.FRONT_RIGHT_CORNER, Zone.Orientation.BACK);
        Zone zone_F_R_R = new Zone(node2, 2, Zone.Position.FRONT_RIGHT_CORNER, Zone.Orientation.RIGHT);
        Zone zone_F_R_FR = new Zone(node2, 3, Zone.Position.FRONT_RIGHT_CORNER, Zone.Orientation.BACK_RIGHT);
        node2.setZone(1, zone_F_R_F);
        node2.setZone(2, zone_F_R_R);
        node2.setZone(3, zone_F_R_FR);

        addObstacleToDisplay(300, new Obstacle(zone_F_L_F, 300), FeedbackSeverity.SEVERE);
        addObstacleToDisplay(300, new Obstacle(zone_F_L_L, 300), FeedbackSeverity.MODERATE);
        addObstacleToDisplay(300, new Obstacle(zone_F_L_LF, 300), FeedbackSeverity.MILD);
        addObstacleToDisplay(300, new Obstacle(zone_F_R_F, 300), FeedbackSeverity.MILD);
        addObstacleToDisplay(300, new Obstacle(zone_F_R_R, 300), FeedbackSeverity.LOW);
        addObstacleToDisplay(300, new Obstacle(zone_F_R_FR, 300), FeedbackSeverity.SEVERE);
        */
        displayCurrentProfile();
    }

    @Override
    public void onPause() {

        this.view = null;
        interactor.disconnect();
        AudibleFeedbackRunnable.stopAudibleFeedback();
    }

    public void onDestroy(){

        view = null;
        interactor = null;
        DrivingAssistanceApp.unregisterPresenter(this);
    }

    @Override
    public boolean viewExists() {
        return (view != null);
    }

    @Override
    public void onProfileChange(UserProfile newProfile) {

        displayCurrentProfile();
    }

    @Override
    public void onWheelchairConnectionStateChange(boolean newState) {

    }

    @Override
    public void onWebsocketError(Exception ex) {

    }

    @Override
    public void displayCurrentProfile() {

        if(!viewExists()){
            return;
        }

        UserProfile profile = UserProfileManager.getCurrentProfile();
        view.displayCurrentProfileInfo(String.format("Active Profile: %s", profile.getName()));
    }

    @Override
    public void selectProfile(int profileIndex) {

        UserProfile profile = UserProfileManager.getUserProfileByIndex(profileIndex);
        UserProfileManager.setCurrentProfile(profile, ProfileChangeReason.SELECTED);
        displayCurrentProfile();
    }

    @Override
    public void provideAudibleFeedback(int volume, FeedbackSeverity severity) {

        AudibleFeedbackRunnable.provideAudibleFeedback(volume, severity, 1000);
    }

    @Override
    public void clearDisplayedObstacles() {

        if(!viewExists()){
            return;
        }

        view.hideAllObstacles();
    }

    @Override
    public void addObstacleToDisplay(int maxDistance, Obstacle obstacle, FeedbackSeverity severity) {

        if(!viewExists()){
            return;
        }

        // Ignore any obstacles that are too far away
        if(obstacle.getDistance() > maxDistance){
            return;
        }

        // Get the position of the obstacle
        Zone zone = obstacle.getZone();

        int xOffset = 0;
        int yOffset = 0;
        int arcMidAngle = 0;
        int arcStartAngle = 0;
        int arcEndAngle = 0;

        int viewHeight = 500;

        int zoneId = obstacle.getZone().getZoneNumber();

        // Get X Offset
        switch(zone.getPosition()) {

            case FRONT_RIGHT_CORNER:
            case RIGHT_CENTRE:
            case BACK_RIGHT_CORNER:
                xOffset = 200;
                break;

            case FRONT_LEFT_CORNER:
            case LEFT_CENTRE:
            case BACK_LEFT_CORNER:
                xOffset = -200;
                break;
        }

        // Get Y Offset
        switch (zone.getPosition()){

            case FRONT_LEFT_CORNER:
            case FRONT_CENTRE:
            case FRONT_RIGHT_CORNER:
                yOffset = 300;
                break;

            case BACK_LEFT_CORNER:
            case BACK_CENTRE:
            case BACK_RIGHT_CORNER:
                yOffset = 10;
                break;
        }

        // Drawing angle 0 is at 3 o'clock
        arcMidAngle = zone.getOrientation().getAngle() - 90;

        arcStartAngle = arcMidAngle - 22;
        arcEndAngle = arcMidAngle + 22;

        float drawScaleFactor = 1.6f * ((float)viewHeight / maxDistance);
        int distance = (int)(drawScaleFactor * obstacle.getDistance());

        // Map the severity to a colour
        int colour = view.getColourFromSeverity(severity);

        Timber.i("Draw zone: %d - %d - %s - %s", zone.getParentNode().getId(), zone.getZoneNumber(), zone.getPosition().toString() ,zone.getOrientation().toString());
        Timber.i("%d %d", xOffset, yOffset);
        Timber.i("%d %d %d", arcStartAngle, arcEndAngle, distance);

        view.addObstacle(arcStartAngle, arcEndAngle, distance, xOffset, yOffset, colour);
    }

    @Override
    public void drawObstacles() {

        view.drawObstacles();
    }

    @Override
    public void onPWCInterfaceEvent(PWCInterfaceEvent e) {

        Timber.i("Presenter PWC Interface Event");

        PWCInterfaceEvent.EventType type = e.getType();
        switch(type){
            case OBSTACLE:
                PWCInterfacePayloadObstacle payload = (PWCInterfacePayloadObstacle) e.getPayload();
                Obstacle o = payload.getObstacle();
                obstacles.add(o);
                break;

            case OBSTACLES_COMPLETE:
                UserProfile profile = UserProfileManager.getCurrentProfile();
                FeedbackConfiguration config = profile.getFeedbackConfiguration();
                DefaultFeedbackController feedbackController = new DefaultFeedbackController(config);

                // Provide feedback based on the current obstacles
                feedbackController.provideFeedback(obstacles, this, this, PWCHapticFeedback.getInstance());

                // Clear the obstacle list
                obstacles.clear();
                break;
        }

    }
}
