package io.nuvalence.user.management.api.service.entity;

import static org.mockito.Mockito.*;

import io.nuvalence.user.management.api.service.util.RequestContextTimestamp;
import io.nuvalence.user.management.api.service.util.SpringApplicationContext;
import io.nuvalence.user.management.api.service.util.UserUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.OffsetDateTime;
import java.util.Optional;

class UpdateTrackedEntityEventListenerTest {

    private UpdateTrackedEntityEventListener listener;
    private UpdateTrackedEntity entity;

    private static final String USER_ID = "userId";

    @BeforeEach
    void setUp() {
        listener = new UpdateTrackedEntityEventListener();
        entity = mock(UpdateTrackedEntity.class); // Assume UpdateTrackedEntity is your entity class
    }

    @Test
    void preUpdateTrackedEntityPersist() {
        try (MockedStatic<UserUtility> mockedUserUtility = mockStatic(UserUtility.class);
                MockedStatic<SpringApplicationContext> mockedApplicationContext =
                        mockStatic(SpringApplicationContext.class)) {

            OffsetDateTime timestamp = OffsetDateTime.now();
            RequestContextTimestamp requestContextTimestamp = mock(RequestContextTimestamp.class);
            when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(timestamp);

            mockedUserUtility
                    .when(UserUtility::getCurrentApplicationUserId)
                    .thenReturn(Optional.of(USER_ID));
            mockedApplicationContext
                    .when(
                            () ->
                                    SpringApplicationContext.getBeanByClass(
                                            RequestContextTimestamp.class))
                    .thenReturn(requestContextTimestamp);

            listener.preUpdateTrackedEntityPersist(entity);

            verify(entity).setCreatedBy(USER_ID);
            verify(entity).setLastUpdatedBy(USER_ID);
            verify(entity).setCreatedTimestamp(timestamp);
            verify(entity).setLastUpdatedTimestamp(timestamp);
        }
    }

    @Test
    void preUpdateTrackedEntityUpdate() {
        try (MockedStatic<UserUtility> mockedUserUtility = mockStatic(UserUtility.class);
                MockedStatic<SpringApplicationContext> mockedApplicationContext =
                        mockStatic(SpringApplicationContext.class)) {

            OffsetDateTime timestamp = OffsetDateTime.now();
            RequestContextTimestamp requestContextTimestamp = mock(RequestContextTimestamp.class);
            when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(timestamp);

            mockedUserUtility
                    .when(UserUtility::getCurrentApplicationUserId)
                    .thenReturn(Optional.of(USER_ID));
            mockedApplicationContext
                    .when(
                            () ->
                                    SpringApplicationContext.getBeanByClass(
                                            RequestContextTimestamp.class))
                    .thenReturn(requestContextTimestamp);

            listener.preUpdateTrackedEntityUpdate(entity);

            verify(entity).setLastUpdatedBy(USER_ID);
            verify(entity).setLastUpdatedTimestamp(timestamp);
        }
    }
}
