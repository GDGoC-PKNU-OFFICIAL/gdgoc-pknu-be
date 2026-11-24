-- 스터디 소개 (상세 카드의 "스터디 소개" 문단들). 프로젝트 description 과 같은 모양 — 문단 단위 문자열 배열.
-- 기존 스터디는 빈 배열로 시작하고, 화면은 소개가 비어 있으면 그 칸을 그리지 않는다.
ALTER TABLE study
    ADD COLUMN description jsonb NOT NULL DEFAULT '[]';
