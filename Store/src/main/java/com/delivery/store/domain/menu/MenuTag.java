package com.delivery.store.domain.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_tags")
@IdClass(MenuTagId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuTag {
    @Id
    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Id
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MenuTag(Long menuId, Long tagId) {
        this.menuId = menuId;
        this.tagId = tagId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
