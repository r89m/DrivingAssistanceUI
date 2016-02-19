package uk.ac.kent.rm538.PWCInterface.Hardware;

/**
 * Created by rm538 on 01/09/2014.
 */
public class JoystickPosition {

    private int turn;
    private int speed;
    private long dataReadTimestamp;

    public JoystickPosition(int turn, int speed){

        this.turn = turn;
        this.speed = speed;
        dataReadTimestamp = System.currentTimeMillis();
    }

    public int getDataAge(){

        return (int) (System.currentTimeMillis() - dataReadTimestamp);
    }

    public int getSpeed(){

        return speed;
    }

    public int getTurn(){

        return turn;
    }
}
