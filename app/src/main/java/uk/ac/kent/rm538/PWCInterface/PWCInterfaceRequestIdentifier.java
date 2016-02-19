package uk.ac.kent.rm538.PWCInterface;

/**
 * Created by rm538 on 26/08/2014.
 *
 * A lightweight class used to uniquely identify requests of a certain EventType to a certain Node.
 *
 * Allows previous requests to be retrieved and checked to see if they have timed-out.
 *
 */
public class PWCInterfaceRequestIdentifier {

    private PWCInterfaceEvent.EventType type;
    private int nodeId;

    public PWCInterfaceRequestIdentifier(PWCInterfaceEvent.EventType type, int nodeId){

        this.type = type;
        this.nodeId = nodeId;
    }

    public PWCInterfaceEvent.EventType getType(){

        return type;
    }


    /*      equals and hashCode must be implemented so that the correct PWCInterfaceRequest
            can be retrieved from the request list HashMap

            these functions were automatically generated using IntelliJ
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PWCInterfaceRequestIdentifier that = (PWCInterfaceRequestIdentifier) o;

        if (nodeId != that.nodeId) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + nodeId;
        return result;
    }
}
