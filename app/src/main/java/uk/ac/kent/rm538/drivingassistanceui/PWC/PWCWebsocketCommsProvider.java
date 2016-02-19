package uk.ac.kent.rm538.drivingassistanceui.PWC;

import android.app.ExpandableListActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.WebSocket;

import timber.log.Timber;
import uk.ac.kent.rm538.PWCInterface.PWCInterface;
import uk.ac.kent.rm538.PWCInterface.PWCInterfaceCommunicationProvider;

/**
 * Created by Richard on 14/01/2016.
 */
public class PWCWebsocketCommsProvider implements PWCInterfaceCommunicationProvider, AsyncHttpClient.WebSocketConnectCallback {

    private String clientAddress;
    private String port;
    private boolean isClientConnected = false;
    private PWCInterface pwcInterface;
    private Future<WebSocket> webSocket;
    private ErrorListener errorListener;
    private Gson gson;

    public PWCWebsocketCommsProvider(String clientAddress){

        this(clientAddress, null);
    }

    public PWCWebsocketCommsProvider(String clientAddress, final ErrorListener errorListener){

        this.clientAddress = clientAddress;
        this.errorListener = errorListener;

        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();

        Timber.i("Websocket connection attempted");
    }

    @Override
    public boolean isAvailable() {

        return isClientConnected;
    }

    @Override
    public void setPWCInterface(PWCInterface pwcInterface) {

        this.pwcInterface = pwcInterface;
    }

    @Override
    public void write(String output) {

        output += '\n';

        Timber.i("Sending message: [%s]", output);

        try {
            if(webSocket != null) {
                webSocket.get().send(String.format("sendnobuf %s %s", port, output));
            } else {
                Timber.i("Websocket is null, can't write to it!");
            }
        } catch (Exception e){
            Timber.e(e.getMessage());
            forwardError(e);
        }
    }

    public void connect(final String port){

        Timber.i("Attempt comms connection");

        if(webSocket == null || !port.equals(this.port)){
            this.port = port;
            AsyncHttpGet request = new AsyncHttpGet(clientAddress.replace("ws://", "http://").replace("wss://", "https://"));
            request.setTimeout(500);
            webSocket = AsyncHttpClient.getDefaultInstance().websocket(request, "ws", this);
        }
    }

    public void disconnect(){

        Timber.i("Attempt to disconnect");

        try {
            if(webSocket != null && webSocket.get().isOpen()) {
                webSocket.get().close();
            }
        } catch (Exception e) {
            Timber.e(e.getMessage());
            forwardError(e);
        }
        webSocket = null;
        pwcInterface.setConnected(false);
        Timber.i("Disconnected");
        isClientConnected = false;
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {

        if (ex != null) {
            forwardError(ex);
            return;
        }

        isClientConnected = true;

        Timber.i("Comms connected");

        webSocket.send(String.format("open %s 9600", port)); // Baud rate doesn't matter

        webSocket.setStringCallback(new WebSocket.StringCallback() {
            @Override
            public void onStringAvailable(String s) {

                Timber.i("Response raw: %s", s);

                try {
                    WebsocketSerialPortOpenResponse openResponse = gson.fromJson(s, WebsocketSerialPortOpenResponse.class);

                    if (openResponse.isOpen()) {
                        Thread.sleep(500);
                        pwcInterface.setConnected(true);
                    }
                } catch (Exception exception) {
                }

                try {
                    WebsocketWheelchairDataResponse response = gson.fromJson(s, WebsocketWheelchairDataResponse.class);
                    Timber.i("Response: [%s] %s", response.getPort(), response.getData());
                    // Buffer each character of the response
                    String data = response.getData();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            pwcInterface.buffer(data.substring(i, i + 1));
                        }
                    }
                } catch (Exception e) {
                    //Timber.i("An error occurred");
                    Timber.e(e.getMessage());
                }
            }
        });
    }


    public interface ErrorListener {

        void onError(Exception ex);
    }

    private void forwardError(Exception e){

        if(errorListener != null){
            errorListener.onError(e);
        }
    }
}
