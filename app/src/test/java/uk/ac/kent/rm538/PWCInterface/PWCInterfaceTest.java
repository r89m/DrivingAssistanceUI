package uk.ac.kent.rm538.PWCInterface;

import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.Hardware.Obstacle;
import uk.ac.kent.rm538.PWCInterface.Hardware.Sensor;
import uk.ac.kent.rm538.PWCInterface.Hardware.Zone;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadAckNack;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadError;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadFirmwareInfo;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadJoystickFeedback;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadLogFileInfo;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeConfiguration;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeCurrentData;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadNodeDataFormat;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadObstacle;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadParseError;
import uk.ac.kent.rm538.PWCInterface.Payloads.PWCInterfacePayloadTimeout;

import static uk.ac.kent.rm538.PWCInterface.Hardware.Sensor.SensorType;
import static uk.ac.kent.rm538.PWCInterface.PWCInterfaceEvent.EventType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Created by Richard on 14/01/2016.
 */
public class PWCInterfaceTest {

    public PWCInterface pwcInterface;
    public PWCInterfaceTestCommunicationProvider comms;
    public PWCInterfaceTestListener listener;

    private String shared_node_config_str = "C4:1gIE,1hu,1aO.";
    private String shared_node_format_str = "F4:EWcIDrOWc";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup(){

        comms = new PWCInterfaceTestCommunicationProvider();
        listener = new PWCInterfaceTestListener();
        pwcInterface = new PWCInterface(comms);
        pwcInterface.registerListener(listener);
    }

    /**
     *     *
     * @param type The expected EventType for the data sent
     * @param response The expected data that will be sent
     * @return A string with the EventType and raw data correctly formatted
     */
    private String response(PWCInterfaceEvent.EventType type, String response){

        return String.format("&%02d&", type.ordinal()) + response;
    }

    @Test
    public void checkAvailability_IsNotAvailable(){

        comms.setIsAvailable(false);
        assertEquals(false, pwcInterface.isAvailable());
    }

    @Test
    public void checkAvailability_IsAvailable(){

        comms.setIsAvailable(true);
        assertEquals(true, pwcInterface.isAvailable());
    }

    @Test
    public void checkAvailability_NotAvailableButUsed(){

        comms.setIsAvailable(false);
        exception.expect(PWCInterfaceException.class);
        exception.expectMessage("Communication provider is not available");
        pwcInterface.requestVersion();
    }

    @Test
    public void checkInvalidResponse(){

        comms.sendData("gibberish");
        assertEquals(EventType.UNKNOWN, listener.getLastEvent().getType());
    }

    @Test
    public void checkUnknownCharacterCode(){

        comms.sendData("X:unknown code");
        assertEquals(EventType.UNKNOWN, listener.getLastEvent().getType());
    }

    @Test
    public void checkVersionRequest(){

        pwcInterface.requestVersion();
        assertEquals(response(EventType.FIRMWARE_INFO, "I"), comms.getSentData());
    }

    @Test
    public void checkVersionParsing_TypeCheck(){

        comms.sendData("I:v423");
        assertEquals(EventType.FIRMWARE_INFO, listener.getLastEvent().getType());
    }

    @Test
    public void checkVersionParsing_Simple(){

        comms.sendData("I:v123");
        PWCInterfacePayloadFirmwareInfo payload = (PWCInterfacePayloadFirmwareInfo)listener.getLastEvent().getPayload();
        assertEquals("v123", payload.getVersion());
    }

    @Test
    public void checkVersionParsing_AlphaMix(){

        comms.sendData("I:va373mdg232");
        PWCInterfacePayloadFirmwareInfo payload = (PWCInterfacePayloadFirmwareInfo)listener.getLastEvent().getPayload();
        assertEquals("va373mdg232", payload.getVersion());
    }

    @Test
    public void checkTimeout(){

        pwcInterface.setSystemTime(12345);
        // Check using -1 timeout - will force a Timeout
        pwcInterface.checkForTimedoutRequests(-1);
        assertEquals(EventType.TIMEOUT, listener.getLastEvent().getType());
    }

    @Test
    public void checkTimeout_Details(){

        pwcInterface.setSystemTime(12345);
        pwcInterface.checkForTimedoutRequests(-1);
        PWCInterfacePayloadTimeout payload = (PWCInterfacePayloadTimeout) listener.getLastEvent().getPayload();
        assertEquals(EventType.SET_TIME, payload.getRequest().getType());
    }

