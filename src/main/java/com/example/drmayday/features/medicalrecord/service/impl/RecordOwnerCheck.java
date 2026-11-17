package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra bệnh nhân đang gọi có phải chủ hồ sơ không. Dùng chung cho hồ sơ (spec 0002) và phiên khám (spec 0004).
 */
@Component
class RecordOwnerCheck {

    private final UserAccountRepository userAccountRepository;

    RecordOwnerCheck(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    // Token chỉ mang ma_tai_khoan, nên phải đọc taikhoan để lấy ma_nguoi (= ma_benh_nhan)
    void assertCurrentPatientOwns(Long patientId) {
        UserAccount account = userAccountRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA));
        Person person = account.getPerson();
        if (person == null || !patientId.equals(person.getPersonId())) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }
    }
}
