package uk.ac.kent.rm538.PWCInterface.Hardware;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by rm538 on 11/08/2014.
 *
 * A class representing a zone within a node
 *
 */
public class Zone {

    /** Static Enums **/

    /** An Enum representing the different positions a zone can be located **/
    public static enum Position{
        UNKNOWN(""), FRONT_CENTRE("F"), BACK_CENTRE("B"), LEFT_CENTRE("L"), RIGHT_CENTRE("R"),
        FRONT_LEFT_CORNER("1"), FRONT_RIGHT_CORNER("2"), BACK_RIGHT_CORNER("3"), BACK_LEFT_CORNER("4");

        private final String code;  // How the locations are coded in the protocol (see Documentation)
        private Position(final String code){ this.code = code; }
        public String toString(){ return this.code; }
    }

    /** An Enum representing the different orientations a zone can be have **/
    public static enum Orientation{
        UNKNOWN("", 0),
        FORWARD("a", 0),          // 0°
        FORWARD_RIGHT("b", 45),   // 45°
        RIGHT("c", 90),           // 90°
        BACK_RIGHT("d", 135),     // 135°
        BACK("e", 180),           // 180°
        BACK_LEFT("f", 225),      // 225°
        LEFT("g", 270),           // 270°
        FORWARD_LEFT("h", 315);   // 315°

        private final String code;  // How the orientations are coded in the protocol (see Documentation)
        private final int angle;    // The angle at which this zone faces
        private Orientation(final String code, final int angle){
            this.code = code;
            this.angle = angle;
        }
        public String toString(){ return this.code; }
        public int getAngle(){ return this.angle; }
    }

    /** An Enum representing the different separation that a zone's sensors can take **/
    public static enum SensorSeparation{
        FUSED('F'), SEPARATE('S');

        private final char code;    // The character to send to the node to configure its Zone's sensor separation
        private SensorSeparation(final char code){
            this.code = code;
        }
        public char getCode(){ return this.code; }
    }


    /** Zone Properties **/

    private Node parentNode;
    private int zoneNumber;
    private Position position;
    private Orientation orientation;
    private ArrayList<Sensor> sensors;
    private HashMap<Integer, Integer> sensorByType = new HashMap<Integer, Integer>();
    private SensorSeparation sensorSeparation = SensorSeparation.SEPARATE;  // Sensors are separate by default

    public Zone(Node parentNode, int zoneNumber, Position position, Orientation orientation){

        this.parentNode = parentNode;
        this.zoneNumber = zoneNumber;
        this.position = position;
        this.orientation = orientation;
    }

    /**
     * Get the Node that this Zone belongs to
     *
     * @return Returns the Node that this Zone belongs to
     */
    public Node getParentNode(){

        return parentNode;
    }

    /**
     * Get the Orientation of this Zone
     *
     * @return Returns the Orientation of this Zone
     */
    public Orientation getOrientation(){

        return orientation;
    }

    /**
     * Get the Position of this Zone
     *
     * @return Returns the Position of this Zone
     */
    public Position getPosition(){

        return position;
    }

    /**
     * Get this Zone's zone number
     *
     * @return Returns this Zone's zone number
     */
    public int getZoneNumber(){

        return zoneNumber;
    }

    /**
     * Get a Sensor within this Zone by its SensorType
     *
     * @param sensorType The SensorType of the Sensor desired
     * @return Returns the Sensor within this Node that is of the specified SensorType, or null if no Sensor was found
     */
    public Sensor getSensorByType(Sensor.SensorType sensorType){

        return getSensorById(sensorType.getBitmask() + getZoneNumber());
    }

    /**
     * Get how the Sensors within this zone are separated, ie Separate or Fused
     *
     * @return Returns the SensorSeparation of the Sensors within this Zone
     */
    public SensorSeparation getSensorSeparation(){

        return this.sensorSeparation;
    }

    /**
     * Get a Sensor by it's SensorId
     *
     * @param sensorId The SensorId of the Sensor to return
     * @return Returns the Sensor with the given SensorId
     */

    public Sensor getSensorById(int sensorId){

        int checkZoneNum = sensorId & 0x3;   // Bitmask = 0x3  -> 11 (as we need to get the 1st and second bits)
        int sensorType = sensorId & 0x1C;    // Bitmask = Ox1C -> 11100 (as we need to get the 3rd, 4th & 5th bits

        // Check that the sensorId given belongs to this Zone
        if(getZoneNumber() != checkZoneNum){
            return null;
        }

        Integer index = sensorByType.get(sensorType);
        if(index != null){
            return sensors.get(index);
        } else {
            return null;
        }
    }

    /**
     * Set the Sensors within a Zone.  Shouldn't be called directly, but must be public
     * so that instances of PWCInterface can use it
     *
     * @param inSensors A List of Sensors to be added to the Zone
     */
    public void setSensors(ArrayList<Sensor> inSensors){

        sensors = inSensors;
        int index = 0;
        for(Sensor sensor : sensors){
            sensorByType.put(sensor.getType().getBitmask(), index);
            if(sensor.getType() == Sensor.SensorType.FUSED){
                setSeparation(SensorSeparation.FUSED);
            }
            index++;
        }
    }

    /**
     * Set the separation of Sensors within a Zone. Shouldn't be called directly, but must be public
     * so that instances of PWCInterface can use it
     *
     * @param sensorSeparation The SensorSeparation of Sensors within this Zone
     */
    public void setSeparation(SensorSeparation sensorSeparation){

        this.sensorSeparation = sensorSeparation;
    }
}
