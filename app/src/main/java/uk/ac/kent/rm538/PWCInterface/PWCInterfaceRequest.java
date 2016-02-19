package uk.ac.kent.rm538.PWCInterface;

import uk.ac.kent.rm538.PWCInterface.Hardware.Node;;

/**
 * Created by rm538 on 21/08/2014.
 *
 * A class representing a request sent to the wheelchair via the Interface
 *
 */
public class PWCInterfaceRequest {

    private PWCInterfaceEvent.EventType type;
    private Node node;
    private long requestTime;

    public PWCInterfaceRequest(PWCInterfaceEvent.EventType type, Node node){

        this.type = type;
        this.node = node;
        requestTime = System.currentTimeMillis();
    }

    /**
     * Return the EventType of this request
     *
     * @return The EventType of this request
     */
    public PWCInterfaceEvent.EventType getType(){

        return type;
    }

    /**
     * Return the Node that this request relates to
     *
     * @return The Node that this request relates to
     */
    public Node getNode(){

        return node;
    }

    /**
     * Return whether or not this request has timed out. That is, if it was sent over timeoutSecs ago and has not yet
     * received a response
     *
     * @param timeoutSecs
     *
     * @return True if this request has timed out
     */
    public boolean hasTimedout(int timeoutSecs){

        return ((System.currentTimeMillis() - requestTime) > (timeoutSecs * 1000));
    }
}
