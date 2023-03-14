package com.mailservivepoc.demo.service;

import com.mailservivepoc.demo.model.*;
import com.sun.mail.imap.IMAPFolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.SearchTerm;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {


    private static final String IMAP_HOST = "imap.gmail.com";
    private static final String IMAP_PORT = "993";
    private static final String USERNAME = "ENTER_YOUR_EMAIL";
    private static final String PASSWORD = "ENTER_YOUR PASSWORD";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public List<EmailResponse> getLatestMessage() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");

        Session session = Session.getInstance(properties);
        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);

        // to do - find method to fetch unread messages efficiently

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.getMessages();

        List<EmailResponse> emailResponseList = new ArrayList<>();


        for(int i= messages.length-1; i >messages.length-2;i--){
            EmailResponse emailResponse = new EmailResponse();

            Message mostRecent = messages[i];
            processEmail(mostRecent);


        /*
        Flags seen = new Flags(Flags.Flag.SEEN);
        FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
        Message[] messages = inbox.search(unseenFlagTerm);
        Message mostRecent = messages[messages.length-1];


        */

            // now let's check that this email is new email or it is a reply
            //processEmail(mostRecent);

            emailResponse.setSubject(mostRecent.getSubject());
            emailResponse.setFrom(Arrays.toString(mostRecent.getFrom()));
            emailResponse.setTo(Arrays.toString(mostRecent.getRecipients(Message.RecipientType.TO)));
            emailResponse.setSentDate(mostRecent.getSentDate());
            //logic to get the content of main body
            emailResponse.setBody(getTextFromMessage(mostRecent));
            emailResponse.setAttachmentList(getAttachments(mostRecent));


            emailResponseList.add(emailResponse);

        /*

        //logic 1
         String body = "";
        if (mostRecent.isMimeType("text/plain")) {
            body += mostRecent.getContent().toString();
        } else if (mostRecent.isMimeType("multipart/*")) {
            Multipart multiPart = (Multipart) mostRecent.getContent();
            for (int i = 0; i < multiPart.getCount(); i++) {
                BodyPart bodyPart = multiPart.getBodyPart(i);
                System.out.println(bodyPart.getContent().toString());
                if (bodyPart.isMimeType("text/plain")) {
                    body += bodyPart.getContent().toString();
                }
            }
        }

       // logic 2
        Object content = mostRecent.getContent();
        if (content instanceof String) {
            // handle text/plain content
            body += content.toString();
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    // handle text/plain body part
                    body+= bodyPart.getContent().toString();
                    break;
                } else if (bodyPart.isMimeType("text/html")) {
                    // handle text/html body part
                    body+= bodyPart.getContent().toString();
                    break;
                }
            }
        }
        */

        }



        return emailResponseList;
    }

    public EmailReplyResponse replyToLastEmail(@RequestBody EmailReplyRequest emailReply) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(properties);


        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        Message[] messages = inbox.getMessages();
        System.out.println(messages.length);
        if (messages.length == 0) {
            throw new Exception("Inbox is empty");
        }
        Message message = messages[messages.length - 1];

        EmailReplyResponse response = new EmailReplyResponse();


        Date date = message.getSentDate();
        response.setSentDate(date);
        // Get all the information from the message
        String from = InternetAddress.toString(message.getFrom());
        response.setReplyingTo(from);

        //String replyTo = InternetAddress.toString(message.getReplyTo());


        //String to = InternetAddress.toString(message.getRecipients(Message.RecipientType.TO));


        String subject = message.getSubject();
        response.setSubject(subject);

        response.setBody(emailReply.getBody());

        Message replyMessage = new MimeMessage(session);
        replyMessage = (MimeMessage) message.reply(false);
        //replyMessage.setFrom(new InternetAddress(to));
        replyMessage.setText(emailReply.getBody());
        //replyMessage.setReplyTo(message.getReplyTo());

        Transport t = session.getTransport("smtp");
        t.connect(USERNAME, PASSWORD);
        t.sendMessage(replyMessage,replyMessage.getAllRecipients());
        t.close();
        System.out.println("message replied successfully ....");

        return response;
    }


    public void sendEmail(@RequestBody EmailRequest emailRequest) throws Exception {
        Properties props = new Properties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.host", SMTP_HOST);
        props.setProperty("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(USERNAME));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(emailRequest.getTo()));
        message.setSubject(emailRequest.getSubject());
        message.setText(emailRequest.getBody());
        Transport.send(message);
    }

    public void processEmail(Message message) throws MessagingException {
        //String messageId = message.getHeader("Message-ID")[0];
        String[] inReplyTo = message.getHeader("In-Reply-To");
        String[] references = message.getHeader("References");

        if (references != null){
            System.out.println("this is the reference" + references[0]);
            System.out.println("   ");

        }

        if (inReplyTo != null){
            System.out.println("this is the reference" + inReplyTo[0]);
            System.out.println("   ");

        }





        // how do you know the ticket id for which this reply is

        if (inReplyTo != null || references != null) {
            // This is a reply to a previous email
            String parentMessageId = inReplyTo != null ? inReplyTo[0] : references[0];
            System.out.println("this is the mail received as a reply");
            // Do something with the parent message ID
        } else {
            // This is a new email
            System.out.println("this is a new email");
            // Do something with the new email
        }
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String content = getTextFromPart(bodyPart);
                result = result + content;
            }
        }
        return result;
    }


    private String getTextFromPart(BodyPart bodyPart) throws MessagingException, IOException {
        String result = "";
        if (bodyPart.isMimeType("text/plain")) {
            try {
                result = (String) bodyPart.getContent();
            } catch (ClassCastException e) {
                Object content = bodyPart.getContent();
                if (content instanceof InputStream) {
                    InputStream is = (InputStream) content;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    result = sb.toString();
                }
            }
        } else if (bodyPart.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) bodyPart.getContent();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart nestedBodyPart = multipart.getBodyPart(i);
                String content = getTextFromPart(nestedBodyPart);
                result = result + content;
            }
        }
        return result;
    }


    public List<Attachment> getAttachments(Message message) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();

        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String disposition = bodyPart.getDisposition();
                if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
                    String fileName = bodyPart.getFileName();
                    String contentType = bodyPart.getContentType();
                    long size = bodyPart.getSize();

                    // create an attachment object and add it to the list of attachments
                    Attachment attachment = new Attachment(fileName, contentType, size);
                    attachments.add(attachment);
                }
            }
        }
        return attachments;
    }

    public void listenForNewMessages() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");

        Session session = Session.getInstance(properties);
        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);

        IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);


        Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        for (Message message : unreadMessages) {
            EmailResponse emailResponse = new EmailResponse();

            emailResponse.setSubject(message.getSubject());
            emailResponse.setFrom(Arrays.toString(message.getFrom()));
            emailResponse.setTo(Arrays.toString(message.getRecipients(Message.RecipientType.TO)));
            emailResponse.setSentDate(message.getSentDate());
            emailResponse.setBody(getTextFromMessage(message));
            emailResponse.setAttachmentList(getAttachments(message));

            // do something with the email response, such as forwarding it to another email address
            System.out.println(emailResponse.toString());

            message.setFlag(Flags.Flag.SEEN, true);
        }


        inbox.addMessageCountListener(new MessageCountAdapter() {
            public void messagesAdded(MessageCountEvent ev) {
                Message[] messages = ev.getMessages();
                for (Message message : messages) {
                    try {
                        EmailResponse emailResponse = new EmailResponse();

                        emailResponse.setSubject(message.getSubject());
                        emailResponse.setFrom(Arrays.toString(message.getFrom()));
                        emailResponse.setTo(Arrays.toString(message.getRecipients(Message.RecipientType.TO)));
                        emailResponse.setSentDate(message.getSentDate());
                        emailResponse.setBody(getTextFromMessage(message));
                        emailResponse.setAttachmentList(getAttachments(message));

                        // do something with the email response, such as forwarding it to another email address
                        System.out.println(emailResponse.toString());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        idle(inbox);
    }


    private void idle(IMAPFolder folder) {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (5 * 60 * 1000); // 5 minutes
            while (System.currentTimeMillis() < endTime) {
                if (!folder.isOpen()) {
                    return;
                }
                folder.idle();
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void replyToEmail(String messageId) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(properties);

        // Connect to the IMAP server and open the inbox folder
        Store store = session.getStore();
        store.connect(IMAP_HOST, USERNAME, PASSWORD);
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        // Create a search term to search for the email with the specified Message-ID
        SearchTerm searchTerm = new MessageIDTerm(messageId);

        // Search for the email with the specified Message-ID
        Message[] messages = inbox.search(searchTerm);

        if (messages.length == 1) {
            // The email with the specified Message-ID was found
            Message originalMessage = messages[0];

            // Create a reply message
            Message replyMessage = new MimeMessage(session);
            replyMessage = (MimeMessage) originalMessage.reply(false); // This creates a reply to the original message without including its content
            replyMessage.setFrom(new InternetAddress(USERNAME));
            replyMessage.setText("This is my reply text.");
            Transport t = session.getTransport("smtp");
            t.connect(USERNAME, PASSWORD);
            t.sendMessage(replyMessage,replyMessage.getAllRecipients());
            t.close();
            System.out.println("message replied successfully ....");

        } else {
            // No email with the specified Message-ID was found
            System.out.println("Email with Message-ID " + messageId + " not found in inbox.");
        }

        // Close the inbox and store
        inbox.close(false);
        store.close();
    }






}





