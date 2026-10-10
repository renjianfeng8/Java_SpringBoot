package com.example.springboot.service;

import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("zhangsan");
        mockUser.setPassword(encoder.encode("user123"));
        mockUser.setName("张三");
        mockUser.setRole("USER");
    }

    @Test
    void loginWithCorrectCredentialsShouldSucceed() {
        Account account = new Account();
        account.setUsername("zhangsan");
        account.setPassword("user123");

        when(userMapper.selectByUsername("zhangsan")).thenReturn(mockUser);

        User result = userService.login(account);

        assertNotNull(result);
        assertEquals("zhangsan", result.getUsername());
    }

    @Test
    void loginWithNonExistentUsernameShouldThrow() {
        Account account = new Account();
        account.setUsername("ghost");
        account.setPassword("pwd");
        when(userMapper.selectByUsername("ghost")).thenReturn(null);

        assertThrows(CustomException.class, () -> userService.login(account));
    }

    @Test
    void loginWithWrongPasswordShouldThrow() {
        Account account = new Account();
        account.setUsername("zhangsan");
        account.setPassword("wrong-pwd");
        when(userMapper.selectByUsername("zhangsan")).thenReturn(mockUser);

        assertThrows(CustomException.class, () -> userService.login(account));
    }

    @Test
    void registerWithNewUsernameShouldSucceed() {
        Account account = new Account();
        account.setUsername("newuser");
        account.setPassword("pwd123");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);

        userService.register(account);

        verify(userMapper).insert(any(User.class));
    }

    @Test
    void registerWithExistingUsernameShouldThrow() {
        Account account = new Account();
        account.setUsername("zhangsan");
        account.setPassword("pwd123");

        when(userMapper.selectByUsername("zhangsan")).thenReturn(mockUser);

        assertThrows(CustomException.class, () -> userService.register(account));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void updateShouldClearPasswordAndRole() {
        User user = new User();
        user.setId(1);
        user.setPassword("should-be-cleared");
        user.setRole("should-be-cleared");

        userService.update(user);

        assertNull(user.getPassword());
        assertNull(user.getRole());
        verify(userMapper).updateById(user);
    }
}
