package uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackController;

import java.util.List;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.AudibleFeedbackProvider;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.HapticFeedbackProvider;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.VisualFeedbackProvider;
import uk.ac.kent.rm538.PWCInterface.Hardware.Obstacle;

/**
 * Created by Richard on 20/01/2016.
 */
public class DefaultFeedbackController implements FeedbackController {

    private FeedbackConfiguration feedbackConfiguration;

    public DefaultFeedbackController(FeedbackConfiguration feedbackConfiguration){

        this.feedbackConfiguration = feedbackConfiguration;
    }

    @Override
    public void setFeedbackConfiguration(FeedbackConfiguration feedbackConfiguration) {

        this.feedbackConfiguration = feedbackConfiguration;
    }

    @Override
    public void provideFeedback(List<Obstacle> obstacles,
                                VisualFeedbackProvider visualFeedbackProvider,
                                AudibleFeedbackProvider audibleFeedbackProvider,
                                HapticFeedbackProvider hapticFeedbackProvider) {

        // Draw any visual feedback
        visualFeedbackProvider.clearDisplayedObstacles();
        if(feedbackConfiguration.isVisualFeedbackEnabled()) {
            for (Obstacle obstacle : obstacles) {
                visualFeedbackProvider.addObstacleToDisplay(feedbackConfiguration.getVisualFeedbackDistance(),
                        obstacle, feedbackConfiguration.determineSeverity(obstacle.getDistance()));
            }
        }

        // Get the closest obstacle
        Integer nearestObstacleDistance = 1000;
        for(Obstacle obstacle : obstacles){
            nearestObstacleDistance = Math.min(nearestObstacleDistance, obstacle.getDistance());
        }
        FeedbackSeverity severity = feedbackConfiguration.determineSeverity(nearestObstacleDistance);

        // Handle any audible feedback
        if(feedbackConfiguration.isAudibleFeedbackEnabled()){
            audibleFeedbackProvider.provideAudibleFeedback(feedbackConfiguration.getAudibleFeedbackVolume(), severity);
        }

        // Handle any haptic feedback
        if(feedbackConfiguration.isHapticFeedbackEnabled()){
            hapticFeedbackProvider.provideHapticFeedback(feedbackConfiguration.getHapticFeedbackIntensity(),
                    severity);
        }

    }
}
