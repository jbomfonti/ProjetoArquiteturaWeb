package com.sistema.gestao.sistemagestao;

import org.junit.jupiter.api.Test;
<<<<<<< HEAD
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SistemagestaoApplicationTests {

	@Test
	void contextLoads() {
	}

=======
import org.junit.jupiter.api.Disabled;

class SistemagestaoApplicationTests {
    @Test
    @Disabled("Requer banco PostgreSQL e Kafka rodando")
    void contextLoads() { }
>>>>>>> 01b8b43 (Testes Imoveis)
}
