package com.eone.service;

import com.eone.repository.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.io.IOException;
import java.time.*;

@Service @RequiredArgsConstructor @ConditionalOnProperty(name="eone.push.enabled",havingValue="true")
public class PushDelivery {
    private final NotificationRepository notifications;
    private final DeviceTokenRepository devices;
    private final FirebaseMessaging firebase;

    // A separate scheduled transaction processes only committed inbox events.
    @Scheduled(fixedDelayString="${eone.push.poll-ms:30000}") @Transactional
    public void deliver() {
        for (var notification : notifications.pendingPush(Instant.now(), PageRequest.of(0,10))) {
            boolean failed=false;
            for (var device : devices.findByUserId(notification.getUser().getId())) {
                if (device.getUpdatedAt().isBefore(Instant.now().minus(Duration.ofDays(30)))) { devices.delete(device); continue; }
                try {
                    firebase.send(Message.builder().setToken(device.getToken())
                        // Lock-screen payload deliberately excludes academic/personal information.
                        .setNotification(com.google.firebase.messaging.Notification.builder().setTitle("EONE").setBody("You have a new update. Open EONE to view it.").build())
                        .putData("notificationId",notification.getId().toString()).build());
                } catch (FirebaseMessagingException e) {
                    if (e.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED) devices.delete(device);
                    else failed=true;
                }
            }
            notification.setPushAttempts(notification.getPushAttempts()+1);
            notification.setPushDueAt(failed && notification.getPushAttempts()<5
                ? Instant.now().plusSeconds(60L << notification.getPushAttempts()) : null);
            if (!failed) notification.setPushDeliveredAt(Instant.now());
        }
    }

    @Configuration @EnableScheduling @ConditionalOnProperty(name="eone.push.enabled",havingValue="true")
    static class FirebaseConfiguration {
        @Bean(destroyMethod="delete") FirebaseApp eoneFirebase() throws IOException {
            return FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
                .setConnectTimeout(10000).setReadTimeout(10000).build(),"eone");
        }
        @Bean FirebaseMessaging eoneMessaging(FirebaseApp eoneFirebase) { return FirebaseMessaging.getInstance(eoneFirebase); }
    }
}
