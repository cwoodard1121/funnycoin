package org.funnycoin;

import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;
import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.funnycoin.blocks.Block;
import org.funnycoin.blocks.MempoolBlock;
import org.funnycoin.miner.VerificationUtils;
import org.funnycoin.p2p.Peer;
import org.funnycoin.p2p.server.PeerServer;
import org.funnycoin.transactions.Transaction;
import org.funnycoin.wallet.SignageUtils;

import java.io.*;
import java.security.Security;
import java.util.Base64;
import java.util.Scanner;

import static org.funnycoin.FunnycoinCache.*;
import static org.funnycoin.p2p.RequestParams.*;

public class Funnycoin {


    private void mine() throws IOException {
        Block mine = FunnycoinCache.getTxStatus().block;
        mine.difficulty = getBlockDifficulty();
        mine.transactions.add(new Transaction("coinbase", wallet.getBase64Key(wallet.publicKey),50.f,"null","FUNNY"));
        int diff = getDifficulty;
        System.out.println("mining block with a difficulty of: " + diff);
        if(mine.mine(diff)) {
            Gson gson = new Gson();
            String json = gson.toJson(mine);
            peerServer.broadcast(json, "newBlock");
            blockChain.add(mine);
            syncBlockchainFile();
            interrupted = false;
        }
    }

    private void loadConfig() {
        try {
            final File config = new File("config.json");
            final StringBuilder builder = new StringBuilder();
            final BufferedReader reader = new BufferedReader(new FileReader(config));
            String tmp;
            while((tmp = reader.readLine()) != null) builder.append(tmp);
            JsonParser parser = new JsonParser();
            JsonObject obj = (JsonObject) parser.parse(builder.toString());
            FunnycoinCache.port = obj.get("port").getAsInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Funnycoin(NodeType type) throws Exception {
        Gson gson = new Gson();
        if(type == NodeType.MINER) {
            FunnycoinCache.blk = new MempoolBlock(new Block("null"),false);
            System.out.println("selected type: miner");
            // NetworkManager manager = new NetworkManager();
            System.out.println("connecting to other people");
            loadConfig();
            System.out.println(port);
            loadBlockChain();
            /**
             * Later on in the class, i define a method that asks other nodes to send their blockchain starting after a block height of what i currently have.
             */
            if(blockChain.size() == 0) {
                peerServer.init();
                peerLoader.init();
                System.out.println("done loading SERVER and PEER");
                if(peerLoader.peers.size() > 0) {
                    for (int k = 0; k < peerLoader.peers.size(); k++) {
                        Peer p = peerLoader.peers.get(k);
                        BeamMessage message = new BeamMessage();
                        message.set("event", "nodejoin");
                        message.set("address", getIp());
                        message.set("port", String.valueOf(peerServer.port));
                        p.socket.queueMessage(message);
                    }
                }
                System.out.println("done sending notification");
                System.out.println("Blockchain empty.");
                Block genesis = FunnycoinCache.getCurrentBlock();
                genesis.transactions.add(new Transaction("coinbase",wallet.getBase64Key(wallet.publicKey),50.0f,"null","FUNNYCOIN"));
                if(genesis.mine(getBlockDifficulty())) {
                    blockChain.add(genesis);
                    syncBlockchainFile();
                    /**
                     * We have set the local blockchain since we know our own block is valid at genesis. who else would make a fraudulent block when they don't know the chain exists;
                     * we are going to send the block to other people now.
                     */
                    peerServer.broadcast(gson.toJson(genesis),"newBlock");
                }
                while (true) {
                    System.out.println("continuing chain.. mining...");
                    mine();
                }
            } else {
                peerServer.init();
                peerLoader.init();
                for(Peer p : peerLoader.peers) {
                    BeamMessage message = new BeamMessage();
                    message.set("event","nodejoin");
                    message.set("address", getIp());
                    message.set("port", String.valueOf(peerServer.port));
                    p.socket.queueMessage(message);
                }

                // getBlocksAfter(getCurrentBlock().height);
                while(true) {
                    mine();
                }
            }
        } else if(type == NodeType.WALLET) {

            FunnycoinCache.loadBlockChain();
            peerLoader.init();
            while(true) {
                Scanner p = new Scanner(System.in);
                String l = p.nextLine();
                if(l != null) {
                    String[] args = l.split(" ");
                    if(args[0].equals("send") && (Float.parseFloat(args[2]) < getBalanceFromChain(wallet.getBase64Key(wallet.publicKey),args[3]))) {
                        JsonObject object = new JsonObject();
                        object.addProperty("ownerWallet",wallet.getBase64Key(wallet.publicKey));
                        object.addProperty("targetWallet",args[1]);
                        object.addProperty("amount",Float.parseFloat(args[2]));
                        object.addProperty("token",args[3]);
                        object.addProperty("version",1);

                        String txHash = SignageUtils.applySha256(wallet.getBase64Key(wallet.publicKey) + args[1] + args[2] + args[3] + 1);
                        String signature = SignageUtils.sign(txHash,wallet.privateKey);
                        object.addProperty("signature", signature);
                        for(Peer a : peerLoader.peers) {
                            BasicMessage beamMessage = new BasicMessage();
                            beamMessage = (BasicMessage) beamMessage.set("message",object.toString());
                            beamMessage = (BasicMessage) beamMessage.set("event","newTransaction");

                            a.socket.queueMessage(beamMessage);
                        }
                    }
                }
            }
        }
    }

    private float getBalanceFromChain(String publicKey, String token) {
        token = token.toUpperCase();
        float balance = 0.0f;
        for(Block block : FunnycoinCache.blockChain) {
            for(Transaction transaction : block.getTransactions()) {
                if((transaction.getOutputKey().contains(publicKey)) && transaction.getToken().equals(token)) {
                    balance += transaction.getAmount();
                }
            }
        }
        return balance;
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        if(args.length == 1) {
            if(args[0].toLowerCase().contains("miner")) {
                new Funnycoin(NodeType.MINER);
            } else if(args[0].toLowerCase().contains("wallet")) {
                new Funnycoin(NodeType.WALLET);
            }
        }
    }





    public enum NodeType {
        MINER,
        WALLET
    }
}
