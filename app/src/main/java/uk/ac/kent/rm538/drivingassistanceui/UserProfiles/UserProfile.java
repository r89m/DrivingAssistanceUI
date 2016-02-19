package uk.ac.kent.rm538.drivingassistanceui.UserProfiles;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.TimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.UserLocation;

/**
 * Created by Richard on 20/01/2016.
 */
public interface UserProfile {

    public String getName();
    public void setName(String name);
    public UserLocation getActivationLocation();
    public void setActivationLocation(UserLocation activationLocation);
    public FeedbackConfiguration getFeedbackConfiguration();
    public void setFeedbackConfiguration(FeedbackConfiguration feedbackConfiguration);
    public void setTimeTrigger(TimeTrigger timeTrigger);
    public TimeTrigger getTimeTrigger();
    public void setWheelchairSensitivity(int forwardSensitivity, int backwardSensitivity, int sidewaysSensitivity);
    public int getForwardSensitivity();
    public int getBackwardSensitivity();
    public int getSidewaysSensitivity();
}
