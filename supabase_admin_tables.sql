-- ============================================
-- DiaCalendar 관리자 기능 추가 SQL
-- 기존 게시판 테이블 생성 후 실행
-- Supabase Dashboard > SQL Editor 에서 실행
-- ============================================

-- 1. admins 테이블 (관리자 목록)
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE admins ENABLE ROW LEVEL SECURITY;

-- 관리자만 admins 테이블 조회 가능
CREATE POLICY "admins_select" ON admins
    FOR SELECT TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

-- 2. announcements 테이블 (공지사항)
CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    is_pinned BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE announcements ENABLE ROW LEVEL SECURITY;

-- 모든 로그인 사용자가 공지 조회 가능
CREATE POLICY "announcements_select" ON announcements
    FOR SELECT TO authenticated
    USING (true);

-- 관리자만 공지 생성/수정/삭제
CREATE POLICY "announcements_insert" ON announcements
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() IN (SELECT id FROM admins));

CREATE POLICY "announcements_update" ON announcements
    FOR UPDATE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

CREATE POLICY "announcements_delete" ON announcements
    FOR DELETE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

-- 3. banned_users 테이블 (정지된 사용자)
CREATE TABLE IF NOT EXISTS banned_users (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    reason TEXT NOT NULL DEFAULT '',
    banned_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id)
);

ALTER TABLE banned_users ENABLE ROW LEVEL SECURITY;

-- 본인의 정지 상태 확인 가능
CREATE POLICY "banned_users_select_self" ON banned_users
    FOR SELECT TO authenticated
    USING (auth.uid() = user_id);

-- 관리자는 전체 조회/관리 가능
CREATE POLICY "banned_users_select_admin" ON banned_users
    FOR SELECT TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

CREATE POLICY "banned_users_insert" ON banned_users
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() IN (SELECT id FROM admins));

CREATE POLICY "banned_users_update" ON banned_users
    FOR UPDATE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

CREATE POLICY "banned_users_delete" ON banned_users
    FOR DELETE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

-- 4. reports 테이블에 관리자 조회/삭제 RLS 추가
CREATE POLICY "reports_select_admin" ON reports
    FOR SELECT TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

CREATE POLICY "reports_delete_admin" ON reports
    FOR DELETE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

-- 5. posts 테이블에 관리자 삭제 권한 추가
CREATE POLICY "posts_delete_admin" ON posts
    FOR DELETE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

-- 6. comments 테이블에 관리자 수정 권한 추가 (soft delete)
CREATE POLICY "comments_update_admin" ON comments
    FOR UPDATE TO authenticated
    USING (auth.uid() IN (SELECT id FROM admins));

-- ============================================
-- 관리자용 헬퍼 함수
-- ============================================

-- is_admin 함수 (관리자 여부 확인)
CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (SELECT 1 FROM admins WHERE id = auth.uid());
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 통계 함수 (대시보드용)
CREATE OR REPLACE FUNCTION get_admin_stats()
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    IF NOT is_admin() THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;

    SELECT json_build_object(
        'total_users', (SELECT COUNT(*) FROM profiles),
        'total_posts', (SELECT COUNT(*) FROM posts),
        'total_comments', (SELECT COUNT(*) FROM comments WHERE is_deleted = false),
        'total_reports', (SELECT COUNT(*) FROM reports),
        'pending_reports', (SELECT COUNT(*) FROM reports),
        'banned_users', (SELECT COUNT(*) FROM banned_users WHERE banned_until IS NULL OR banned_until > now()),
        'today_posts', (SELECT COUNT(*) FROM posts WHERE created_at >= CURRENT_DATE),
        'today_comments', (SELECT COUNT(*) FROM comments WHERE created_at >= CURRENT_DATE AND is_deleted = false)
    ) INTO result;

    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 인덱스
-- ============================================
CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON announcements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_announcements_is_pinned ON announcements(is_pinned);
CREATE INDEX IF NOT EXISTS idx_banned_users_user_id ON banned_users(user_id);
CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(created_at DESC);

-- ============================================
-- 본인 계정을 관리자로 등록 (최초 1회)
-- auth.users 테이블에서 본인 이메일로 UUID를 찾아 등록
-- ============================================
-- INSERT INTO admins (id) VALUES ('여기에-본인-UUID-입력');
--
-- UUID 확인 방법:
-- SELECT id, email FROM auth.users WHERE email = '본인이메일@gmail.com';
