package uk.ac.kent.rm538.drivingassistanceui.TimeTrigger;


import android.media.TimedMetaData;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import hirondelle.date4j.DateTime;
import timber.log.Timber;

/**
 * Created by Richard on 20/01/2016.
 */
public class SimpleTimeTrigger implements TimeTrigger {

    private int startTime;
    private int endTime;
    private boolean enabled = false;

    public static TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");

    public void setStartTime(int hrs, int mins){

        setStartTime(((hrs * 60) + mins) * 60);
    }

    public void setStartTime(int startTimeSecs){

        this.startTime = startTimeSecs;
    }

    public int getStartTime() {

        return startTime;
    }

    public void setEndTime(int hrs, int mins){

        setEndTime(((hrs * 60) + mins) * 60);
    }

    public void setEndTime(int endTimeSecs) {

        this.endTime = endTimeSecs;
    }

    public int getEndTime() {
        return endTime;
    }

    @Override
    public DateTime getNextStartTriggerTime() {
        return getNextStartTriggerTime(DateTime.now(TIMEZONE_GMT));
    }

    @Override
    public DateTime getNextStartTriggerTime(DateTime startTime) {
        return getNextTriggerTime(startTime, this.startTime);
    }

    @Override
    public DateTime getNextEndTriggerTime() {
        return getNextEndTriggerTime(DateTime.now(TIMEZONE_GMT));
    }

    @Override
    public DateTime getNextEndTriggerTime(DateTime startTime) {
        return getNextTriggerTime(startTime, this.endTime);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {

        this.enabled = enabled;
    }

    @Override
    public boolean currentlyWithinTimeWindow() {
        return withinTimeWindow(DateTime.now(TIMEZONE_GMT));
    }

    @Override
    public boolean withinTimeWindow(DateTime checkTime){
        DateTime startOfDay = DateTime.now(TIMEZONE_GMT).getStartOfDay();
        DateTime startTime = getNextStartTriggerTime(startOfDay);
        DateTime endTime = getNextEndTriggerTime(startOfDay);

//        Timber.i("Window Time");
//        Timber.i("%s vs. %s - %b", checkTime.toString(), startTime.toString(), checkTime.gteq(startTime));
//        Timber.i("%s vs. %s - %b", checkTime.toString(), endTime.toString(), checkTime.lteq(endTime));

        return (checkTime.gteq(startTime) && checkTime.lteq(endTime));
    }

    private DateTime getNextTriggerTime(DateTime startTime, int triggerTime){

        // Account for local timezone
        int offset = TimeZone.getDefault().getRawOffset() / 1000;
        triggerTime += offset;

        int mins = (int) TimeUnit.SECONDS.toMinutes(triggerTime);
        int secs = (int) (triggerTime - TimeUnit.MINUTES.toSeconds(mins));

        DateTime checkTime = startTime.getStartOfDay().plus(0, 0, 0, 0, mins, secs, 0, DateTime.DayOverflow.Spillover);
        if(checkTime.gteq(startTime)){
            return checkTime;
        } else {
            //return checkTime;
            return checkTime.plusDays(1);
        }
    }
}
