package uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;

/**
 * Created by Richard on 20/01/2016.
 */
public interface AudibleFeedbackProvider {

    public void provideAudibleFeedback(int volume, FeedbackSeverity severity);
}
