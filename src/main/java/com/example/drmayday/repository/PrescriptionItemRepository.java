package com.example.drmayday.repository;

import com.example.drmayday.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionIdInOrderByPrescriptionItemIdAsc(Collection<Long> prescriptionIds);

    List<PrescriptionItem> findByPrescriptionIdOrderByPrescriptionItemIdAsc(Long prescriptionId);

    void deleteByPrescriptionId(Long prescriptionId);
}
