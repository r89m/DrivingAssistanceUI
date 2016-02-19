package uk.ac.kent.rm538.drivingassistanceui.PWC;

import butterknife.internal.ListenerClass;
import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackSeverity.FeedbackSeverity;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.Providers.HapticFeedbackProvider;

/**
 * Created by Richard on 21/01/2016.
 */
public class PWCHapticFeedback implements HapticFeedbackProvider {

    private static PWCHapticFeedback instance;
    private static Thread endHapticThread;
    private static EndHapticRunnable endHapticRunnable;

    private PWCInterface pwcInterface;
    private long lastUpdateTime = 0;
    private long maxUpdateRate = 500;

    public static PWCHapticFeedback getInstance(){

        if(instance == null){
            instance = new PWCHapticFeedback(DrivingAssistanceApp.getPWCInterface());
        }

        return instance;
    }

    private static void startEndHapticThread(){

        killEndHapticThread();

        endHapticRunnable = new EndHapticRunnable();
        endHapticThread = new Thread(endHapticRunnable);
        endHapticThread.start();
    }

    private static void killEndHapticThread(){

        if(endHapticRunnable != null){
            endHapticRunnable.cancel();
        }
    }

    private static class EndHapticRunnable implements Runnable{

        private boolean killThread = false;

        public EndHapticRunnable(){

        }

        public void cancel(){

            killThread = true;
        }

        @Override
        public void run() {

            // Wait 1 second, then send signal to turn of haptic if this thread has not been cancelled
            try{
                Thread.sleep(1000);
            } catch (Exception ex){
                // We don't mind about this
            }
            if(!killThread){
                PWCHapticFeedback.getInstance().provideHapticFeedback(0, FeedbackSeverity.NONE, false);
            }
        }
    }

    public PWCHapticFeedback(PWCInterface pwcInterface){

        this.pwcInterface = pwcInterface;
    }

    private void provideHapticFeedback(int intensity, FeedbackSeverity severity, boolean startNewThread){

        long currentTimestamp = System.currentTimeMillis();

        if(currentTimestamp - lastUpdateTime > maxUpdateRate) {
            Timber.i("Haptic delta time: %d - %d = %d", currentTimestamp, lastUpdateTime, currentTimestamp - lastUpdateTime);
            Thread.dumpStack();
            lastUpdateTime = currentTimestamp;

            // Map 0 - 10 (easy to use) intensity to 50 - 200 (calibrated PWC values)
            int oldMin = 0;
            int oldMax = 10;
            int newMin = 50;
            int newMax = 200;
            int sendIntensity;

            if(intensity == 0){
                sendIntensity = 0;
            } else {
                sendIntensity = (((intensity - oldMin) * (newMax - newMin)) / (oldMax - oldMin)) + newMin;
            }

            Timber.i("Haptic Feedback: %d - %d:%d", intensity, severity.getOnDuration(), severity.getOffDuration());

            try {
                // Kill the old thread
                killEndHapticThread();

                pwcInterface.hapticFeedback(sendIntensity, severity.getOnDuration(), severity.getOffDuration());

                if(startNewThread) {
                    // Start end haptic thread
                    startEndHapticThread();
                }
            } catch (Exception e){
                Timber.e(e.getMessage());
            }
        }

    }

    @Override
    public void provideHapticFeedback(int intensity, FeedbackSeverity severity) {

       provideHapticFeedback(intensity, severity, true);
    }
}
