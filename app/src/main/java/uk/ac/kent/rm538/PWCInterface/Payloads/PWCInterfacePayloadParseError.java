package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by rm538 on 06/08/2014.
 *
 * A class representing the payload for an error event from the Interface - usually caused by a malformed response
 *
 */
public class PWCInterfacePayloadParseError extends PWCInterfaceEventPayload {

    private Exception exception;

    public PWCInterfacePayloadParseError(PWCInterface chairInterface, String response, Exception e) {

        super(chairInterface, response);
        exception = e;
    }

    /**
     * Return the exception that caused this event to be raised
     *
     * @return The Exception that caused this event to be raised
     */
    public Exception getException(){

        return exception;
    }

    /**
     * Return the error message from the exception that caused this event to be raised
     *
     * @return The error message from the Exception that caused this event to be raised
     */
    public String getErrorMessage(){

        return exception.getClass().getName() + ": " + exception.getMessage();
    }
}
