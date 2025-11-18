package com.borsibaar.service;

import com.borsibaar.dto.UserDTO;
import com.borsibaar.entity.Role;
import com.borsibaar.entity.User;
import com.borsibaar.mapper.UserMapper;
import com.borsibaar.repository.RoleRepository;
import com.borsibaar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AuthServiceTest {

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private RoleRepository roleRepository;

    @Autowired
    private AuthService authService;

    @Test
    void processOAuthLogin_NewUser_AssignsDefaultRoleAndGeneratesToken() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "new@test.com", "name", "New User"),
                "email");
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        Role defaultRole = Role.builder().id(10L).name("USER").build();
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));
        when(jwtService.generateToken("new@test.com")).thenReturn("jwt-token");
        when(userMapper.toDto(any(User.class), eq("jwt-token"))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new UserDTO(u.getEmail(), u.getName(), u.getRole().getName(), "jwt-token");
        });

        AuthService.AuthResult result = authService.processOAuthLogin(token);

        assertNotNull(result);
        assertEquals("new@test.com", result.dto().email());
        assertEquals("USER", result.dto().role());
        assertEquals("jwt-token", result.dto().token());
        assertTrue(result.needsOnboarding());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("new@test.com", captor.getValue().getEmail());
        assertNotNull(captor.getValue().getRole());
    }

    @Test
    void processOAuthLogin_ExistingUser_UpdatesNameAndReturnsToken() {
        User existing = User.builder().email("exist@test.com").name("Old Name").role(Role.builder().id(1L).name("USER").build()).build();
        when(userRepository.findByEmail("exist@test.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken("exist@test.com")).thenReturn("jwt-token");
        when(userMapper.toDto(any(User.class), eq("jwt-token"))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new UserDTO(u.getEmail(), u.getName(), u.getRole().getName(), "jwt-token");
        });

        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "exist@test.com", "name", "Updated Name"),
                "email");
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        AuthService.AuthResult result = authService.processOAuthLogin(token);

        assertEquals("exist@test.com", result.dto().email());
        assertEquals("Updated Name", result.dto().name());
        verify(userRepository).save(existing);
    }

    @Test
    void processOAuthLogin_MissingDefaultRole_ThrowsIllegalArgument() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "fail@test.com", "name", "Fail User"),
                "email");
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        when(userRepository.findByEmail("fail@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.processOAuthLogin(token));
    }
}
