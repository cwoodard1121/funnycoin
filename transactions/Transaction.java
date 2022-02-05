package org.funnycoin.transactions;


import org.funnycoin.wallet.SignageUtils;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Transaction {
    String ownerKey;
    String outputKey;
    float amount;
    public String transactionId;
    public String signature;
    public String token;
    public Transaction(String ownerKey, String outputKey, float amount, String signature, String token) {
        this.token = token;
        this.signature = signature;
        this.outputKey = outputKey;
        this.ownerKey = ownerKey;
        this.amount = amount;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public String getToken() {
        return token;
    }

    public float getAmount() {
        return amount;
    }

    public boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(plainText.getBytes(UTF_8));
        return ecdsaVerify.verify(Base64.getDecoder().decode(signature));
    }


    public String getHash() {
        return applySha256(ownerKey + outputKey + amount + signature + token);
    }

    public String applySha256(String input){
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
}
