-- Migration for APIs 13-18.
-- Run this once against an existing PostgreSQL database. A fresh database is
-- already covered by sql/init.sql.

CREATE TABLE IF NOT EXISTS public.khoa (
    ma_khoa      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ma_chi_nhanh BIGINT NOT NULL REFERENCES public.chinhanh (ma_chi_nhanh) ON DELETE RESTRICT,
    ten_khoa     VARCHAR(150) NOT NULL CHECK (btrim(ten_khoa) <> ''),
    UNIQUE (ma_chi_nhanh, ten_khoa)
);

ALTER TABLE public.vaitro
    ADD COLUMN IF NOT EXISTS ma_vai_tro_cha BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_vai_tro_cha') THEN
        ALTER TABLE public.vaitro
            ADD CONSTRAINT fk_vai_tro_cha FOREIGN KEY (ma_vai_tro_cha)
            REFERENCES public.vaitro (ma_vai_tro) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE public.nhanvien
    ADD COLUMN IF NOT EXISTS ma_khoa BIGINT,
    ADD COLUMN IF NOT EXISTS ma_phong_kham BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_nhan_vien_khoa') THEN
        ALTER TABLE public.nhanvien
            ADD CONSTRAINT fk_nhan_vien_khoa FOREIGN KEY (ma_khoa)
            REFERENCES public.khoa (ma_khoa) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_nhan_vien_phong') THEN
        ALTER TABLE public.nhanvien
            ADD CONSTRAINT fk_nhan_vien_phong FOREIGN KEY (ma_phong_kham)
            REFERENCES public.phongkham (ma_phong_kham) ON DELETE RESTRICT;
    END IF;
END $$;
