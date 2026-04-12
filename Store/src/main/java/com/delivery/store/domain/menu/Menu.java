package com.delivery.store.domain.menu;

import com.delivery.store.domain.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_group_id")
    private MenuGroup menuGroup;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "is_sold_out", nullable = false)
    private boolean isSoldOut;

    @Column(name = "menu_image_url", length = 512)
    private String menuImageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Menu(
        Store store,
        MenuGroup menuGroup,
        String name,
        String description,
        int price,
        boolean isAvailable,
        boolean isSoldOut,
        String menuImageUrl,
        int displayOrder
    ) {
        this.store = store;
        this.menuGroup = menuGroup;
        this.name = name;
        this.description = description;
        this.price = price;
        this.isAvailable = isAvailable;
        this.isSoldOut = isSoldOut;
        this.menuImageUrl = menuImageUrl;
        this.displayOrder = displayOrder;
    }

    public static Menu create(
        Store store,
        MenuGroup menuGroup,
        String name,
        String description,
        int price,
        boolean isAvailable,
        boolean isSoldOut,
        String menuImageUrl,
        int displayOrder
    ) {
        return new Menu(store, menuGroup, name, description, price, isAvailable, isSoldOut, menuImageUrl, displayOrder);
    }

    public void update(
        MenuGroup menuGroup,
        String name,
        String description,
        int price,
        boolean isAvailable,
        boolean isSoldOut,
        String menuImageUrl
    ) {
        this.menuGroup = menuGroup;
        this.name = name;
        this.description = description;
        this.price = price;
        this.isAvailable = isAvailable;
        this.isSoldOut = isSoldOut;
        this.menuImageUrl = menuImageUrl;
    }
}
