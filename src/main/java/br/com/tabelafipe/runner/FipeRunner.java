package br.com.tabelafipe.runner;

import br.com.tabelafipe.dto.Brand;
import br.com.tabelafipe.dto.Model;
import br.com.tabelafipe.dto.Vehicle;
import br.com.tabelafipe.dto.Year;
import br.com.tabelafipe.service.FipeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Scanner;

@Component
public class FipeRunner implements CommandLineRunner {

    private final FipeService service;

    public FipeRunner(FipeService service) {   // Spring injects the service here
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {

        Scanner sc = new Scanner(System.in);

        String vehicleType;
        String brandCode;
        String modelCode;
        String yearCode;

        // Select the vehicle type
        while (true){
            try {
                System.out.println("Enter the vehicle type: ");
                vehicleType = sc.nextLine().trim();
                break;
            }
            catch (HttpClientErrorException.NotFound e){
                System.out.println("Type not found. TRY AGAIN!");
            }
        }


        // Show the brands
        List<Brand> brands = service.findBrands(vehicleType);
        System.out.println(brands);


        // Select the brand and show the models
        while (true){
            System.out.println("Enter the brand code:: ");
            brandCode = sc.nextLine().trim();
            try {
                List<Model> models = service.findModels(vehicleType, brandCode);

                System.out.println(models);
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Brand not found. TRY AGAIN!");
            }
        }

        // Select the model and show the years
        while (true){
            System.out.println("Enter the model code: ");
            modelCode = sc.nextLine().trim();

            try {
                List<Year> years = service.findYears(vehicleType, brandCode, modelCode);

                System.out.println(years);
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Brand not found. TRY AGAIN!");
            }
        }

        // Select the year and show the vehicle
        while (true){
            System.out.println("Enter the year code: ");
            yearCode = sc.nextLine().trim();

            try {
                Vehicle vehicle = service.findVehicle(vehicleType, brandCode, modelCode, yearCode);

                System.out.println(vehicle);
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Brand not found. TRY AGAIN!");
            }
        }
    }
}