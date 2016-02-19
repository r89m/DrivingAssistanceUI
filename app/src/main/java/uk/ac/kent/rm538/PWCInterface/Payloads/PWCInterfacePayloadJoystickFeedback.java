package uk.ac.kent.rm538.PWCInterface.Payloads;

import uk.ac.kent.rm538.PWCInterface.Hardware.JoystickPosition;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by rm538 on 21/08/2014.
 *
 * A class representing the payload for Joystick data from the Interface
 *
 */
public class PWCInterfacePayloadJoystickFeedback extends PWCInterfaceEventPayload {

    private boolean isAvoidanceEnabled;

    private static int parseJoystickValue(String inString){

        // Check for an empty string
        if("".equals(inString.trim())){
            return 0;
        }

        int pos =  Integer.parseInt(inString) - 100;    // This is necessary to undo the addition of 100 to values before being sent out
                                                        // which is required to ensure that all values are sent as 3 digits

        // Now map the position from the 0(min) - 128(neutral) - 255(max) range to a ±100
        return map(pos, 0, 255, -100, 100);
    }

    private static int map(int x, int in_min, int in_max, int out_min, int out_max)
    {
        return (x - in_min) * (out_max - out_min + 1) / (in_max - in_min + 1) + out_min;
    }

    private JoystickPosition inputPosition;
    private JoystickPosition outputPosition;

    public PWCInterfacePayloadJoystickFeedback(PWCInterface chairInterface, String response) {

        super(chairInterface, response);

        // Ensure the response is 14 characters long (2 for 'J:' + 12 for the data)
        response = String.format("%-14s", response);

        // Get Joystick position for input
        int inTurn = parseJoystickValue(response.substring(2, 5));
        int inSpeed = parseJoystickValue(response.substring(5, 8));
        inputPosition = new JoystickPosition(inTurn, inSpeed);

        // Get Joystick position for output
        int outTurn = parseJoystickValue(response.substring(8, 11));
        int outSpeed = parseJoystickValue(response.substring(11, 14));
        outputPosition = new JoystickPosition(outTurn, outSpeed);

        // Check whether the last character is set to 1 or 0
        isAvoidanceEnabled = !("1".equals(response.substring(14, 15)));
    }

    public JoystickPosition getInputPosition(){

        return inputPosition;
    }

    public JoystickPosition getOutputPosition(){

        return outputPosition;
    }

    public boolean isAvoidanceEnabled(){

        return isAvoidanceEnabled;
    }
}
