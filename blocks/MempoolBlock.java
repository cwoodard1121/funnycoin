package org.funnycoin.blocks;

public class MempoolBlock {
    public Block block;
    public boolean real;

    public MempoolBlock(Block b, boolean real) {
        this.block = b;
        this.real = real;
    }
}
