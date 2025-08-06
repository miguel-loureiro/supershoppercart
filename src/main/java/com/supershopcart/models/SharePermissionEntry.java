package com.supershopcart.models;

import com.supershopcart.enums.SharePermission;
import java.util.Objects;

public class SharePermissionEntry {
    private String shopperId;
    private SharePermission permission;

    public SharePermissionEntry() {} // Required for Firestore

    public SharePermissionEntry(String shopperId, SharePermission permission) {
        this.shopperId = shopperId;
        this.permission = permission;
    }

    public String getShopperId() { return shopperId; }
    public void setShopperId(String shopperId) { this.shopperId = shopperId; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharePermissionEntry that = (SharePermissionEntry) o;
        return Objects.equals(shopperId, that.shopperId) && permission == that.permission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shopperId, permission);
    }
}
