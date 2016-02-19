package uk.ac.kent.rm538.drivingassistanceui.UserFeedback;


import android.graphics.Color;
import android.graphics.Point;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.MVPBase.BaseView;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;

/**
 * Created by Richard on 23/11/2015.
 */
public interface UserFeedbackView extends BaseView {

    void displayLastMessage(String lastMessage);


    void hideAllObstacles();
    void addObstacle(int arcStartAngle, int arcEndAngle, int distance, int xOffset, int yOffset, int colour);
    void drawObstacles();
    void displayCurrentProfileInfo(String infoString);

    int getColourFromSeverity(FeedbackSeverity severity);


    void showToastShort(String msg);
    void showToastLong(String msg);
}
