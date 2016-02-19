package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceRequest;

/**
 * Created by rm538 on 26/08/2014.
 *
 * A class representing the payload for a timeout event from the Interface
 *
 */
public class PWCInterfacePayloadTimeout extends PWCInterfaceEventPayload {

    private PWCInterfaceRequest request;

    public PWCInterfacePayloadTimeout(PWCInterface chairInterface, PWCInterfaceRequest request){

        super(chairInterface, String.format("Request timed out - Type: %1$s", request.getType()));
        this.request = request;

        Node timeoutNode = getNode();
        // Force this event's Node to indicate it has timed out
        if(timeoutNode != null){
            timeoutNode.setTimedOut(true);
        }
    }

    /**
     * Return the Node that this event relates to
     *
     * @return The Node that this event relates to
     */
    @Override
    public Node getNode(){

        return request.getNode();
    }

    /**
     * Return the PWCInterfaceRequest that this event relates to
     *
     * @return The PWCInterfaceRequest that this event relates to
     */
    public PWCInterfaceRequest getRequest(){

        return request;
    }
}
