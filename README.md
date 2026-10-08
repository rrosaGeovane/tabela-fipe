# 🚗 FIPE Table API Consumer

An interactive Spring Boot command-line application that consumes the public **FIPE Table API** to look up the average market price of vehicles in Brazil. The user navigates step by step (brand → model → year) and the vehicle details are displayed directly in the terminal.

This project was built as a hands-on study of **how to consume an external REST API with Spring**, instead of building one. Most tutorials teach how to *create* APIs; this one focuses on the other side of the conversation: being the **client**.

---

## 📌 About the FIPE Table

The FIPE Table (*Tabela FIPE*) is the reference for average vehicle prices in the Brazilian market. It is widely used for buying and selling cars, insurance, and financing. The data is updated monthly.

This project uses the community-maintained API available at [fipe.api.br](https://fipe.api.br), which exposes the FIPE data as JSON over REST.

---

## ✨ Features

- Lists all car brands available in the FIPE Table
- Interactive navigation through brand, model, and year using terminal input
- Validates each choice: invalid codes show a friendly message and ask again, instead of crashing
- Displays the full vehicle details: model, year, fuel, FIPE code, price, and reference month
- Layered architecture separating API access (service) from user interaction (runner)

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 17+** | Language |
| **Spring Boot 3.2+** | Application framework |
| **Spring Web** | Provides `RestClient` (HTTP calls) and Jackson (JSON mapping) |
| **RestClient** | Sends HTTP requests to the external API |
| **Jackson** | Converts JSON responses into Java objects |
| **Java Records** | Immutable data models (DTOs) |
| **Scanner** | Reads user input from the terminal |
| **Maven** | Build and dependency management |

---

## 🖥️ Example Session

```
1 - Acura
2 - Agrale
3 - Alfa Romeo
...

Enter the brand code: 999
Brand not found. TRY AGAIN!

Enter the brand code: 4
21 - Hummer Hard-Top 6.5 4x4 Diesel TB
22 - Hummer Open-Top 6.5 4x4 Diesel TB
23 - Hummer Wagon 6.5 4x4 Diesel TB

Enter the model code: 21
...  (list of available years)

Enter the year code: ...

===== RESULT =====
Vehicle:      <brand> - <model>
Year:         <model year>
Fuel:         <fuel>
FIPE code:    <FIPE code>
Price:        R$ <price>
Reference:    <reference month>
```

> The application itself prints its messages in Portuguese.

---

## ⚙️ How It Works

The FIPE API works like a **funnel**: each request narrows down the search, and each answer provides the codes needed for the next request.

```
          ┌──────────────────────────────┐
          │  1. List brands              │  GET /cars/brands
          └──────────────┬───────────────┘
                         │ user types a brand code
          ┌──────────────▼───────────────┐
          │  2. List models              │  GET /cars/brands/{brand}/models
          └──────────────┬───────────────┘
                         │ user types a model code
          ┌──────────────▼───────────────┐
          │  3. List years               │  GET /cars/brands/{brand}/models/{model}/years
          └──────────────┬───────────────┘
                         │ user types a year code
          ┌──────────────▼───────────────┐
          │  4. Vehicle details + price  │  GET /cars/brands/{brand}/models/{model}/years/{year}
          └──────────────────────────────┘
```

Behind each step, the request follows the same path:

```
FipeRunner ──► FipeService ──► RestClient ──► FIPE API
    ▲                                            │
    │                                            ▼
    └──── Java object ◄── Jackson ◄──── JSON response
```

### 🏪 Mental model

To understand each component, I used a store analogy:

| In the analogy | In the code |
|---|---|
| Supplier with the stock | FIPE API and its database |
| Shipping company | `FipeService` + `RestClient` |
| Employee who unpacks the boxes | Jackson |
| Shelf format | Records (`Marca`, `Modelo`, `Ano`, `Veiculo`) |
| Store clerk who talks to the customer | `FipeRunner` |
| Opening routine of the store | `CommandLineRunner` |
| Product label | `toString()` in `Veiculo` |
| Store door closed to the public | No embedded web server (`web-application-type=none`) |
| Rules for writing the orders | REST (HTTP methods + URLs) |

---

## 📂 Project Structure

```
src/main/java/br/com/tabelafipe/
├── TabelaFipeApplication.java   # Entry point (@SpringBootApplication)
├── runner/
│   └── FipeRunner.java          # Talks to the user and coordinates the flow
├── service/
│   └── FipeService.java         # Makes the HTTP requests to the FIPE API
└── dto/
    ├── Marca.java               # Brand  (code, name)
    ├── Modelo.java              # Model  (code, name)
    ├── Ano.java                 # Year   (code, name)
    └── Veiculo.java             # Vehicle details + custom toString()

src/main/resources/
└── application.properties       # Disables the web server
```

Each layer has a single responsibility:

| Layer | Responsibility | Knows about |
|---|---|---|
| `runner` | User interaction (input, output, retry loops) | The service, never the URLs |
| `service` | Communication with the external API | URLs and HTTP, never the terminal |
| `dto` | Data shape | Only its own fields |

> `TabelaFipeApplication` stays in the root package so Spring can find the `@Component` and `@Service` classes in the subpackages.

---

## 🔍 Code Walkthrough

### 1. Running as a terminal application

Spring Web includes an embedded Tomcat server. Since this project only makes requests (it does not receive them), the server is disabled:

```properties
spring.main.web-application-type=none
```

### 2. Data models (records)

Brands, models, and years share the same JSON format, so each one is a simple record:

```java
public record Marca(String code, String name) {}
public record Modelo(String code, String name) {}
public record Ano(String code, String name) {}
```

The vehicle details are a single object. The field names match the JSON keys so Jackson can map them automatically:

```java
public record Veiculo(int vehicleType,
                      String price,
                      String brand,
                      String model,
                      int modelYear,
                      String fuel,
                      String codeFipe,
                      String referenceMonth,
                      String fuelAcronym) {

    @Override
    public String toString() {
        return """

                ===== RESULTADO =====
                Veículo:      %s - %s
                Ano:          %d
                Combustível:  %s
                Código FIPE:  %s
                Preço:        %s
                Referência:   %s
                """.formatted(brand, model, modelYear, fuel, codeFipe, price, referenceMonth);
    }
}
```

> `price` is a `String`, not a `double`, because the API returns it as formatted text (`"R$ 10.000,00"`).
> Fields present in the JSON but missing from the record (such as `priceHistory`) are simply ignored.
> The custom `toString()` uses a **text block** (`"""`) and `formatted()` to print a clean result with a single `println(veiculo)`.

### 3. The service: talking to the API

```java
@Service
public class FipeService {

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    public List<Marca> buscarMarcas() {
        return client.get()
                .uri("/cars/brands")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Marca>>() {});
    }

    public List<Ano> buscarAnos(String codigoMarca, String codigoModelo) {
        return client.get()
                .uri("/cars/brands/{marca}/models/{modelo}/years",
                        codigoMarca, codigoModelo)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Ano>>() {});
    }

    public Veiculo buscarVeiculo(String codigoMarca, String codigoModelo, String codigoAno) {
        return client.get()
                .uri("/cars/brands/{marca}/models/{modelo}/years/{ano}",
                        codigoMarca, codigoModelo, codigoAno)
                .retrieve()
                .body(Veiculo.class);
    }

    // buscarModelos(...) follows the same pattern
}
```

| Step | What it does |
|---|---|
| `RestClient.create(...)` | Static factory method that builds the client with the base URL |
| `.get()` | Defines the HTTP method (fetch data) |
| `.uri(path, values...)` | Appends the path to the base URL and fills the `{placeholders}` **in order** |
| `.retrieve()` | Sends the request, receives the response, and throws an exception on error status |
| `.body(Veiculo.class)` | Single JSON object `{ }` → one Java object |
| `.body(new ParameterizedTypeReference<List<...>>() {})` | JSON array `[ ]` → `List`, working around Java's **type erasure** |

Each method receives exactly the codes its URL needs, and the service never touches the terminal.

### 4. The runner: talking to the user

The runner receives the service through **constructor injection** and implements `CommandLineRunner`, so Spring calls `run()` as soon as the application starts:

```java
@Component
public class FipeRunner implements CommandLineRunner {

    private final FipeService service;
    private final Scanner sc = new Scanner(System.in);

    public FipeRunner(FipeService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        String codigoMarca;   // declared outside the loops
        String codigoModelo;  // so they survive until
        String codigoAno;     // the last step

        service.buscarMarcas()
               .forEach(m -> System.out.println(m.code() + " - " + m.name()));

        while (true) {
            System.out.print("\nDigite o código da marca: ");
            codigoMarca = sc.nextLine().trim();

            try {
                service.buscarModelos(codigoMarca)
                       .forEach(m -> System.out.println(m.code() + " - " + m.name()));
                break;                                   // success: next step
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("Não existe essa marca. TENTE NOVAMENTE!");
            }
        }

        // The same pattern repeats for model → years and year → vehicle,
        // ending with: System.out.println(veiculo);
    }
}
```

**Retry pattern:** `while (true)` + `try/catch` + `break`. The loop only ends when the request succeeds; a `404 Not Found` shows a message and asks again.

---

## 🌐 API Reference

Base URL: `https://fipe.parallelum.com.br/api/v2`

| Step | Endpoint | Returns |
|---|---|---|
| 1 | `/{vehicleType}/brands` | List of brands |
| 2 | `/{vehicleType}/brands/{brandId}/models` | List of models |
| 3 | `/{vehicleType}/brands/{brandId}/models/{modelId}/years` | List of years |
| 4 | `/{vehicleType}/brands/{brandId}/models/{modelId}/years/{yearId}` | Vehicle details and price |

`vehicleType` can be `cars`, `motorcycles`, or `trucks`.

**Rate limit:** 500 requests per day without authentication, or 1,000 per day with a free token sent in the `X-Subscription-Token` header.

---

## ▶️ How to Run

**Requirements:** Java 17+ and Maven (or use the included Maven Wrapper).

```bash
# Clone the repository
git clone https://github.com/<your-username>/tabela-fipe.git
cd tabela-fipe

# Run
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

You can also run `TabelaFipeApplication` directly from your IDE and interact through its console.

---

## 📚 What I Learned

**Concepts**
- **API vs. database:** an API is the communication layer; the data lives in a database behind it. As a client, I never access the database directly.
- **Being a client, not a server:** how to make an application *consume* an API instead of *exposing* one.
- **REST:** HTTP methods (`GET`, `POST`, `PUT`, `DELETE`) and URLs as the rules for writing requests.
- **HTTP status codes:** `200` success, `404` not found, `429` too many requests, `500` server error.

**Spring**
- **Spring Boot starters:** what `spring-boot-starter-web` brings (Spring MVC, Tomcat, Jackson, RestClient) and which parts this project uses.
- **`CommandLineRunner`:** runs code once at startup, similar to a `main` method, with Spring managing object creation.
- **Dependency injection:** the runner receives the service through its constructor; Spring creates and connects both.
- **Layered architecture:** separating API access (`@Service`) from user interaction, and keeping the main class in the root package for component scanning.
- **`RestClient`:** building requests with method chaining (`get → uri → retrieve → body`) and URI placeholders filled in order.

**Java**
- **Static factory methods:** why `RestClient.create()` is used instead of `new` (`RestClient` is an interface).
- **Jackson and deserialization:** JSON keys are mapped to record fields by name, and types must match (a formatted price is a `String`).
- **Single object vs. list:** `{ }` maps to `.body(MyClass.class)`, while `[ ]` requires `ParameterizedTypeReference` because of **type erasure**.
- **Exception handling:** `throws` passes the error up; `try/catch` handles it. Nested classes appear with `$` in stack traces but are written with `.` in code.
- **Variable scope:** variables declared inside a loop do not exist outside it.
- **Overriding `toString()`** in a record, using text blocks and `formatted()`.

**Debugging**
- Adding "flashlights" (`println` checkpoints) to find where the flow breaks.
- Testing endpoints in the browser before writing code.

**Tools**
- Git and GitHub: initializing a repository, committing, and pushing from the terminal and from IntelliJ.

---

## 🚀 Next Steps

- [x] Make it interactive: let the user choose brand, model, and year in the terminal
- [x] Handle invalid codes with `try/catch` and retry
- [x] Split the project into layers (`dto`, `service`, `runner`)
- [ ] Remove the repeated retry loops with a reusable method
- [ ] Handle more errors: `400` (invalid input), `429` (rate limit), and connection failures
- [ ] Support motorcycles and trucks
- [ ] Move the base URL to `application.properties`
- [ ] Send the access token through the `X-Subscription-Token` header
- [ ] Display the price history
- [ ] Add automated tests
- [ ] Expose the data through my own REST endpoints (`@RestController`), so the project both **consumes and provides** an API

---

## 👤 Author

**Geovane Ribeiro Rosa**
Information Systems student at Uniube (Universidade de Uberaba)

---

## 📄 Credits

Vehicle data provided by [FIPE API](https://fipe.api.br), a free community API by Deivid Fortuna.
