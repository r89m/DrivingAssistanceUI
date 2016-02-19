package uk.ac.kent.rm538.PWCInterface;

/**
 * Created by Richard on 14/01/2016.
 */
public class PWCInterfaceTestCommunicationProvider implements PWCInterfaceCommunicationProvider {

    private PWCInterface pwcInterface;
    private boolean isAvailable = true;
    private String sentData;

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean isAvailable){

        this.isAvailable = isAvailable;
    }

    @Override
    public void setPWCInterface(PWCInterface pwcInterface) {

        this.pwcInterface = pwcInterface;
    }

    @Override
    public void write(String output) {

        sentData = output;
    }

    public String getSentData(){

        return sentData;
    }

    public void sendData(String data){

        if(pwcInterface != null){
            pwcInterface.buffer(data + '\n');
        }
    }
}
