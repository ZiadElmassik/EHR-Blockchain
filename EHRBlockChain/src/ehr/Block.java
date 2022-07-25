package ehr;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block implements Serializable {
    @Override
    public String toString() {
        return "Block{" +
                "hash='" + hash + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setData(String data) {
        this.data = data;
    }

    private String hash;
    private String previousHash;
    private String data;
    private long timestamp;

    public Block(String data, String previousHash, long timestamp){

        this.data = data;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.hash = calculateBlockHash();
    }

    public String calculateBlockHash(){

        String dataToHash = previousHash + Long.toString(timestamp) + data;
        MessageDigest digest = null;
        byte[] bytes = null;
        try{
            digest = MessageDigest.getInstance("SHA-256");
            bytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        StringBuffer buffer = new StringBuffer();
        for (byte b: bytes){
            buffer.append(String.format("%02x", b));
        }

        return  buffer.toString();
    }

}
