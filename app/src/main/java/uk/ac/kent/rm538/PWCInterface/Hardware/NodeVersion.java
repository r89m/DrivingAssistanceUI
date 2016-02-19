package uk.ac.kent.rm538.PWCInterface.Hardware;

/**
 * Created by rm538 on 20/10/2014.
 */
public class NodeVersion {

    // Shorts must be used, as Java bytes are signed so have a range -127 to 128 rather than the desired 0-255
    private short versionMajor;
    private short versionMinor;

    public NodeVersion(short majorVersion, short minorVersion){

        versionMajor = majorVersion;
        versionMinor = minorVersion;
    }

    public short getMajorVersion(){

        return versionMajor;
    }

    public short getMinorVersion(){

        return versionMinor;
    }

    private int getRawVersion(){

        return (getMajorVersion() << 8) + getMinorVersion();
    }

    public boolean isGreaterThan(NodeVersion testVersion){

        if(testVersion == null){
            return false;
        }

        return getRawVersion() > testVersion.getRawVersion();

    }

    public boolean isLessThan(NodeVersion testVersion){

        if(testVersion == null){
            return false;
        }

        return getRawVersion() < testVersion.getRawVersion();
    }

    public boolean isEqualTo(NodeVersion testVersion){

        if(testVersion == null){
            return false;
        }

        return (getMajorVersion() == testVersion.getMajorVersion() && getMinorVersion() == testVersion.getMinorVersion());
    }

    @Override
    public String toString(){

        return String.valueOf(versionMajor) + "." + String.valueOf(versionMinor);
    }

    public static NodeVersion parseVersionString(String versionString){

        int vRaw = Integer.parseInt(versionString, 16);

        // This byte mask is required to shift values in the -127 - 128 range to 0 - 255
        short byteMask = (short) 0xFF;

        // Get the MSB - that's the major version; ie 1.x, 4.x
        short vMajor = (short) (vRaw >> 8 & byteMask);
        // Get the LSB - that's the minor version; ie x.3, x.29
        short vMinor = (short) (vRaw & byteMask);

        return new NodeVersion(vMajor, vMinor);
    }
}
