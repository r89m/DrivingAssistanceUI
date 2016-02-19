package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by rm538 on 02/09/2014.
 */
public class PWCInterfacePayloadUnknown extends PWCInterfaceEventPayload {
    public PWCInterfacePayloadUnknown(PWCInterface chairInterface, String response) {
        super(chairInterface, response);
    }
}
