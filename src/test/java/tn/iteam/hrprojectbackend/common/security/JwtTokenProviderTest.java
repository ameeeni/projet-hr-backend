package tn.iteam.hrprojectbackend.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", EXPIRATION);
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private UserDetails buildUserDetails(String username, String... roles) {
        return User.withUsername(username)
                .password("")
                .authorities(roles)
                .build();
    }

    // ──────────── generateToken ────────────

    @Test
    void generateToken_ReturnsNonNullToken() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");

        String token = jwtTokenProvider.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_ContainsCorrectSubject() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");

        String token = jwtTokenProvider.generateToken(userDetails);
        String username = jwtTokenProvider.extractUsername(token);

        assertEquals("john@test.com", username);
    }

    @Test
    void generateToken_ContainsAuthorities() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");

        String token = jwtTokenProvider.generateToken(userDetails);
        List<String> authorities = jwtTokenProvider.extractAuthorities(token);

        assertTrue(authorities.contains("ROLE_EMPLOYEE"));
    }

    // ──────────── extractUsername ────────────

    @Test
    void extractUsername_ReturnsCorrectUsername() {
        UserDetails userDetails = buildUserDetails("manager@test.com", "ROLE_MANAGER");
        String token = jwtTokenProvider.generateToken(userDetails);

        String username = jwtTokenProvider.extractUsername(token);

        assertEquals("manager@test.com", username);
    }

    // ──────────── isTokenValid(token, userDetails) ────────────

    @Test
    void isTokenValid_WithUserDetails_ValidToken_ReturnsTrue() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");
        String token = jwtTokenProvider.generateToken(userDetails);

        boolean result = jwtTokenProvider.isTokenValid(token, userDetails);

        assertTrue(result);
    }

    @Test
    void isTokenValid_WithUserDetails_WrongUser_ReturnsFalse() {
        UserDetails user1 = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");
        UserDetails user2 = buildUserDetails("jane@test.com", "ROLE_EMPLOYEE");
        String token = jwtTokenProvider.generateToken(user1);

        boolean result = jwtTokenProvider.isTokenValid(token, user2);

        assertFalse(result);
    }

    @Test
    void isTokenValid_WithUserDetails_InvalidToken_ReturnsFalse() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");

        boolean result = jwtTokenProvider.isTokenValid("invalid.token.here", userDetails);

        assertFalse(result);
    }

    @Test
    void isTokenValid_WithUserDetails_ExpiredToken_ReturnsFalse() {
        // Build an expired token manually
        String expiredToken = Jwts.builder()
                .subject("john@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(getKey())
                .compact();

        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");
        boolean result = jwtTokenProvider.isTokenValid(expiredToken, userDetails);

        assertFalse(result);
    }

    // ──────────── isTokenValid(token) ────────────

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");
        String token = jwtTokenProvider.generateToken(userDetails);

        boolean result = jwtTokenProvider.isTokenValid(token);

        assertTrue(result);
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        boolean result = jwtTokenProvider.isTokenValid("bad.token.string");

        assertFalse(result);
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        String expiredToken = Jwts.builder()
                .subject("john@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(getKey())
                .compact();

        boolean result = jwtTokenProvider.isTokenValid(expiredToken);

        assertFalse(result);
    }

    @Test
    void isTokenValid_NullToken_ReturnsFalse() {
        // Should handle gracefully (null → IllegalArgumentException → false)
        boolean result = jwtTokenProvider.isTokenValid((String) null);

        assertFalse(result);
    }

    // ──────────── extractAuthorities ────────────

    @Test
    void extractAuthorities_FromAuthoritiesField_ReturnsList() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE", "ROLE_MANAGER");
        String token = jwtTokenProvider.generateToken(userDetails);

        List<String> result = jwtTokenProvider.extractAuthorities(token);

        assertTrue(result.contains("ROLE_EMPLOYEE") || result.contains("ROLE_MANAGER"));
    }

    @Test
    void extractAuthorities_FromRolesField_ReturnsList() {
        // Create token with "roles" claim instead of "authorities"
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of("ROLE_HR"));
        String token = Jwts.builder()
                .claims(claims)
                .subject("hr@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractAuthorities(token);

        assertTrue(result.contains("ROLE_HR"));
    }

    @Test
    void extractAuthorities_FromRoleField_ReturnsList() {
        // Create token with "role" claim
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", List.of("ROLE_MANAGER"));
        String token = Jwts.builder()
                .claims(claims)
                .subject("mgr@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractAuthorities(token);

        assertTrue(result.contains("ROLE_MANAGER"));
    }

    @Test
    void extractAuthorities_RoleAsString_SplitsByComma() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", "ROLE_HR,ROLE_ADMIN");
        String token = Jwts.builder()
                .claims(claims)
                .subject("hr@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractAuthorities(token);

        assertTrue(result.contains("ROLE_HR") || result.contains("ROLE_ADMIN"));
    }

    @Test
    void extractAuthorities_NoClaim_ReturnsEmptyList() {
        Map<String, Object> claims = new HashMap<>();
        // No authorities, roles, or role claims
        String token = Jwts.builder()
                .claims(claims)
                .subject("user@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractAuthorities(token);

        assertTrue(result.isEmpty());
    }

    // ──────────── extractPermissions ────────────

    @Test
    void extractPermissions_WithPermissionsList_ReturnsList() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", List.of("HR_READ", "HR_WRITE"));
        String token = Jwts.builder()
                .claims(claims)
                .subject("hr@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractPermissions(token);

        assertTrue(result.contains("HR_READ"));
        assertTrue(result.contains("HR_WRITE"));
    }

    @Test
    void extractPermissions_AsString_SplitsByComma() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", "HR_READ,HR_WRITE");
        String token = Jwts.builder()
                .claims(claims)
                .subject("hr@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractPermissions(token);

        assertTrue(result.size() >= 1);
    }

    @Test
    void extractPermissions_NoPermissions_ReturnsEmptyList() {
        Map<String, Object> claims = new HashMap<>();
        String token = Jwts.builder()
                .claims(claims)
                .subject("user@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        List<String> result = jwtTokenProvider.extractPermissions(token);

        assertTrue(result.isEmpty());
    }

    // ──────────── extractAllClaimsPublic ────────────

    @Test
    void extractAllClaimsPublic_ReturnsAllClaims() {
        UserDetails userDetails = buildUserDetails("john@test.com", "ROLE_EMPLOYEE");
        String token = jwtTokenProvider.generateToken(userDetails);

        var claims = jwtTokenProvider.extractAllClaimsPublic(token);

        assertNotNull(claims);
        assertEquals("john@test.com", claims.getSubject());
    }
}
