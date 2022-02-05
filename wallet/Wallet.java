package org.funnycoin.wallet;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.funnycoin.FunnycoinCache;
import org.funnycoin.blocks.Block;
import org.funnycoin.transactions.Transaction;

import java.io.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;

    public Wallet() throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {
        File walletFile = new File("wallet.json");
        if(walletFile.exists()) {
            BufferedReader walletReader = new BufferedReader(new FileReader(walletFile));
            String tempLine;
            if((tempLine = walletReader.readLine()) != null) {
                JsonObject wallet = JsonParser.parseString(tempLine).getAsJsonObject();
                privateKey = (PrivateKey) SignageUtils.getPrivateKey(wallet.get("privateKey").getAsString());
                publicKey = (PublicKey) SignageUtils.getPublicKey(wallet.get("publicKey").getAsString());
            }
        } else {
            generateKeyPair();
        }
    }


    public void generateKeyPair() throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(ecSpec, new SecureRandom());
        KeyPair keypair = g.generateKeyPair();
        PublicKey publicKey = keypair.getPublic();
        PrivateKey privateKey = keypair.getPrivate();
        File walletFile = new File("wallet.json");
        walletFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(walletFile));
        JsonObject object = new JsonObject();
        object.addProperty("publicKey",getBase64Key(publicKey));
        object.addProperty("privateKey",getBase64Key(privateKey));
        writer.write(object.toString());
        writer.close();
    }




    public String getBase64Key(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }



    public float getBalanceFromChain(PublicKey publicKey) {
        float balance = 0.0f;
        for(Block block : FunnycoinCache.blockChain) {
            for(Transaction transaction : block.getTransactions()) {
                if((transaction.getOutputKey().contains(getBase64Key(publicKey)))) {
                    balance += transaction.getAmount();
                } else if(transaction.getOwnerKey().contains(getBase64Key(publicKey))) {
                    balance -= transaction.getAmount();
                }
            }
        }
        return balance;
    }

}
