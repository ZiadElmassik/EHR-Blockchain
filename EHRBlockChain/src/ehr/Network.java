package ehr;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

public class Network {

    //Email, Public Key Pairs
    static HashMap<String, PublicKey> publicKeyPairs = new HashMap<String, PublicKey>();
    //Email, Type Of Account
    static HashMap<String, String> userTypes = new HashMap<String, String>();
    //ID, Password for Patient
    static HashMap<String, Patient> patientNodes = new HashMap<String, Patient>();
    //ID, Password for Doctor
    static HashMap<String, Doctor> doctorNodes = new HashMap<String, Doctor>();
    //Centralized Server
    static Server server = new Server();

    public Network(){
        File f = new File("src/Doctors.ser");
        if(f.exists()) {
            try {
                // Read From database, Doctors
                FileInputStream fileInput = new FileInputStream("src/Doctors.ser");
                ObjectInputStream objectInput = new ObjectInputStream(fileInput);
                Network.doctorNodes = (HashMap)objectInput.readObject();
                objectInput.close();
                fileInput.close();
            }

            catch (IOException | ClassNotFoundException obj1) {
                obj1.printStackTrace();
                return;
            }
        }

        File n = new File("src/Nodes.ser");
        if (n.exists()){

            try {
                // Read From database, users and their types
                FileInputStream secondFileInput = new FileInputStream("src/Nodes.ser");
                ObjectInputStream secondObjectInput = new ObjectInputStream(secondFileInput);
                Network.userTypes = (HashMap) secondObjectInput.readObject();
                secondObjectInput.close();
                secondFileInput.close();
            }

            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        File p = new File("src/PublicKeys.ser");
        if(p.exists()) {

            try{
                //Read from database, the public keys
                FileInputStream fileInput = new FileInputStream("src/PublicKeys.ser");
                ObjectInputStream objectInput = new ObjectInputStream(fileInput);
                Network.publicKeyPairs = (HashMap) objectInput.readObject();
                objectInput.close();
                fileInput.close();
            }
            catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }

        }

        File pa = new File("src/Patients.ser");
        if(pa.exists()) {

            try{
                //Read from database, the public keys
                FileInputStream fileInput = new FileInputStream("src/Patients.ser");
                ObjectInputStream objectInput = new ObjectInputStream(fileInput);
                Network.patientNodes = (HashMap) objectInput.readObject();
                objectInput.close();
                fileInput.close();
            }
            catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }

        }

