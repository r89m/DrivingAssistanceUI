package uk.ac.kent.rm538.PWCInterface.Hardware;

import uk.ac.kent.rm538.PWCInterface.PWCInterface;

/**
 * Created by Richard on 21/01/2016.
 */
public class Obstacle {

    private Zone zone;
    private int distance;

    public Obstacle(Zone zone, int distance){

        this.zone = zone;
        this.distance = distance;
    }

    public Zone getZone(){

        return zone;
    }

    public int getDistance(){

        return distance;
    }
}
