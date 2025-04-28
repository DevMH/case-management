package com.devmh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .x509(x509 -> x509
                        .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                        .userDetailsService(username -> org.springframework.security.core.userdetails.User.withUsername(username)
                                .password("")
                                .roles("ADMIN")
                                .build())
                )
                //.httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public X509PrincipalExtractor x509PrincipalExtractor() {
        return new SubjectDnX509PrincipalExtractor();
    }

}
