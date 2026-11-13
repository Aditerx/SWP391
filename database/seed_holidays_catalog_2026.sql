-- SCMS demo: add a 2026 holiday catalog only. Run this WHOLE file.
-- Missing dates are inserted INACTIVE; existing dates/flags/history stay unchanged.
-- These catalog rows are not evidence that the center actually closed in the past.
-- No sessions, subscriptions, invoices, audit rows or other application tables are written.
-- References for lunar dates and the sample Sep 1-2 pair (not a center closure policy):
-- https://xaydungchinhsach.chinhphu.vn/lich-nghi-le-gio-to-hung-vuong-30-4-1-5-quoc-khanh-2-9-nam-2026-119260222115822151.htm
-- https://vpubnd.daklak.gov.vn/CMS/Content/VanBan/2025/THANG%2010/1_Thong%20bao%20nghi%20le%20tet%202026.pdf

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

-- Serialize holiday catalog inserts without locking unrelated application tables.
LOCK TABLE public.holidays IN SHARE ROW EXCLUSIVE MODE;

-- Refuse custom triggers/rules that could write outside this script's scope.
DO $scope$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgrelid = 'public.holidays'::regclass
          AND NOT tgisinternal AND tgenabled <> 'D'
    ) OR EXISTS (
        SELECT 1 FROM pg_rewrite
        WHERE ev_class = 'public.holidays'::regclass AND rulename <> '_RETURN'
    ) THEN
        RAISE EXCEPTION 'HOLIDAY_CATALOG_SCOPE: custom holiday triggers/rules require review; no rows inserted';
    END IF;
END $scope$;

WITH catalog(holiday_date, holiday_name, tag) AS (
    VALUES
        (DATE '2026-01-01', 'Tết Dương lịch',                 'NATIONAL_FIXED'),
        (DATE '2026-02-16', 'Tết Nguyên đán — 29 tháng Chạp', 'LUNAR_FESTIVAL'),
        (DATE '2026-02-17', 'Tết Nguyên đán — Mùng 1',        'LUNAR_FESTIVAL'),
        (DATE '2026-02-18', 'Tết Nguyên đán — Mùng 2',        'LUNAR_FESTIVAL'),
        (DATE '2026-02-19', 'Tết Nguyên đán — Mùng 3',        'LUNAR_FESTIVAL'),
        (DATE '2026-02-20', 'Tết Nguyên đán — Mùng 4',        'LUNAR_FESTIVAL'),
        (DATE '2026-04-26', 'Giỗ Tổ Hùng Vương',              'LUNAR_FESTIVAL'),
        (DATE '2026-04-30', 'Ngày Giải phóng miền Nam',       'NATIONAL_FIXED'),
        (DATE '2026-05-01', 'Ngày Quốc tế Lao động',          'NATIONAL_FIXED'),
        (DATE '2026-09-01', 'Quốc khánh — Ngày liền kề mẫu',  'NATIONAL_FIXED'),
        (DATE '2026-09-02', 'Quốc khánh',                     'NATIONAL_FIXED')
), inserted AS (
    INSERT INTO public.holidays (
        holiday_date, holiday_name, tag, is_active, description, actual_closed_until
    )
    SELECT c.holiday_date, c.holiday_name, c.tag, FALSE,
           'DEMO: danh mục lễ 2026, chưa áp dụng nghỉ; không xác nhận trung tâm đã đóng cửa.',
           NULL
    FROM catalog c
    WHERE NOT EXISTS (
        SELECT 1 FROM public.holidays h WHERE h.holiday_date = c.holiday_date
    )
    ON CONFLICT (holiday_date) DO NOTHING
    RETURNING holiday_date
)
SELECT 'PASS' AS result,
       (SELECT count(*) FROM inserted) AS inserted_count,
       (SELECT count(*) FROM catalog) - (SELECT count(*) FROM inserted) AS existing_dates_unchanged;

COMMIT;

SELECT holiday_id, holiday_date, holiday_name, tag, is_active
FROM public.holidays
WHERE holiday_date >= DATE '2026-01-01' AND holiday_date < DATE '2027-01-01'
ORDER BY holiday_date;
