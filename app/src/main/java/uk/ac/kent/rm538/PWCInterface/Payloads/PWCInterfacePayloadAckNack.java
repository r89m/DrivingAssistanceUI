package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEvent;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceRequest;

/**
 * Created by rm538 on 21/08/2014.
 *
 * A class representing the payload for an ACK / NACK event from the Interface
 *
 */
public class PWCInterfacePayloadAckNack extends PWCInterfaceEventPayload {

    private PWCInterfaceRequest request;
    private boolean isAck = false;

    public PWCInterfacePayloadAckNack(PWCInterface chairInterface, String response, boolean isAck) {

        super(chairInterface, response);

        this.isAck = isAck;

        // Get Node and Type information from ACK / NACK
        int nodeId = Integer.parseInt(response.substring(1, 2));
        Node node = getResponseNodeFromId(nodeId);
        int requestType = Integer.parseInt(response.substring(3));

        // Create an instance of PWCInterfaceRequest that represents the request that was initially sent
        request = new PWCInterfaceRequest(PWCInterfaceEvent.EventType.values()[requestType], node);


        if(isAck()) {
            if (request.getType() == PWCInterfaceEvent.EventType.LOG_START) {
                // Check if the request was to start logging - if it was and we received an ACK set logging to be true
                chairInterface.setLoggingStatus(true);
            } else if(request.getType() == PWCInterfaceEvent.EventType.LOG_END){
                // If we've just received an ACK from a logging end event, then sent logging to be false
                chairInterface.setLoggingStatus(false);
            }
        }
    }

    public PWCInterfaceRequest getRequest(){

        return request;
    }

    public boolean isAck(){

        return isAck;
    }

    public boolean isNack(){

        return !isAck();
    }
}
