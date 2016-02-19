package uk.ac.kent.rm538.PWCInterface.Hardware;

/**
 * Created by Richard on 26/03/2015.
 */
public class LogFile {

    private int size;
    private String filename;

    public LogFile(String filename){

        this.filename = filename;
    }

    public void setFilename(String filename){

        this.filename = filename;
    }

    public void setSize(int size){

        this.size = size;
    }

    public String getFilename(){

        return this.filename;
    }

    public int getSize(){

        return this.size;
    }
}
