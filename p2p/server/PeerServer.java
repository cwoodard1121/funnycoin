package org.funnycoin.p2p.server;

import com.codebrig.beam.BeamServer;
import com.codebrig.beam.messages.BasicMessage;
import com.dosse.upnp.UPnP;
import org.funnycoin.FunnycoinCache;
import org.funnycoin.miner.VerificationUtils;


public class PeerServer {
    BeamServer s;
    public int port = 55616;

    public void init() {
        try {
            UPnP.openPortTCP(port);
            System.out.println("server created" + port);
            s = new BeamServer(String.valueOf(FunnycoinCache.getIp().hashCode()),port,false);
            s.setDaemon(true);
            s.start();
            s.addHandler(PeerHandler.class);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public void broadcast(String message,String event) {
        System.out.println("sending: " + message);
        /**
         * I've never used this message library before but we're gonna try it.
         */
        BasicMessage beamMessage = new BasicMessage();
        beamMessage = (BasicMessage) beamMessage.set("message",message);
        beamMessage = (BasicMessage) beamMessage.set("event",event);
        s.broadcast(beamMessage);
    }
}
