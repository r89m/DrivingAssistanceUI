package uk.ac.kent.rm538.PWCInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.Hardware.LogFile;
import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadAckNack;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadBusScan;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadConnectionStatus;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadError;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadFirmwareInfo;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadJoystickFeedback;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadLogFileInfo;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeConfiguration;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeCurrentData;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeDataFormat;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeFirmwareInfo;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadObstacle;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadParseError;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadTimeout;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadUnknown;

import static uk.ac.kent.rm538.PWCInterface.PWCInterfaceEvent.EventType;

/**
 * Created by rm538 on 06/08/2014.
 *
 */
public class PWCInterface {

    private ArrayList<PWCInterfaceListener> listeners = new ArrayList<PWCInterfaceListener>();
    private PWCInterfaceCommunicationProvider commsProvider;
    private ArrayList<Node> nodes = new ArrayList<Node>(10);
    private Map<String, LogFile> logFiles = new HashMap<String, LogFile>();
    private boolean connected = false;
    private boolean isBootComplete = false;
    private boolean isLogging = false;
    private String currentLogFilename = null;
    private String currentLogFilenameNext = null;
    private long currentLogStartTime = 0;
    private boolean loggingHasRequestedList = false;

    /***************************************
     *  IMPORTANT: The numbers given to each error type MUST match those listed in the documentation!
     *  See - https://github.com/rh416/COALAS/wiki/Wheelchair-Communication-Protocol#error-codes
     */
    public static enum Error {
        UNKNOWN(0),
        LOG_SD_CARD_INACCESSIBLE(50),
        ANOTHER_KIND_OF_ERROR(35);

        // How the error codes are sent from the Wheelchair Firmware
        private final int code;
        Error(int code){
            this.code = code;
        }
        public int getCode(){
            return this.code;
        }

        public static Error fromCode(int code){

            // Find the error that matches the given code
            for(Error e : Error.values()){
                if(e.getCode() == code){
                    return e;
                }
            }
            // If none are found, return an unknown error
            return UNKNOWN;
        }
    }

    private LinkedList<String> commandQueue = new LinkedList<String>();

    private Map<PWCInterfaceRequestIdentifier, PWCInterfaceRequest> requests = new HashMap<PWCInterfaceRequestIdentifier, PWCInterfaceRequest>();

    private String buffer = "";

