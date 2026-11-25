-- 멤버 역할(리드 · 파트 리드 · 코어)을 없애고, 관리자가 정하는 표시 순서로 바꾼다.
-- 역할 배지는 화면에서 이미 빠졌고, 역할은 코어멤버를 세우는 순서에만 쓰이고 있었다.
--
-- 순서는 구분(코어멤버 · 시니어)마다 1부터 매긴다. 처음 값은 지금 공개 화면에 보이는 순서 그대로다
-- (코어멤버: 리드 → 파트 리드 → 코어, 같은 역할은 최근 등록순 / 시니어: 최근 등록순 · 같으면 이름순).
ALTER TABLE member ADD COLUMN sort_order integer;

UPDATE member AS m
SET sort_order = ranked.position
FROM (
    SELECT id,
           row_number() OVER (
               PARTITION BY status
               ORDER BY CASE role WHEN 'lead' THEN 0 WHEN 'part-lead' THEN 1 WHEN 'core' THEN 2 ELSE 3 END,
                        created_at DESC,
                        name,
                        id
           ) AS position
    FROM member
) AS ranked
WHERE m.id = ranked.id;

-- 순서는 크기 비교에만 쓰므로(빈 번호 · 같은 번호는 id 순) 범위 제약은 두지 않는다
ALTER TABLE member
    ALTER COLUMN sort_order SET NOT NULL,
    DROP CONSTRAINT ck_member_status_role,
    DROP CONSTRAINT ck_member_role;

DROP INDEX idx_member_status_role;
ALTER TABLE member DROP COLUMN role;

CREATE INDEX idx_member_status_sort_order ON member (status, sort_order);
