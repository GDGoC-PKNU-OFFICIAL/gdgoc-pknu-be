package com.gdgocpknu.gdgoc_pknu_be.event.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /** 해당 날짜의 마지막 순서 일정. 등록 · 날짜 변경 시 "마지막 순서 + 1"을 구하는 데 쓴다. */
    Optional<Event> findFirstByDateOrderBySortOrderDesc(LocalDate date);

    /** 순서 변경(`PUT /order`) 대상. 같은 날짜의 동시 순서 변경이 서로 덮어쓰지 않도록 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Event> findByDate(LocalDate date);
}
