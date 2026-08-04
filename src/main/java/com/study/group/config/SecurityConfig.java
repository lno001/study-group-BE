package com.study.group.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.study.group.auth.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
            	    // 회원가입, 로그인만 허용
            		.requestMatchers(
            		        "/api/auth/signup",
            		        "/api/auth/login",
            		        "/api/auth/refresh"
            		).permitAll()

            	    // 스웨거, 액추에이터 허용
            	    .requestMatchers(
            	            "/swagger-ui/**",
            	            "/swagger-ui.html",
            	            "/api-docs/**",
            	            "/v3/api-docs/**",
            	            "/actuator/**"
            	    ).permitAll()

            	    .requestMatchers(HttpMethod.GET, "/api/groups", "/api/groups/**").permitAll()
            	    // 나머지 (logout 포함)는 인증 필요
            	    .anyRequest().authenticated()
            	)
            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}