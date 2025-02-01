package uk.gov.hmcts.reform.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.demo.entities.AccountRequest;
import uk.gov.hmcts.reform.demo.entities.User;

import java.util.Optional;

@Repository
public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {
    Optional<AccountRequest> findByUser(User user);
}
