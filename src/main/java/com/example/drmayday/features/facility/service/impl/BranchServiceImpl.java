package com.example.drmayday.features.facility.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.features.facility.dto.BranchResponse;
import com.example.drmayday.features.facility.dto.CreateBranchRequest;
import com.example.drmayday.features.facility.dto.EditBranchRequest;
import com.example.drmayday.features.facility.service.BranchService;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchServiceImpl implements BranchService {

    private static final Logger log = LoggerFactory.getLogger(BranchServiceImpl.class);

    private final BranchRepository branchRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final EmployeeRepository employeeRepository;

    public BranchServiceImpl(BranchRepository branchRepository,
                             ClinicRoomRepository clinicRoomRepository,
                             EmployeeRepository employeeRepository) {
        this.branchRepository = branchRepository;
        this.clinicRoomRepository = clinicRoomRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public BranchResponse createBranch(CreateBranchRequest req) {
        log.info("Admin tạo chi nhánh mới");
        Branch branch = new Branch(null, req.getName().trim(), req.getAddress().trim(),
                normalizePhone(req.getPhone()), true);
        return BranchResponse.from(branchRepository.saveAndFlush(branch));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getListBranches() {
        return branchRepository.findAll(Sort.by(Sort.Direction.ASC, "branchId")).stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchDetails(String id) {
        return BranchResponse.from(findBranch(id));
    }

    @Override
    @Transactional
    public BranchResponse editBranch(String id, EditBranchRequest req) {
        log.info("Admin sửa chi nhánh: id={}", id);
        Branch branch = findBranch(id);

        if (req.getName() != null) {
            if (req.getName().isBlank()) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
            }
            branch.setName(req.getName().trim());
        }
        if (req.getAddress() != null) {
            if (req.getAddress().isBlank()) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
            }
            branch.setAddress(req.getAddress().trim());
        }
        if (req.getPhone() != null) {
            branch.setPhoneNumber(normalizePhone(req.getPhone()));
        }
        if (req.getStatus() != null) {
            branch.setActive(req.getStatus());
        }

        return BranchResponse.from(branchRepository.saveAndFlush(branch));
    }

    @Override
    @Transactional
    public void deleteBranch(String id) {
        log.info("Admin xóa chi nhánh: id={}", id);
        Branch branch = findBranch(id);
        Long branchId = branch.getBranchId();

        // Kiểm tra chi nhánh cuối trước phụ thuộc: chi nhánh cuối luôn trả 2107 dù còn phòng hay nhân viên
        if (branchRepository.count() <= 1) {
            throw new AppException(ErrorCode.LAST_BRANCH_CANNOT_BE_DELETED);
        }
        if (clinicRoomRepository.existsByBranchId(branchId)
                || employeeRepository.existsByBranchId(branchId)) {
            throw new AppException(ErrorCode.BRANCH_HAS_DEPENDENCIES);
        }

        try {
            branchRepository.delete(branch);
            branchRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (FacilitySqlErrors.isForeignKeyViolation(ex)) {
                throw new AppException(ErrorCode.BRANCH_HAS_DEPENDENCIES);
            }
            throw ex;
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Admin khác đã xóa chi nhánh trước, câu DELETE không còn dòng nào để xóa
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
    }

    private Branch findBranch(String id) {
        return branchRepository.findById(parseBranchId(id))
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private Long parseBranchId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
    }

    // Cột so_dien_thoai cấm chuỗi rỗng nên giá trị rỗng được lưu thành NULL
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
