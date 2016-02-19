package uk.ac.kent.rm538.PWCInterface;

/**
 * Created by rm538 on 06/08/2014.
 *
 * A class representing an event that has been received by the wheelchair interface
 */
public class PWCInterfaceEvent {

    public static enum EventType {
        PARSE_ERROR, UNKNOWN, FIRMWARE_INFO, BUS_SCAN, ACK, NACK, TIMEOUT, HAPTIC_FEEDBACK,
        NODE_CURRENT_DATA, NODE_CONFIGURATION, NODE_DATA_FORMAT, NODE_THRESHOLDS, NODE_MODE,
        NODE_FIRMWARE_INFO, JOYSTICK_FEEDBACK, SET_TIME, LOG_START, LOG_END, LOG_EVENT, LOG_LIST,
        CONNECTED, DISCONNECTED, BOOT_COMPLETE, OBSTACLE, SENSITIVITY, OBSTACLES_COMPLETE, ERROR}

    private PWCInterface chairInterface;
    private EventType type;
    private PWCInterfaceEventPayload payload;

    public PWCInterfaceEvent(PWCInterface chairInterface, EventType type, PWCInterfaceEventPayload payload){

        this.chairInterface = chairInterface;
        this.type = type;
        this.payload = payload;
    }

    /**
     * Get the Interface that raised this event. This allows support for multiple interfaces in one application
     *
     * @return The Interface that raised this event
     */
    public PWCInterface getChairInterface(){

        return chairInterface;
    }

    /**
     * Get the EventType of this event.
     *
     * @return EventType of this event
     */
    public EventType getType(){

        return type;
    }

    /**
     * Return the payload for this event, an object which contains much more specific information depending on the type of event.
     *
     * @return The Payload for this event.
     */
    public PWCInterfaceEventPayload getPayload(){

        return payload;
    }
}
