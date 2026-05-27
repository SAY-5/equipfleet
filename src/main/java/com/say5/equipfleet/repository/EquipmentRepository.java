package com.say5.equipfleet.repository;

import com.say5.equipfleet.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {}
