package com.supershopcart.models;

import com.supershopcart.enums.SharePermission;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class SharePermissionEntry {
    private String shopperId;
    private SharePermission permission;

    public SharePermissionEntry() {} // Required for Firestore

    public SharePermissionEntry(String shopperId, SharePermission permission) {
        this.shopperId = shopperId;
        this.permission = permission;
    }

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
