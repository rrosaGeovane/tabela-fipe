package br.com.tabelafipe;

public record Veiculo(int vehicleType,
                      String price,
                      String brand,
                      String model,
                      int modelYear,
                      String fuel,
                      String codeFipe,
                      String referenceMonth,
                      String fuelAcronym) {
}