    @Test
    public void checkAckNack_Ack(){

        comms.sendData("Y0:" + EventType.SET_TIME.ordinal());
        PWCInterfacePayloadAckNack payload = (PWCInterfacePayloadAckNack) listener.getLastEvent().getPayload();
        assertEquals(EventType.ACK, listener.getLastEvent().getType());
        assertEquals(EventType.SET_TIME, payload.getRequest().getType());
    }

    @Test
    public void checkAckNack_Nack(){

        comms.sendData("N0:" + EventType.SET_TIME.ordinal());
        PWCInterfacePayloadAckNack payload = (PWCInterfacePayloadAckNack) listener.getLastEvent().getPayload();
        assertEquals(EventType.NACK, listener.getLastEvent().getType());
        assertEquals(EventType.SET_TIME, payload.getRequest().getType());
    }

    @Test
    public void checkFirmwareError(){

        comms.sendData("E:" + PWCInterface.Error.LOG_SD_CARD_INACCESSIBLE.getCode());
        PWCInterfacePayloadError payload = (PWCInterfacePayloadError) listener.getLastEvent().getPayload();
        assertEquals(PWCInterface.Error.LOG_SD_CARD_INACCESSIBLE, payload.getError());
    }

    @Test
    public void checkBootStatus_Initial(){

        assertEquals(false, pwcInterface.isBootComplete());
    }

    @Test
    public void checkBootStatus_StatusSent(){

        comms.sendData("B:Boot complete");
        assertEquals(true, pwcInterface.isBootComplete());
    }

    @Test
    public void checkBusScan_Send(){

        // Check if Node 1 exists on the bus
        Node node1 = pwcInterface.getNode(1);
        pwcInterface.checkNodeExistsOnBus(node1);
        assertEquals(response(EventType.BUS_SCAN, "S1"), comms.getSentData());
    }

    @Test
    public void checkBusScan_ReceivePresent(){

        // Check initial state
        Node busNode = pwcInterface.getNode(1);
        assertEquals(false, busNode.isConnectedToBus());
        comms.sendData("S1:Y");
        assertEquals(true, busNode.isConnectedToBus());
    }

    @Test
    public void checkBusScan_ReceiveMissing(){

        // Check initial state
        Node busNode = pwcInterface.getNode(4);
        assertEquals(false, busNode.isConnectedToBus());
        comms.sendData("S4:N");
        assertEquals(false, busNode.isConnectedToBus());
    }

    @Test
    public void checkBusScan_InvalidResponse(){

        comms.sendData("S4:NotOnBus");
        assertEquals(EventType.PARSE_ERROR, listener.getLastEvent().getType());
    }

    @Test
    public void checkNodeFirmwareInfo_Send(){

        Node node1 = pwcInterface.getNode(1);
        pwcInterface.requestNodeFirmwareVersion(node1);
        assertEquals(response(EventType.NODE_FIRMWARE_INFO, "V1"), comms.getSentData());
    }

    @Test
    public void checkNodeFirmwareInfo_Receive(){

        comms.sendData("V1:0313");
        assertEquals("3.19", pwcInterface.getNode(1).getFirmwareVersionString());
    }

    @Test
    public void checkNodeConfiguration_Request(){

        Node configNode = pwcInterface.getNode(4);
        pwcInterface.requestNodeConfiguration(configNode);
        assertEquals(response(EventType.NODE_CONFIGURATION, "R4"), comms.getSentData());
    }

