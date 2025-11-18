package io.github.jhipster.sample.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jhipster.sample.IntegrationTest;
import io.github.jhipster.sample.domain.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for DTO validation.
 */
@IntegrationTest
class DTOValidationTest {

    @Autowired
    private Validator validator;

    private AdminUserDTO adminUserDTO;
    private UserDTO userDTO;
    private PasswordChangeDTO passwordChangeDTO;

    @BeforeEach
    void setUp() {
        adminUserDTO = new AdminUserDTO();
        adminUserDTO.setLogin("testuser");
        adminUserDTO.setEmail("test@example.com");
        adminUserDTO.setFirstName("Test");
        adminUserDTO.setLastName("User");
        adminUserDTO.setActivated(true);
        adminUserDTO.setLangKey("en");

        userDTO = new UserDTO();
        userDTO.setLogin("testuser");

        passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("currentPassword");
        passwordChangeDTO.setNewPassword("newPassword");
    }

    @Test
    void testValidAdminUserDTO() {
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
        assertThat(violations).isEmpty();
    }

    @Test
    void testAdminUserDTOWithInvalidEmail() {
        adminUserDTO.setEmail("invalid-email");
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testAdminUserDTOWithNullEmail() {
        adminUserDTO.setEmail(null);
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
        // Email is not @NotNull, so null is allowed
        assertThat(violations).isEmpty();
    }

    @Test
    void testAdminUserDTOWithNullLogin() {
        adminUserDTO.setLogin(null);
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testAdminUserDTOWithInvalidLogin() {
        adminUserDTO.setLogin("invalid login with spaces");
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testAdminUserDTOFromUser() {
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActivated(true);
        user.setLangKey("en");

        AdminUserDTO dto = new AdminUserDTO(user);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getLogin()).isEqualTo("testuser");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getFirstName()).isEqualTo("Test");
        assertThat(dto.getLastName()).isEqualTo("User");
        assertThat(dto.isActivated()).isTrue();
        assertThat(dto.getLangKey()).isEqualTo("en");
    }

    @Test
    void testValidUserDTO() {
        Set<ConstraintViolation<UserDTO>> violations = validator.validate(userDTO);
        assertThat(violations).isEmpty();
    }

    @Test
    void testUserDTOFromUser() {
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");

        UserDTO dto = new UserDTO(user);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getLogin()).isEqualTo("testuser");
    }

    @Test
    void testUserDTOEquality() {
        UserDTO dto1 = new UserDTO();
        dto1.setId(1L);
        dto1.setLogin("testuser");

        UserDTO dto2 = new UserDTO();
        dto2.setId(1L);
        dto2.setLogin("testuser");

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void testUserDTOToString() {
        userDTO.setId(1L);
        String toString = userDTO.toString();
        assertThat(toString).contains("testuser");
        assertThat(toString).contains("1");
    }

    @Test
    void testValidPasswordChangeDTO() {
        Set<ConstraintViolation<PasswordChangeDTO>> violations = validator.validate(passwordChangeDTO);
        assertThat(violations).isEmpty();
    }

    @Test
    void testPasswordChangeDTOWithNullCurrentPassword() {
        passwordChangeDTO.setCurrentPassword(null);
        Set<ConstraintViolation<PasswordChangeDTO>> violations = validator.validate(passwordChangeDTO);
        // PasswordChangeDTO doesn't have validation annotations, so null is allowed
        assertThat(violations).isEmpty();
    }

    @Test
    void testPasswordChangeDTOWithNullNewPassword() {
        passwordChangeDTO.setNewPassword(null);
        Set<ConstraintViolation<PasswordChangeDTO>> violations = validator.validate(passwordChangeDTO);
        // PasswordChangeDTO doesn't have validation annotations, so null is allowed
        assertThat(violations).isEmpty();
    }

    @Test
    void testPasswordChangeDTOConstructor() {
        PasswordChangeDTO dto = new PasswordChangeDTO("current", "new");
        assertThat(dto.getCurrentPassword()).isEqualTo("current");
        assertThat(dto.getNewPassword()).isEqualTo("new");
    }
}
