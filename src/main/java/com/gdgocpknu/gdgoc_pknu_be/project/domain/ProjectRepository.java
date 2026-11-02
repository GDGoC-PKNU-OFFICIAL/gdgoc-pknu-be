package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** 대시보드 최근 수정. 수정 시각이 같으면 나중에 등록한 것 먼저. */
    List<Project> findTop5ByOrderByUpdatedAtDescIdDesc();
}
