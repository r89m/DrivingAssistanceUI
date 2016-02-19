package uk.ac.kent.rm538.drivingassistanceui.PWC;

/**
 * Created by Richard on 05/02/2016.
 */
public class WebsocketSerialPortOpenResponse {

    private String Cmd;
    private String Desc;

    public boolean isOpen(){

        return "Open".equals(Cmd) && "Got register/open on port.".equals(Desc);
    }
}
