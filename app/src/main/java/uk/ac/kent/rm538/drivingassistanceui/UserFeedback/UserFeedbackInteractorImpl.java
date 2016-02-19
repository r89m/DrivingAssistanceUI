package uk.ac.kent.rm538.drivingassistanceui.UserFeedback;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.net.URI;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.Hardware.Zone;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEvent;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceListener;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadBusScan;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadJoystickFeedback;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadParseError;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.PWC.PWCWebsocketCommsProvider;

/**
 * Created by Richard on 23/11/2015.
 */
public class UserFeedbackInteractorImpl implements UserFeedbackInteractor, PWCInterfaceListener {

    private PWCWebsocketCommsProvider.ErrorListener socketErrorListener;
    private PWCInterfaceListener pwcInterfaceListener;

    UserFeedbackInteractorImpl(PWCInterfaceListener interfaceListener){

        pwcInterfaceListener = interfaceListener;
    }

    @Override
    public void connect() {

        DrivingAssistanceApp.connectComms();
        DrivingAssistanceApp.getPWCInterface().registerListener(this);

        // Once the chair is connected, check which nodes are present
        for(int i = 1; i <= 9; i++){
            Timber.i("Check node %d", i);
//            DrivingAssistanceApp.getPWCInterface().checkNodeExistsOnBus(i);
        }
    }

    @Override
    public void disconnect() {

        DrivingAssistanceApp.disconnectComms();
        DrivingAssistanceApp.getPWCInterface().unregisterListener(this);
    }

    @Override
    public void onPWCInterfaceEvent(final PWCInterfaceEvent e) {

        Timber.i("Interactor recevied event of type: %s", e.getType());

        switch(e.getType()){

            case CONNECTED:
                // Once the chair is connected, check which nodes are present
                for(int i = 1; i <= 9; i++){
                    Timber.i("Check node %d", i);
                    DrivingAssistanceApp.getPWCInterface().checkNodeExistsOnBus(i);
                }
                break;

            case BUS_SCAN:
                // Check if the node exists. If so, get it's configuration
                PWCInterfacePayloadBusScan scanPayload = (PWCInterfacePayloadBusScan) e.getPayload();
                Node node = scanPayload.getNode();
                // Setup the node configurations

                switch(node.getId()){

                    case 1:
                        node.setZone(1, new Zone(node, 1, Zone.Position.FRONT_RIGHT_CORNER, Zone.Orientation.FORWARD));
                        node.setZone(2, new Zone(node, 2, Zone.Position.FRONT_RIGHT_CORNER, Zone.Orientation.RIGHT));
                        node.setZone(3, new Zone(node, 3, Zone.Position.FRONT_RIGHT_CORNER, Zone.Orientation.FORWARD_RIGHT));
                        Timber.i("Configured node #%d", node.getId());
                        break;

                    case 2:
                        node.setZone(1, new Zone(node, 1, Zone.Position.FRONT_LEFT_CORNER, Zone.Orientation.LEFT));
                        node.setZone(2, new Zone(node, 2, Zone.Position.FRONT_LEFT_CORNER, Zone.Orientation.FORWARD_LEFT));
                        node.setZone(3, new Zone(node, 3, Zone.Position.FRONT_LEFT_CORNER, Zone.Orientation.FORWARD));
                        Timber.i("Configured node #%d", node.getId());
                        break;

                    case 3:
                        node.setZone(1, new Zone(node, 1, Zone.Position.BACK_CENTRE, Zone.Orientation.BACK));
                        node.setZone(2, new Zone(node, 2, Zone.Position.BACK_LEFT_CORNER, Zone.Orientation.LEFT));
                        node.setZone(3, new Zone(node, 3, Zone.Position.BACK_LEFT_CORNER, Zone.Orientation.BACK_LEFT));
                        Timber.i("Configured node #%d", node.getId());
                        break;

                    case 4:
                        node.setZone(3, new Zone(node, 3, Zone.Position.BACK_RIGHT_CORNER, Zone.Orientation.RIGHT));
                        node.setZone(2, new Zone(node, 2, Zone.Position.BACK_RIGHT_CORNER, Zone.Orientation.BACK_RIGHT));
                        Timber.i("Configured node #%d", node.getId());
                        break;
                }


                break;

            case PARSE_ERROR:
                PWCInterfacePayloadParseError errorPayload = (PWCInterfacePayloadParseError) e.getPayload();
                Timber.e(errorPayload.getErrorMessage());
                errorPayload.getException().printStackTrace();
                break;

        }

        // Use a handler to ensure that onPWCInterfaceEvent is called on the UI thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(pwcInterfaceListener != null) {
                    pwcInterfaceListener.onPWCInterfaceEvent(e);
                } else {
                    Timber.i("Interactor's listener was null");
                }
            }
        };
        mainHandler.post(runnable);
    }
}
