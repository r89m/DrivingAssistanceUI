package uk.ac.kent.rm538.drivingassistanceui.TimeTrigger;

import hirondelle.date4j.DateTime;

/**
 * Created by Richard on 20/01/2016.
 */
public interface TimeTrigger {

    public DateTime getNextStartTriggerTime();
    public DateTime getNextStartTriggerTime(DateTime startTime);
    public DateTime getNextEndTriggerTime();
    public DateTime getNextEndTriggerTime(DateTime startTime);
    public boolean isEnabled();
    public void setEnabled(boolean enabled);
    public boolean withinTimeWindow(DateTime checkTime);
    public boolean currentlyWithinTimeWindow();

}
