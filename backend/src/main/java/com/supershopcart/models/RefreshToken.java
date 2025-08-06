package com.supershopcart.models;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshToken {

    @DocumentId
    private String token;
    private String shopperId;
    private String deviceId;
    private long expiry;

    public RefreshToken(String token, String shopperId, String deviceId, long expiry) {
        this.token = token;
        this.shopperId = shopperId;
        this.deviceId = deviceId;
        this.expiry = expiry;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setShopperId(String shopperId) {
        this.shopperId = shopperId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
