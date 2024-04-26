package kz.spring.ratelimitspring.controller;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import kz.spring.ratelimitspring.model.User;
import kz.spring.ratelimitspring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private Bucket bucket;

    @GetMapping("generate-token")
    public ResponseEntity<String> generateToken() {
        Refill refill = Refill.of(5, Duration.ofMinutes(1));
        bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(5, refill))
                .build();
        return new ResponseEntity<>("Token generated successfully", HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        if (bucket.tryConsume(1)) {
            List<User> users = userRepository.findAll();
            return new ResponseEntity<>(users, HttpStatus.OK);
        } else {
            List<User> emptyList = new ArrayList<>();
            return new ResponseEntity<>(emptyList, HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        if (bucket.tryConsume(1)) {
            User user = userRepository.findById(id).get();
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            User emptyUser = new User();
            return new ResponseEntity<>(emptyUser, HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userRepository.findById(id).get();
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        return userRepository.save(existingUser);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        try {
            userRepository.findById(id).get();
            userRepository.deleteById(id);
            return "User deleted successfully";
        } catch (Exception e) {
            return "User not found";
        }
    }
}
