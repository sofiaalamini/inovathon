package br.com.panocerto.seguranca;

import br.com.panocerto.config.Configuracao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, FiltroJwt filtroJwt) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)   // API sem cookie de sessão: o token vai no cabeçalho
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/banca/**").permitAll()   // visitantes; só responde no modo banca
                        .requestMatchers("/webhook/**").permitAll()   // a Meta chama aqui; a assinatura é conferida no controle
                        .requestMatchers("/midias/**").permitAll()    // fotos e áudios (nomes aleatórios, usados em <img>)
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().permitAll())   // telas e arquivos do painel (públicos); os dados ficam em /api
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Libera o painel (Vite em localhost:5173) a chamar a API durante o desenvolvimento. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(Configuracao config) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(Arrays.stream(config.origensPermitidas().split(",")).map(String::trim).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));   // origens aceitam curinga (link do túnel)
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cors);
        return fonte;
    }
}
