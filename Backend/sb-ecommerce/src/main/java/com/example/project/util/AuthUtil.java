package com.example.project.util;

import com.example.project.model.User;
import com.example.project.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    @Autowired
    UserRepository userRepository;

    public String loggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username " + authentication.getName()));
        return user.getEmail();
    }

    public Long loggedInUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User user = userRepository.findByUserName(authentication.getName())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username " + authentication.getName()));
        return user.getUserId();
}

   public User loggedInUser(){
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       User user = userRepository.findByUserName(authentication.getName())
               .orElseThrow(() -> new UsernameNotFoundException("User not found with username " + authentication.getName()));
       return user;
   }
}
