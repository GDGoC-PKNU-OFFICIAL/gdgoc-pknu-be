package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    long countByStatus(MemberStatus status);

    /** 대시보드 최근 수정. 수정 시각이 같으면 나중에 등록한 것 먼저. */
    List<Member> findTop5ByOrderByUpdatedAtDescIdDesc();
}
