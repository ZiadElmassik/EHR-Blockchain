package ehr;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.sql.Timestamp;

public class Doctor implements Serializable {

    public String name;
    public String hashedPass;
    public String email;
    public byte[] salt;
    public HashMap<String, HashMap<String, SecretKey>> patientKeys = new HashMap<String, HashMap<String, SecretKey>>();
    //public HashMap<String, SecretKey> patientKeys = new HashMap<String, SecretKey>();
    public HashSet<String> patientEmails = new HashSet<String>();
    private PrivateKey privateKey;
    public PublicKey publicKey;

    public Doctor(String name, String email, String password) {

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


        }
        catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }

    public void createTransaction(String patientEmail, String condition, String description) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {

        if (!Network.patientNodes.containsKey(patientEmail)){
            System.out.println("Patient Email either does not exist or is not of one your patients, try again.");
            return;
        }

        if(!this.patientEmails.contains(patientEmail)){
            System.out.println("Patient Email either does not exist or is not of one your patients, try again.");
            return;
        }

        String data = "Report Written By Doctor: " + this.email + ", " +  "Patient Email: " + patientEmail + ", " + "Condition: " + condition + ", " + "Description: " + description;

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestampAsString = timestamp.toString();

        SecretKey key = AES.generateKey(128);

        HashMap<String, SecretKey> timeStampKey;
        if (this.patientKeys.containsKey(patientEmail)){
            timeStampKey = this.patientKeys.get(patientEmail);
        }
        else {
            timeStampKey = new HashMap<String, SecretKey>();
        }

        timeStampKey.put(timestampAsString, key);

        this.patientKeys.put(patientEmail, timeStampKey);
        //this.patientKeys.put(patientEmail, key);

        FileOutputStream file = new FileOutputStream("src/Doctors.ser");
        ObjectOutputStream out = new ObjectOutputStream(file);

        Network.doctorNodes.put(this.email, this);
        out.writeObject(Network.doctorNodes);

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

        //Give Patient Symmetric Key
        Patient patient = Network.patientNodes.get(patientEmail);
        patient.doctorKeys.put(this.email, timeStampKey);

        FileOutputStream patientSer = new FileOutputStream("src/Patients.ser");
        ObjectOutputStream outPatSer = new ObjectOutputStream(patientSer);

        Network.patientNodes.put(patientEmail, patient);
        outPatSer.writeObject(Network.patientNodes);

        outPatSer.close();
        patientSer.close();

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

        int unknownCounter = 1;
        for (Map.Entry<String, HashMap<String, SecretKey>> entryEmailTimeStampKey : this.patientKeys.entrySet()){

            String email = entryEmailTimeStampKey.getKey();

            for (Map.Entry<String, SecretKey> entryTimeStampKey : entryEmailTimeStampKey.getValue().entrySet()){

                BlockChain blockChain = Network.server.blockChain;

                for (int i=1; i<blockChain.blockChain.size(); i++){

                    String data = blockChain.blockChain.get(i).getData();
                    String[] dataSplit = data.split("--");
                    String sender = dataSplit[0];
                    String timestamp = dataSplit[1];

                    if (sender.equals(this.email)){

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

        System.out.println("   Patient Email(s) + Time of Transaction    |                      Transaction Data");

        for (Map.Entry<String, String> out : output.entrySet()){

            if(out.getKey().contains("Unknown")){
                System.out.println(out.getKey() + "                                     |    " + out.getValue());
            }
            else {
                System.out.println(out.getKey() + "       |    " + out.getValue());
            }

        }


    }


    @Override
    public String toString() {
        return "Doctor{" +
                "name='" + name + '\'' +
                ", hashedPass='" + hashedPass + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {

//        // Sender Side
//        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DSA");
//        SecureRandom sigRandom = new SecureRandom();
//        keyPairGen.initialize(1024, sigRandom);
//
//        KeyPair pair = keyPairGen.generateKeyPair();
//        PrivateKey privKey = pair.getPrivate();
//        PublicKey pubKey = pair.getPublic();
//
//        Signature sign = Signature.getInstance("SHA256withDSA");
//        sign.initSign(privKey);
//
//        String data = "Patient Email: "  + ", " + "Condition: " + ", " + "Description: ";
//        byte[] bytes = data.getBytes();
//
//        sign.update(bytes);
//        byte[] signature = sign.sign();
//        String signatureToSend = Base64.getEncoder().encodeToString(signature);
//
//        //Receiver Side
//        Signature signReceiver = Signature.getInstance("SHA256withDSA");
//        signReceiver.initVerify(pubKey);
//        byte[] signatureReceived = Base64.getDecoder().decode(signatureToSend);
//        signReceiver.update(bytes);
//
//        boolean verify = signReceiver.verify(signatureReceived);
//        System.out.println(verify);

        Network network = new Network();
        Doctor doc = Network.doctorNodes.get("hatem@gmail.com");
        //doc.createTransaction("ziad@gmail.com", "Tired", "Patient is tired.");
        //System.out.println(doc.patientKeys.toString());
        //doc.createTransaction("khaled@gmail.com", "Zafloot Syndrome", "Patient has a strong tendency to evading situations of potential peril");
        //doc.viewTransactions();
//        //Patient pat = new Patient("Ali", "ali@gmail.com", "Glasses");
//        //Network.patientNodes.put("ali@gmail.com", pat);
//        //System.out.println("Hatem's Symmetric Keys: " + Network.doctorNodes.get("hatem@gmail.com").patientKeys.toString());
//        //System.out.println("Ahmed's Symmetric Keys: " + Network.patientNodes.get("ahmed@gmail.com").doctorKeys.toString());
        Doctor docOne = Network.doctorNodes.get("ayman@gmail.com");
        docOne.viewTransactions();
        //doc.createTransaction("ziad@gmail.com", "Exhausted", "Patient is exhausted.");

        //hatem@gmail.com--OCGtG7HK5pPmpWIzBpiynQ==--MCwCFHssTR7wXpD0Kqti4vALKDMLw+H9AhRIgcV5mr/GYWMTk5ThfQVHRHh5jg==--SORsyun+/PdHkhnqDh1skHPGt3TeMsicboIjM2zKBO9j5+DNNdDRXrx4h7oeqQlc8HV+kfLO14f0MpSD1O/VMPrlfwVeb19rRPfKPkpIJ53h/Bi1neWOJAnkQgz2b3q1uhGjGndqxgbQDsdcZOrZc62hT3EFY6SR/dMhwMapBRI=

    }

}