    private Thread TimeoutCheck = new Thread(){

        public void run(){

            while(!Thread.currentThread().isInterrupted()){
                checkForTimedoutRequests();
                try{
                    Thread.sleep(TIME_BETWEEN_TIMEOUT_CHECKS_MS);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

    };

    public final Node ALL_NODES;

    public final int DEFAULT_TIMEOUT_SECS = 15;
    public final int TIME_BETWEEN_TIMEOUT_CHECKS_MS = 500;

    /**
     * A method to look for requests which have not received an appropriate response.
     *
     * This method immediately returns void, but will initiate a PWCInterfaceEvent with the
     * EventType of TIMEOUT for any requests that have timed-out
     *
     * When used with no parameters, DEFAULT_TIMEOUT_SECS is used
     *
     */
    public synchronized void checkForTimedoutRequests(){

        checkForTimedoutRequests(DEFAULT_TIMEOUT_SECS);
    }

    /**
     * A method to look for requests which have not received an appropriate response.
     *
     * This method immediately returns void, but will initiate a PWCInterfaceEvent with the
     * EventType of TIMEOUT for any requests that have timed-out
     *
     * @param timeout   A Timeout in seconds - any requests initiated longer ago than this value will produce a
     *                  TIMEOUT event.
     *
     */
    public synchronized void checkForTimedoutRequests(int timeout){

        // Loop over all requests in the request list to find any that have gone over the timeout period
        // Need to use an iterator so that the list can be altered whilst being iterated over
        Iterator it = requests.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();
            PWCInterfaceRequest request = (PWCInterfaceRequest) entry.getValue();

            if(request.hasTimedout(timeout)){
                // Remove this request from the list, otherwise it'll keep triggering events
                it.remove();

                // Create a timeout event
                PWCInterfacePayloadTimeout timeoutPayload = new PWCInterfacePayloadTimeout(this, request);
                PWCInterfaceEvent timeoutEvent = new PWCInterfaceEvent(this, EventType.TIMEOUT, timeoutPayload);

                // Dispatch the event
                dispatchPWCInterfaceEvent(timeoutEvent);
            }
        }
    }

    public static enum UltrasoundMode{
        CONTINUOUS('C'), PULSED('P');

        private final char charCode;
        private UltrasoundMode(final char charCode){ this.charCode = charCode; }
        public char getCharCode(){ return this.charCode; }
    }

    public PWCInterface(PWCInterfaceCommunicationProvider commsProvider){

        this.commsProvider = commsProvider;
        commsProvider.setPWCInterface(this);

        // Init nodes array - chair has a max of 9 so lets create them
        for(int i = 1; i <= 9; i++){
            nodes.add(new Node(i));
        }

        // Create a special node with an ID of 0 - this can be used to address all nodes
        ALL_NODES = new Node(0);

        // Start the thread which will keep an eye on timeouts
        TimeoutCheck.start();
    }

    public boolean isConnected(){

        return this.connected;
    }

    public void setConnected(boolean connected){

        // Only dispatch an event if the connected state has changed
        if(this.connected != connected) {
            this.connected = connected;

            PWCInterfaceEvent.EventType eventType = (connected ? EventType.CONNECTED : EventType.DISCONNECTED);
            PWCInterfacePayloadConnectionStatus eventPayload = new PWCInterfacePayloadConnectionStatus(this, connected);
            dispatchPWCInterfaceEvent(new PWCInterfaceEvent(this, eventType, eventPayload));

            // If the connection state has changed, the firmware will not be booted
            setBootComplete(false);

            // Send a message to the firmware:
            //      if connected via the Due's Programming port this will just be ignored
            //      if connected via the Due's Native port it'll generate a boot message and allow the system to work as expected
            sendCommand(EventType.BOOT_COMPLETE, 0, "B");
        }
    }

    public boolean isBootComplete(){

        return this.isBootComplete;
    }

    public void setBootComplete(boolean isBootComplete){

        this.isBootComplete = isBootComplete;

        // Once the chair is booted, request the latest version and set the time
        if(isBootComplete) {
            requestVersion();
            setSystemTime();
        }
    }

    public boolean isLogging(){

        return isLogging;
    }

    public void setLoggingStatus(boolean isLogging){

        this.isLogging = isLogging;
        if(isLogging) {
            this.currentLogFilename = this.currentLogFilenameNext;
            this.currentLogStartTime = System.currentTimeMillis();
        }
    }

    public String getCurrentLoggingFilename(){

        return this.currentLogFilename;
    }

    public void setCurrentLoggingFilename(String filename){

        this.currentLogFilename = null;
        this.currentLogFilenameNext = filename;
    }

    public long getCurrentLoggingStartTime(){

        return this.currentLogStartTime;
    }

    public boolean hasRequestedLogFileList(){

        return this.loggingHasRequestedList;
    }

    public void addLogFile(LogFile logFile){

        synchronized (logFiles){
            logFiles.put(logFile.getFilename(), logFile);
        }
    }

    public Map<String, LogFile> getLogFiles(){

        return logFiles;
    }

    public void clearLogFileList(){

        synchronized (logFiles){
            logFiles.clear();
        }
    }

    public LogFile getLogFile(String filename){

        return logFiles.get(filename);
    }

    public void registerListener(PWCInterfaceListener listener){

        this.listeners.add(listener);
    }

    public void unregisterListener(PWCInterfaceListener listener){

        listeners.remove(listener);
    }

    public Node getNode(int nodeId){

        if(nodeId > 0) {
            return nodes.get(nodeId - 1);
        } else {
            return null;
        }
    }


    /**
     * Request the current firmware version being used on the wheelchair.
     *
     * Immediately returns void, but will initiate a FIRMWARE_INFO event when the chair responds
     */
    public void requestVersion(){

        sendCommand(EventType.FIRMWARE_INFO, 0, "I");
    }


    /**
     * Check whether a node exists on the RS-485 bus
     *
     * Immediately returns void, but will initiate a BUS_SCAN event when the chair responds
     *
     * @param node  The Node which is to have its presence on the bus checked
     */
    public void checkNodeExistsOnBus(Node node){

        checkNodeExistsOnBus(node.getId());
    }

    /**
     * Check whether a node exists on the RS-485 bus
     *
     * Immediately returns void, but will initiate a BUS_SCAN event when the chair responds
     *
     * @param nodeId  The ID of a Node which is to have its presence on the bus checked
     */
    public void checkNodeExistsOnBus(int nodeId){

        sendCommand(EventType.BUS_SCAN, nodeId, "S%d");
    }


    /**
     * Configure a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate an ACK or NACK event when the chair responds
     *
     * @param node          The Node which is to have its presence on the bus checked
     * @param configString  A string defining the Node's new configuration (see Documentation)
     */
    public void configureNodeSensors(Node node, String configString){

        configureNodeSensors(node.getId(), configString);
    }

    /**
     * Configure a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param nodeId        The ID of a Node which is to have its presence on the bus checked
     * @param configString  A string defining the Node's new configuration (see Documentation)
     */
    public void configureNodeSensors(int nodeId, String configString){

        sendCommand(EventType.NODE_CONFIGURATION, nodeId, "C%d" + configString);
    }


    /**
     * Request the sensor configuration of a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_CONFIGURATION event when the chair responds
     *
     * @param node          The Node which is to have its presence on the bus checked
     */
    public void requestNodeConfiguration(Node node){

        requestNodeConfiguration(node.getId());
    }

    /**
     * Request the sensor configuration of a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_CONFIGURATION event when the chair responds
     *
     * @param nodeId          The ID of a Node which is to have its presence on the bus checked
     */
    public void requestNodeConfiguration(int nodeId){

        sendCommand(EventType.NODE_CONFIGURATION, nodeId, "R%d");
    }


    /**
     * Request the current sensor data from a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_CURRENT_DATA event when the chair responds
     *
     * @param node          The Node whose sensor data is to be returned
     */
    public void requestNodeCurrentData(Node node){

        requestNodeCurrentData(node.getId());
    }

    /**
     * Request the current sensor data from a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_CURRENT_DATA event when the chair responds
     *
     * @param nodeId          The ID of a Node whose sensor data is to be returned
     */
    public void requestNodeCurrentData(int nodeId){

        sendCommand(EventType.NODE_CURRENT_DATA, nodeId, "D%d");
    }


    /**
     * Request the sensor data format from a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_DATA_FORMAT event when the chair responds
     *
     * @param node          The Node whose sensor data format is to be returned
     */
    public void requestNodeDataFormat(Node node){

        requestNodeDataFormat(node.getId());
    }

    /**
     * Request the sensor data format from a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_DATA_FORMAT event when the chair responds
     *
     * @param nodeId          The ID of a Node whose sensor data format is to be returned
     */
    public void requestNodeDataFormat(int nodeId){

        sendCommand(EventType.NODE_DATA_FORMAT, nodeId, "F%d");
    }


    /**
     * Request the firmware version from a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_FIRMWARE_INFO event when the chair response
     *
     *  @param node         The Node whose firmware version is to be returned
     */
    public void requestNodeFirmwareVersion(Node node){

        requestNodeFirmwareVersion(node.getId());
    }

    /**
     * Request the firmware version from a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate a NODE_FIRMWARE_INFO event when the chair response
     *
     *  @param nodeId       The ID of a Node whose firmware version is to be returned
     */
    public void requestNodeFirmwareVersion(int nodeId){

        sendCommand(EventType.NODE_FIRMWARE_INFO, nodeId, "V%d");
    }


    /**
     * Set the sensor thresholds for each Zone in a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param node              The Node whose zone thresholds are to be set
     * @param zone1Threshold    The distance threshold for Zone 1
     * @param zone2Threshold    The distance threshold for Zone 2
     * @param zone3Threshold    The distance threshold for Zone 3
     */
    public void setNodeThresholds(Node node, int zone1Threshold, int zone2Threshold, int zone3Threshold){

        setNodeThresholds(node.getId(), zone1Threshold, zone2Threshold, zone3Threshold);
    }

    /**
     * Set the sensor thresholds for each Zone in a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param nodeId            The ID of a Node whose zone thresholds are to be set
     * @param zone1Threshold    The distance threshold for Zone 1
     * @param zone2Threshold    The distance threshold for Zone 2
     * @param zone3Threshold    The distance threshold for Zone 3
     */
    public void setNodeThresholds(int nodeId, int zone1Threshold, int zone2Threshold, int zone3Threshold){

        sendCommand(EventType.NODE_THRESHOLDS,
                nodeId, "T%d" +
                        intTo12BitHex(zone1Threshold) +
                        intTo12BitHex(zone2Threshold) +
                        intTo12BitHex(zone3Threshold));
    }


    /**
     * Set the mode for the ultrasound sensors on a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param node    The Node whose ultrasound mode is to be set
     * @param mode    The ultrasound mode to be set
     */
    public void setNodeUltrasoundMode(Node node, UltrasoundMode mode){

        setNodeUltrasoundMode(node.getId(), mode);
    }


    /**
     * Set the mode for the ultrasound sensors on a node on the RS-485 bus
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param nodeId  The ID of a Node whose ultrasound mode is to be set
     * @param mode    The ultrasound mode to be set
     */
    public void setNodeUltrasoundMode(int nodeId, UltrasoundMode mode){

        char modeStr = mode.getCharCode();
        sendCommand(EventType.NODE_MODE, nodeId, "M%d" + modeStr);
    }

    /**
     * Set the system time of the chair to the given time (in seconds since January 1st 1970)
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param newTime The new system time
     *
     */
    public void setSystemTime(int newTime){

        sendCommand(EventType.SET_TIME, 0, String.format("Z0%d", newTime));
    }

    /**
     * Set the system time of the chair to the host's current time (in seconds since January 1st 1970)
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     */
    public void setSystemTime(){

        setSystemTime((int) (System.currentTimeMillis() / 1000));
    }


    /**
     * Request the wheelchair to provide some haptic feedback
     *
     * @param intensity The intensity of the vibration motor in the range 0 - 100
     * @param onDuration The number of milliseconds the motor should be on
     * @param offDuration The number of milliseconds the motor should be off
     */
    public void hapticFeedback(int intensity, int onDuration, int offDuration){

        sendCommand(EventType.HAPTIC_FEEDBACK, 0,
                String.format("H0%s,%s,%s",
                        intTo12BitHex(intensity),
                        intTo12BitHex(onDuration),
                        intTo12BitHex(offDuration)));
    }

    /**
         * Start logging, writing the data to a logfile with the given name
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     * @param filename
     */
    public void startLogging(String filename){

        // Store the current filename
        this.setCurrentLoggingFilename(filename);

        // '0' must appear in the command string to indicate that this is not a node specific command
        sendCommand(EventType.LOG_START, 0, String.format("L0S:%s", filename));
    }

    /**
     * End the current logging session
     *
     * Immediately returns void, but will initiate a LOG_FILE_INFO event when the chair responds
     *
     */
    public void endLogging(){

        // '0' must appear in the command string to indicate that this is not a node specific command
        sendCommand(EventType.LOG_END, 0, "L0E");
    }

    /**
     * Records an event at the current time
     *
     * Immediately returns void, but will initiate an ACK event when the chair responds
     *
     */
    public void logEvent(String eventDescription){

        // '0' must appear in the command string to indicate that this is not a node specific command
        sendCommand(EventType.LOG_EVENT, 0, String.format("E0%s", eventDescription));
    }


    /**
     * List all the log files already on the SD card
     *
     * Immediately returns void, but will initiate a LOG_FILE_INFO event for each log file found when the chair responds
     *
     */
    public void refreshLogFileList(){

        // Clear the array of log files
        clearLogFileList();

        // Record that we have requested the list
        loggingHasRequestedList = true;

        // '0' must appear in the command string to indicate that this is not a node specific command
        sendCommand(EventType.LOG_LIST, 0, "L0?");
    }


    /**
     * Set the sensitivity of the chair on a scale of 0 - 100, 100 being most sensitive. Different
     * sensitivities can be sent for forwards, backwards and side-to-side obstacles.
     *
     * @param forwardSensitivity
     * @param backwardSensitivity
     * @param sidewaysSensitivity
     */

    public void setSensitivity(int forwardSensitivity, int backwardSensitivity, int sidewaysSensitivity) {

        sendCommand(EventType.SENSITIVITY, 0, String.format("P0%s,%s,%s",
                intTo12BitHex(forwardSensitivity),
                intTo12BitHex(backwardSensitivity),
                intTo12BitHex(sidewaysSensitivity)));
    }



    /**
     * Returns whether or not the interface is able to send / receive messages.
     *
     * @return True if the underlying communication provider is ready to send / receive messages
     */
    public boolean isAvailable(){

        if(commsProvider != null) {
            return commsProvider.isAvailable();
        }
        return false;
    }



    /**
     * Internal method to send commands to the chair.
     *
     * @param type      The type of command that is being sent
     * @param nodeId    The ID of the Node that this command affects
     *
     * @param command   The command to be sent to the chair
     */
    private void sendCommand(PWCInterfaceEvent.EventType type, int nodeId, String command){

        if(commsProvider != null){
            if(commsProvider.isAvailable()) {
                // Record this request
                addRequestToRequestList(type, nodeId);
                /* Build the command to send the firmware - this includes:
                    &   - command indicator
                    #   - command type id (Enum Ordinal - allows us to recover which type of request was called.
                                            Should NOT be stored long term - ie could change at next compile)
                    &   - command indicator
                    A-Z - command
                    #   - Node ID
                    ... - any command parameters
                */
                String commandToSend = String.format("&%02d&", type.ordinal()) + String.format(command, nodeId);
                commsProvider.write(commandToSend);
            } else{
                throw new PWCInterfaceException("Communication provider is not available");
            }
        } else {
            throw new PWCInterfaceException("No valid communication provider supplied");
        }
    }

    /**
     * Internal method used to record when a request was sent, so that requests can be checked for timeouts
     *                                                      
     * @param type      The type of command that is being sent
     * @param nodeId    The ID of the Node that this command affects
     */
    private void addRequestToRequestList(PWCInterfaceEvent.EventType type, int nodeId){

        // Check that a matching request doesn't already exist - we don't want to overwrite an older request,
        // as this could hide a timeout - ie if we're regularly asking for data from a node that doesn't response
        PWCInterfaceRequestIdentifier identifier = new PWCInterfaceRequestIdentifier(type, nodeId);
        if(requests.get(identifier) == null) {
            requests.put(identifier, new PWCInterfaceRequest(type, getNode(nodeId)));
        }
    }





    public void buffer(String inString){

        // Ignore null strings
        if(inString == null){
            return;
        }

        // Append the input to the current buffer
        buffer += inString;

        // If the last character is a newline or carriage return, parse the buffer
        char lastChar = buffer.charAt(buffer.length() - 1);
        if(lastChar == '\n' || lastChar == '\r'){
            // Trim any whitespace from the buffer string
            String tempBuffer = buffer.trim();

            // Clear the buffer (needs to be called before parsing - not exactly sure why!?)
            buffer = "";

            // If the buffer is not empty, parse it
            if(!tempBuffer.isEmpty()) {
                parse(tempBuffer);
            }
        }
    }


    /**
     * Method used to parse a response from the chair and dispatch the appropriate event
     *
     * @param response    The response from the chair
     */
    public void parse(String response) {

        //TODO: Fix logger.info("Parsing: " + response);

        PWCInterfaceEvent.EventType type = EventType.UNKNOWN;
        PWCInterfaceEventPayload payload = null;

        try {

            // Get first character to determine response type
            char firstChar = response.charAt(0);

            // Ignore lines that start with an underscore _
            if(firstChar == '_'){
                // System.out.println(response);
                return;
            }

            // Check that the second or third letter is a colon - only valid responses will have a colon as the second character.
            if(response.charAt(1) == ':' || response.charAt(2) == ':') {

                // We have received some kind of response from the chair, therefore it must be connected
                setConnected(true);

                // Now work out what kind of response it was
                switch (firstChar) {

                    // Version information from the firmware
                    case 'I':
                        type = EventType.FIRMWARE_INFO;
                        payload = new PWCInterfacePayloadFirmwareInfo(this, response);
                        break;

                    // The version of the firmware running on the specified Node
                    case 'V':
                        type = EventType.NODE_FIRMWARE_INFO;
                        payload = new PWCInterfacePayloadNodeFirmwareInfo(this, response);
                        break;

                    // Result from requesting node configuration
                    case 'C':
                        type = EventType.NODE_CONFIGURATION;
                        payload = new PWCInterfacePayloadNodeConfiguration(this, response);
                        break;

                    // Result of scanning whether a node exists or not
                    case 'S':
                        type = EventType.BUS_SCAN;
                        payload = new PWCInterfacePayloadBusScan(this, response);
                        break;

                    // Result from requesting node data format
                    case 'F':
                        type = EventType.NODE_DATA_FORMAT;
                        payload = new PWCInterfacePayloadNodeDataFormat(this, response);
                        break;

                    // Diagnostic information from sensors
                    case 'D':
                        type = EventType.NODE_CURRENT_DATA;
                        payload = new PWCInterfacePayloadNodeCurrentData(this, response);
                        break;

                    // Detect Acks from the node
                    case 'Y':
                        type = EventType.ACK;
                        payload = new PWCInterfacePayloadAckNack(this, response, true);
                        break;

                    // Detect Nacks from the node
                    case 'N':
                        type = EventType.NACK;
                        payload = new PWCInterfacePayloadAckNack(this, response, false);
                        break;

                    // Detect Joystick position data
                    case 'J':
                        type = EventType.JOYSTICK_FEEDBACK;
                        payload = new PWCInterfacePayloadJoystickFeedback(this, response);
                        break;

                    case 'L':
                        type = EventType.LOG_LIST;
                        payload = new PWCInterfacePayloadLogFileInfo(this, response);
                        break;

                    case 'E':
                        type = EventType.ERROR;
                        payload = new PWCInterfacePayloadError(this, response);
                        break;

                    case 'B':
                        setBootComplete(true);
                        type = EventType.BOOT_COMPLETE;
                        payload = null;
                        break;

                    case 'O':
                        if(response.charAt(3) == 'C'){
                            type = EventType.OBSTACLES_COMPLETE;
                            payload = null;
                        } else {
                            type = EventType.OBSTACLE;
                            payload = new PWCInterfacePayloadObstacle(this, response);
                        }

                    /*
                    case 'CUSTOM_CHARACTER':
                        type = EventType.CUSTOM_TYPE
                        payload = new PWCInterfacePayloadCUSTOMPAYLOAD extends PWCInterfaceEventPayload
                        break;

                        Then handle this type in onPWCInterfaceEvent of the window expecting data

                     */
                }
            }
        }catch(Exception e){
            type = EventType.PARSE_ERROR;
            payload= new PWCInterfacePayloadParseError(this, response, e);
            //e.printStackTrace();
            Timber.e(e.getMessage());
        }

        if(payload == null){
            payload = new PWCInterfacePayloadUnknown(this, response);
        }

        dispatchPWCInterfaceEvent(new PWCInterfaceEvent(this, type, payload));
    }

    /**
     * Internal method used to dispatch PWCInterfaceEvents to the application via the InterfaceListener supplied
     * when setting up the Interface
     *
     * @param event    The PWCInterfaceEvent to be dispatched
     */
    private void dispatchPWCInterfaceEvent(PWCInterfaceEvent event){

        // Find the request in the request list that relates to this response
        PWCInterfaceEvent.EventType eventType = event.getType();
        PWCInterfaceEvent.EventType identifierType = eventType;

        // If the event is an ACK or NACK, we need to get the original request's type in order to identify it properly
        if(eventType == EventType.ACK || eventType == EventType.NACK){
            PWCInterfacePayloadAckNack payload = (PWCInterfacePayloadAckNack) event.getPayload();
            identifierType = payload.getRequest().getType();
        }

        Node identifierNode = event.getPayload().getNode();
        int identifierNodeId = 0;

        // Get the specific Node Id, if a node was given - otherwise use the default value of 0
        if(identifierNode != null){
            identifierNodeId = identifierNode.getId();
        }

        // Remove the past event using an identifier made up of the EventType and Node ID
        PWCInterfaceRequestIdentifier identifier = new PWCInterfaceRequestIdentifier(identifierType, identifierNodeId);
        requests.remove(identifier);

        //TODO: Fix logger.info("Event Dispatched: " + event.getType());
        Timber.i("Event Dispatched: %s", event.getType().toString());

        ArrayList<PWCInterfaceListener> listenersIterator = new ArrayList<PWCInterfaceListener>(listeners);
        // Send the event out to any registered listener
        for (PWCInterfaceListener listener : listenersIterator) {
            listener.onPWCInterfaceEvent(event);
        }
    }

    /**
     * Return the 12-bit hex (3 character) representation of the input integer.
     *
     * @param input    Integer to be converted to hex. Has a max value of 4096.
     *
     * @return 12-bit hex representation of the input integer
     */
    public String intTo12BitHex(int input){

        if(input >= 4096){
            throw new RuntimeException("Input is too large");
        }

        return String.format("%03x", input).toUpperCase();
    }
}
