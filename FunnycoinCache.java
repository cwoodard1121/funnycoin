package org.funnycoin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.funnycoin.blocks.Block;
import org.funnycoin.blocks.MempoolBlock;
import org.funnycoin.p2p.PeerLoader;
import org.funnycoin.p2p.server.PeerServer;
import org.funnycoin.wallet.Wallet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunnycoinCache {
    public static MempoolBlock blk;

    public static List<Block> blockChain = new ArrayList<>();

    public static int getCirculation() {
        return blockChain.size();
    }

    public static int getReward() {
        return 50;
    }



    public static int getInputReward() {
        return blockChain.size() < 10000 ? 50 : 40;
    }

    public static String getAdHash() throws IOException {
        return String.valueOf(ip.hashCode());
    }

    public static Block getCurrentBlock() {
        if(FunnycoinCache.blockChain.size() == 0) {
            return new Block("genesis");
        }
        return FunnycoinCache.blockChain.get(FunnycoinCache.blockChain.size() - 1);
    }


    public static int getBlockDifficulty() {
        return 6;
    }


    public static PeerServer peerServer = new PeerServer();

    public static Block getNextBlock() {
        return new Block(getCurrentBlock().getHash());
    }

    public static PeerLoader peerLoader = new PeerLoader();

    public static MempoolBlock getTxStatus() {
        if(blk.real) {
            MempoolBlock t = blk;
            blk.real = false;
            return t;
        } else {
            return new MempoolBlock(getNextBlock(),false);
        }
    }


    public static boolean heightCorrect = false;

    public static Wallet wallet;

    static {
        try {
            wallet = new Wallet();
        } catch (NoSuchAlgorithmException | IOException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }
    public static void syncBlockchainFile() {
        try {
            BufferedWriter chainWriter = new BufferedWriter(new FileWriter(new File("blockchain.json")));
            Gson gson = new Gson();
            chainWriter.write(gson.toJson(FunnycoinCache.blockChain));
            chainWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadBlockChain() throws IOException {
        File blockChainFile = new File("blockchain.json");
        BufferedReader chainReader = new BufferedReader(new FileReader(blockChainFile));
        StringBuilder chainBuilder = new StringBuilder();
        String line;
        while((line = chainReader.readLine()) != null) {
            chainBuilder.append(line);
        }
        if(chainBuilder.toString().length() > 1) {
            String json = chainBuilder.toString();
            JsonParser chainParser = new JsonParser();
            JsonArray blockChainArray = (JsonArray) chainParser.parse(json);
            Gson gson = new Gson();
            Block[] blockChain = gson.fromJson(blockChainArray, Block[].class);
            List<Block> blocksList = Arrays.asList(blockChain);
            FunnycoinCache.blockChain.addAll(blocksList);
        }
    }

    public static int getDifficulty = 6;

    public static List<List<Block>> gatheredBlocks = new ArrayList<>();

    public static String ip;

    static {
        try {
            ip = getIp();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int port = -1;

    public static String getIp() throws IOException {
        URL url = new URL("http://checkip.amazonaws.com/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        return "localhost";
        //return reader.readLine();
    }

    public static JsonParser parser = new JsonParser();

}
