package uk.ac.kent.rm538.PWCInterface.Payloads;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.Hardware.Node;
import uk.ac.kent.rm538.PWCInterface.Hardware.Obstacle;
import uk.ac.kent.rm538.PWCInterface.Hardware.Zone;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceEventPayload;

/**
 * Created by rm538 on 06/08/2014.
 *
 * A class representing the payload for a firmware information event from the Interface
 *
 */
public class PWCInterfacePayloadObstacle extends PWCInterfaceEventPayload {

    private Obstacle obstacle;

    public PWCInterfacePayloadObstacle(PWCInterface chairInterface, String response) throws Exception{

        super(chairInterface, response);

        int nodeId = Integer.parseInt(response.substring(1, 2));
        Node node = getResponseNodeFromId(nodeId);

        int zoneId =  Integer.parseInt(response.substring(3, 4));
        Zone zone = node.getZone(zoneId);

        int distance = Integer.parseInt(response.substring(4), 16);

        obstacle = new Obstacle(zone, distance);
    }


    public Obstacle getObstacle(){

        return obstacle;
    }
}