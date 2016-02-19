package uk.ac.kent.rm538.drivingassistanceui.UserFeedback;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.MVPBase.BasePresenter;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;

/**
 * Created by Richard on 23/11/2015.
 */
public interface UserFeedbackPresenter extends BasePresenter {
    public void displayCurrentProfile();
    public void selectProfile(int profileIndex);
}
