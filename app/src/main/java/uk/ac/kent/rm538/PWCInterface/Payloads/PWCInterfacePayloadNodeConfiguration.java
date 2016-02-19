package uk.ac.kent.rm538.PWCInterface.Payloads;

import java.util.ArrayList;

import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.Hardware.Sensor;
import uk.ac.kent.rm538.PWCInterface.Hardware.Zone;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by rm538 on 06/08/2014.
 *
 * A class representing the payload for a node configuration event from the Interface
 *
 */
public class PWCInterfacePayloadNodeConfiguration extends PWCInterfaceEventPayload {

    public PWCInterfacePayloadNodeConfiguration(PWCInterface chairInterface, String response) throws Exception{

        super(chairInterface, response);

        int nodeId = Integer.parseInt(response.substring(1, 2));
        Node node = getResponseNodeFromId(nodeId);

        // Get the response minus the first 3 characters and up to the terminating .
        String configStr = response.substring(3, response.indexOf("."));

        // Each Zone's configuration is separated by a ',' - so break the string up
        String[] configItems = configStr.split(",");

        int zoneNum = 1;

        // For each zone returned by the config string
        for(String item : configItems){
            // Initialise some temporary variables
            Zone.Position tmpPosition = Zone.Position.UNKNOWN;
            Zone.Orientation tmpOrientation = Zone.Orientation.UNKNOWN;
            ArrayList<Sensor> tmpSensors = new ArrayList<Sensor>();

            // Find which position the zone is in
            for(Zone.Position pos : Zone.Position.values()){
                if(item.substring(0, 1).equals(pos.toString())){
                    tmpPosition = pos;
                    break;
                }
            }

            // Find which orientation the zone is in
            for(Zone.Orientation orientation : Zone.Orientation.values()){
                if(item.substring(1, 2).equals(orientation.toString())){
                    tmpOrientation = orientation;
                    break;
                }
            }

            // 'u' is returned if no sensors are attached, so check that first
            if(!item.substring(2).equals("u")){
                // Detect how many and what kind of sensors we have in each Zone
                for(char c : item.substring(2).toCharArray()){
                    int charCode = (int)c;
                    // Detect the type of sensor
                    for(Sensor.SensorType sensorType : Sensor.SensorType.values()){
                        if((charCode & 0x1F) == (sensorType.getBitmask() + zoneNum)){      // Bitmask = 0x1F -> 11111 (as we only care about
                            tmpSensors.add(new Sensor(sensorType));           //            the first 5 bits)
                            break;
                        }
                    }
                }

            }

            // Create a new instance of a zone
            Zone tmpZone = new Zone(node, zoneNum, tmpPosition, tmpOrientation);
            // Add the sensors to the zone too
            tmpZone.setSensors(tmpSensors);

            // Add the Zone to the Node
            node.setZone(zoneNum, tmpZone);

            zoneNum++;
        }
    }
}
