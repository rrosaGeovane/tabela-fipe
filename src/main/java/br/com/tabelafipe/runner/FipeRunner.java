package br.com.tabelafipe.runner;

import br.com.tabelafipe.dto.Ano;
import br.com.tabelafipe.dto.Marca;
import br.com.tabelafipe.dto.Modelo;
import br.com.tabelafipe.dto.Veiculo;
import br.com.tabelafipe.service.FipeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Scanner;

@Component
public class FipeRunner implements CommandLineRunner {

    private final FipeService service;

    public FipeRunner(FipeService service) {   // o Spring entrega o service aqui
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {

        Scanner sc = new Scanner(System.in);

        String codigoMarca;
        String codigoModelo;
        String codigoAno;

        //Mostra as Marcas
        List<Marca> marcas = service.buscarMarcas();
        System.out.println(marcas);


        //Seleciona a marca e mostra os modelos
        while (true){
            System.out.println("Digite o código da Marca: ");
            codigoMarca = sc.nextLine().trim();
            try {
                List<Modelo> modelos = service.buscarModelos(codigoMarca);

                System.out.println(modelos);
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Não existe essa Marca. TENTE NOVAMENTE!");
            }
        }

        //Seleciona o modelo e mostra os anos
        while (true){
            System.out.println("Digite o modelo: ");
            codigoModelo = sc.nextLine().trim();

            try {
                List<Ano> anos = service.buscarAno(codigoMarca, codigoModelo);

                System.out.println(anos);
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Não existe esse modelo. TENTE NOVAMENTE!");
            }
        }

        //seleciona o ano e mostra o veículo
        while (true){
            System.out.println("Digite o ano: ");
            codigoAno = sc.nextLine().trim();

            try {
                Veiculo veiculo = service.buscarVeiculo(codigoMarca, codigoModelo, codigoAno);

                System.out.println(veiculo);
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Não existe esse ano. TENTE NOVAMENTE!");
            }
        }
    }
}
