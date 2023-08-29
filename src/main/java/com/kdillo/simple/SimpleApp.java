package com.kdillo.simple;

import com.kdillo.simple.db.PostgresqlConnectionProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;

import java.sql.*;
import java.util.UUID;

public class SimpleApp {

    private static final String SHA512 = "SHA512";

    private static final Logger LOGGER = LogManager.getLogger(SimpleApp.class);

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    //initialize a random number generator when the app launches
    public static final SecureRandom rnd = new SecureRandom();

    private static Properties props = null;

    public static void main(String[] args){

        try {
            loadApplicationProperties();

//            PGSimpleDataSource ds = new PGSimpleDataSource();
            PostgresqlConnectionProvider pgConProvider = new PostgresqlConnectionProvider(props);

            DummyRun(pgConProvider);

            //main loop
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void DummyRun(PostgresqlConnectionProvider pgConProvider) {
        //just a dummy run to see if I can make the database access work.

        try (Connection conn = pgConProvider.getConnection()) {

            //getting a user from the users table, print all the info from the rows returned.
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users WHERE email = 'kdillo893@gmail.com'");
            while (rs.next()) {

                //getting values for each of the columns: uid, first_name, last_name, email, email, created, updated, pass_hash, pass_salt.

                String uid = rs.getString("uid");
                String fname = rs.getString("first_name");
                String lname = rs.getString("last_name");
                String email = rs.getString("email");
                Date created = rs.getDate("created");
                Date updated = rs.getDate("updated");
                String passHash = rs.getString("pass_hash");
                String passSalt = rs.getString("pass_salt");

                System.out.printf("uid=%s, fname=%s, lname=%s, email=%s, created=%s, updated=%s, passHash=%s, passSalt=%s\n", uid, fname, lname, email, created, updated, passHash, passSalt);
            }

            rs.close();
            st.close();

            //        conn.close();
            //        conn = createNewDbConnection();

            //testing to update the passHash of my user row with an update statement.rAN
            //make a password hash out of random salt
            String newSalt = randomString(32);
            String passHash = digestMessageWithAlg("silly-Thing123;xD", SHA512);

            UUID myUid = UUID.fromString("7d95e88a-f10f-4b20-8303-e26db72ddd74");
            PreparedStatement pst = conn.prepareStatement("update users set pass_hash = ?, pass_salt = ? where uid = ? ;;");

            pst.setString(1, passHash);
            pst.setString(2, newSalt);
            pst.setObject(3, myUid);

            //execute update, returns just the number of effected rows.
            int rowsUpdated = pst.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.printf("rowsUpdated=%d, passHash=%s, passSalt=%s, UUID=%s", rowsUpdated, passHash, newSalt, myUid);
            }

            rs.close();
            st.close();

        //can handle sqlexception differently later
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Got from reference online; should move this to where it will be relevant (either User or different file later)
     * @param message
     * @param alg
     * @return
     */
    protected static String digestMessageWithAlg(String message, String alg) {

        try {
            MessageDigest md = MessageDigest.getInstance(alg);

            byte[] messageDigest = md.digest(message.getBytes());

            //convert byte into signum representation? seems like this is for proper char bytesize conversion.
            //converts the very large value into string simply
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtextSb = new StringBuilder(no.toString(16));

            //if the hash is too small, fill with 0's
            while(hashtextSb.length() < 32) hashtextSb.insert(0, '0');

            return hashtextSb.toString();

        } catch (NoSuchAlgorithmException ex) {
            System.err.printf("Couldn't find algorithm: %s", alg);
            throw new RuntimeException(ex);
        } catch (Exception ex) {
           // unknown
        }


        return null;
    }

    public static String randomString(int len){
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++){
            sb.append(ALPHANUMERIC.charAt(rnd.nextInt(ALPHANUMERIC.length())));
        }

        LOGGER.debug("random string produced: " + sb.toString());

        return sb.toString();
    }

    private static void loadApplicationProperties() {

        try (InputStream propsStream = new FileInputStream("src/main/resources/config.properties")) {
            props = new Properties();

            props.load(propsStream);
        } catch (IOException ex) {
            LOGGER.debug("Unable to load app properties");
            ex.printStackTrace();
        }
    }
}