    @Test
    public void checkNodeConfiguration_Response(){

        comms.sendData(shared_node_config_str);

        PWCInterfacePayloadNodeConfiguration payload = (PWCInterfacePayloadNodeConfiguration) listener.getLastEvent().getPayload();
        assertEquals(EventType.NODE_CONFIGURATION, listener.getLastEvent().getType());

        Zone zone_1 = payload.getNode().getZone(1);
        Zone zone_2 = payload.getNode().getZone(2);
        Zone zone_3 = payload.getNode().getZone(3);

        assertEquals(Zone.Position.FRONT_LEFT_CORNER, zone_1.getPosition());
        assertEquals(Zone.Orientation.LEFT, zone_1.getOrientation());
        assertNotEquals(null, zone_1.getSensorByType(SensorType.ULTRASONIC));
        assertNotEquals(null, zone_1.getSensorByType(SensorType.INFRARED));
        assertEquals(null, zone_1.getSensorByType(SensorType.FUSED));
        assertEquals(Zone.SensorSeparation.SEPARATE, zone_1.getSensorSeparation());

        assertEquals(Zone.Position.FRONT_LEFT_CORNER, zone_2.getPosition());
        assertEquals(Zone.Orientation.FORWARD_LEFT, zone_2.getOrientation());
        assertEquals(null, zone_2.getSensorByType(SensorType.ULTRASONIC));
        assertEquals(null, zone_2.getSensorByType(SensorType.INFRARED));
        assertEquals(null, zone_2.getSensorByType(SensorType.FUSED));

        assertEquals(Zone.Position.FRONT_LEFT_CORNER, zone_3.getPosition());
        assertEquals(Zone.Orientation.FORWARD, zone_3.getOrientation());
        assertEquals(null, zone_3.getSensorByType(SensorType.ULTRASONIC));
        assertEquals(null, zone_3.getSensorByType(SensorType.INFRARED));
        assertNotEquals(null, zone_3.getSensorByType(SensorType.FUSED));
        assertEquals(Zone.SensorSeparation.FUSED, zone_3.getSensorSeparation());
    }

    @Test
    public void checkNodeConfiguration_Set(){

        Node configNode = pwcInterface.getNode(4);
        String configStr = "test_config_str";

        pwcInterface.configureNodeSensors(configNode, configStr);
        assertEquals(response(EventType.NODE_CONFIGURATION, String.format("C%d%s", configNode.getId(), configStr)), comms.getSentData());
    }

    @Test
    public void checkNodeDataFormat_Request(){

        Node formatNode = pwcInterface.getNode(4);
        pwcInterface.requestNodeDataFormat(formatNode);
        assertEquals(response(EventType.NODE_DATA_FORMAT, "F4"), comms.getSentData());
    }

    @Test
    public void checkNodeDataFormat_Response(){

        Node dataNode = pwcInterface.getNode(4);
        comms.sendData(shared_node_config_str);
        comms.sendData(shared_node_format_str);

        PWCInterfacePayloadNodeDataFormat payload = (PWCInterfacePayloadNodeDataFormat) listener.getLastEvent().getPayload();
        assertEquals(EventType.NODE_DATA_FORMAT, listener.getLastEvent().getType());

        assertEquals(dataNode.getId(), payload.getNode().getId());

        Zone zone_1 = payload.getNode().getZone(1);
        Zone zone_3 = payload.getNode().getZone(3);

        assertEquals(Sensor.SensorDataFormat.WORD, zone_1.getSensorByType(SensorType.ULTRASONIC).getDataFormat());
        assertEquals(Sensor.SensorDataInterpretation.CM, zone_1.getSensorByType(SensorType.ULTRASONIC).getDataInterpretation());
        assertEquals(Sensor.SensorDataFormat.DWORD, zone_1.getSensorByType(SensorType.INFRARED).getDataFormat());
        assertEquals(Sensor.SensorDataInterpretation.RAW, zone_1.getSensorByType(SensorType.INFRARED).getDataInterpretation());

        assertEquals(Sensor.SensorDataFormat.WORD, zone_3.getSensorByType(SensorType.FUSED).getDataFormat());
        assertEquals(Sensor.SensorDataInterpretation.CM, zone_3.getSensorByType(SensorType.FUSED).getDataInterpretation());
    }
    
    @Test
    public void checkNodeCurrentData_Request(){

        Node dataNode = pwcInterface.getNode(5);
        pwcInterface.requestNodeCurrentData(dataNode);
        assertEquals(response(EventType.NODE_CURRENT_DATA, "D5"), comms.getSentData());
    }
    
    @Test
    public void checkNodeCurrentData_Response(){
        
        Node dataNode = pwcInterface.getNode(4);
        comms.sendData(shared_node_config_str);
        comms.sendData(shared_node_format_str);

        int sensor1Value = 450;
        int sensor2Value = 973;
        int sensor3Value = 143;

        String data_str = String.format("D4:%04X%08X%04X", sensor1Value, sensor2Value, sensor3Value);
        comms.sendData(data_str);

        PWCInterfacePayloadNodeCurrentData payload = (PWCInterfacePayloadNodeCurrentData) listener.getLastEvent().getPayload();
        assertEquals(EventType.NODE_CURRENT_DATA, listener.getLastEvent().getType());

        assertEquals(dataNode.getId(), payload.getNode().getId());

        Zone zone_1 = payload.getNode().getZone(1);
        Zone zone_3 = payload.getNode().getZone(3);

        assertEquals(sensor1Value, zone_1.getSensorByType(SensorType.ULTRASONIC).getCurrentValue());
        assertEquals(sensor2Value, zone_1.getSensorByType(SensorType.INFRARED).getCurrentValue());
        assertEquals(sensor3Value, zone_3.getSensorByType(SensorType.FUSED).getCurrentValue());
    }

