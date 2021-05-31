package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.ReportStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IReportStatRepository extends JpaRepository<ReportStatEntity, Long> {
}
