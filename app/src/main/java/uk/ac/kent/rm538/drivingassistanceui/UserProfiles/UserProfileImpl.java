package uk.ac.kent.rm538.drivingassistanceui.UserProfiles;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.DefaultFeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.SimpleTimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.TimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.UserLocation;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.UserLocationImpl;

/**
 * Created by Richard on 20/01/2016.
 */
public class UserProfileImpl implements UserProfile {

    private String name;
    private UserLocation activationLocation = new UserLocationImpl();
    private FeedbackConfiguration feedbackConfiguration = new DefaultFeedbackConfiguration();
    private TimeTrigger timeTrigger = new SimpleTimeTrigger();
    private int forwardSensitivity = 80;
    private int backwardSensitivity = 80;
    private int sidewaysSensitivity = 80;


    public UserProfileImpl(String name){

        setName(name);
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public void setName(String name) {

        this.name = name;
    }

    @Override
    public UserLocation getActivationLocation() {

        return activationLocation;
    }

    @Override
    public void setActivationLocation(UserLocation activationLocation) {

        this.activationLocation = activationLocation;
    }

    @Override
    public FeedbackConfiguration getFeedbackConfiguration() {

        return feedbackConfiguration;
    }

    @Override
    public void setFeedbackConfiguration(FeedbackConfiguration feedbackConfiguration) {

        this.feedbackConfiguration = feedbackConfiguration;
    }

    @Override
    public void setTimeTrigger(TimeTrigger timeTrigger) {

        this.timeTrigger = timeTrigger;
    }

    @Override
    public TimeTrigger getTimeTrigger() {

        return timeTrigger;
    }

    @Override
    public void setWheelchairSensitivity(int forwardSensitivity, int backwardSensitivity, int sidewaysSensitivity) {

        this.forwardSensitivity = forwardSensitivity;
        this.backwardSensitivity = backwardSensitivity;
        this.sidewaysSensitivity = sidewaysSensitivity;
    }

    @Override
    public int getForwardSensitivity() {
        return forwardSensitivity;
    }

    @Override
    public int getBackwardSensitivity() {
        return backwardSensitivity;
    }

    @Override
    public int getSidewaysSensitivity() {
        return sidewaysSensitivity;
    }
}