    @Test
    public void checkJoystickMonitor_ZeroInputOutput(){

        comms.sendData("J:2282282282280");

        PWCInterfacePayloadJoystickFeedback payload = (PWCInterfacePayloadJoystickFeedback) listener.getLastEvent().getPayload();

        assertEquals(0, payload.getInputPosition().getTurn());
        assertEquals(0, payload.getInputPosition().getSpeed());

        assertEquals(0, payload.getInputPosition().getTurn());
        assertEquals(0, payload.getInputPosition().getSpeed());
    }

    @Test
    public void checkJoystickMonitor_MinInput(){

        comms.sendData("J:1001001001000");

        PWCInterfacePayloadJoystickFeedback payload = (PWCInterfacePayloadJoystickFeedback) listener.getLastEvent().getPayload();

        assertEquals(-100, payload.getInputPosition().getTurn());
        assertEquals(-100, payload.getInputPosition().getSpeed());

        assertEquals(-100, payload.getInputPosition().getTurn());
        assertEquals(-100, payload.getInputPosition().getSpeed());
    }

    @Test
    public void checkJoystickMonitor_MaxInput(){

        comms.sendData("J:3553553553550");

        PWCInterfacePayloadJoystickFeedback payload = (PWCInterfacePayloadJoystickFeedback) listener.getLastEvent().getPayload();

        assertEquals(100, payload.getInputPosition().getTurn());
        assertEquals(100, payload.getInputPosition().getSpeed());

        assertEquals(100, payload.getInputPosition().getTurn());
        assertEquals(100, payload.getInputPosition().getSpeed());
    }

    @Test
    public void checkJoystickMonitor_AvoidanceEnabled(){

        comms.sendData("J:3553553553550");

        PWCInterfacePayloadJoystickFeedback payload = (PWCInterfacePayloadJoystickFeedback) listener.getLastEvent().getPayload();
        assertTrue(payload.isAvoidanceEnabled());
    }

    @Test
    public void checkJoystickMonitor_AvoidanceDisabled(){

        comms.sendData("J:3553553553551");

        PWCInterfacePayloadJoystickFeedback payload = (PWCInterfacePayloadJoystickFeedback) listener.getLastEvent().getPayload();
        assertFalse(payload.isAvoidanceEnabled());
    }

    @Test
    public void checkJoystickMonitor_InvalidResponse(){

        comms.sendData("J:fdsfdsffsd");
        assertEquals(EventType.PARSE_ERROR, listener.getLastEvent().getType());
    }

    @Test
    public void checkJoystickMonitor_InvalidResponseDetail(){

        comms.sendData("J:fdsfdsffsd");
        PWCInterfacePayloadParseError payload = (PWCInterfacePayloadParseError) listener.getLastEvent().getPayload();
        assertEquals("java.lang.NumberFormatException: For input string: \"fds\"", payload.getErrorMessage());
    }

    @Test
    public void checkHapticControl(){

        int intensity = 50;
        int onDuration = 200;
        int offDuration = 800;

        pwcInterface.hapticFeedback(intensity, onDuration, offDuration);
        assertEquals(response(EventType.HAPTIC_FEEDBACK, String.format("H0%03X,%03X,%03X",
                intensity, onDuration, offDuration)), comms.getSentData());
    }

    @Test
    public void checkSystemTimeSetting(){

        int unix_timestamp = 1439638942;
        pwcInterface.setSystemTime(unix_timestamp);
        assertEquals(response(EventType.SET_TIME, "Z0" + String.valueOf(unix_timestamp)), comms.getSentData());
    }

    @Test
    public void checkLogging_StartLogging(){

        String logfile_name = "log_file_name.txt";
        pwcInterface.startLogging(logfile_name);
        assertEquals(response(EventType.LOG_START, "L0S:" + logfile_name), comms.getSentData());
    }

