package org.funnycoin.p2p;

import com.codebrig.beam.messages.BeamMessage;
import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.funnycoin.FunnycoinCache;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PeerLoader {
    public static List<Peer> peers;
    public PeerLoader() {
        peers = new ArrayList<>();
    }


    public boolean checkPeerAdded() {
        if (RequestParams.peerAdded);
        return RequestParams.peerAdded;
    }

    public void syncPeerFile() throws IOException {
        File peersf = new File("peers.json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(peersf));
        writer.write(new Gson().toJson(FunnycoinCache.peerLoader.peers));
        writer.close();
    }

    public void init() {
        File peersf = new File("peers.json");
        StringBuilder peerBuilder = new StringBuilder();
        try {
            String bufs;
            BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(peersf)));
            while((bufs = buf.readLine()) != null) {
                peerBuilder.append(bufs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Peer[] peersArray = new Gson().fromJson(peerBuilder.toString(),Peer[].class);
        if(peerBuilder.toString().length() > 1) {
            for (int k = 0; k < peersArray.length; k++) {
                Peer p = peersArray[k];
                if (p.peerIsOnline()) {
                    try {
                        System.out.println(p.address + ":" + p.port);
                            System.out.println("connecting");
                            peers.add(p);
                            p.connectToPeer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(peers.size() == 0) {
            System.out.println("we dont have any peers unfortunately, gonna wait till someone tries to connect to someone i guess.");
            while ((peers.size() == 0 && !RequestParams.test) || checkPeerAdded()) {
                if (peersArray.length > 0) {
                    if (peersArray.length > 0) {
                        for (int k = 0; k < peersArray.length; k++) {
                            Peer p = peersArray[k];
                            if (p.peerIsOnline()) {
                                try {
                                    System.out.println("connecting");
                                    peers.add(p);
                                    p.connectToPeer();
                                    break;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                if (peers.size() > 0 || checkPeerAdded()) break;
            }
        } else {
            System.out.println("peers added successfully!");
        }
        System.out.println("hey2");
        System.out.println(peers.size());
        peers.forEach(peer -> {
            try {
                peer.socket.queueMessage(new BeamMessage().set("event","getHeight").set("adHash",FunnycoinCache.getAdHash()));
                System.out.println("sent msg to:" + peer.port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        while(RequestParams.needsBlocks) {
            if(!RequestParams.needsBlocks) break;
        }
    }

}
