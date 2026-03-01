package uy.christian.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AbstractAuditEntity implements Serializable {

    @CreatedDate
    @Column(updatable = false)
    private @Nullable LocalDateTime createdAt;

    @LastModifiedDate
    private @Nullable LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private @Nullable String createdBy;

    @LastModifiedBy
    private @Nullable String updatedBy;
}
