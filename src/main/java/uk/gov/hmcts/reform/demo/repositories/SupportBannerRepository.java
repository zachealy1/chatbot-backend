package uk.gov.hmcts.reform.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.demo.entities.SupportBanner;

public interface SupportBannerRepository extends JpaRepository<SupportBanner, Long> {

}

