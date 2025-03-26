package app.notification.client.dto;

import lombok.Data;

@Data
public class NotificationPreference {

    private String notificationType;

    private boolean enabled;

    private String contactInfo;
}
