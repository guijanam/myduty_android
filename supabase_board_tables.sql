-- ============================================
-- DiaCalendar 게시판 Supabase 테이블 생성 SQL
-- Supabase Dashboard > SQL Editor 에서 실행
-- ============================================

-- 1. profiles 테이블
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    nickname TEXT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "profiles_select" ON profiles
    FOR SELECT TO authenticated
    USING (true);

CREATE POLICY "profiles_insert" ON profiles
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = id);

CREATE POLICY "profiles_update" ON profiles
    FOR UPDATE TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- 2. posts 테이블
CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "posts_select" ON posts
    FOR SELECT TO authenticated
    USING (true);

CREATE POLICY "posts_insert" ON posts
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = author_id);

CREATE POLICY "posts_update" ON posts
    FOR UPDATE TO authenticated
    USING (auth.uid() = author_id);

CREATE POLICY "posts_delete" ON posts
    FOR DELETE TO authenticated
    USING (auth.uid() = author_id);

-- 3. comments 테이블
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE comments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "comments_select" ON comments
    FOR SELECT TO authenticated
    USING (true);

CREATE POLICY "comments_insert" ON comments
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = author_id);

CREATE POLICY "comments_update" ON comments
    FOR UPDATE TO authenticated
    USING (auth.uid() = author_id);

-- 4. reports 테이블
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content_type TEXT NOT NULL,
    content_id BIGINT NOT NULL,
    target_author_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (reporter_id, content_type, content_id)
);

ALTER TABLE reports ENABLE ROW LEVEL SECURITY;

CREATE POLICY "reports_insert" ON reports
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = reporter_id);

-- 5. blocks 테이블
CREATE TABLE IF NOT EXISTS blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (blocker_id, blocked_id)
);

ALTER TABLE blocks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "blocks_select" ON blocks
    FOR SELECT TO authenticated
    USING (auth.uid() = blocker_id);

CREATE POLICY "blocks_insert" ON blocks
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = blocker_id);

CREATE POLICY "blocks_delete" ON blocks
    FOR DELETE TO authenticated
    USING (auth.uid() = blocker_id);

-- 6. board_categories 테이블 (동적 게시판 카테고리)
CREATE TABLE IF NOT EXISTS board_categories (
    id BIGSERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE board_categories ENABLE ROW LEVEL SECURITY;

CREATE POLICY "board_categories_select" ON board_categories
    FOR SELECT TO authenticated
    USING (true);

-- 초기 데이터
INSERT INTO board_categories (code, display_name, sort_order) VALUES
    ('FREE', '자유게시판', 1),
    ('QA', 'Q&A', 2)
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 인덱스 (성능 최적화)
-- ============================================
CREATE INDEX IF NOT EXISTS idx_posts_category ON posts(category);
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts(author_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_blocks_blocker_id ON blocks(blocker_id);


-- 1. posts 테이블에 view_count, like_count 컬럼 추가
ALTER TABLE posts ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN like_count INTEGER NOT NULL DEFAULT 0;

-- 2. post_likes 테이블 생성
CREATE TABLE post_likes (
  id BIGSERIAL PRIMARY KEY,
  post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(post_id, user_id)
);

-- 2-1. post_views 테이블 (조회수 중복 방지)
CREATE TABLE post_views (
  id BIGSERIAL PRIMARY KEY,
  post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(post_id, user_id)
);

ALTER TABLE post_views ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can insert own views" ON post_views FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can view all views" ON post_views FOR SELECT USING (true);

CREATE INDEX IF NOT EXISTS idx_post_views_post_id ON post_views(post_id);

-- 3. view_count 증가 RPC 함수 (중복 방지)
CREATE OR REPLACE FUNCTION increment_view_count(p_post_id BIGINT, p_user_id UUID)
RETURNS VOID AS $$
BEGIN
  -- 이미 조회한 사용자는 카운트하지 않음
  INSERT INTO post_views (post_id, user_id) VALUES (p_post_id, p_user_id)
  ON CONFLICT (post_id, user_id) DO NOTHING;

  IF FOUND THEN
    UPDATE posts SET view_count = view_count + 1 WHERE id = p_post_id;
  END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. 좋아요 토글 RPC 함수
CREATE OR REPLACE FUNCTION toggle_post_like(p_post_id BIGINT, p_user_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
  already_liked BOOLEAN;
BEGIN
  SELECT EXISTS(SELECT 1 FROM post_likes WHERE post_id = p_post_id AND user_id = p_user_id) INTO already_liked;
  IF already_liked THEN
    DELETE FROM post_likes WHERE post_id = p_post_id AND user_id = p_user_id;
    UPDATE posts SET like_count = like_count - 1 WHERE id = p_post_id;
    RETURN FALSE;
  ELSE
    INSERT INTO post_likes (post_id, user_id) VALUES (p_post_id, p_user_id);
    UPDATE posts SET like_count = like_count + 1 WHERE id = p_post_id;
    RETURN TRUE;
  END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. RLS 정책
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view all likes" ON post_likes FOR SELECT USING (true);
CREATE POLICY "Users can insert own likes" ON post_likes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own likes" ON post_likes FOR DELETE USING (auth.uid() = user_id);

-- ============================================
-- 닉네임 금칙어 필터링
-- ============================================

-- 1. banned_words 테이블
CREATE TABLE banned_words (
    id SERIAL PRIMARY KEY,
    word TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE banned_words ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read banned_words" ON banned_words
    FOR SELECT TO authenticated
    USING (true);

-- 초기 금칙어 데이터
INSERT INTO banned_words (word) VALUES
    ('시발'), ('씨발'), ('씨팔'), ('시팔'),
    ('병신'), ('좆'), ('지랄'), ('개새'),
    ('닥쳐'), ('꺼져')
ON CONFLICT (word) DO NOTHING;

-- 2. 닉네임 유니크 제약
ALTER TABLE profiles ADD CONSTRAINT profiles_nickname_unique UNIQUE (nickname);

-- 3. 닉네임 검증 RPC 함수 (금칙어 + 중복 체크)
CREATE OR REPLACE FUNCTION check_nickname(p_nickname TEXT, p_user_id UUID)
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_banned TEXT;
    v_existing UUID;
BEGIN
    -- 금칙어 체크
    SELECT word INTO v_banned
    FROM banned_words
    WHERE p_nickname LIKE '%' || word || '%'
    LIMIT 1;

    IF v_banned IS NOT NULL THEN
        RETURN '{"valid":false,"reason":"banned_word"}'::jsonb;
    END IF;

    -- 닉네임 중복 체크 (본인 제외)
    SELECT id INTO v_existing
    FROM profiles
    WHERE nickname = p_nickname AND id != p_user_id
    LIMIT 1;

    IF v_existing IS NOT NULL THEN
        RETURN '{"valid":false,"reason":"duplicate"}'::jsonb;
    END IF;

    RETURN '{"valid":true,"reason":null}'::jsonb;
END;
$$;

-- ============================================
-- 익명게시판 지원
-- ============================================

-- board_categories에 is_anonymous 컬럼 추가
ALTER TABLE board_categories ADD COLUMN is_anonymous BOOLEAN DEFAULT false;
