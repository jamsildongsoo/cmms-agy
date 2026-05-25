package com.cmms.repository;

import com.cmms.model.SequenceGenerator;
import com.cmms.model.SequenceGeneratorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SequenceGeneratorRepository extends JpaRepository<SequenceGenerator, SequenceGeneratorId> {
}
