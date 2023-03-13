package com.mailservivepoc.demo.service;

import com.mailservivepoc.demo.dal.UserRepository;
import com.mailservivepoc.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;


@Service
public class UserService {
    private static final String ENCRYPTION_KEY = "C01D24F3A9B8E7615FED2D50E96CA7B5";

    @Autowired
    private UserRepository userRepository;

    public User saveUser(User user) {
        user.setPassword(encrypt(user.getPassword()));
        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(decrypt(user.getPassword()));
            return user;
        }
        return null;
    }

    private String encrypt(String password) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String password) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(password)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


