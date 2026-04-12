package com.delivery.store.domain.option;

import com.delivery.store.domain.option.MenuOptionGroup;
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
@Table(name = "menu_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MenuOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_group_id", nullable = false)
    private MenuOptionGroup optionGroup;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "extra_price", nullable = false)
    private int extraPrice;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private MenuOption(MenuOptionGroup optionGroup, String name, int extraPrice, boolean isAvailable, int displayOrder) {
        this.optionGroup = optionGroup;
        this.name = name;
        this.extraPrice = extraPrice;
        this.isAvailable = isAvailable;
        this.displayOrder = displayOrder;
    }

    public static MenuOption create(
        MenuOptionGroup optionGroup,
        String name,
        int extraPrice,
        boolean isAvailable,
        int displayOrder
    ) {
        return new MenuOption(optionGroup, name, extraPrice, isAvailable, displayOrder);
    }
}
