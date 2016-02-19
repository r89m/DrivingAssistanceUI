package uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.PWCInterface.Hardware.Obstacle;

/**
 * Created by Richard on 20/01/2016.
 */
public interface VisualFeedbackProvider {

    public void clearDisplayedObstacles();
    public void addObstacleToDisplay(int maxDistance, Obstacle obstacle, FeedbackSeverity severity);
    public void drawObstacles();
}
