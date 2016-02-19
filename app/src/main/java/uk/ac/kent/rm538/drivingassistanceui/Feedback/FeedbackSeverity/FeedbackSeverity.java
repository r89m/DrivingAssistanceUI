package uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity;

/**
 * Created by Richard on 20/01/2016.
 */
public enum FeedbackSeverity {
    // Ensure this list is in order of severity
    NONE(0, 1000), LOW(0, 1000), MILD(0, 1000), MODERATE(250, 750), SEVERE(600, 400);

    private int onDuration;
    private int offDuration;
    FeedbackSeverity(int onDuration, int offDuration){

        this.onDuration = onDuration;
        this.offDuration = offDuration;
    }

    public int getOnDuration(){ return this.onDuration;}
    public int getOffDuration(){ return this.offDuration;}
    public int getTotalDuration(){ return getOnDuration() + getOffDuration();}
};
