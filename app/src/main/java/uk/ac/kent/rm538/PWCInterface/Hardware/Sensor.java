package uk.ac.kent.rm538.PWCInterface.Hardware;

/**
 * Created by rm538 on 11/08/2014.
 *
 * A class that represents a physical sensor on the chair.
 *
 */
public class Sensor {

    /* Static Enums and Methods */

    /** An Enum representing the different types of sensor that may be used **/
    public enum SensorType {
        ULTRASONIC("US", 4), INFRARED("IR", 8), FUSED("F", 12); //, ADDITIONAL_SENSOR_TYPE(16), AND_ANOTHER(32), etc;

        private final String name;
        private final int bitmask;
        SensorType(final String name, final int code){
            this.name = name;
            this.bitmask = code;

        }
        public String getName(){ return this.name; }
        public int getBitmask(){ return this.bitmask; }
    }

    /** An Enum representing the different formats sensors can use to represent their data **/
    public enum SensorDataFormat{
        UNKNOWN('-', 0), BYTE('B', 2), WORD('W', 4), DWORD('D', 8), LOGIC('L', 1);

        private final char representation;  // How this data format is represented in a response from a node reporting its configuration
        private final int charLength;       // The number of characters data of this type occupies in a response from a node reporting its current values
        SensorDataFormat(final char representation,  int charLength){
            this.representation = representation;
            this.charLength = charLength;
        }
        public char getRepresentation(){ return this.representation; }
        public int getCharLength(){ return this.charLength; }
    }

    /** An Enum representing the different ways of interpreting sensor data **/
    public enum SensorDataInterpretation{
        // UNKNOWN MUST BE FIRST! The order of the rest doesn't matter
        UNKNOWN('u', " - "), DISABLED('0', "D"), CM('c', "cm"), RAW('r', "raw"), THRESHOLD('1', "T");

        private final char representation;  // What to send to a node to configure its sensors to report their data in this way
        private final String suffix;        // A string to display what interpretation is being used
        SensorDataInterpretation(final char representation, final String suffix){
            this.representation = representation;
            this.suffix = suffix;
        }
        public char getRepresentation(){ return this.representation; }
        public String getSuffix(){ return this.suffix; }
    }

    /**
     * Get the SensorDataFormat object that is represented by the given character
     *
     * @param dataFormatChar A character that represents the desired SensorDataFormat
     * @return Returns a SensorDataFormat that matches the given character
     */
    public static SensorDataFormat getDataFormatFromCharacter(char dataFormatChar){

        // Cycle through all the data formats to see which one is represented by the given character
        for(SensorDataFormat format : SensorDataFormat.values()){
            // If this character matches, return the SensorDataFormat
            if(dataFormatChar == format.getRepresentation()){
                return format;
            }
        }

        // If the characters was not matched, return an UNKNOWN format
        return SensorDataFormat.UNKNOWN;
    }

    /**
     * Get the SensorDataInterpretation object that is represented by the given character
     *
     * @param dataInterpretationChar A character that represents the desired SensorDataInterpretation
     * @return Returns a SensorDataInterpretation that matches the given character
     */
    public static SensorDataInterpretation getDataInterpretationFromCharacter(char dataInterpretationChar) {

        SensorDataInterpretation responseInterpretation = SensorDataInterpretation.UNKNOWN;

        // Cycle through all the data representations to see which one is represented by the given character
        for (SensorDataInterpretation interpretation : SensorDataInterpretation.values()) {
            if (dataInterpretationChar == interpretation.getRepresentation()) {
                responseInterpretation = interpretation;
                break;
            }
        }

        return responseInterpretation;
    }

    /* Sensor properties */

    private SensorType sensorType;
    private SensorDataFormat dataFormat = SensorDataFormat.UNKNOWN;
    private SensorDataInterpretation dataInterpretation = SensorDataInterpretation.UNKNOWN;
    private boolean faulty = false;
    private int currentValue = 0;
    private long valueUpdatedTimestamp;

    public Sensor(SensorType sensorType){
        this.sensorType = sensorType;
    }