        File b = new File("src/Blockchain.ser");
        if(b.exists()) {

            try{
                //Read from database, the public keys
                FileInputStream fileInput = new FileInputStream("src/Blockchain.ser");
                ObjectInputStream objectInput = new ObjectInputStream(fileInput);
                Network.server.blockChain = (BlockChain) objectInput.readObject();
                objectInput.close();
                fileInput.close();
            }
            catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }

        }

        System.out.println(doctorNodes.toString());
        System.out.println(patientNodes.toString());
        System.out.println(userTypes.toString());
        System.out.println(server.blockChain.toString());
        //System.out.println(publicKeyPairs.toString());
        System.out.println();

    }

    public static void registration(String typeOfAccount, String name, String email, String password) throws IOException {



        if (typeOfAccount.equals("Doctor")){
            Doctor newDoc = new Doctor(name, email, password);
            Network.doctorNodes.put(email, newDoc);
            FileOutputStream file = new FileOutputStream("src/Doctors.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);

            out.writeObject(Network.doctorNodes);

            out.close();
            file.close();

            Network.userTypes.put(email, "Doctor");
            FileOutputStream fileNodes = new FileOutputStream("src/Nodes.ser");
            ObjectOutputStream outNodes = new ObjectOutputStream(fileNodes);

            outNodes.writeObject(Network.userTypes);

            outNodes.close();
            fileNodes.close();

        }

        else if(typeOfAccount.equals("Client")){
            Patient newPatient = new Patient(name, email, password);
            Network.patientNodes.put(email, newPatient);
            FileOutputStream file = new FileOutputStream("src/Patients.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);

            out.writeObject(Network.patientNodes);

            out.close();
            file.close();

            Network.userTypes.put(email, "Patient");
            FileOutputStream fileNodes = new FileOutputStream("src/Nodes.ser");
            ObjectOutputStream outNodes = new ObjectOutputStream(fileNodes);

            outNodes.writeObject(Network.userTypes);

            outNodes.close();
            fileNodes.close();

        }

        System.out.println("Registration Successful.");

    }

    public static void authentication(String email, String password) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, SignatureException, BadPaddingException, InvalidKeyException {

        boolean verification = false;
        //System.out.println("Authentication started");
        if (Network.userTypes.containsKey(email)){

            if (Network.doctorNodes.containsKey(email)){

                Doctor doc = Network.doctorNodes.get(email);
                byte[] salt = doc.salt;

                String hashedPass = "";

                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-512");
                    md.update(salt);
                    byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
                    hashedPass = Base64.getEncoder().encodeToString(hashedPassword);

                }
                catch (NoSuchAlgorithmException e) {
                    System.out.println("Here");
                    e.printStackTrace();
                }

                //System.out.println("Password to Authenticate: " + hashedPass);
                //System.out.println("Password to Match: " + doc.hashedPass);
                if (hashedPass.equals(doc.hashedPass)){
                    System.out.println("Verification Successful.");
                    verification = true;
                }
                else{
                    System.out.println("Password is incorrect. Please try to sign in again.");
                }

                //System.out.println("Hashed Pass: " + this.hashedPass);

            }

            else if (Network.patientNodes.containsKey(email)){

                Patient pat = Network.patientNodes.get(email);
                byte[] salt = pat.salt;

                String hashedPass = "";

                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-512");
                    md.update(salt);
                    byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
                    hashedPass = Base64.getEncoder().encodeToString(hashedPassword);

                }
                catch (NoSuchAlgorithmException e) {
                    System.out.println("Here");
                    e.printStackTrace();
                }

                //System.out.println("Password to Authenticate: " + hashedPass);
                //System.out.println("Password to Match: " + doc.hashedPass);
                if (hashedPass.equals(pat.hashedPass)){
                    System.out.println("Verification Successful.");
                    verification = true;
                }
                else{
                    System.out.println("Password is incorrect. Please try to sign in again.");
                }

            }

        }

        if (verification){

            System.out.println("Welcome. Which operation would you like to perform?");
            System.out.println("View your transactions or create a new transaction?");


            performOperation(email);

        }

    }

    public static void performOperation(String email) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, SignatureException, BadPaddingException, InvalidKeyException {

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String operationOfChoice = input.readLine();

        if (operationOfChoice.equals("View")){

            if (Network.doctorNodes.containsKey(email)){

                Doctor doc = Network.doctorNodes.get(email);
                doc.viewTransactions();

            }
            else if (Network.patientNodes.containsKey(email)){

                Patient pat = Network.patientNodes.get(email);
                pat.viewTransactions();

            }

        }

        else if(operationOfChoice.equals("Create")){

            if (Network.doctorNodes.containsKey(email)){

                Doctor doc = Network.doctorNodes.get(email);

                System.out.println("Please enter the following:");
                System.out.println("Patient Email:");
                String patientEmail = input.readLine();
                System.out.println("Condition:");
                String condition = input.readLine();
                System.out.println("Description of the Patient's Condition: ");
                String description = input.readLine();

                doc.createTransaction(patientEmail, condition, description);

                input.close();

            }

            else if (Network.patientNodes.containsKey(email)){

                Patient pat = Network.patientNodes.get(email);

                System.out.println("Please enter the following:");
                System.out.println("Doctor's Email:");
                String doctorEmail = input.readLine();
                System.out.println("Reason of Transaction:");
                String reasonOfTransaction = input.readLine();

                pat.createTransaction(doctorEmail, reasonOfTransaction);
                input.close();

            }

        }
        input.close();

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, SignatureException, BadPaddingException, InvalidKeyException {

        Network network = new Network();

        System.out.println("Hello! Would you like to sign up or sign in?");
        BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
        String signInOrUp = inp.readLine();
        //System.out.println(signInOrUp);

        if (signInOrUp.contains("Up")){
            System.out.println("Please fill the following credentials: ");
            System.out.println("Are you registering as a doctor or client?");
            String docClient = inp.readLine();

            System.out.println("Your name, Please: ");
            String docClientName = inp.readLine();

            boolean emailTaken = false;
            System.out.println("Your e-mail, Please: ");
            String docClientEmail = inp.readLine();
            if(Network.userTypes.containsKey(docClientEmail)){
                emailTaken = true;
                System.out.println("E-mail is already taken, please pick another: ");
                while (emailTaken){
                    docClientEmail = inp.readLine();
                    if(!Network.userTypes.containsKey(docClientEmail)){
                        emailTaken = false;
                    }
                    else{
                        System.out.println("E-mail is already taken, please pick another: ");
                        docClientEmail = inp.readLine();
                    }
                }
            }

            System.out.println("Please enter a password: ");
            String docClientPassword = inp.readLine();

            System.out.println("Entered Pass: " + docClientPassword);
            inp.close();
            Network.registration(docClient, docClientName, docClientEmail, docClientPassword);
        }
        else if(signInOrUp.contains("In")){
            System.out.println("Please enter your email and password: ");
            System.out.println("Email:");
            String email = inp.readLine();

            if(!Network.userTypes.containsKey(email)){
                boolean emailIncorrect = true;
                System.out.println("E-mail is incorrect, try again: ");
                while (emailIncorrect){
                    email = inp.readLine();
                    if(Network.userTypes.containsKey(email)){
                        emailIncorrect = false;
                    }
                    else{
                        System.out.println("E-mail is incorrect, please try again: ");
                        email = inp.readLine();
                    }
                }
            }
            System.out.println("Password, Please: ");
            String password = inp.readLine();

            Network.authentication(email, password);

        }
    }
}
