package ehr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class BlockChain implements Serializable {

    public ArrayList<Block> blockChain = new ArrayList<Block>();
    public Block genesisBlock;
    int prefix = 4;
    String prefixString = new String(new char[prefix]).replace('\0', '0');

    @Override
    public String toString() {
        return "BlockChain{" +
                "blockChain=" + blockChain +
                '}';
    }

    public  BlockChain(){
        this.genesisBlock = new Block("", "", new Date().getTime());
        //this.genesisBlock.
        this.blockChain.add(this.genesisBlock);

    }

    public void insertData(String data){
            addBlock(data);

    }

    public void addBlock(String data){

        Block newBlock = new Block(data, this.blockChain.get(this.blockChain.size()-1).getHash(), new Date().getTime());
        this.blockChain.add(newBlock);
    }

    public void givenBlockchain_whenValidated_thenSuccess() {
        boolean flag = true;
        for (int i = 0; i < blockChain.size(); i++) {
            if (!blockChain.get(i).getHash().equals(blockChain.get(i).calculateBlockHash())){
                System.out.println("Blockchain is not valid.");
                return;
            }
        }

        for (int i=1; i<blockChain.size(); i++){
            if(!blockChain.get(i-1).getHash().equals(blockChain.get(i).getPreviousHash())){
                System.out.println("Blockchain is not valid.");
                return;
            }
        }

        System.out.println("Blockchain is valid.");
    }

    public static void main(String[] args){
        BlockChain sampleBlockChain = new BlockChain();

        sampleBlockChain.insertData("Test1");

        sampleBlockChain.insertData("Test2");

        Network network = new Network();

        Network.server.blockChain.givenBlockchain_whenValidated_thenSuccess();

        //sampleBlockChain.givenBlockchain_whenValidated_thenSuccess();


    }

}
