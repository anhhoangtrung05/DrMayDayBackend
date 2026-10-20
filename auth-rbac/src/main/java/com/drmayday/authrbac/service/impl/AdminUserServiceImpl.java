package com.drmayday.authrbac.service.impl;

import com.drmayday.authrbac.dto.CreateUserRequest;
import com.drmayday.authrbac.dto.EditUserRequest;
import com.drmayday.authrbac.dto.UserDetailsResponse;
import com.drmayday.authrbac.dto.UserResponse;
import com.drmayday.authrbac.entity.BenhNhan;
import com.drmayday.authrbac.entity.Nguoi;
import com.drmayday.authrbac.entity.NhanVien;
import com.drmayday.authrbac.entity.TaiKhoan;
import com.drmayday.authrbac.entity.VaiTro;
import com.drmayday.authrbac.repository.BenhNhanRepository;
import com.drmayday.authrbac.repository.NguoiRepository;
import com.drmayday.authrbac.repository.NhanVienRepository;
import com.drmayday.authrbac.repository.TaiKhoanRepository;
import com.drmayday.authrbac.repository.VaiTroRepository;
import com.drmayday.authrbac.service.AdminUserService;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import com.drmayday.facility.entity.ChiNhanh;
import com.drmayday.facility.repository.ChiNhanhRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TaiKhoanRepository taiKhoanRepository;
    private final NguoiRepository nguoiRepository;
    private final BenhNhanRepository benhNhanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final VaiTroRepository vaiTroRepository;
    private final ChiNhanhRepository chiNhanhRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(TaiKhoanRepository taiKhoanRepository,
                                NguoiRepository nguoiRepository,
                                BenhNhanRepository benhNhanRepository,
                                NhanVienRepository nhanVienRepository,
                                VaiTroRepository vaiTroRepository,
                                ChiNhanhRepository chiNhanhRepository,
                                PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nguoiRepository = nguoiRepository;
        this.benhNhanRepository = benhNhanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.chiNhanhRepository = chiNhanhRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getListUsers(int page, int limit) {
        log.info("Admin truy xuất danh sách người dùng: page={}, limit={}", page, limit);
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 20;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "maTaiKhoan"));
        Page<TaiKhoan> accountsPage = taiKhoanRepository.findAllWithDetails(pageable);

        return accountsPage.map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserDetails(String id) {
        log.info("Admin truy xuất chi tiết người dùng: id={}", id);
        Long userId = parseUserId(id);

        TaiKhoan taiKhoan = taiKhoanRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        return mapToUserDetailsResponse(taiKhoan);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Admin tạo mới người dùng: phone={}, role_id={}", request.getPhone(), request.getRoleId());
        String phone = request.getPhone().trim();

        if (taiKhoanRepository.existsByTenDangNhap(phone) || nguoiRepository.existsBySoDienThoai(phone)) {
            throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        VaiTro vaiTro = resolveRole(request.getRoleId());
        boolean active = !"0".equals(request.getIsActive()) && !"false".equalsIgnoreCase(request.getIsActive());

        OffsetDateTime creationTime = OffsetDateTime.now().minusSeconds(10);

        Nguoi nguoi = Nguoi.builder()
                .hoTen(request.getFullName().trim())
                .soDienThoai(phone)
                .thoiDiemTao(creationTime)
                .thoiDiemCapNhat(creationTime)
                .build();
        nguoi = nguoiRepository.save(nguoi);

        if ("benh_nhan".equalsIgnoreCase(vaiTro.getTenVaiTro()) || Long.valueOf(4L).equals(vaiTro.getMaVaiTro())) {
            BenhNhan benhNhan = BenhNhan.builder()
                    .maBenhNhan(nguoi.getMaNguoi())
                    .nguoi(nguoi)
                    .build();
            benhNhanRepository.save(benhNhan);
        } else {
            Long chiNhanhId = chiNhanhRepository.findFirstByOrderByMaChiNhanhAsc()
                    .map(ChiNhanh::getMaChiNhanh)
                    .orElse(1L);
            NhanVien nhanVien = NhanVien.builder()
                    .maNhanVien(nguoi.getMaNguoi())
                    .nguoi(nguoi)
                    .maChiNhanh(chiNhanhId)
                    .build();
            nhanVienRepository.save(nhanVien);
        }

        TaiKhoan taiKhoan = TaiKhoan.builder()
                .nguoi(nguoi)
                .vaiTro(vaiTro)
                .tenDangNhap(phone)
                .matKhauMaHoa(passwordEncoder.encode(request.getPassword()))
                .trangThaiHoatDong(active)
                .thoiDiemTao(creationTime)
                .thoiDiemCapNhat(creationTime)
                .build();
        taiKhoan = taiKhoanRepository.save(taiKhoan);

        return mapToUserResponse(taiKhoan);
    }

    @Override
    @Transactional
    public UserResponse editUser(String id, EditUserRequest request) {
        log.info("Admin chỉnh sửa người dùng: id={}", id);
        Long userId = parseUserId(id);

        TaiKhoan taiKhoan = taiKhoanRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            if (taiKhoan.getNguoi() != null) {
                taiKhoan.getNguoi().setHoTen(request.getFullName().trim());
                OffsetDateTime now = OffsetDateTime.now();
                if (taiKhoan.getNguoi().getThoiDiemTao() != null && now.isBefore(taiKhoan.getNguoi().getThoiDiemTao())) {
                    now = taiKhoan.getNguoi().getThoiDiemTao();
                }
                taiKhoan.getNguoi().setThoiDiemCapNhat(now);
                nguoiRepository.save(taiKhoan.getNguoi());
            }
        }

        if (request.getIsActive() != null && !request.getIsActive().trim().isEmpty()) {
            boolean active = "1".equals(request.getIsActive().trim()) || "true".equalsIgnoreCase(request.getIsActive().trim());
            taiKhoan.setTrangThaiHoatDong(active);
        }

        if (request.getRoleId() != null && !request.getRoleId().trim().isEmpty()) {
            VaiTro vaiTro = resolveRole(request.getRoleId().trim());
            taiKhoan.setVaiTro(vaiTro);

            if (taiKhoan.getNguoi() != null) {
                Long personId = taiKhoan.getNguoi().getMaNguoi();
                if ("benh_nhan".equalsIgnoreCase(vaiTro.getTenVaiTro()) || Long.valueOf(4L).equals(vaiTro.getMaVaiTro())) {
                    if (!benhNhanRepository.existsById(personId)) {
                        BenhNhan bn = BenhNhan.builder()
                                .maBenhNhan(personId)
                                .nguoi(taiKhoan.getNguoi())
                                .build();
                        benhNhanRepository.save(bn);
                    }
                } else {
                    if (!nhanVienRepository.existsById(personId)) {
                        Long chiNhanhId = chiNhanhRepository.findFirstByOrderByMaChiNhanhAsc()
                                .map(ChiNhanh::getMaChiNhanh)
                                .orElse(1L);
                        NhanVien nv = NhanVien.builder()
                                .maNhanVien(personId)
                                .nguoi(taiKhoan.getNguoi())
                                .maChiNhanh(chiNhanhId)
                                .build();
                        nhanVienRepository.save(nv);
                    }
                }
            }
        }

        OffsetDateTime nowTaiKhoan = OffsetDateTime.now();
        if (taiKhoan.getThoiDiemTao() != null && nowTaiKhoan.isBefore(taiKhoan.getThoiDiemTao())) {
            nowTaiKhoan = taiKhoan.getThoiDiemTao();
        }
        taiKhoan.setThoiDiemCapNhat(nowTaiKhoan);
        taiKhoan = taiKhoanRepository.save(taiKhoan);

        return mapToUserResponse(taiKhoan);
    }

    private UserResponse mapToUserResponse(TaiKhoan taiKhoan) {
        Nguoi nguoi = taiKhoan.getNguoi();
        VaiTro vaiTro = taiKhoan.getVaiTro();

        return UserResponse.builder()
                .userId(String.valueOf(taiKhoan.getMaTaiKhoan()))
                .fullName(nguoi != null ? nguoi.getHoTen() : "")
                .phone(taiKhoan.getTenDangNhap())
                .roleId(vaiTro != null ? String.valueOf(vaiTro.getMaVaiTro()) : "4")
                .isActive(Boolean.TRUE.equals(taiKhoan.getTrangThaiHoatDong()) ? "1" : "0")
                .createdAt(formatOffsetDateTime(taiKhoan.getThoiDiemTao()))
                .updatedAt(formatOffsetDateTime(taiKhoan.getThoiDiemCapNhat()))
                .build();
    }

    private UserDetailsResponse mapToUserDetailsResponse(TaiKhoan taiKhoan) {
        Nguoi nguoi = taiKhoan.getNguoi();
        VaiTro vaiTro = taiKhoan.getVaiTro();

        return UserDetailsResponse.builder()
                .userId(String.valueOf(taiKhoan.getMaTaiKhoan()))
                .fullName(nguoi != null ? nguoi.getHoTen() : "")
                .phone(taiKhoan.getTenDangNhap())
                .roleId(vaiTro != null ? String.valueOf(vaiTro.getMaVaiTro()) : "4")
                .isActive(Boolean.TRUE.equals(taiKhoan.getTrangThaiHoatDong()) ? "1" : "0")
                .createdAt(formatOffsetDateTime(taiKhoan.getThoiDiemTao()))
                .updatedAt(formatOffsetDateTime(taiKhoan.getThoiDiemCapNhat()))
                .build();
    }

    private Long parseUserId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
    }

    private VaiTro resolveRole(String roleIdentifier) {
        if (roleIdentifier == null || roleIdentifier.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại");
        }
        String trimmed = roleIdentifier.trim();
        try {
            long roleId = Long.parseLong(trimmed);
            return vaiTroRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        } catch (NumberFormatException e) {
            String roleName = trimmed.toLowerCase();
            if ("admin".equals(roleName)) {
                return vaiTroRepository.findByTenVaiTro("admin")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("doctor".equals(roleName) || "bac_si".equals(roleName)) {
                return vaiTroRepository.findByTenVaiTro("bac_si")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("nurse".equals(roleName) || "dieu_duong".equals(roleName)) {
                return vaiTroRepository.findByTenVaiTro("dieu_duong")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("patient".equals(roleName) || "benh_nhan".equals(roleName)) {
                return vaiTroRepository.findByTenVaiTro("benh_nhan")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            return vaiTroRepository.findByTenVaiTro(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        }
    }

    private String formatOffsetDateTime(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
}
