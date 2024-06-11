package it.pagopa.selfcare.onboarding.dto;

import java.util.List;

public class FindNotificationToSendResponse {
    private List<NotificationToSend> notifications;
    private Long count;

    public FindNotificationToSendResponse() {
    }

    public FindNotificationToSendResponse(List<NotificationToSend> notifications, Long count) {
        this.notifications = notifications;
        this.count = count;
    }

    public List<NotificationToSend> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationToSend> notifications) {
        this.notifications = notifications;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
