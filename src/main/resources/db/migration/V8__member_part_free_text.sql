-- 멤버 분야를 정해진 5가지 코드값에서 관리자가 직접 적는 글자로 바꾼다 (예: "백엔드", "DevOps", "PM").
-- 저장돼 있던 코드값은 공개 화면에 보이던 이름 그대로 옮겨, 카드에 보이는 글자가 바뀌지 않게 한다.
ALTER TABLE member DROP CONSTRAINT ck_member_part;

UPDATE member
SET part = CASE part
    WHEN 'frontend' THEN '프론트엔드'
    WHEN 'backend' THEN '백엔드'
    WHEN 'mobile' THEN '모바일'
    WHEN 'ai-ml' THEN 'AI·ML'
    WHEN 'design' THEN '디자인'
    ELSE part
END;
