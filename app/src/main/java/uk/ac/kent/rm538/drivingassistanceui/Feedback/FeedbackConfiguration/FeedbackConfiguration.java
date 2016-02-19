package uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration;

import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;

/**
 * Created by Richard on 20/01/2016.
 */
public interface FeedbackConfiguration {

    public boolean isVisualFeedbackEnabled();
    public boolean isAudibleFeedbackEnabled();
    public boolean isHapticFeedbackEnabled();

    public void setVisualFeedbackEnabled(boolean enabled);
    public void setAudibleFeedbackEnabled(boolean enabled);
    public void setHapticFeedbackEnabled(boolean enabled);

    public int getVisualFeedbackDistance();
    public int getAudibleFeedbackVolume();
    public int getHapticFeedbackIntensity();

    public void setVisualFeedbackDistance(int maxDistance);
    public void setAudibleFeedbackVolume(int volume);
    public void setHapticFeedbackIntensity(int intensity);

    public FeedbackSeverity determineSeverity(Integer distance);
    public void setSeverityThreshold(FeedbackSeverity severity, Integer distance);

}
