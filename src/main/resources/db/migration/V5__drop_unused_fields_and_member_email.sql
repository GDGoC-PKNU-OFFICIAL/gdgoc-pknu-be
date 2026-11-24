-- 공개 화면에 쓰이지 않는 입력을 정리한다.
--  · 사이트 설정 배너 문구: 홈은 모집 상태 · 기간만 보여 준다
--  · 활동 통계 단위 · 접미사: 운영진이 쓰지 않는다
ALTER TABLE site_setting DROP COLUMN banner_text;

ALTER TABLE site_stat
    DROP COLUMN unit,
    DROP COLUMN suffix;

-- 멤버 링크: 홈페이지 대신 이메일을 받는다 (카드의 GitHub · 메일 아이콘).
-- 홈페이지 주소는 공개 화면에 나간 적이 없어 옮기지 않는다.
ALTER TABLE member
    ADD COLUMN email varchar(254),
    DROP COLUMN homepage_url;
