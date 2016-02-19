package uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfile;
import uk.ac.kent.rm538.drivingassistanceui.UserProfiles.UserProfileManager;

/**
 * Created by Richard on 20/01/2016.
 */
public class DefaultFeedbackConfiguration implements FeedbackConfiguration {

    private boolean isVisualFeedbackEnabled = true;
    private boolean isAudibleFeedbackEnabled = true;
    private boolean isHapticFeedbackEnable = true;

    private int visualFeedbackDistance = 300;
    private int audibleFeedbackVolume = 50;
    private int hapticFeedbackIntensity = 50;

    private LinkedHashMap<FeedbackSeverity, Integer> severityThresholds = new LinkedHashMap<>();

    public DefaultFeedbackConfiguration(){

        // Setup default severity thresholds
        int severityCount = FeedbackSeverity.values().length;
        for(FeedbackSeverity severity : FeedbackSeverity.values()){
            severityThresholds.put(severity, new Integer((severityCount - severity.ordinal()) * 50));
        }
    }


    @Override
    public boolean isVisualFeedbackEnabled() {
        return isVisualFeedbackEnabled;
    }

    @Override
    public boolean isAudibleFeedbackEnabled() {
        return isAudibleFeedbackEnabled;
    }

    @Override
    public boolean isHapticFeedbackEnabled() {
        return isHapticFeedbackEnable;
    }

    @Override
    public void setVisualFeedbackEnabled(boolean enabled) {

        this.isVisualFeedbackEnabled = enabled;
    }

    @Override
    public void setAudibleFeedbackEnabled(boolean enabled) {

        this.isAudibleFeedbackEnabled = enabled;
    }

    @Override
    public void setHapticFeedbackEnabled(boolean enabled) {

        this.isHapticFeedbackEnable = enabled;
    }

    @Override
    public int getVisualFeedbackDistance() {

        return visualFeedbackDistance;
    }

    @Override
    public int getAudibleFeedbackVolume() {
        return audibleFeedbackVolume;
    }

    @Override
    public int getHapticFeedbackIntensity() {
        return hapticFeedbackIntensity;
    }

    @Override
    public void setVisualFeedbackDistance(int maxDistance) {

        int sevCount = FeedbackSeverity.values().length + 1;
        int factor = maxDistance / sevCount;

        for(UserProfile profile : UserProfileManager.getUserProfiles()){
            for(FeedbackSeverity severity : FeedbackSeverity.values()){
                profile.getFeedbackConfiguration().setSeverityThreshold(severity, (sevCount - severity.ordinal()) * factor);
            }
        }

        this.visualFeedbackDistance = maxDistance;
    }

    @Override
    public void setAudibleFeedbackVolume(int volume) {

        this.audibleFeedbackVolume = volume;
    }

    @Override
    public void setHapticFeedbackIntensity(int intensity) {

        this.hapticFeedbackIntensity = intensity;
    }

    @Override
    public FeedbackSeverity determineSeverity(Integer distance) {

        // Iterate over the list of thresholds backwards. This makes determining the severity level easier
        ListIterator<FeedbackSeverity> listIterator =
                new ArrayList<>(severityThresholds.keySet()).listIterator(severityThresholds.size());

        while(listIterator.hasPrevious()){
            FeedbackSeverity key = listIterator.previous();
            Integer thresholdDistance = severityThresholds.get(key);
            Timber.i("%03d vs. %03d", thresholdDistance, distance);
            if(distance <= thresholdDistance){
                return key;
            }
        }
        // If we get this far, the nearest obstacle is far enough away to ignore
        return FeedbackSeverity.NONE;
    }

    @Override
    public void setSeverityThreshold(FeedbackSeverity severity, Integer distance) {

        Timber.i("Threshold for sevrity [%s] set to %03dcm", severity.toString(), distance);
        severityThresholds.put(severity, distance);
    }
}
