package com.plog.api.domain.user;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserSeeder {

    private final UserRepository userRepository;


    @Bean
    public ApplicationRunner seedDefaultUser() {
        return args -> {
            if (userRepository.count() == 0) {
                User u = userRepository.save(User.of("플로그 테스터", "tester@plog.local", "test1234"));
                log.info("Seeded default user id={}", u.getId());
            }
        };
    }
}
