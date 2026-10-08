package com.eone;
import com.eone.domain.*;
import com.eone.repository.*;
import com.eone.service.PushDelivery;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PushDeliveryTest {
    @Test void failureSchedulesRetryWithoutLosingInboxEvent() throws Exception {
        var repository=mock(NotificationRepository.class);var devices=mock(DeviceTokenRepository.class);var firebase=mock(FirebaseMessaging.class);
        var n=new com.eone.domain.Notification();n.setId(UUID.randomUUID());var u=new User();u.setId(UUID.randomUUID());n.setUser(u);
        var device=new DeviceToken();device.setToken("device-token");device.setUpdatedAt(Instant.now());
        when(repository.pendingPush(any(),any())).thenReturn(List.of(n));when(devices.findByUserId(u.getId())).thenReturn(List.of(device));
        var failure=mock(FirebaseMessagingException.class);when(failure.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(firebase.send(any(Message.class))).thenThrow(failure);
        new PushDelivery(repository,devices,firebase).deliver();
        assertThat(n.getPushAttempts()).isEqualTo(1);assertThat(n.getPushDueAt()).isAfter(Instant.now());assertThat(n.getPushDeliveredAt()).isNull();
        verify(repository,never()).delete(any());verify(devices,never()).delete(any());
    }
    @Test void unregisteredTokenIsRemovedAndEventStopsRetrying() throws Exception {
        var repository=mock(NotificationRepository.class);var devices=mock(DeviceTokenRepository.class);var firebase=mock(FirebaseMessaging.class);
        var n=new com.eone.domain.Notification();n.setId(UUID.randomUUID());var u=new User();u.setId(UUID.randomUUID());n.setUser(u);
        var device=new DeviceToken();device.setToken("dead-token");device.setUpdatedAt(Instant.now());
        when(repository.pendingPush(any(),any())).thenReturn(List.of(n));when(devices.findByUserId(u.getId())).thenReturn(List.of(device));
        var failure=mock(FirebaseMessagingException.class);when(failure.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebase.send(any(Message.class))).thenThrow(failure);
        new PushDelivery(repository,devices,firebase).deliver();
        verify(devices).delete(device);assertThat(n.getPushDueAt()).isNull();assertThat(n.getPushDeliveredAt()).isNotNull();
    }
}
