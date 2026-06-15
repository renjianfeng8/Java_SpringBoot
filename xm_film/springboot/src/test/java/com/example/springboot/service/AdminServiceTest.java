package com.example.springboot.service;

import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.AdminMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private AdminService adminService;

    private Admin mockAdmin;

    @BeforeEach
    void setUp() {
        mockAdmin = new Admin();
        mockAdmin.setId(1);
        mockAdmin.setUsername("999");
        mockAdmin.setPassword(new BCryptPasswordEncoder().encode("999"));
        mockAdmin.setName("管理员");
        mockAdmin.setRole("ADMIN");
    }

    @Test
    void login_withCorrectCredentials_shouldSucceed() {
        Account account = new Account();
        account.setUsername("999");
        account.setPassword("999");

        when(adminMapper.selectByUsername("999")).thenReturn(mockAdmin);

        Admin result = adminService.login(account);

        assertNotNull(result);
        assertEquals("999", result.getUsername());
        verify(adminMapper).selectByUsername("999");
    }

    @Test
    void login_withNonExistentUsername_shouldThrow() {
        Account account = new Account();
        account.setUsername("ghost");
        account.setPassword("pwd");

        when(adminMapper.selectByUsername("ghost")).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class, () -> adminService.login(account));
        assertEquals("401", ex.getCode());
        assertTrue(ex.getMsg().contains("不存在"));
    }

    @Test
    void login_withWrongPassword_shouldThrow() {
        Account account = new Account();
        account.setUsername("999");
        account.setPassword("wrong-password");

        when(adminMapper.selectByUsername("999")).thenReturn(mockAdmin);

        CustomException ex = assertThrows(CustomException.class, () -> adminService.login(account));
        assertEquals("401", ex.getCode());
        assertTrue(ex.getMsg().contains("错误"));
    }

    @Test
    void add_withNewUsername_shouldSucceed() {
        Admin newAdmin = new Admin();
        newAdmin.setUsername("newadmin");
        newAdmin.setPassword("pwd123");

        when(adminMapper.selectByUsername("newadmin")).thenReturn(null);

        adminService.add(newAdmin);

        assertEquals("ADMIN", newAdmin.getRole());
        assertNotNull(newAdmin.getPassword());
        assertNotEquals("pwd123", newAdmin.getPassword());
        verify(adminMapper).insert(newAdmin);
    }

    @Test
    void add_withExistingUsername_shouldThrow() {
        Admin newAdmin = new Admin();
        newAdmin.setUsername("999");
        newAdmin.setPassword("pwd123");

        when(adminMapper.selectByUsername("999")).thenReturn(mockAdmin);

        CustomException ex = assertThrows(CustomException.class, () -> adminService.add(newAdmin));
        assertTrue(ex.getMsg().contains("已存在"));
        verify(adminMapper, never()).insert(any());
    }

    @Test
    void update_shouldClearPasswordAndRole() {
        Admin update = new Admin();
        update.setId(1);
        update.setPassword("should-be-cleared");
        update.setRole("should-be-cleared");
        update.setName("新名字");

        adminService.update(update);

        assertNull(update.getPassword());
        assertNull(update.getRole());
        assertEquals("新名字", update.getName());
        verify(adminMapper).updateById(update);
    }

    @Test
    void updatePassword_withCorrectOldPassword_shouldSucceed() {
        Account account = new Account();
        account.setId(1);
        account.setPassword("999");
        account.setNewPassword("new-pass-456");

        when(adminMapper.selectById(1)).thenReturn(mockAdmin);

        adminService.updatePassword(account);

        verify(adminMapper).updatePassword(argThat(admin ->
                admin.getId().equals(1)
                        && admin.getPassword() != null
                        && !admin.getPassword().equals("new-pass-456")
        ));
        verify(adminMapper, never()).updateById(any(Admin.class));
    }

    @Test
    void updatePassword_withWrongOldPassword_shouldThrow() {
        Account account = new Account();
        account.setId(1);
        account.setPassword("wrong-old-password");
        account.setNewPassword("new-pass-456");

        when(adminMapper.selectById(1)).thenReturn(mockAdmin);

        CustomException ex = assertThrows(CustomException.class, () -> adminService.updatePassword(account));
        assertTrue(ex.getMsg().contains("原密码错误"));
    }

    @Test
    void updatePassword_withNonExistentUser_shouldThrow() {
        Account account = new Account();
        account.setId(999);
        account.setPassword("pwd");

        when(adminMapper.selectById(999)).thenReturn(null);

        assertThrows(CustomException.class, () -> adminService.updatePassword(account));
    }
}
