package uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;

/**
 * Created by Richard on 21/01/2016.
 */
public interface HapticFeedbackProvider {

    public void provideHapticFeedback(int intensity, FeedbackSeverity severity);

}
