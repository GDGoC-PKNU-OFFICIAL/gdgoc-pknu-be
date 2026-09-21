-- FK 조회 (자식 목록 로딩 · ON DELETE CASCADE)
CREATE INDEX idx_project_team_project_id ON project_team (project_id);
CREATE INDEX idx_study_participant_study_id ON study_participant (study_id);
CREATE INDEX idx_study_output_study_id ON study_output (study_id);
CREATE INDEX idx_site_stat_setting_id ON site_stat (setting_id);

-- 목록 정렬: period_start 최신순 (공개 · 관리자 기본 정렬), 이번 달 기준 status 필터
CREATE INDEX idx_project_period_start ON project (period_start DESC);
CREATE INDEX idx_study_period_start ON study (period_start DESC);

-- 일정: 날짜 → 같은 날 순서 (명세 3-9)
CREATE INDEX idx_event_start_date_sort_order ON event (start_date, sort_order);

-- 멤버: 구분 → 역할 순 정렬 · 필터
CREATE INDEX idx_member_status_role ON member (status, role);
