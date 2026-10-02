package br.com.tabelafipe;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;

@Component
public class FipeRunner implements CommandLineRunner {

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    @Override
    public void run(String... args) throws Exception {

        Veiculo veiculo = client
                .get()
                .uri("/cars/brands/25/models/7693/years/2020-5")
                .retrieve()
                .body(Veiculo.class);


        System.out.println(veiculo.brand() + " - " + veiculo.model());
        System.out.println("Ano: " + veiculo.modelYear());
        System.out.println("Preço: " + veiculo.price());

    }
}
