# 🚗 FIPE Table API Consumer

An interactive Spring Boot command-line application that consumes the public **FIPE Table API** to look up the average market price of **cars, motorcycles, and trucks** in Brazil. The user navigates step by step (vehicle type → brand → model → year) and the vehicle details are displayed directly in the terminal.

This project was built as a hands-on study of **how to consume an external REST API with Spring**, instead of building one. Most tutorials teach how to *create* APIs; this one focuses on the other side of the conversation: being the **client**.

---

## 📌 About the FIPE Table

The FIPE Table (*Tabela FIPE*) is the reference for average vehicle prices in the Brazilian market. It is widely used for buying and selling vehicles, insurance, and financing. The data is updated monthly.

This project uses the community-maintained API available at [fipe.api.br](https://fipe.api.br), which exposes the FIPE data as JSON over REST.

---

## ✨ Features

- Supports **cars, motorcycles, and trucks** with a single service, using the vehicle type as a URL parameter
- Interactive navigation through vehicle type, brand, model, and year using terminal input
- Validates brand, model, and year codes: invalid codes show a message and ask again, instead of crashing
- Displays the full vehicle details: type, price, brand, model, year, fuel, FIPE code, and reference month
- Custom `toString()` in every record for readable terminal output
- Layered architecture separating API access (service) from user interaction (runner)

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 17** | Language |
| **Spring Boot 4.1** | Application framework |
| **Spring Web MVC** (`spring-boot-starter-webmvc`) | Provides `RestClient` (HTTP calls) and Jackson (JSON mapping) |
| **RestClient** | Sends HTTP requests to the external API |
| **Jackson** | Converts JSON responses into Java objects |
| **Java Records** | Immutable data models (DTOs) |
| **Scanner** | Reads user input from the terminal |
| **Maven** | Build and dependency management |

---

## 🖥️ Example Session

```
Enter the vehicle type:
cars

Acura: 1
Agrale: 2
Alfa Romeo: 3
...

Enter the brand code:
999
Brand not found. TRY AGAIN!

Enter the brand code:
4
Hummer Hard-Top 6.5 4x4 Diesel TB: 21
Hummer Open-Top 6.5 4x4 Diesel TB: 22
Hummer Wagon 6.5 4x4 Diesel TB: 23

Enter the model code:
21
...  (list of available years)

Enter the year code:
...

Vehicle Type: <type code>
Price: R$ <price>
Brand: <brand>
Model: <model>
Model Year: <year>
Fuel: <fuel>
Code Fipe: <FIPE code>
Reference Month: <month>
Fuel Acronym: <acronym>
```

> Accepted vehicle types: `cars`, `motorcycles`, `trucks`.

---

## ⚙️ How It Works

The FIPE API works like a **funnel**: each request narrows down the search, and each answer provides the codes needed for the next request. The vehicle type chosen at the start is carried through every step.

```
          ┌──────────────────────────────┐
          │  0. Choose vehicle type      │  cars | motorcycles | trucks
          └──────────────┬───────────────┘
                         │
          ┌──────────────▼───────────────┐
          │  1. List brands              │  GET /{type}/brands
          └──────────────┬───────────────┘
                         │ user types a brand code
          ┌──────────────▼───────────────┐
          │  2. List models              │  GET /{type}/brands/{brand}/models
          └──────────────┬───────────────┘
                         │ user types a model code
          ┌──────────────▼───────────────┐
          │  3. List years               │  GET /{type}/brands/{brand}/models/{model}/years
          └──────────────┬───────────────┘
                         │ user types a year code
          ┌──────────────▼───────────────┐
          │  4. Vehicle details + price  │  GET /{type}/brands/{brand}/models/{model}/years/{year}
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
| Type of merchandise written on the order | `vehicleType` (`cars`, `motorcycles`, `trucks`) |
| Employee who unpacks the boxes | Jackson |
| Shelf format | Records (`Brand`, `Model`, `Year`, `Vehicle`) |
| Store clerk who talks to the customer | `FipeRunner` |
| Opening routine of the store | `CommandLineRunner` |
| Product label | `toString()` in each record |
| Store door closed to the public | No embedded web server (`web-application-type=none`) |
| Rules for writing the orders | REST (HTTP methods + URLs) |

---

## 📂 Project Structure

```
src/main/java/br/com/tabelafipe/
├── TabelafipeApplication.java   # Entry point (@SpringBootApplication)
├── runner/
│   └── FipeRunner.java          # Talks to the user and coordinates the flow
├── service/
│   └── FipeService.java         # Makes the HTTP requests to the FIPE API
└── dto/
    ├── Brand.java               # (code, name)
    ├── Model.java               # (code, name)
    ├── Year.java                # (code, name)
    └── Vehicle.java             # Vehicle details

src/main/resources/
└── application.properties       # Disables the web server
```

Each layer has a single responsibility:

| Layer | Responsibility | Knows about |
|---|---|---|
| `runner` | User interaction (input, output, retry loops) | The service, never the URLs |
| `service` | Communication with the external API | URLs and HTTP, never the terminal |
| `dto` | Data shape and how it is displayed | Only its own fields |

> `TabelafipeApplication` stays in the root package so Spring can find the `@Component` and `@Service` classes in the subpackages.

---

## 🔍 Code Walkthrough

### 1. Running as a terminal application

Spring Web MVC includes an embedded Tomcat server. Since this project only makes requests (it does not receive them), the server is disabled:

```properties
spring.main.web-application-type=none
```

### 2. Data models (records)

Brands, models, and years share the same JSON format (`code` and `name`). Each one is a record with a custom `toString()`:

```java
public record Brand(String code,
                    String name) {

    @Override
    public String toString() {
        return "%s: %s\n".formatted(name, code);
    }
}
```

`Model` and `Year` follow the same pattern.

The vehicle details are a single object. The field names match the JSON keys so Jackson can map them automatically:

```java
public record Vehicle(int vehicleType,
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
                Vehicle Type: %d
                Price: %s
                Brand: %s
                Model: %s
                Model Year: %d
                Fuel: %s
                Code Fipe: %s
                Reference Month: %s
                Fuel Acronym: %s
                """.formatted(vehicleType,
                price, brand, model, modelYear,
                fuel, codeFipe, referenceMonth, fuelAcronym);
    }
}
```

> `price` is a `String`, not a `double`, because the API returns it as formatted text (`"R$ 10.000,00"`).
> Fields present in the JSON but missing from the record (such as `priceHistory`) are simply ignored.
> `toString()` uses a **text block** (`"""`) and `formatted()`, so a single `println(vehicle)` prints a clean result.
> The same records work for cars, motorcycles, and trucks, because the API returns the same JSON format for all of them.

### 3. The service: one class for every vehicle type

Instead of duplicating the service for each vehicle type, the type is just **one more placeholder** in the URL:

```java
@Service
public class FipeService {

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    public List<Brand> findBrands(String vehicleType) {
        return client.get()
                .uri("/{type}/brands", vehicleType)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Brand>>() {});
    }

    public List<Model> findModels(String vehicleType, String brandCode) {
        return client.get()
                .uri("/{type}/brands/{brand}/models", vehicleType, brandCode)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Model>>() {});
    }

    public List<Year> findYears(String vehicleType, String brandCode, String modelCode) {
        return client.get()
                .uri("/{type}/brands/{brand}/models/{model}/years",
                        vehicleType, brandCode, modelCode)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Year>>() {});
    }

    public Vehicle findVehicle(String vehicleType, String brandCode, String modelCode, String yearCode) {
        return client.get()
                .uri("/{type}/brands/{brand}/models/{model}/years/{year}",
                        vehicleType, brandCode, modelCode, yearCode)
                .retrieve()
                .body(Vehicle.class);
    }
}
```

| Step | What it does |
|---|---|
| `RestClient.create(...)` | Static factory method that builds the client with the base URL |
| `.get()` | Defines the HTTP method (fetch data) |
| `.uri(path, values...)` | Appends the path to the base URL and fills the `{placeholders}` **in order** |
| `.retrieve()` | Sends the request, receives the response, and throws an exception on error status |
| `.body(Vehicle.class)` | Single JSON object `{ }` → one Java object |
| `.body(new ParameterizedTypeReference<List<...>>() {})` | JSON array `[ ]` → `List`, working around Java's **type erasure** |

Each method receives exactly the values its URL needs, and the service never touches the terminal. Supporting a new vehicle type requires **no new code** in the service.

### 4. The runner: talking to the user

The runner receives the service through **constructor injection** and implements `CommandLineRunner`, so Spring calls `run()` as soon as the application starts:

```java
@Component
public class FipeRunner implements CommandLineRunner {

