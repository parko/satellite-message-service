package com.sokolmeteo.satellitemessageservice.repo;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IridiumMessageRepository extends JpaRepository<IridiumMessage, Long> {
}
