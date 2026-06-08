package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}