package org.ssglobal.revalida.ssclassroom.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserModel {

    @Id
    @PrefixedIdService(prefix = "usr_")
    @Column(name = "user_id", length = 30)
    private String userId;

    @NotBlank
    @Size(max = 600)
    @Column(name = "first_name", nullable = false, length = 600)
    private String firstName;

    @NotBlank
    @Size(max = 600)
    @Column(name = "last_name", nullable = false, length = 600)
    private String lastName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;
}
