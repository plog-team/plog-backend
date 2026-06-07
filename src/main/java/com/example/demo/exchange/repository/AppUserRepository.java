package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}