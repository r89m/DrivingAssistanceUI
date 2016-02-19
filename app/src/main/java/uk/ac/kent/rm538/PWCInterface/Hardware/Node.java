package uk.ac.kent.rm538.PWCInterface.Hardware;

import java.util.ArrayList;

/**
 * Created by rm538 on 11/08/2014.
 *
 * A class that represents a physical node on the chair
 *
 */
public class Node {

    // Maximum number of zones a node can contain
    public static final int MAX_ZONES = 3;

    private int id;
    private ArrayList<Zone> zones = new ArrayList<Zone>(MAX_ZONES);
    private ArrayList<Integer> sensorDataOrder = new ArrayList<Integer>();
    private boolean connected = false;
    private boolean dataFormatKnown = true; // TODO: Change this to false for production
    private boolean hasTimedOut = false;
    private NodeVersion nodeVersion;

    public Node(int nodeId){

        id = nodeId;

        // Each node has a maximum of 3 zones - lets create some empty ones ready for use later
        for(int i = 0; i < MAX_ZONES; i++){
            zones.add(i, new Zone(this, i+1, Zone.Position.UNKNOWN, Zone.Orientation.UNKNOWN));
        }
    }

    /**
     * Get whether a node has timed out and stopped responding to requests.
     *
     * @return Returns true if the node appears to have timed out
     */
    public boolean hasTimedOut(){

        return hasTimedOut;
    }

    /**
     * Get whether the node is connected to the bus, as given by the last response from the chair.
     *
     * Calling this method DOES NOT automatically check whether the node is still on the bus - this should be done
     * by calling checkNodeExistsOnBus on an instance of PWCInterface
     *
     * @return
     */
    public boolean isConnectedToBus(){

        return connected;
    }

    /**
     * Set whether or not the node is connected to the bus. Shouldn't be called directly, but must be public
     * so that instances of PWCInterface can use it
     *
     * @param connectedToBus Whether or not the node is connected to the bus
     */
    public void setConnectedToBus(boolean connectedToBus){

        connected = connectedToBus;
    }

    /**
     * Set whether or not the node has timed out and stopped responding to requests. Shouldn't be called directly,
     * but must be public so that instances of PWCInterface can use it
     *
     * @param timedOut Whether or not the node has timed out
     */
    public void setTimedOut(boolean timedOut){

        hasTimedOut = timedOut;
    }

    /**
     * Get the Node's ID.
     *
     * @return Returns the Node's ID
     */
    public int getId(){

        return this.id;
    }

    /**
     * Adds a sensor to the next available slot in the sensor order list. Shouldn't be called directly, but must be public
     * so that instances of PWCInterface can use it
     *
     * @param sensorId The ID of the sensor to insert at this position
     */
    public void addSensorToDataRequestList(Integer sensorId){

        sensorDataOrder.add(sensorId);
    }

    /**
     * Clears the sensor order list, so that any new entries start from the beginning. Shouldn't be called directly,
     * but must be public so that instances of PWCInterface can use it
     */
    public void clearSensorDataRequestList(){

        sensorDataOrder.clear();
    }

    /**
     * Return this Node's firmware version
     *
     * @return This Node's firmware version
     */
    public NodeVersion getFirmwareVersion(){

        return nodeVersion;
    }


    public String getFirmwareVersionString(){

        if(getFirmwareVersion() != null){
            return getFirmwareVersion().toString();
        } else {
            return "??.??";
        }
    }

    /**
     * Get the number of sensors found in this Node.
     *
     * @return The number of sensors in this Node
     */
    public int getSensorCount(){

        return sensorDataOrder.size();
    }

    /**
     * Get the sensor that is found at the given position within the sensor order list. Shouldn't be called directly,
     * but must be public so that instances of PWCInterface can use it
     *
     * @param index The position with the sensor list to return
     * @return Returns the Sensor that is at the position index in the order list
     */
    public Sensor getSensorFromDataOrder(int index){

        return getSensor(sensorDataOrder.get(index));
    }

    /**
     * Get a Sensor given its ID.
     *
     * @param sensorId The ID of the sensor to retrieve
     * @return Returns the Sensor that the ID represents
     */
    public Sensor getSensor(int sensorId){

        return getZone(sensorId).getSensorById(sensorId);
    }

    /**
     * Get a Zone given its zone number.
     *
     * @param zoneNumber The zone number of the Zone to retrieve
     * @return Returns the Zone with the given zone number
     */
    public Zone getZone(int zoneNumber){

        int zoneNumberMasked = zoneNumber  & 0x3; // Bitmask = Ox3 -> 11 (as we only want to get the first 2 bits)
        if(zoneNumberMasked > 0) {
            return zones.get(zoneNumberMasked - 1);
        } else {
            return null;
        }
    }

    /**
     * Get whether or not we know the data format of the sensors within this Node. The data format must be known
     * before values from the sensors can be parsed correctly.
     *
     * @return Returns true if the sensor data format is known, false otherwise
     */
    public boolean isDataFormatKnown(){

        return dataFormatKnown;
    }

    /**
     * Set whether or not the data format is known. Shouldn't be called directly, but must be public
     * so that instances of PWCInterface can use it
     *
     * @param dataFormatKnown Whether or not the data format is known
     */
    public void setDataFormatKnown(boolean dataFormatKnown){

        this.dataFormatKnown = dataFormatKnown;
    }

    /**
     * Set the firmware version for this Node
     *
     * @param nodeVersion   The Node's Firmware Version
     */
    public void setFirmwareVersion(NodeVersion nodeVersion){

        this.nodeVersion = nodeVersion;
    }

    /**
     * Set the zone number for a given Zone
     *
     * @param zoneNumber The new Zone Number
     * @param newZone The Zone whose zone number is about to be changed
     */
    public void setZone(int zoneNumber, Zone newZone){

        zones.set(zoneNumber - 1, newZone);
    }
}
