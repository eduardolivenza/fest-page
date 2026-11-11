package com.festdescans.api.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Read-only for the api: admin users are created with SQL (see apps/api/dev/seed.sql). */
@Entity
@Table(name = "AdminUser")
public class AdminUserEntity {

    @Id
    private String id;
    private String email;
    private String passwordHash;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AdminUserEntity() {
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
