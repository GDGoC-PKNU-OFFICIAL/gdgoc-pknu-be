-- 프로젝트 배지(카드 · 상세의 작은 라벨)를 프로젝트마다 직접 정한다.
-- 둘 다 비어 있으면 화면은 기존처럼 category 라벨 · 색을 쓴다. 글자와 색은 함께 있거나 함께 없다.
ALTER TABLE project
    ADD COLUMN badge_label varchar(20),
    ADD COLUMN badge_color varchar(10);

ALTER TABLE project
    ADD CONSTRAINT ck_project_badge_color
        CHECK (badge_color IS NULL OR badge_color IN ('blue', 'red', 'yellow', 'green', 'purple', 'gray')),
    ADD CONSTRAINT ck_project_badge_pair
        CHECK ((badge_label IS NULL) = (badge_color IS NULL));
