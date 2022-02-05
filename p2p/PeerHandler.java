package org.funnycoin.p2p.server;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.funnycoin.Funnycoin;
import org.funnycoin.FunnycoinCache;
import org.funnycoin.blocks.Block;
import org.funnycoin.blocks.MempoolBlock;
import org.funnycoin.p2p.Peer;
import org.funnycoin.p2p.PeerLoader;
import org.funnycoin.p2p.RequestParams;
import org.funnycoin.transactions.Transaction;
import org.funnycoin.wallet.SignageUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;


public class PeerHandler extends BeamHandler {
    public PeerHandler() {
        super(0);
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
    public String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void g() {
        System.out.println("A");
    }



    @Override
    public BeamMessage messageReceived(Communicator communicator, BeamMessage beamMessage) {
        String msg = beamMessage.get("message");
        Gson gson = new Gson();
        if(beamMessage.get("event").toLowerCase().contains("newtransaction")) {
            JsonParser parser = new JsonParser();
            JsonObject obj = (JsonObject) parser.parse(beamMessage.get("message"));
            System.out.println("TRANSACTION RECIEVED: " + beamMessage.get("message"));
            System.out.println("whats up");
            System.out.println(getBalanceFromChain(obj.get("ownerWallet").getAsString(),obj.get("token").getAsString()) + " AA" + obj.get("amount").getAsFloat());
            if(getBalanceFromChain(obj.get("ownerWallet").getAsString(),obj.get("token").getAsString()) > obj.get("amount").getAsFloat()) {
                System.out.println("balance valid");
                // Think it works.
                RequestParams.interrupted = true;
                String ownerWallet = obj.get("ownerWallet").getAsString();
                String targetWallet = obj.get("targetWallet").getAsString();
                float amount = obj.get("amount").getAsFloat();
                String token = obj.get("token").getAsString();
                int version = obj.get("version").getAsInt();
                String signature = obj.get("signature").getAsString();
                System.out.println(signature + " sig");

                String txHash = applySha256(ownerWallet + targetWallet + amount + token + version);

                Transaction b = new Transaction(obj.get("ownerWallet").getAsString(),obj.get("targetWallet").getAsString(),obj.get("amount").getAsFloat(),signature,token);
                try {
                    System.out.println(b.verify(txHash,signature, SignageUtils.getPublicKey(ownerWallet)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if(b.verify(txHash,signature, SignageUtils.getPublicKey(ownerWallet))) {
                        System.out.println("sig valid");
                         /*
                         Transaction was signed and is valid and is not double spending.
                         */
                        Block nee = FunnycoinCache.getNextBlock();
                        nee.transactions.add(b);
                        FunnycoinCache.blk = new MempoolBlock(nee,true);
                        RequestParams.interrupted = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        if(beamMessage.get("event").toLowerCase().contains("heightresponse")) {
            System.out.println("heightresponse, " + (FunnycoinCache.blockChain.size() < 1 ? 0 : FunnycoinCache.blockChain.size() - 1));
            System.out.println(Integer.parseInt(beamMessage.get("height")));
            if((FunnycoinCache.blockChain.size() < 1 ? 0 : FunnycoinCache.blockChain.size() - 1) < Integer.parseInt(beamMessage.get("height"))) {
                FunnycoinCache.peerLoader.peers.forEach(p -> {
                    try {
                        final int size = FunnycoinCache.blockChain.size() < 1 ? 0 : FunnycoinCache.blockChain.size() - 1;
                        for(int i = size; i < Integer.parseInt(beamMessage.get("height")); i++) {
                            RequestParams.interrupted = true;
                            p.socket.queueMessage(new BasicMessage().set("event","getBlock").set("adHash",FunnycoinCache.getAdHash()).set("block",String.valueOf(i)));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            RequestParams.needsBlocks = false;
        }
        if(beamMessage.get("event").toLowerCase().contains("getheight")) {
            String adHash = beamMessage.get("adHash");
            System.out.println("getheight: " + adHash);
            BasicMessage m = new BasicMessage();
            Gson g = new Gson();
            m.set("event","heightResponse");
            m.set("adHash",adHash);
            m.set("height", String.valueOf(FunnycoinCache.blockChain.size() < 1 ? 0 : FunnycoinCache.blockChain.size() - 1));
            return m;
        }

        if(beamMessage.get("event").toLowerCase().contains("blockresponse")) {
            try {
                if(beamMessage.get("adHash").toLowerCase().equals(FunnycoinCache.getAdHash())) {
                    Block b = new Gson().fromJson(beamMessage.get("blockData"),Block.class);
                    List<Block> blockchain = FunnycoinCache.blockChain;
                    blockchain.add(b);
                    if(isChainValid(blockchain)) {
                        FunnycoinCache.blockChain.set(b.height,b);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(beamMessage.get("event").toLowerCase().contains("getblock")) {
            String adHash = beamMessage.get("adHash");
            int block = Integer.parseInt(beamMessage.get("block"));
            if(block < FunnycoinCache.blockChain.size() - 1) {
                BasicMessage m = new BasicMessage();
                Gson g = new Gson();
                m.set("event","blockResponse");
                m.set("adHash",adHash);
                m.set("blockData",g.toJson(FunnycoinCache.blockChain.get(block)));
                return m;
            }
        }
        if(beamMessage.get("event").toLowerCase().contains("newblock")) {
            List<Block> tempChain = new ArrayList<>(FunnycoinCache.blockChain);
            tempChain.add(gson.fromJson(msg,Block.class));
            try {
                if(isChainValid(tempChain)) {
                    RequestParams.interrupted = true;
                    System.out.println("chain is valid.");
                    FunnycoinCache.blockChain = tempChain;
                    FunnycoinCache.syncBlockchainFile();
                    System.out.println("synced blockchain file");
                } else {
                    System.out.println("the chain is not valid");
                }
            } catch (Exception e) {
                //TODO: REMOVE LATER BECAUSE IT WILL BE SPAMMY
                System.out.println("exception");
                e.printStackTrace();
            }
        } else if(beamMessage.get("event").toLowerCase().contains("nodejoin")) {
            System.out.println("A node has logged on.");
            String address = beamMessage.get("address");
            int port = Integer.parseInt(beamMessage.get("port"));
            System.out.println(address + ":" + port);
            Peer p = new Peer(address, port);
            RequestParams.test = true;
            if (PeerLoader.peers.size() < 1) {
                PeerLoader.peers.add(p);
                try {
                    if(p.peerIsOnline()) {
                        p.connectToPeer();
                        System.out.println("added peer to array");
                        PeerLoader.peers.add(p);
                        FunnycoinCache.peerLoader.syncPeerFile();
                        RequestParams.peerAdded = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                for (int k = 0; k < PeerLoader.peers.size(); k++) {
                    Peer j = PeerLoader.peers.get(k);
                    if (p.port != j.port) {
                        try {
                            PeerLoader.peers.add(p);
                            FunnycoinCache.peerLoader.syncPeerFile();
                            System.out.println("added peer to peers.");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.err.println("couldn't write to peer file for some reason:" + e.getMessage());
                        }
                        PeerLoader.peers.get(PeerLoader.peers.size()).connectToPeer();
                    }
                }
            }
        }
        return new BasicMessage().set("event","null");
    }


    private boolean isChainValid(List<Block> blockChain) throws Exception {
        boolean valid = false;
        for(int i = 0; i < blockChain.size(); i++) {
            Block currentBlock = blockChain.get(i);
            if(blockChain.size() > 1 && i != 0) {
                Block previousBlock = blockChain.get(i - 1);
                /**
                 * Performing checks on the temporary blockchain to see if it's valid.
                 */
                if(!currentBlock.hash.equals(currentBlock.getHash())) {
                    System.out.println("The hash of the block is not equal to the calculated value.");
                    return false;
                }
                if(!previousBlock.hash.equals(previousBlock.getHash())) {
                    System.out.println("The hash of the previous block is not equal to the calculated value.");
                    return false;
                }
                int difficulty = 6;
                String difficultyTarget = new String(new char[difficulty]).replace('\0','0');

                if(!currentBlock.hash.substring(0,difficulty).equals(difficultyTarget)) {
                    System.out.println("The block does not have a Proof-Of-Work attached to it.");
                    return false;
                }
                /**
                 * Checking transactions for validity or errors.
                 */
                for(int j = 0; j < currentBlock.getTransactions().size(); j++) {
                    Transaction currentTransaction = currentBlock.transactions.get(j);
                    System.out.println("Transaction sender,reciever:" + currentTransaction);
                    if(currentTransaction.getOwnerKey().toLowerCase().equals("coinbase")) {
                        valid = true;
                        continue;
                    }
                    if(!currentTransaction.verify(currentTransaction.getHash(),currentTransaction.signature, SignageUtils.getPublicKey(currentTransaction.getOwnerKey()))) {
                        return false;
                    }
                }


            } else {
                /**
                 * We are going to just say it's correct because it's at genesis, no one knows about the chain so an attack is improbable.
                 */
                valid = true;
            }
        }
        return valid;
    }
}
