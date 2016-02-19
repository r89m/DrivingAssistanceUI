package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by rm538 on 06/08/2014.
 *
 * A class representing the payload for a firmware information event from the Interface
 *
 */
public class PWCInterfacePayloadFirmwareInfo extends PWCInterfaceEventPayload {

    private String version;

    public PWCInterfacePayloadFirmwareInfo(PWCInterface chairInterface, String response) throws Exception{

        super(chairInterface, response);

        version = response.substring(2);
    }

    public String getVersion(){

        return version;
    }
}
