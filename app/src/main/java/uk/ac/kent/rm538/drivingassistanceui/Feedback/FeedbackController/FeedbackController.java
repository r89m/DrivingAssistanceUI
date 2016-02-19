package uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackController;

import java.util.List;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.AudibleFeedbackProvider;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.HapticFeedbackProvider;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.VisualFeedbackProvider;
import uk.ac.kent.rm538.PWCInterface.Hardware.Obstacle;

/**
 * Created by Richard on 20/01/2016.
 */
public interface FeedbackController {

    public void setFeedbackConfiguration(FeedbackConfiguration feedbackConfiguration);
    public void provideFeedback(List<Obstacle> obstacles,
                                VisualFeedbackProvider visualFeedbackProvider,
                                AudibleFeedbackProvider audibleFeedbackProvider,
                                HapticFeedbackProvider hapticFeedbackProvider);

}
