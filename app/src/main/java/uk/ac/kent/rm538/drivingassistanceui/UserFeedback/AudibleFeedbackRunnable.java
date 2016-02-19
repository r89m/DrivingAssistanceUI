package uk.ac.kent.rm538.drivingassistanceui.UserFeedback;

import android.media.ToneGenerator;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;

import static android.media.AudioManager.STREAM_ALARM;

/**
 * Created by Richard on 07/02/2016.
 */
public class AudibleFeedbackRunnable implements Runnable{

    private static AudibleFeedbackRunnable audibleFeedbackRunnable;
    private static Thread audibleFeedbackThread;
    private static long lastSeverityTimestamp = 0;


    public static void provideAudibleFeedback(int volume, FeedbackSeverity severity, int durationMs){

        Timber.i("Provide settings audible feedback");

        // If an existing thread is running, use it. Otherwise create a new thread
        if(audibleFeedbackRunnable != null && !audibleFeedbackRunnable.isComplete()){
            audibleFeedbackRunnable.setOutputParameters(volume, severity, durationMs);
        } else {
            audibleFeedbackRunnable = new AudibleFeedbackRunnable(volume, severity, durationMs);
            audibleFeedbackThread = new Thread(audibleFeedbackRunnable);
            audibleFeedbackThread.start();
        }
    }

    public static void stopAudibleFeedback(){

        if(audibleFeedbackRunnable != null){
            audibleFeedbackRunnable.stopThread();
        }
    }


    private int volume;
    private FeedbackSeverity severity;
    private int durationMs;
    private boolean stopThread = false;
    private boolean isComplete = false;
    private ToneGenerator toneGenerator = null;
    private long loopStartTimestamp;

    public AudibleFeedbackRunnable(int volume, FeedbackSeverity severity, int durationMs){

        setOutputParameters(volume, severity, durationMs);
    }

    @Override
    public void run() {
        loopStartTimestamp = System.currentTimeMillis();
        lastSeverityTimestamp = 0;

        // Repeat for 1 second
        while (!stopThread && (System.currentTimeMillis() - loopStartTimestamp < durationMs)){

            long currentTimestamp = System.currentTimeMillis();

            // Ensure that the tone is not played too often
            boolean playTone = false;
            if (currentTimestamp - lastSeverityTimestamp > severity.getTotalDuration()) {
                playTone = true;
                lastSeverityTimestamp = currentTimestamp;
            }

            if (playTone) {
                try {
                    if(toneGenerator == null) {
                        toneGenerator = new ToneGenerator(STREAM_ALARM, volume);
                    }
                    if(severity.getOnDuration() > 0) {
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_7, severity.getOnDuration());
                    }
                    Thread.sleep(100);
                } catch(InterruptedException e){
                    // We don't care about interrupted exceptions
                } catch (Exception ex) {
                    // Report any other errors
                    Timber.e(ex.getMessage());
                }
            }
        }
        if(toneGenerator != null){
            toneGenerator.release();
        }
        isComplete = true;
    }

    public void setOutputParameters(int volume, FeedbackSeverity severity, int durationMs){

        // Only continue if new parameters are arriving
        if(volume != this.volume || severity != this.severity || durationMs != this.durationMs) {
            this.volume = volume;
            this.severity = severity;
            this.durationMs = durationMs;
            loopStartTimestamp = System.currentTimeMillis();

            if (toneGenerator != null) {
                toneGenerator.release();
                toneGenerator = null;
            }
        }
    }

    public void stopThread(){

        stopThread = true;
    }

    public boolean isComplete(){
        return isComplete;
    }
}
