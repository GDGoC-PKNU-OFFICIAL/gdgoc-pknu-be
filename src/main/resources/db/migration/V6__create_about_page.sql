-- About 페이지 내용 (관리자가 수정). 사이트 설정처럼 단일 행이고, 화면 구역마다 JSON 배열 한 칸씩 둔다.
-- 행이 없으면 공개 API 는 404 이고, 프론트는 코드에 들어 있는 기본 내용을 보여 준다 (처음 저장할 때 행이 생긴다).
CREATE TABLE about_page (
    id         smallint    PRIMARY KEY,
    identity   jsonb       NOT NULL DEFAULT '[]',  -- Who We Are 문단
    activities jsonb       NOT NULL DEFAULT '[]',  -- What We Do 카드
    rules      jsonb       NOT NULL DEFAULT '[]',  -- 활동 규칙 문서 (본문은 마크다운)
    faq        jsonb       NOT NULL DEFAULT '[]',  -- 자주 묻는 질문
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_about_page_single_row CHECK (id = 1)
);
