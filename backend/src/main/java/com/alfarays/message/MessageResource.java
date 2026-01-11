package com.alfarays.message;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/messages")
public class MessageResource {

    @GetMapping
    public ResponseEntity<MessageResponse> message() {
        return ResponseEntity.ok(
                MessageResponse.builder()
                        .content("Introducing the FitVerse debut. Our first line of genuine gear is officially live and ready to power your next move. Welcome to the future of your fitness.")
                        .build()
        );
    }

}
