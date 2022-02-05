package org.funnycoin.p2p;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.connection.ConnectionType;
import org.funnycoin.p2p.server.PeerHandler;
import java.io.*;
import java.net.*;

public class Peer {
    public String address;
    public transient BeamClient socket;
    public int port;

    public Peer(String address,int port) {
        this.address = address;
        this.port = port;
    }

    public void connectToPeer() {
        try {
            socket = new BeamClient(InetAddress.getLoopbackAddress().getHostAddress(),"mam",port,false);
            socket.connect();
            socket.addHandler(PeerHandler.class);
            socket.setIncomingConnectionTypes(new ConnectionType.Incoming[]{ConnectionType.Incoming.DIRECT});
            socket.setDebugOutput(true);
            System.out.println("Added handler");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean peerIsOnline() {
        String hostName = address;
        int port = this.port;
        boolean isAlive = false;
        BeamClient client = new BeamClient(address, "Peer" + hostName.hashCode(),port,false);
        try {
            client.connect();
            isAlive = true;

        } catch (SocketTimeoutException exception) {
            System.out.println("SocketTimeoutException " + hostName + ":" + port + ". " + exception.getMessage());
            isAlive = false;
        } catch (IOException exception) {
            exception.printStackTrace();
            isAlive = false;
        } catch (Exception e) {
            isAlive = false;
        }
        client.close();
        return isAlive;
    }
}
