package com.gdgocpknu.gdgoc_pknu_be.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** createdAt · updatedAt (timestamptz). 응답의 KST 오프셋 표기는 매퍼에서 변환한다. */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 자식 컬렉션(`mappedBy`)만 바뀐 수정은 부모 행이 dirty하지 않아 `@LastModifiedDate`가 동작하지 않는다.
     * 그런 수정에서 부모의 수정 시각을 직접 갱신한다.
     */
    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
