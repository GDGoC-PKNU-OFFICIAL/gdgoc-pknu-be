package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    long countByStatus(MemberStatus status);

    /** 같은 구분의 멤버를 표시 순서대로 (순서 바꾸기 · 맨 뒤 번호 계산) */
    List<Member> findAllByStatusOrderBySortOrderAscIdAsc(MemberStatus status);

    /** 같은 구분의 마지막 표시 순서. 아무도 없으면 0 → 새 멤버는 이 값 + 1 */
    @Query("select coalesce(max(m.sortOrder), 0) from Member m where m.status = :status")
    int findMaxSortOrder(@Param("status") MemberStatus status);

    /** 대시보드 최근 수정. 수정 시각이 같으면 나중에 등록한 것 먼저. */
    List<Member> findTop5ByOrderByUpdatedAtDescIdDesc();
}
