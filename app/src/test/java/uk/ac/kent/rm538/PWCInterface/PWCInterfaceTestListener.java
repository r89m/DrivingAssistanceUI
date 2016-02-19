package uk.ac.kent.rm538.PWCInterface;

/**
 * Created by Richard on 14/01/2016.
 */
public class PWCInterfaceTestListener implements PWCInterfaceListener {

    PWCInterfaceEvent lastEvent;

    @Override
    public void onPWCInterfaceEvent(PWCInterfaceEvent e) {

        lastEvent = e;
    }

    public PWCInterfaceEvent getLastEvent() {

        return lastEvent;
    }
}