package com.plog.api.domain.user;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean emailVerified = false;

    @Builder
    private User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public static User of(String name, String email, String password) {
        return User.builder().name(name).email(email).password(password).build();
    }
}