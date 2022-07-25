package ehr;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.print.Doc;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.util.*;

public class Patient implements Serializable {

    public String name;
    public String hashedPass;
    public String email;
    public byte[] salt;
    public HashMap<String, HashMap<String, SecretKey>> doctorKeys = new HashMap<String, HashMap<String, SecretKey>>();
    //public HashMap<String, SecretKey> doctorKeys = new HashMap<String, SecretKey>();
    public HashSet<String> doctorEmails = new HashSet<String>();
    //public BlockChain patientChain = new BlockChain();

    private PrivateKey privateKey;
    public PublicKey publicKey;

    @Override
    public String toString() {
        return "Patient{" +
                "name='" + name + '\'' +
                ", hashedPass='" + hashedPass + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public Patient(String name, String email, String password){

        this.name = name;
        this.email = email;
        this.hashedPass = "";

        //Salt and hash password
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        this.salt = salt;

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            this.hashedPass = Base64.getEncoder().encodeToString(hashedPassword);

        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Here");
            e.printStackTrace();
        }
        System.out.println("Hashed Pass: " + this.hashedPass);

        //Generate Public and Private Key
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DSA");
            SecureRandom sigRandom = new SecureRandom();
            keyPairGen.initialize(1024, sigRandom);

            KeyPair pair = keyPairGen.generateKeyPair();
            PrivateKey privKey = pair.getPrivate();
            PublicKey pubKey = pair.getPublic();

            this.privateKey = privKey;
            this.publicKey = pubKey;

            System.out.println(this.publicKey.toString());

            // Add the new Public Key Entry and store
            FileOutputStream file = new FileOutputStream("src/PublicKeys.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);

            Network.publicKeyPairs.put(email, this.publicKey);
            out.writeObject(Network.publicKeyPairs);

            out.close();
            file.close();

//            System.out.println("Print statement to verify that network content is intact: ");
//            System.out.println("Doctors: " + Network.doctorNodes.toString());
//            System.out.println("Patients: " + Network.patientNodes.toString());
//            System.out.println("User Types: " + Network.userTypes.toString());
//            System.out.println("Public Key Pairs: " + Network.publicKeyPairs.toString());


        }
        catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }

