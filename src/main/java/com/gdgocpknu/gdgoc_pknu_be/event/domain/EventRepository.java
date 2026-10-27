package com.gdgocpknu.gdgoc_pknu_be.event.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /** 해당 날짜의 마지막 순서 일정. 등록 · 날짜 변경 시 "마지막 순서 + 1"을 구하는 데 쓴다. */
    Optional<Event> findFirstByDateOrderBySortOrderDesc(LocalDate date);

    /**
     * 순서 변경(`PUT /order`) 대상. 그 날짜의 행에 비관적 잠금(`SELECT ... FOR UPDATE`)을 걸어, 같은 날짜의 순서 변경을
     * 한 번에 하나씩 처리한다. 잠금이 없으면 두 요청이 서로 다른 순서로 행을 UPDATE하다 교착 상태에 빠질 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Event> findByDate(LocalDate date);

    /** 순서만 바꾼다. 벌크 UPDATE는 auditing을 거치지 않아 `updatedAt`이 바뀌지 않는다(순서 변경은 내용 수정이 아니다). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Event e SET e.sortOrder = :sortOrder WHERE e.id = :id")
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);
}
