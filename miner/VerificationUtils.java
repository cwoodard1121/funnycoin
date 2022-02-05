package org.funnycoin.miner;
/**
 * Trust me there will be a reason for it being in a different package in the future, trust me.
 */

import org.funnycoin.blocks.Block;
import org.funnycoin.transactions.Transaction;
import org.funnycoin.wallet.SignageUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class VerificationUtils {
    /**
     **
     * @param plainText
     * @param signature
     * @param publicKey
     * @return boolean
     * @throws Exception
     */
    public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("RSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes(UTF_8));

        byte[] signatureBytes = Base64.getDecoder().decode(signature);

        return publicSignature.verify(signatureBytes);
    }
    public static boolean isChainValid(List<Block> blockChain) throws Exception {
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
