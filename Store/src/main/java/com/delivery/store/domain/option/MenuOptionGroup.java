package com.delivery.store.domain.option;

import com.delivery.store.domain.menu.Menu;
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
@Table(name = "menu_option_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MenuOptionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Column(name = "is_multiple", nullable = false)
    private boolean isMultiple;

    @Column(name = "min_select_count", nullable = false)
    private int minSelectCount;

    @Column(name = "max_select_count", nullable = false)
    private int maxSelectCount;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private MenuOptionGroup(
        Menu menu,
        String name,
        boolean isRequired,
        boolean isMultiple,
        int minSelectCount,
        int maxSelectCount,
        int displayOrder
    ) {
        this.menu = menu;
        this.name = name;
        this.isRequired = isRequired;
        this.isMultiple = isMultiple;
        this.minSelectCount = minSelectCount;
        this.maxSelectCount = maxSelectCount;
        this.displayOrder = displayOrder;
    }

    public static MenuOptionGroup create(
        Menu menu,
        String name,
        boolean isRequired,
        boolean isMultiple,
        int minSelectCount,
        int maxSelectCount,
        int displayOrder
    ) {
        return new MenuOptionGroup(menu, name, isRequired, isMultiple, minSelectCount, maxSelectCount, displayOrder);
    }
}
