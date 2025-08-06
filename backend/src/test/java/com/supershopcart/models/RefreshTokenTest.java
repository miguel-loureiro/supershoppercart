package com.supershopcart.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void testConstructorAndGetters() {
        String token = "abc123";
        String shopperId = "shopper1";
        String deviceId = "device42";
        long expiry = 1234567890L;

        RefreshToken refreshToken = new RefreshToken(token, shopperId, deviceId, expiry);

        assertEquals(token, refreshToken.getToken());
        assertEquals(shopperId, refreshToken.getShopperId());
        assertEquals(deviceId, refreshToken.getDeviceId());
        assertEquals(expiry, refreshToken.getExpiry());
    }

    @Test
    void testSetters() {
        RefreshToken refreshToken = new RefreshToken("initToken", "initShopper", "initDevice", 10L);

        refreshToken.setToken("newToken");
        refreshToken.setShopperId("newShopperId");
        refreshToken.setDeviceId("newDeviceId");
        refreshToken.setExpiry(9999L);

        assertEquals("newToken", refreshToken.getToken());
        assertEquals("newShopperId", refreshToken.getShopperId());
        assertEquals("newDeviceId", refreshToken.getDeviceId());
        assertEquals(9999L, refreshToken.getExpiry());
    }

    @Test
    void testEqualityBasic() {
        String token = "equalToken";
        RefreshToken t1 = new RefreshToken(token, "shopperA", "deviceB", 123L);
        RefreshToken t2 = new RefreshToken(token, "shopperA", "deviceB", 123L);

        // They are not the same object, but fields are equal
        assertNotSame(t1, t2);
        assertEquals(t1.getToken(), t2.getToken());
        assertEquals(t1.getShopperId(), t2.getShopperId());
        assertEquals(t1.getDeviceId(), t2.getDeviceId());
        assertEquals(t1.getExpiry(), t2.getExpiry());
    }

    @Test
    void testTokenCanBeNull() {
        RefreshToken refreshToken = new RefreshToken(null, "shopper", "device", 1L);

        assertNull(refreshToken.getToken());
        refreshToken.setToken("notNull");
        assertEquals("notNull", refreshToken.getToken());
    }

    @Test
    void testExpiryRange() {
        RefreshToken refreshToken = new RefreshToken("tok", "shopper", "dev", 0L);
        assertEquals(0L, refreshToken.getExpiry());
        refreshToken.setExpiry(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, refreshToken.getExpiry());
    }
}
