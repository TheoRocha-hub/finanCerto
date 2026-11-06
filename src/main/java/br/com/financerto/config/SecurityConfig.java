package br.com.financerto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtro(HttpSecurity http) throws Exception {
        http
            // CSRF desligado para simplificar este trabalho acadêmico. Em produção real o ideal é manter
            // ligado (a sessão é por cookie), enviando o token CSRF nos formulários e nas chamadas fetch.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/css/**", "/js/**", "/error",
                                 "/api/auth/registrar", "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login.html?erro"))
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login.html"))
            // chamadas /api sem login recebem 401 (e não um redirect para a página de login)
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                req -> req.getRequestURI().startsWith("/api/")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
