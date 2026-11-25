-- 홈 히어로 배경 사진을 관리자가 바꿀 수 있게 한다 (사이트 설정).
-- 비어 있으면 프론트가 사이트에 들어 있는 기본 사진을 쓴다. 위치는 CSS object-position "가로% 세로%".
ALTER TABLE site_setting
    ADD COLUMN hero_image_url varchar(500),
    ADD COLUMN hero_image_position varchar(9);
