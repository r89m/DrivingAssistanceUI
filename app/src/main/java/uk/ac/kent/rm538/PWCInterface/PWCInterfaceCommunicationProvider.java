package uk.ac.kent.rm538.PWCInterface;

/**
 * Created by rm538 on 07/08/2014.
 */
public interface PWCInterfaceCommunicationProvider {

    public boolean isAvailable();
    public void setPWCInterface(PWCInterface pwcInterface);
    public void write(String output);
}
