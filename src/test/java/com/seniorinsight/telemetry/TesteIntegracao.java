package com.seniorinsight.telemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Contexto completo com PostgreSQL e RabbitMQ do Testcontainers. Todos os testes de integração usam a mesma
 * configuração, então o Spring reaproveita um único contexto (e um único par de containers).
 *
 * <p>O issuer do Keycloak é fictício: o decoder só é criado na primeira validação de token, e os testes autenticam
 * com {@code jwt()} do spring-security-test. Os {@link TratadoresTeste} só reagem a eventos {@code teste.*}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "KEYCLOAK_ISSUER_URI=http://keycloak.invalid/realms/seniorinsight")
@AutoConfigureMockMvc
@Import({ContainersConfig.class, TratadoresTeste.class})
public @interface TesteIntegracao {
}
