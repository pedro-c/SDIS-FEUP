package Server;

import java.math.BigInteger;

import static Utilities.Utilities.createHash;

/**
 * Created by mariajoaomirapaulo on 13/05/17.
 */
public class User {

    private String email;
    private BigInteger password;

    public User(String email, BigInteger password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public BigInteger getPassword() {
        return password;
    }

    public BigInteger getUserId(){
        return createHash(email);
    }
}
