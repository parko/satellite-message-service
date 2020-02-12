package com.sokolmeteo.satellitemessageservice.repo;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Repository
public interface IridiumMessageRepository extends JpaRepository<IridiumMessage, Long> {
    List<IridiumMessage> findByErrorCounterAndSent(int error, boolean isSent);
}