    @Test
    public void checkLogging_LogEvent(){

        String event_description = "event";
        pwcInterface.logEvent(event_description);
        assertEquals(response(EventType.LOG_EVENT, "E0" + event_description), comms.getSentData());
    }

    @Test
    public void checkLogging_EndLogging(){

        pwcInterface.endLogging();
        assertEquals(response(EventType.LOG_END, "L0E"), comms.getSentData());
    }

    @Test
    public void checkLogging_RefreshListRequest(){

        pwcInterface.refreshLogFileList();
        assertEquals(response(EventType.LOG_LIST, "L0?"), comms.getSentData());
    }

    @Test
    public void checkLogging_LogFileList_Receive(){

        String log_filename = "filename.txt";
        int log_filesize = 1234;

        comms.sendData(String.format("L:%s:%d", log_filename, log_filesize));
        PWCInterfacePayloadLogFileInfo payload = (PWCInterfacePayloadLogFileInfo) listener.getLastEvent().getPayload();
        assertEquals(log_filename, payload.getLogFile().getFilename());
        assertEquals(log_filesize, payload.getLogFile().getSize());
    }

    @Test
    public void checkLogging_LogFileList_ReceiveMultiple(){

        pwcInterface.refreshLogFileList();
        assertEquals(0, pwcInterface.getLogFiles().size());

        comms.sendData("L:filename:1234");
        comms.sendData("L:filename2:1234");

        assertEquals(2, pwcInterface.getLogFiles().size());
    }

    @Test
    public void checkUltrasound_SetMode(){


        comms.sendData(shared_node_config_str);
        Node usNode = pwcInterface.getNode(4);
        pwcInterface.setNodeUltrasoundMode(usNode, PWCInterface.UltrasoundMode.CONTINUOUS);
        assertEquals(response(EventType.NODE_MODE, "M4C"), comms.getSentData());
    }

    @Test
    public void checkUltrasound_SetThreshold(){

        comms.sendData(shared_node_config_str);
        Node usNode = pwcInterface.getNode(4);
        int threshold_1 = 50;
        int threshold_2 = 150;
        int threshold_3 = 89;
        pwcInterface.setNodeThresholds(usNode, threshold_1, threshold_2, threshold_3);
        assertEquals(response(EventType.NODE_THRESHOLDS, String.format("T4%03X%03X%03X", threshold_1, threshold_2, threshold_3)), comms.getSentData());
    }

    @Test
    public void checkObstacles_ReceiveObstacle(){

        int nodeId = 3;
        int zoneNumber = 1;
        int distance = 300;

        comms.sendData(String.format("O%1d:%1d%03X", nodeId, zoneNumber, distance));
        PWCInterfacePayloadObstacle payload = (PWCInterfacePayloadObstacle) listener.getLastEvent().getPayload();
        Obstacle obstacle = payload.getObstacle();
        assertEquals(EventType.OBSTACLE, listener.getLastEvent().getType());
        assertEquals(nodeId, obstacle.getZone().getParentNode().getId());
        assertEquals(zoneNumber, obstacle.getZone().getZoneNumber());
        assertEquals(distance, obstacle.getDistance());
    }

    @Test
    public void checkSensitivity_SetSensitivity(){

        int forwardSensitivity = 100;
        int backwardSensitivity = 40;
        int sidewaysSensitivity = 65;

        pwcInterface.setSensitivity(forwardSensitivity, backwardSensitivity, sidewaysSensitivity);
        assertEquals(response(EventType.SENSITIVITY,
                String.format("P0%03X,%03X,%03X",
                        forwardSensitivity,
                        backwardSensitivity ,
                        sidewaysSensitivity)), comms.getSentData());
    }

    @Test
    public void checkUtility_intTo12BitHex_ZeroValue(){

        assertEquals("000", pwcInterface.intTo12BitHex(0));
    }

    @Test
    public void checkUtility_intTo12BitHex_Valid(){

        assertEquals("ABC", pwcInterface.intTo12BitHex(2748));
    }

    @Test
    public void checkUtility_intTo12BitHex_MaxBounds(){

        assertEquals("FFF", pwcInterface.intTo12BitHex(4095));
    }

    @Test
    public void checkUtility_intTo12BitHex_TooLarge(){

        exception.expect(RuntimeException.class);
        exception.expectMessage("Input is too large");
        pwcInterface.intTo12BitHex(4096);
    }
}
