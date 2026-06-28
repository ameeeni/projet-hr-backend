package tn.iteam.hrprojectbackend.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("john@test.com");
        user.setRole(Role.EMPLOYEE);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("john@test.com");

        assertNotNull(result);
        assertEquals("john@test.com", result.getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
    }

    @Test
    void loadUserByUsername_WithTeam() {
        tn.iteam.hrprojectbackend.users.entities.Team team =
                new tn.iteam.hrprojectbackend.users.entities.Team();
        team.setId(1L);

        User user = new User();
        user.setId(2L);
        user.setEmail("manager@test.com");
        user.setRole(Role.MANAGER);
        user.setTeam(team);

        when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("manager@test.com");

        assertNotNull(result);
        UserPrincipal principal = (UserPrincipal) result;
        assertEquals(1L, principal.getTeamId());
    }

    @Test
    void loadUserByUsername_NotFound_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("unknown@test.com"));
    }
}
