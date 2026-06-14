package tn.iteam.hrprojectbackend.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

/**
 * Charge un UserPrincipal depuis la base HR en recherchant par email.
 * Le "username" dans le token IAM correspond à l'email de l'employé.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));
        return UserPrincipal.from(user);
    }
}
