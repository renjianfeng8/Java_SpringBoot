package com.example.springboot.service;

import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {

    @Mock
    private CinemaMapper cinemaMapper;

    @InjectMocks
    private CinemaService cinemaService;

    private Cinema mockCinema;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        mockCinema = new Cinema();
        mockCinema.setId(1);
        mockCinema.setUsername("asks");
        mockCinema.setPassword(encoder.encode("cinema123"));
        mockCinema.setName("测试影院");
        mockCinema.setRole("CINEMA");
        mockCinema.setStatus("已审核");
    }

    @Test
    void login_withCorrectCredentials_shouldSucceed() {
        Account account = new Account();
        account.setUsername("asks");
        account.setPassword("cinema123");

        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Cinema result = cinemaService.login(account);

        assertNotNull(result);
        assertEquals("asks", result.getUsername());
    }

    @Test
    void login_withNonExistentUsername_shouldThrow() {
        when(cinemaMapper.selectByUsername("ghost")).thenReturn(null);

        Account account = new Account();
        account.setUsername("ghost");
        account.setPassword("pwd");
        assertThrows(CustomException.class, () -> cinemaService.login(account));
    }

    @Test
    void login_withWrongPassword_shouldThrow() {
        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Account account2 = new Account();
        account2.setUsername("asks");
        account2.setPassword("wrong-pwd");
        assertThrows(CustomException.class, () -> cinemaService.login(account2));
    }

    @Test
    void register_shouldCreateCinema() {
        Account account = new Account();
        account.setUsername("newcinema");
        account.setPassword("pwd123");
        when(cinemaMapper.selectByUsername("newcinema")).thenReturn(null);

        cinemaService.register(account);

        verify(cinemaMapper).insert(any(Cinema.class));
    }

    @Test
    void register_withExistingUsername_shouldThrow() {
        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Account account3 = new Account();
        account3.setUsername("asks");
        account3.setPassword("pwd");
        assertThrows(CustomException.class, () -> cinemaService.register(account3));
        verify(cinemaMapper, never()).insert(any());
    }

    @Test
    void add_shouldSetDefaultValues() {
        Cinema cinema = new Cinema();
        cinema.setUsername("newcinema");
        cinema.setPassword("pwd123");

        when(cinemaMapper.selectByUsername("newcinema")).thenReturn(null);

        cinemaService.add(cinema);

        assertEquals("CINEMA", cinema.getRole());
        assertEquals("待审核", cinema.getStatus());
        assertNotNull(cinema.getPassword());
        assertNotEquals("pwd123", cinema.getPassword());
        verify(cinemaMapper).insert(cinema);
    }

    @Test
    void update_shouldClearPasswordAndRole() {
        Cinema cinema = new Cinema();
        cinema.setId(1);
        cinema.setPassword("should-be-cleared");
        cinema.setRole("should-be-cleared");

        cinemaService.update(cinema);

        assertNull(cinema.getPassword());
        assertNull(cinema.getRole());
        verify(cinemaMapper).updateById(cinema);
    }
}
