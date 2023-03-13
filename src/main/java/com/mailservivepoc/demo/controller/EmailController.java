package com.mailservivepoc.demo.controller;

import com.mailservivepoc.demo.model.*;
import com.mailservivepoc.demo.service.EmailService;
import com.mailservivepoc.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class EmailController {
    private final EmailService emailService;


    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/latest")
    public List<EmailResponse> getUnreadMessages() throws Exception {
        return emailService.getLatestMessage();
    }

    @GetMapping("/latestidle")
    public void getUnreadMessageswithIdle() throws Exception {
         emailService.listenForNewMessages();
    }

    @PostMapping("/reply")
    public EmailReplyResponse replyToLastEmail(@RequestBody EmailReplyRequest emailReply) throws Exception {
        return emailService.replyToLastEmail(emailReply);
    }




    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest emailRequest) throws Exception {
        try{
            emailService.sendEmail(emailRequest);
            return ResponseEntity.ok().body("sent successfully");
        }
        catch (Exception e){
            return ResponseEntity.internalServerError().body("Failed");
        }

    }






}



