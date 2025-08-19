package com.foh.contacto_total_web_service.repository;

import com.foh.contacto_total_web_service.model.FtpConsult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface FtpConsultRepository extends JpaRepository<FtpConsult, Integer> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE ftp_consult", nativeQuery = true)
    void truncateTable();
}