    /**
     * Get the data format that is used by this sensor.
     * This determines the length and and how to parse responses from the sensor
     *
     *  @return Returns the SensorDataFormat that this sensor uses to send data
     */
    public SensorDataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * Get the data interpretation for this sensor.
     * This indicates how the sensor's value should be interpreted
     *
     * @return Returns the SensorDataInterpretation that represents how this sensor's data should be interpreted
     */
    public SensorDataInterpretation getDataInterpretation() {
        return dataInterpretation;
    }

    /**
     * Get this sensors type.
     * Identifies whether this sensor is using Infrared / Ultrasound etc.
     *
     * @return Returns the SensorType of this sensor
     */
    public SensorType getType(){

        return sensorType;
    }

    /**
     * Get the sensor's current value, as given by the last response from the chair.
     *
     * Calling this method DOES NOT automatically request new data from the chair - this should be done
     * by calling requestNodeCurrentData on an instance of PWCInterface
     *
     * @return Returns the sensor's current value
     */
    public int getCurrentValue(){

        return currentValue;
    }

    /**
     * Get the sensor's current boolean value, as given by the last response from the chair.
     * This should only be used for Sensors of Type THRESHOLD, as it's value will almost always return true
     * for any other sensor type
     *
     * Calling this method DOES NOT automatically request new data from the chair - this should be done
     * by calling requestNodeCurrentData on an instance of PWCInterface
     *
     * @return
     */
    public boolean getCurrentBoolValue(){

        return (getCurrentValue() == 1);
    }

    /**
     * Get how long it has been since this sensor's current value was updated.
     *
     * @return Returns how many milliseconds have elapsed since the sensor's data was updated from the wheelchair
     */
    public int getCurrentValueAge(){

        return (int) (System.currentTimeMillis() - valueUpdatedTimestamp);
    }

    /**
     * Check whether the data from the chair is too old to be useful.
     *
     * @param maxAgeMs  The maximum acceptable age of the sensor's data in milliseconds.
     * @return Returns true if the sensor's data was updated longer than maxAgeMs ago, false otherwise
     */
    public boolean hasCurrentValueExpired(int maxAgeMs){

        return (getCurrentValueAge() > maxAgeMs);
    }

    /**
     * Parse a string representing this strings data from the chair. Shouldn't be called directly, but must be public
     * so that instances of PWCInterface can use it
     *
     * @param currentSensorData A hex encoded string representing this sensor's current data
     */
    public void parseDataString(String currentSensorData) {

        valueUpdatedTimestamp = System.currentTimeMillis();
        currentValue = Integer.parseInt(currentSensorData, 16); // Get the integer value of the hex input
    }

    /**
     * Set this sensor's data format, based on a character representing that format; ie B, W, D etc.
     *
     * @param dataFormatChar A character that represents the desired SensorDataFormat
     */
    public void setDataFormat(char dataFormatChar){

        setDataFormat(Sensor.getDataFormatFromCharacter(dataFormatChar));
    }

    /**
     * Set this sensor's data format to a given SensorDataFormat
     *
     * @param dataFormat The desired SensorDataFormat
     */
    public void setDataFormat(SensorDataFormat dataFormat){

        this.dataFormat = dataFormat;
    }

    /**
     * Set this sensor's data interpretation, based on a character representing that interpretation; ie r, cm etc.
     *
     * @param dataInterpretationChar A character that represents the desired SensorDataInterpretation
     */
    public void setDataInterpretation(char dataInterpretationChar){

        setDataInterpretation(Sensor.getDataInterpretationFromCharacter(dataInterpretationChar));
    }

    /**
     * Set this sensor's data interpretation to a given SensorDataInterpretation
     *
     * @param dataInterpretation The desired SensorDataInterpretation
     */
    public void setDataInterpretation(SensorDataInterpretation dataInterpretation){

        this.dataInterpretation = dataInterpretation;
    }

    /**
     * Set whether or not this sensor is faulty. Shouldn't be called directly but must be made public so that
     * instances of PWCInterface can use it
     *
     * @param faulty Whether or not the sensor appears to be faulty
     */
    public void setFaulty(boolean faulty) {
        this.faulty = faulty;
    }

    /**
     * Get whether or not this sensor appears to be faulty
     *
     * @return Returns true if the sensor appears to be faulty, false otherwise
     */
    public boolean isFaulty(){

        return this.faulty;
    }
}
