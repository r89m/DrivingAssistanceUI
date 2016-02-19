package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by Richard on 27/04/2015.
 */
public class PWCInterfacePayloadError extends PWCInterfaceEventPayload {

    private PWCInterface.Error error;

    public PWCInterfacePayloadError(PWCInterface chairInterface, String response) {

        super(chairInterface, response);

        try {
            error = PWCInterface.Error.fromCode(Integer.valueOf(response.substring(2)));
        } catch (NumberFormatException e){
            error = PWCInterface.Error.UNKNOWN;
        }

        // Handle errors here
        switch(error){

            case LOG_SD_CARD_INACCESSIBLE:
                chairInterface.setLoggingStatus(false);
                break;

        }
    }

    public PWCInterface.Error getError(){

        return this.error;
    }
}
