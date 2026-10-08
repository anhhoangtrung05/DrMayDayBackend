-- =====================================================================
-- DrMayDay Backend - Database Init Script (MySQL 8+)
-- He thong quan ly kham benh & theo doi benh me day / da lieu
-- Sinh tu tai lieu: docs/Mo ta db drmayday.docx (ban cap nhat moi nhat)
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- NHOM A — NGUOI DUNG, PHAN QUYEN & CO SO VAT CHAT
-- =====================================================================

-- roles: co phan cap cha-con (parent_role_id tu tham chieu).
-- Cay phan cap tu cao xuong thap: director -> chief_of_branch -> chief_of_department
-- -> doctor / chief_of_nurse -> nurse / technician. "patient" va "admin" dung ngoai cay nay.
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    role_id         INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    description     VARCHAR(255),
    parent_role_id  INT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_roles_parent FOREIGN KEY (parent_role_id) REFERENCES roles (role_id) ON DELETE SET NULL,
    KEY idx_roles_parent (parent_role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS permissions;
CREATE TABLE permissions (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    description   VARCHAR(255),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS api_endpoints;
CREATE TABLE api_endpoints (
    api_endpoints_id INT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    method           VARCHAR(10) NOT NULL,
    path             VARCHAR(255) NOT NULL,
    description      VARCHAR(255),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_api_endpoints_method_path (method, path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2.2 permissions_endpoints: bang trung gian API <-> permission
DROP TABLE IF EXISTS permissions_endpoints;
CREATE TABLE permissions_endpoints (
    permissions_endpoints_id INT AUTO_INCREMENT PRIMARY KEY,
    permission_id INT NOT NULL,
    endpoint_id    INT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_permission_endpoint (permission_id, endpoint_id),
    CONSTRAINT fk_permendpt_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id) ON DELETE CASCADE,
    CONSTRAINT fk_permendpt_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints (api_endpoints_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. role_permissions: tai lieu moi bo sung surrogate key "id", giu them
-- UNIQUE(role_id, permission_id) de tranh gan trung quyen.
DROP TABLE IF EXISTS role_permissions;
CREATE TABLE role_permissions (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_role_permission (role_id, permission_id),
    CONSTRAINT fk_roleperm_role FOREIGN KEY (role_id) REFERENCES roles (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_roleperm_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS branches;
CREATE TABLE branches (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(255),
    phone      VARCHAR(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS departments;
CREATE TABLE departments (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    branch_id   INT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_departments_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS rooms;
CREATE TABLE rooms (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    department_id  INT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rooms_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. users: bang tai khoan goc (chi chua thong tin dinh danh/dang nhap chung).
-- Tai lieu moi yeu cau tach rieng staffs / patients (subtype 1-1), nen role_id,
-- branch_id, department_id, room_id KHONG con nam o bang nay nua.
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    user_id               INT AUTO_INCREMENT PRIMARY KEY,
    password_hash         VARCHAR(255) NOT NULL,
    ho_ten                VARCHAR(255) NOT NULL,
    so_dien_thoai         VARCHAR(20) NOT NULL UNIQUE,
    gioi_tinh             ENUM('Nam', 'Nu') NULL,
    reset_token           VARCHAR(255),
    reset_token_expires   DATETIME,
    is_active             TINYINT(1) NOT NULL DEFAULT 1,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7.a staffs: subtype cua users danh cho nhan vien (bac si/dieu duong/ky thuat vien/quan ly...).
DROP TABLE IF EXISTS staffs;
CREATE TABLE staffs (
    user_id       INT PRIMARY KEY,
    role_id       INT NOT NULL,
    department_id INT,
    branch_id     INT,
    room_id       INT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staffs_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_staffs_role FOREIGN KEY (role_id) REFERENCES roles (role_id) ON DELETE RESTRICT,
    CONSTRAINT fk_staffs_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_staffs_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_staffs_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7.b patients: subtype cua users danh cho benh nhan (ho so hanh chinh + tien su benh).
DROP TABLE IF EXISTS patients;
CREATE TABLE patients (
    user_id                           INT PRIMARY KEY,
    role_id                           INT NOT NULL,
    ngay_sinh                         DATE,
    nghe_nghiep                       ENUM('Cong nhan', 'Nong dan', 'Hoc sinh - Sinh vien', 'Can bo cong chuc', 'Khac'),
    dan_toc                           VARCHAR(100),
    dia_chi_so_nha_thon_pho           VARCHAR(100),
    dia_chi_xa_phuong                 VARCHAR(100),
    dia_chi_tinh_tp                   VARCHAR(100),
    noi_lam_viec                      VARCHAR(100),
    bhyt_so_the                       VARCHAR(100),
    bhyt_gia_tri_den                  DATE,
    nguoi_nha_ho_ten_dia_chi          VARCHAR(100),
    nguoi_nha_so_dien_thoai           VARCHAR(100),
    thoi_diem_kham_lan_dau            DATETIME,
    ngay_bat_dau_dieu_tri_ngoai_tru   DATE,
    ngay_ket_thuc_dieu_tri_ngoai_tru  DATE,
    tien_su_co_dia                    ENUM('Co', 'Khong'),
    tien_su_co_dia_ghi_ro             ENUM('Viem da co dia', 'Hen', 'Viem mui di ung'),
    tien_su_tuyen_giap                ENUM('Co', 'Khong'),
    tien_su_tuyen_giap_ghi_ro         VARCHAR(255),
    tien_su_tu_mien                   ENUM('Co', 'Khong'),
    tien_su_tu_mien_ghi_ro            ENUM('Lupus', 'Viem khop tu mien'),
    tien_su_di_ung                    ENUM('Co', 'Khong'),
    tien_su_di_ung_ghi_ro             ENUM('Tach thuoc', 'Thuc an', 'Di nguyen khac'),
    tien_su_phan_ve                   ENUM('Co', 'Khong'),
    tien_su_phan_ve_ghi_ro            VARCHAR(255),
    tien_su_viem_nhiem_man            ENUM('Co', 'Khong'),
    tien_su_viem_nhiem_man_ghi_ro     ENUM('HP', 'Viem gan B/C', 'O viem man'),
    tien_su_benh_khac                 ENUM('Co', 'Khong'),
    tien_su_benh_khac_ghi_ro          VARCHAR(255),
    tien_su_gia_dinh                  ENUM('Co', 'Khong', 'Khong ro'),
    tien_su_gia_dinh_ghi_ro           VARCHAR(255),
    created_at                        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_patients_role FOREIGN KEY (role_id) REFERENCES roles (role_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. user_notification_settings: cau hinh thong bao ca nhan (1-1 voi users).
DROP TABLE IF EXISTS user_notification_settings;
CREATE TABLE user_notification_settings (
    user_id                INT PRIMARY KEY,
    is_push_enabled        TINYINT(1) NOT NULL DEFAULT 1,
    notify_appointment     TINYINT(1) NOT NULL DEFAULT 1,
    notify_lab_result      TINYINT(1) NOT NULL DEFAULT 1,
    notify_prescription    TINYINT(1) NOT NULL DEFAULT 1,
    sound_on               TINYINT(1) NOT NULL DEFAULT 1,
    sound_defalt           TINYINT(1) NOT NULL DEFAULT 1,
    notify_medical_record  TINYINT(1) NOT NULL DEFAULT 1,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifsettings_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS user_fcm_tokens;
CREATE TABLE user_fcm_tokens (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    fcm_token   TEXT NOT NULL,
    device_type ENUM('android', 'ios', 'web') NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fcm_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS token_blacklist;
CREATE TABLE token_blacklist (
    invalid_token_id INT AUTO_INCREMENT PRIMARY KEY,
    jti              VARCHAR(64) NOT NULL UNIQUE,
    user_id          INT NOT NULL,
    expires_at       DATETIME NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokenblacklist_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- NHOM B — BO TAO MAU PHIEU KHAM DONG (DYNAMIC FORM BUILDER)
-- =====================================================================

-- 10. templates: tai lieu moi doi PK sang ma dinh danh dang chuoi (VD: TMPL_URT_CHRONIC)
-- thay cho id tu tang int truoc day.
DROP TABLE IF EXISTS templates;
CREATE TABLE templates (
    code_template VARCHAR(255) PRIMARY KEY COMMENT 'Ma dinh danh mau, VD: TMPL_URT_CHRONIC, TMPL_RE_EXAM',
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    version       VARCHAR(50) NOT NULL DEFAULT '1.0',
    is_active     TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- template_sections.template_id duoc doi sang VARCHAR(255) de khop voi
-- templates.code_template (PK moi cua templates).
DROP TABLE IF EXISTS template_sections;
CREATE TABLE template_sections (
    section_id   INT AUTO_INCREMENT PRIMARY KEY,
    template_id  VARCHAR(255) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    `order`      INT NOT NULL DEFAULT 0,
    is_required  TINYINT(1) NOT NULL DEFAULT 0,
    filled_by    ENUM('patient', 'doctor', 'patdoc') NOT NULL DEFAULT 'patdoc',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_templatesections_template FOREIGN KEY (template_id) REFERENCES templates (code_template) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- questions: bo sung cot draft_response; cap nhat danh sach type_question theo tai lieu moi
-- (text, number, date, radio, checkbox, image_upload, label, group_questions, mix_text_image).
DROP TABLE IF EXISTS questions;
CREATE TABLE questions (
    question_id         VARCHAR(50) PRIMARY KEY,
    section_id          INT NOT NULL,
    question_text       TEXT NOT NULL,
    type_question       ENUM('text', 'number', 'date', 'radio', 'checkbox', 'image_upload', 'group_questions', 'mix_text_image') NOT NULL,
    placeholder         VARCHAR(255),
    `order`             INT NOT NULL DEFAULT 0,
    is_required         TINYINT(1) NOT NULL DEFAULT 0,
    parent_question_id  VARCHAR(50),
    show_if_answer      JSON,
    condition_rules     JSON,
    help_text           TEXT,
    allow_image         TINYINT(1) NOT NULL DEFAULT 0,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_questions_section FOREIGN KEY (section_id) REFERENCES template_sections (section_id) ON DELETE CASCADE,
    KEY idx_questions_parent (parent_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS question_options;
CREATE TABLE question_options (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    question_id           VARCHAR(50) NOT NULL,
    option_text           VARCHAR(255) NOT NULL,
    option_value          VARCHAR(255) NOT NULL,
    `order`               INT NOT NULL DEFAULT 0,
    has_text_input        TINYINT(1) NOT NULL DEFAULT 0,
    trigger_image_upload  TINYINT(1) NOT NULL DEFAULT 0,
    image_upload_label    VARCHAR(255),
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_questionoptions_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- NHOM C — HO SO BENH AN, CAU TRA LOI & TEP DINH KEM
-- =====================================================================

-- medical_records.template_id doi sang VARCHAR(255) de khop templates.code_template.
DROP TABLE IF EXISTS medical_records;
CREATE TABLE medical_records (
    ma_ho_so                        INT AUTO_INCREMENT PRIMARY KEY,
    ma_benh_nhan                    INT NOT NULL,
    ma_bac_si                       INT,
    created_by                      INT,
    template_id                     VARCHAR(255),
    chan_doan_tuyen_truoc            VARCHAR(255),
    chan_doan_ban_dau_phong_kham     VARCHAR(255),
    benh_phu                        VARCHAR(255),
    ket_qua_dieu_tri_bao_hiem        ENUM('Khoi benh', 'Do', 'Khong do', 'Nang hon', 'Chet', 'Bien chung'),
    luu_huyet_thanh                  ENUM('Co', 'Khong'),
    thoi_gian_khoi_phat_tuan         INT,
    bieu_hien_qua_trinh_benh         ENUM('Chi san phu', 'Chi phu mach', 'San phu + phu mach'),
    sot                             ENUM('Co', 'Khong'),
    sot_nhiet_do_c                   DECIMAL(4,1),
    mach                            INT,
    huyet_ap_tam_thu                 INT,
    huyet_ap_tam_truong              INT,
    bat_thuong_co_quan_khac          ENUM('Co', 'Khong'),
    bat_thuong_co_quan_khac_ghi_ro   VARCHAR(255),
    chan_doan_chinh                  ENUM('CSU', 'CIndU', 'CSU + CIndU', 'Phu mach don thuan'),
    phan_nhom_csu                    ENUM('CSU Type I', 'CSU Type IIb', 'Chong lap Type I + Type IIb', 'Khong xac dinh'),
    phan_nhom_cindu_chinh            ENUM('Da ve noi', 'Cholinergic', 'Do lanh'),
    phan_nhom_cindu_khac             ENUM('Adrenergic', 'Anh sang', 'Do nong', 'Do nuoc'),
    ngay_hen_tai_kham                 DATE,
    tom_tat_buoi_kham                 TEXT,
    huong_xu_tri_lan_sau              TEXT,
    dan_do                            TEXT,
    status                            ENUM('draft', 'pending', 'in_progress', 'completed', 'cancelled') NOT NULL DEFAULT 'draft',
    completed_at                      DATETIME,
    ngay_kham                         DATE,
    created_at                        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_medrec_patient FOREIGN KEY (ma_benh_nhan) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_medrec_doctor FOREIGN KEY (ma_bac_si) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_medrec_createdby FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT fk_medrec_template FOREIGN KEY (template_id) REFERENCES templates (code_template) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS re_examinations;
CREATE TABLE re_examinations (
    ma_tai_kham     INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so        INT NOT NULL,
    ma_benh_nhan    INT NOT NULL,
    lan_tai_kham    TINYINT NOT NULL,
    ngay_tai_kham   DATE NOT NULL,
    chan_doan       TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reexam_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE CASCADE,
    CONSTRAINT fk_reexam_patient FOREIGN KEY (ma_benh_nhan) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14.3 wheal_assessments — tai lieu moi da mo ta day du cot (viet lai hoan toan).
DROP TABLE IF EXISTS wheal_assessments;
CREATE TABLE wheal_assessments (
    wheal_assessmant_id             INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so                        INT NOT NULL,
    hien_tai_co_san_phu              ENUM('Co', 'Khong'),
    anh_san_phu                      TINYINT(1) DEFAULT 0 COMMENT 'Co dinh kem anh hay khong (chi tiet anh quan ly rieng o media_attachments)',
    hoan_canh_xuat_hien_san_phu      ENUM('Ngau nhien', 'Kich thich co hoc/nhiet/vat ly', 'Ca hai'),
    uu_the_san_phu_va_phu_mach       JSON COMMENT 'Multi-enum: Ngau nhien (so tuan); Kich thich (so tuan); Tuong duong; Khong ro',
    dac_diem_san_phu_khong_dung_thuoc JSON COMMENT 'Array[Object]: dang ton thuong, kich thuoc, thoi gian ton tai, thoi diem',
    dac_diem_san_phu_co_dung_thuoc   JSON COMMENT 'Array[Object]: dang ton thuong, kich thuoc, thoi gian ton tai, thoi diem',
    vi_tri_san_phu                   ENUM('Khap co the', 'Vi tri dac biet', 'Khong ro'),
    yeu_to_lam_nang_sa_phu           ENUM('Khong co', 'Stress', 'Kinh nguyet', 'Nhiem trung', 'NSAIDs', 'Thuc an', 'Thuoc khac', 'Khac'),
    danh_gia_muc_do_hoat_dong_benh   JSON COMMENT 'Array[Object]: bang 3 dong (UAS7/42, ISS7, HSS7) x 3 cot (Khong thuoc, Dung deu, Dung khong deu)',
    created_at                       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_whealassess_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS angioedema_assessments;
CREATE TABLE angioedema_assessments (
    angioedema_assessments_id       INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so                        INT NOT NULL,
    trong_dot_co_phu_mach            ENUM('Co', 'Khong'),
    hien_tai_co_phu_mach             ENUM('Co', 'Khong'),
    anh_phu_mach                     TINYINT(1) DEFAULT 0 COMMENT 'Co dinh kem anh hay khong',
    hoan_canh_xuat_hien_phu_mach     VARCHAR(255),
    vi_tri_phu_mach                  JSON COMMENT 'Multi-enum vi tri va ben phu mach',
    tan_suat_va_muc_do_nang_phu_mach JSON,
    thoi_gian_ton_tai_phu_mach       JSON,
    moi_lien_quan_san_phu_va_phu_mach JSON,
    phu_mach_1_tuan_gan_day          ENUM('Co', 'Khong'),
    aas7_diem                       INT COMMENT 'Diem hoat luc phu mach AAS7 (/105)',
    created_at                       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_angioedemaassess_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS cindu_provocation_tests;
CREATE TABLE cindu_provocation_tests (
    cindu_provocation_tests_id   INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so                     INT NOT NULL,
    ngung_khang_histamine_truoc_test ENUM('Co', 'Khong'),
    thoi_gian_ngung_khang_histamine INT,
    ngung_corticoid_truoc_test    ENUM('Co', 'Khong'),
    thoi_gian_ngung_corticoid     INT,
    da_ve_noi_ket_qua              ENUM('Duong tinh', 'Am tinh'),
    fric_score                    DECIMAL(6,2),
    da_ve_noi_ngua_nrs             TINYINT,
    da_ve_noi_dau_nrs              TINYINT,
    da_ve_noi_bong_rat_nrs         TINYINT,
    may_day_cholin_kq              ENUM('Duong tinh', 'Am tinh'),
    thoi_gian_xuat_hien_ton_thuong INT,
    cholin_ngua_nrs                TINYINT,
    cholin_dau_nrs                 TINYINT,
    cholin_bong_rat_nrs            TINYINT,
    may_day_nong_temptest_kq       ENUM('Duong tinh', 'Am tinh'),
    nguong_nhiet_do_nong            DECIMAL(4,1),
    nong_ngua_nrs                  TINYINT,
    nong_dau_nrs                   TINYINT,
    nong_bong_rat_nrs              TINYINT,
    may_day_lanh_temptest_kq        ENUM('Duong tinh', 'Am tinh'),
    vung_nhiet_do_duong_tinh        DECIMAL(4,1),
    temptest_ngua_nrs               TINYINT,
    temptest_dau_nrs                TINYINT,
    temptest_bong_rat_nrs           TINYINT,
    may_day_lanh_test_cuc_da_kq     ENUM('Duong tinh', 'Am tinh'),
    nguong_thoi_gian                INT,
    test_cuc_da_ngua_nrs            TINYINT,
    test_cuc_da_dau_nrs             TINYINT,
    test_cuc_da_bong_rat_nrs        TINYINT,
    may_day_ap_luc_cham_kq          ENUM('Duong tinh', 'Am tinh'),
    nguong_ap_luc                   DECIMAL(5,2),
    ap_luc_ngua_nrs                 TINYINT,
    ap_luc_dau_nrs                  TINYINT,
    ap_luc_bong_rat_nrs             TINYINT,
    may_day_anh_sang_kq             ENUM('Duong tinh', 'Am tinh'),
    buoc_song_duong_tinh             INT,
    anh_sang_ngua_nrs                TINYINT,
    anh_sang_dau_nrs                 TINYINT,
    anh_sang_bong_rat_nrs            TINYINT,
    may_day_nuoc_kq                  ENUM('Duong tinh', 'Am tinh'),
    thoi_gian_xuat_hien_nuoc          INT,
    nuoc_ngua_nrs                    TINYINT,
    nuoc_dau_nrs                     TINYINT,
    nuoc_bong_rat_nrs                TINYINT,
    can_nguyen_khac_ten               VARCHAR(255),
    can_nguyen_khac_test_kq           ENUM('Duong tinh', 'Am tinh'),
    can_nguyen_khac_ngua_nrs          TINYINT,
    can_nguyen_khac_dau_nrs           TINYINT,
    can_nguyen_khac_bong_rat_nrs      TINYINT,
    created_at                       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cindutest_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS proms_scores;
CREATE TABLE proms_scores (
    proms_scores_id   INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so           INT NOT NULL,
    uct_diem           DECIMAL(5,2) COMMENT 'Urticaria Control Test /16, cut-off >=12',
    aect_diem          DECIMAL(5,2) COMMENT 'Angioedema Control Test /16',
    act_diem_legacy    DECIMAL(5,2) COMMENT 'Diem ACT cu (legacy)',
    dlqi_diem          DECIMAL(5,2) COMMENT 'Dermatology Life Quality Index /30',
    cu_q2ol_diem       DECIMAL(5,2) COMMENT 'Chronic Urticaria Quality of Life',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_promsscores_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14.7 disease_history — tai lieu moi da mo ta day du cot (viet lai hoan toan).
DROP TABLE IF EXISTS disease_history;
CREATE TABLE disease_history (
    disease_history_id       INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so                  INT NOT NULL,
    tung_co_dot_tuong_tu       ENUM('Co', 'Khong', 'Khong ro'),
    tuoi_khoi_phat             INT,
    so_dot_truoc               INT,
    kieu_dien_bien             ENUM('Lien tuc', 'Tung dot', 'Khong ro'),
    bieu_hien_dot_truoc        ENUM('San phu', 'Phu mach', 'San phu + phu mach'),
    da_tung_nhap_vien          ENUM('Co', 'Khong', 'Khong ro'),
    so_lan_nhap_vien           INT,
    thoi_gian_tu_dot_gan_nhat  VARCHAR(255),
    dien_bien_cac_dot_truoc    TEXT,
    da_dieu_tri_truoc_kham     ENUM('Co', 'Khong', 'Khong ro'),
    created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_diseasehistory_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14.8 prior_treatments — tai lieu moi doi FK: tham chieu truc tiep ma_ho_so
-- (medical_records), KHONG con qua disease_history_id nhu truoc.
DROP TABLE IF EXISTS prior_treatments;
CREATE TABLE prior_treatments (
    prior_treatments_id   INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so               INT NOT NULL,
    ten_thuoc               VARCHAR(255),
    lieu_dung               VARCHAR(255),
    thoi_gian_dung          VARCHAR(255),
    tuan_thu                ENUM('Tuan thu tot', 'Khong tuan thu', 'Tuan thu mot phan', 'Khong ro'),
    dap_ung                 ENUM('Co', 'Khong'),
    created_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_priortreatments_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS answers;
CREATE TABLE answers (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so             INT NOT NULL,
    question_id          VARCHAR(50) NOT NULL,
    question_option_id   INT,
    answer_text          TEXT,
    answer_value         JSON,
    answered_by          INT,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_answers_record_question (ma_ho_so, question_id),
    CONSTRAINT fk_answers_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE RESTRICT,
    KEY idx_answers_answered_by (answered_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS media_attachments;
CREATE TABLE media_attachments (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    ma_ho_so     INT,
    question_id  VARCHAR(50),
    file_path    VARCHAR(255),
    file_url     VARCHAR(255),
    file_name    VARCHAR(255),
    file_type    VARCHAR(100),
    media_type   ENUM('image', 'video', 'audio') NOT NULL,
    category     VARCHAR(100),
    description  VARCHAR(255),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mediaattach_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_mediaattach_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT,
    CONSTRAINT fk_mediaattach_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS answer_images;
CREATE TABLE answer_images (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    answer_id      INT NOT NULL,
    attachment_id  INT NOT NULL,
    option_value   VARCHAR(255),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_answerimages_answer FOREIGN KEY (answer_id) REFERENCES answers (id) ON DELETE CASCADE,
    CONSTRAINT fk_answerimages_attachment FOREIGN KEY (attachment_id) REFERENCES media_attachments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- NHOM D — THEO DOI TON THUONG DA & THANG DIEM ME DAY (UAS7)
-- =====================================================================

DROP TABLE IF EXISTS lesion_episodes;
CREATE TABLE lesion_episodes (
    episode_id            INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so              INT NOT NULL,
    ma_benh_nhan          INT NOT NULL,
    name                  VARCHAR(255),
    occurred_at           DATETIME,
    context_tags          JSON,
    context_other         TEXT,
    medication_name       VARCHAR(255),
    medication_taken_ago  INT,
    medication_unit       ENUM('hour', 'minute', 'day', 'month', 'year'),
    description           TEXT,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesionepisodes_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT,
    CONSTRAINT fk_lesionepisodes_patient FOREIGN KEY (ma_benh_nhan) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS lesion_locations;
CREATE TABLE lesion_locations (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    episode_id            INT NOT NULL,
    body_part             VARCHAR(100),
    occurred_at           DATETIME,
    context_tags          JSON,
    context_other         TEXT,
    medication_name       VARCHAR(255),
    medication_taken_ago  INT,
    medication_unit       ENUM('hour', 'minute', 'day', 'month', 'year'),
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesionlocations_episode FOREIGN KEY (episode_id) REFERENCES lesion_episodes (episode_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS lesion_photos;
CREATE TABLE lesion_photos (
    photo_id       INT AUTO_INCREMENT PRIMARY KEY,
    episode_id     INT NOT NULL,
    location_id    INT,
    attachment_id  INT NOT NULL,
    taken_at       DATETIME,
    milestone      ENUM('start', 'peak', 'normal') NOT NULL,
    itch_score     TINYINT,
    pain_score     TINYINT,
    burn_score     TINYINT,
    hives_count    INT,
    local_auas     INT,
    annotated_url  VARCHAR(500),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesionphotos_episode FOREIGN KEY (episode_id) REFERENCES lesion_episodes (episode_id) ON DELETE RESTRICT,
    CONSTRAINT fk_lesionphotos_location FOREIGN KEY (location_id) REFERENCES lesion_locations (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_lesionphotos_attachment FOREIGN KEY (attachment_id) REFERENCES media_attachments (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS uas7_scores;
CREATE TABLE uas7_scores (
    uas7_score_id  VARCHAR(50) PRIMARY KEY COMMENT 'VD: p12345_260914 (ma benh nhan + ngay)',
    ma_benh_nhan   INT NOT NULL,
    ma_ho_so       INT NOT NULL,
    score_date     DATE NOT NULL,
    iss7           INT,
    hss7           INT,
    daily_score    INT,
    note           TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_uas7_patient_date (ma_benh_nhan, score_date),
    CONSTRAINT fk_uas7_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT,
    CONSTRAINT fk_uas7_patient FOREIGN KEY (ma_benh_nhan) REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- NHOM E — XET NGHIEM
-- =====================================================================

DROP TABLE IF EXISTS lab_requests;
CREATE TABLE lab_requests (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so      INT NOT NULL,
    requested_by  INT NOT NULL,
    status        ENUM('pending', 'in_progress', 'completed') NOT NULL DEFAULT 'pending',
    note          TEXT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_labrequests_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT,
    CONSTRAINT fk_labrequests_requestedby FOREIGN KEY (requested_by) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS lab_request_items;
CREATE TABLE lab_request_items (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    lab_request_id INT NOT NULL,
    test_name      VARCHAR(255) NOT NULL,
    description    TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_labrequestitems_request FOREIGN KEY (lab_request_id) REFERENCES lab_requests (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS lab_results;
CREATE TABLE lab_results (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    lab_request_id       INT NOT NULL,
    lab_request_item_id  INT,
    result_value         TEXT,
    concluded_by          INT NOT NULL,
    note                  TEXT,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_labresults_request FOREIGN KEY (lab_request_id) REFERENCES lab_requests (id) ON DELETE RESTRICT,
    CONSTRAINT fk_labresults_item FOREIGN KEY (lab_request_item_id) REFERENCES lab_request_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_labresults_concludedby FOREIGN KEY (concluded_by) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 24.2 lab_clinical_values — 20 bien can lam sang
DROP TABLE IF EXISTS lab_clinical_values;
CREATE TABLE lab_clinical_values (
    lab_clinical_value_id      INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so                    INT NOT NULL UNIQUE,
    test_huyet_thanh_tu_than     ENUM('Duong tinh', 'Am tinh', 'Chua lam'),
    asst_duong_kinh_mm           DECIMAL(5,2),
    bat_ket_qua                  ENUM('Duong tinh', 'Am tinh', 'Chua lam'),
    ige_khang_tpo_ket_qua        ENUM('Duong tinh', 'Am tinh', 'Chua lam'),
    ige_khang_il24_ket_qua       ENUM('Duong tinh', 'Am tinh', 'Chua lam'),
    can_lam_sang_khac_ghi_chu    TEXT,
    wbc                         DECIMAL(6,2),
    eo                          DECIMAL(6,2),
    ba_bach_cau                  DECIMAL(6,2),
    crp                         DECIMAL(6,2),
    mau_lang_1h                 DECIMAL(5,2),
    mau_lang_2h                 DECIMAL(5,2),
    ft3                         DECIMAL(6,2),
    ft4                         DECIMAL(6,2),
    tsh                         DECIMAL(6,2),
    ige_toan_phan                DECIMAL(8,2),
    anti_tpo                    DECIMAL(8,2),
    ana_hep2                    ENUM('Duong tinh', 'Am tinh'),
    sieu_am_tuyen_giap           TEXT,
    xet_nghiem_khac              TEXT,
    created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_labclinicalvalues_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- NHOM F — DON THUOC
-- =====================================================================

DROP TABLE IF EXISTS prescriptions;
CREATE TABLE prescriptions (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    ma_ho_so              INT NOT NULL,
    ngung_thuoc_lan_sau    ENUM('Co', 'Khong'),
    ngung_thuoc_bang       JSON,
    ghi_chu_dieu_tri        TEXT,
    created_by             INT,
    note                   TEXT,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescriptions_record FOREIGN KEY (ma_ho_so) REFERENCES medical_records (ma_ho_so) ON DELETE CASCADE,
    CONSTRAINT fk_prescriptions_createdby FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS prescription_items;
CREATE TABLE prescription_items (
    id                        INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id           INT NOT NULL,
    nhom_dieu_tri              ENUM('Khang histamine H1 TH2', 'Dieu tri sinh hoc/dich', 'Uc che mien dich', 'Corticosteroid toan than', 'Dieu tri khac'),
    khang_histamine_ten_thuoc  VARCHAR(255),
    lieu_dung                 VARCHAR(100),
    thoi_gian_dung             VARCHAR(100),
    sinh_hoc_dich_loai         ENUM('Omalizumab', 'Dupilumab', 'Remibrutinib', 'Khac'),
    sinh_hoc_dich_lieu         VARCHAR(100),
    sinh_hoc_dich_thoi_gian    VARCHAR(100),
    uc_che_mien_dich_loai      ENUM('Cyclosporin A', 'Khac'),
    uc_che_mien_dich_lieu      VARCHAR(100),
    uc_che_mien_dich_thoi_gian VARCHAR(100),
    corticosteroid_ten_thuoc   VARCHAR(255),
    corticosteroid_lieu        VARCHAR(100),
    corticosteroid_thoi_gian   VARCHAR(100),
    dieu_tri_khac_ten_thuoc    VARCHAR(255),
    dieu_tri_khac_lieu         VARCHAR(100),
    dieu_tri_khac_thoi_gian    VARCHAR(100),
    created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescriptionitems_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- NHOM G — THONG BAO
-- =====================================================================

-- notifications: bo sung cot avatar theo tai lieu moi.
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    title         VARCHAR(255) NOT NULL,
    body          TEXT,
    type          ENUM('appointment', 'lab_result', 'prescription', 'medical_record', 'general') NOT NULL DEFAULT 'general',
    avatar        VARCHAR(255),
    reference_id  INT,
    is_read       TINYINT(1) NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- SEED DATA CO BAN
-- =====================================================================

-- Cay phan cap vai tro theo tai lieu: director -> chief_of_branch -> chief_of_department
-- -> doctor / chief_of_nurse -> nurse; technician truc thuoc chief_of_department.
-- "admin" va "patient" nam ngoai cay phan cap lam sang.
INSERT INTO roles (name, description, parent_role_id) VALUES
    ('admin', 'Quan tri vien he thong', NULL),
    ('patient', 'Benh nhan', NULL),
    ('director', 'Giam doc benh vien', NULL);

INSERT INTO roles (name, description, parent_role_id)
SELECT 'chief_of_branch', 'Pho giam doc benh vien / Truong chi nhanh', role_id FROM roles WHERE name = 'director';

INSERT INTO roles (name, description, parent_role_id)
SELECT 'chief_of_department', 'Pho truong chi nhanh / Truong khoa', role_id FROM roles WHERE name = 'chief_of_branch';

INSERT INTO roles (name, description, parent_role_id)
SELECT 'doctor', 'Bac si', role_id FROM roles WHERE name = 'chief_of_department';

INSERT INTO roles (name, description, parent_role_id)
SELECT 'chief_of_nurse', 'Dieu duong truong', role_id FROM roles WHERE name = 'chief_of_department';

INSERT INTO roles (name, description, parent_role_id)
SELECT 'technician', 'Ky thuat vien', role_id FROM roles WHERE name = 'chief_of_department';

INSERT INTO roles (name, description, parent_role_id)
SELECT 'nurse', 'Dieu duong', role_id FROM roles WHERE name = 'chief_of_nurse';

INSERT INTO permissions (name, description) VALUES
    ('manage_users', 'Quan ly nguoi dung'),
    ('manage_medical_records', 'Quan ly ho so benh an'),
    ('manage_prescriptions', 'Quan ly don thuoc'),
    ('manage_lab', 'Quan ly xet nghiem'),
    ('view_medical_records', 'Xem ho so benh an');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r, permissions p WHERE r.name = 'admin';
