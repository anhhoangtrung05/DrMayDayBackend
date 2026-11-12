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
        log.info("Admin creating a branch");
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
        log.info("Admin updating a branch: id={}", id);
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
        log.info("Admin deleting a branch: id={}", id);
        Branch branch = findBranch(id);
        Long branchId = branch.getBranchId();

        // Check whether this is the last branch before checking dependencies; it always returns 2107.
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
            // Another admin already deleted the branch, so the DELETE statement found no row.
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

    // The so_dien_thoai column disallows empty strings, so empty values are stored as null.
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