    public void createTransaction(String doctorEmail, String reasonOfTransaction) throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, SignatureException {

        if (!Network.doctorNodes.containsKey(doctorEmail)){
            System.out.println("Patient Email does not exist, try again.");
            return;
        }

        String data = "Request made by Client: " + this.email + " to Doctor: " +  ": " + doctorEmail + ", " + "Condition: " + reasonOfTransaction;

        java.sql.Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestampAsString = timestamp.toString();

        SecretKey key = AES.generateKey(128);

        HashMap<String, SecretKey> timeStampKey;
        if (this.doctorKeys.containsKey(doctorEmail)){
            timeStampKey = this.doctorKeys.get(doctorEmail);
        }
        else {
            timeStampKey = new HashMap<String, SecretKey>();
        }

        timeStampKey.put(timestampAsString, key);

        this.doctorKeys.put(doctorEmail, timeStampKey);
        this.doctorEmails.add(doctorEmail);

        FileOutputStream file = new FileOutputStream("src/Patients.ser");
        ObjectOutputStream out = new ObjectOutputStream(file);

        Network.patientNodes.put(this.email, this);
        out.writeObject(Network.patientNodes);

        out.close();
        file.close();

        //Add symmetric key entry to the patient
        byte[] iv = AES.generateIv();
        String ivAsString = Base64.getEncoder().encodeToString(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        String algorithm = "AES/CBC/PKCS5Padding";
        String cipherText = AES.encrypt(algorithm, data, key, ivParameterSpec);

        //Digital Signature applied to encrypted data
        Signature sign = Signature.getInstance("SHA256withDSA");
        sign.initSign(this.privateKey);

        byte[] bytes = cipherText.getBytes();
        sign.update(bytes);
        byte[] signature = sign.sign();
        String signatureToSend = Base64.getEncoder().encodeToString(signature);

        //Create Data Packet
        String dataPacket = this.email + "--" + timestampAsString + "--" + ivAsString + "--" + signatureToSend + "--" + cipherText;

        //Give Doctor Symmetric Key
        Doctor doctor = Network.doctorNodes.get(doctorEmail);
        doctor.patientEmails.add(this.email);
        doctor.patientKeys.put(this.email, timeStampKey);

        FileOutputStream doctorFile = new FileOutputStream("src/Doctors.ser");
        ObjectOutputStream outDoctor = new ObjectOutputStream(doctorFile);

        Network.doctorNodes.put(doctorEmail, doctor);
        outDoctor.writeObject(Network.doctorNodes);

        outDoctor.close();
        doctorFile.close();

        //Add Data Packet to Blockchain
        Network.server.blockChain.insertData(dataPacket);

        FileOutputStream patientFile = new FileOutputStream("src/Blockchain.ser");
        ObjectOutputStream outPatient = new ObjectOutputStream(patientFile);

        outPatient.writeObject(Network.server.blockChain);

        outPatient.close();
        patientFile.close();

        for (int i=0; i<Network.server.blockChain.blockChain.size(); i++){
            System.out.println("Block Data: " + Network.server.blockChain.blockChain.get(i).getData());
            System.out.println("Block TimeStamp: " + Network.server.blockChain.blockChain.get(i).getTimestamp());
            System.out.println("Block Hash: " + Network.server.blockChain.blockChain.get(i).getHash());
            System.out.println("Block Previous Hash: " + Network.server.blockChain.blockChain.get(i).getPreviousHash());
        }
        System.out.println("Operation Performed Successfully");

    }

    public void viewTransactions() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

        TreeMap<String, String> output = new TreeMap<String, String>();

//        for (int i=1; i<Network.server.blockChain.blockChain.size(); i++){
//
//            String[] dataPacket = Network.server.blockChain.blockChain.get(i).getData().split("--");
//            String sender = dataPacket[0];
//            String timestamp = dataPacket[1];
//
//            if (sender.equals(this.email) || this.doctorEmails.contains(sender)){
//
//                SecretKey symmetricKey =
//
//            }
//            else {
//
//            }
//
//        }


        int unknownCounter = 1;
        for (Map.Entry<String, HashMap<String, SecretKey>> entryEmailTimeStampKey : this.doctorKeys.entrySet()){

            String email = entryEmailTimeStampKey.getKey();

            for (Map.Entry<String, SecretKey> entryTimeStampKey : entryEmailTimeStampKey.getValue().entrySet()){

                for (int i=1; i<Network.server.blockChain.blockChain.size(); i++){

                    String data = Network.server.blockChain.blockChain.get(i).getData();
                    String[] dataSplit = data.split("--");
                    String sender = dataSplit[0];
                    String timestamp = dataSplit[1];

                    if (sender.equals(this.email) || this.doctorEmails.contains(sender)){

                        if (timestamp.equals(entryTimeStampKey.getKey())){

                            SecretKey symmetricKey = entryTimeStampKey.getValue();

                            String ivAsString = dataSplit[2];
                            String message = dataSplit[4];
                            byte[] bytes = message.getBytes();

                            String signatureAsString = data.split("--")[3];
                            PublicKey pubKey = Network.publicKeyPairs.get(sender);

                            Signature signReceiver = Signature.getInstance("SHA256withDSA");
                            signReceiver.initVerify(pubKey);
                            byte[] signatureReceived = Base64.getDecoder().decode(signatureAsString);
                            signReceiver.update(bytes);

                            boolean verify = signReceiver.verify(signatureReceived);

                            if(verify){
                                String algorithm = "AES/CBC/PKCS5Padding";
                                byte[] ivDecoded = Base64.getDecoder().decode(ivAsString);
                                IvParameterSpec ivReceiverSide = new IvParameterSpec(ivDecoded);
                                String plainText = AES.decrypt(algorithm, message, symmetricKey, ivReceiverSide);
                                output.put(email + ":" + entryTimeStampKey.getKey(), plainText);

                            }
                            else {
                                System.out.println("Verification failed");
                            }

                        }

                    }
                    else if(!output.values().contains(dataSplit[dataSplit.length-1])){
                        output.put("Unknown" + unknownCounter, dataSplit[dataSplit.length-1]);
                        unknownCounter++;
                    }

                }
            }

        }

        System.out.println("             Doctor Email(s)                  |                    Transaction Data");

        for (Map.Entry<String, String> out : output.entrySet()){

            if(out.getKey().contains("Unknown")){
                System.out.println(out.getKey() + "                                      |    " + out.getValue());
            }
            else {
                System.out.println(out.getKey() + "       |    " + out.getValue());
            }

        }

    }


    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, SignatureException, BadPaddingException, InvalidKeyException, IOException {
        Network network = new Network();

        //Patient pat = Network.patientNodes.get("ziad@gmail.com");
        //System.out.println(pat.doctorKeys.toString());
        //pat.createTransaction("hatem@gmail.com", "I'm tired");
        //pat.viewTransactions();

        Patient patOne = Network.patientNodes.get("ziad@gmail.com");
        patOne.viewTransactions();
    }

}