    private final FipeService service;

    public FipeRunner(FipeService service) {   // Spring injects the service here
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {

        Scanner sc = new Scanner(System.in);

        String vehicleType;   // declared outside the loops
        String brandCode;     // so they survive until
        String modelCode;     // the last step
        String yearCode;

        // ... vehicle type is read here ...

        List<Brand> brands = service.findBrands(vehicleType);
        brands.forEach(System.out::print);           // one item per line, via toString()

        while (true) {
            System.out.println("Enter the brand code: ");
            brandCode = sc.nextLine().trim();
            try {
                List<Model> models = service.findModels(vehicleType, brandCode);
                models.forEach(System.out::print);
                break;                                   // success: next step
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("Brand not found. TRY AGAIN!");
            }
        }

        // The same pattern repeats for model → years and year → vehicle,
        // ending with: System.out.println(vehicle);
    }
}
```

**Retry pattern:** `while (true)` + `try/catch` + `break`. The loop only ends when the request succeeds; a `404 Not Found` shows a specific message for each step (*brand*, *model*, or *year* not found) and asks again.

**Printing lists:** `forEach(System.out::print)` prints each item using the record's own `toString()`, one per line, without the brackets and commas that `println(list)` would add. `System.out::print` is a **method reference**, a shorter way to write `item -> System.out.print(item)`.

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

**Requirements:** Java 17+ (Maven is optional, the Maven Wrapper is included).

```bash
# Clone the repository
git clone https://github.com/rrosaGeovane/tabela-fipe.git
cd tabela-fipe

# Run
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

You can also run `TabelafipeApplication` directly from your IDE and interact through its console.

---

## 📚 What I Learned

**Concepts**
- **API vs. database:** an API is the communication layer; the data lives in a database behind it. As a client, I never access the database directly.
- **Being a client, not a server:** how to make an application *consume* an API instead of *exposing* one.
- **REST:** HTTP methods (`GET`, `POST`, `PUT`, `DELETE`) and URLs as the rules for writing requests.
- **HTTP status codes:** `200` success, `404` not found, `429` too many requests, `500` server error.

**Spring**
- **Spring Boot starters:** what the web starter brings (Spring MVC, Tomcat, Jackson, RestClient) and which parts this project uses.
- **`CommandLineRunner`:** runs code once at startup, similar to a `main` method, with Spring managing object creation.
- **Dependency injection:** the runner receives the service through its constructor; Spring creates and connects both.
- **Layered architecture:** separating API access (`@Service`) from user interaction, and keeping the main class in the root package for component scanning.
- **`RestClient`:** building requests with method chaining (`get → uri → retrieve → body`) and URI placeholders filled in order.

**Design**
- **Parameterizing instead of duplicating:** turning the vehicle type into a URL placeholder so one service handles cars, motorcycles, and trucks, instead of one copied class per type.
- **Reusing DTOs:** records describe the *shape* of the data, so the same records serve every vehicle type.

**Java**
- **Static factory methods:** why `RestClient.create()` is used instead of `new` (`RestClient` is an interface).
- **Jackson and deserialization:** JSON keys are mapped to record fields by name, and types must match (a formatted price is a `String`).
- **Single object vs. list:** `{ }` maps to `.body(MyClass.class)`, while `[ ]` requires `ParameterizedTypeReference` because of **type erasure**.
- **Exception handling:** `throws` passes the error up; `try/catch` handles it. Nested classes appear with `$` in stack traces but are written with `.` in code.
- **Variable scope:** variables declared inside a loop do not exist outside it.
- **Overriding `toString()`** in records, using text blocks and `formatted()`.

**Debugging**
- Adding "flashlights" (`println` checkpoints) to find where the flow breaks.
- Testing endpoints in the browser before writing code.

**Tools**
- Git and GitHub: initializing a repository, committing, and pushing from the terminal and from IntelliJ.
- IntelliJ **Refactor → Rename** to rename classes and methods across the whole project.

---

## 🚀 Next Steps

- [x] Make it interactive: let the user choose brand, model, and year in the terminal
- [x] Split the project into layers (`dto`, `service`, `runner`)
- [x] Support motorcycles and trucks
- [ ] Search a vehicle directly by its FIPE code
- [ ] Display the price history of a vehicle
- [ ] Let the user choose the reference month (prices from previous months)
- [ ] Compare the prices of two vehicles side by side
- [ ] Send the access token through the `X-Subscription-Token` header to raise the daily limit
- [ ] Save search history in a database
- [ ] Export results to a CSV file
- [ ] Expose the data through my own REST endpoints (`@RestController`), so the project both **consumes and provides** an API

---

## 👤 Author

**Geovane Ribeiro Rosa**
Information Systems student at Uniube (Universidade de Uberaba)

---

## 📄 Credits

Vehicle data provided by [FIPE API](https://fipe.api.br), a free community API by Deivid Fortuna.
