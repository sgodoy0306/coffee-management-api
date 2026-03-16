# Implementation Plan: OrderType + Pastries

## Context

Spring Boot 3.2 · Java 17 · PostgreSQL 16 · Flyway migrations (`ddl-auto=none`).
Base package: `com.brewstack.api`.
Source root: `src/main/java/com/brewstack/api/`.
Migration root: `src/main/resources/db/migration/`.
Last Flyway version in use: `V4`.

### Conventions (MUST follow)
- DTOs → Java `record` (never `@Data` classes).
- Dependency injection → constructor injection via `@RequiredArgsConstructor` (never `@Autowired` on fields).
- Logging → `@Slf4j` + `log.info` / `log.error` (never `System.out.println`).
- Controllers → zero business logic; only HTTP routing + delegation to service.
- All business logic → service layer.
- New DB objects → Flyway SQL script (Hibernate does NOT manage DDL).

---

## Architectural Decision: orderType is NOT persisted

`orderType` lives **only in the DTO layer**. Reason: the project has no `Order` entity; persistence happens in `DailyBalance`, which aggregates many orders of both types in a single row. Storing `orderType` there has no valid semantics.

---

## Pre-flight Check (run before starting)

```bash
# Verify no V5 migration already exists
ls src/main/resources/db/migration/

# Find all OrderSummaryDTO constructors that will break after adding a 6th param
grep -rn "new OrderSummaryDTO" src/
```

Expected: no `V5__*.sql` file exists. Update every `new OrderSummaryDTO(...)` call found in tests after Step 4.

---

## Execution Order

Steps are numbered and labeled with their dependency. Execute sequentially.

```
Step 1  → no deps
Step 2  → requires Step 1
Step 3  → requires Step 1
Step 4  → requires Step 2 + Step 3
Step 5  → no deps
Step 6  → requires Step 5
Step 7  → no deps
Step 8  → requires Step 7
Step 9  → no deps (DTOs are standalone records)
Step 10 → no deps
Step 11 → no deps
Step 12 → requires Steps 5, 6, 7, 8, 9, 10, 11
Step 13 → requires Step 12
Step 14 → requires Steps 5 and 7
Step 15 → run after all steps
```

---

## FEATURE 1 — OrderType in Brew

### Step 1 — Create `OrderType` enum

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/model/OrderType.java`

```java
package com.brewstack.api.model;

public enum OrderType {
    DINE_IN,
    TAKE_AWAY
}
```

---

### Step 2 — Modify `OrderRequest`

**Action:** REPLACE entire file content
**Path:** `src/main/java/com/brewstack/api/dto/OrderRequest.java`

Current content (for reference):
```java
public record OrderRequest(
        @NotEmpty(message = "recipeIds must not be empty")
        List<Long> recipeIds,

        @NotNull(message = "baristaId must not be null")
        Long baristaId
) {}
```

New content:
```java
package com.brewstack.api.dto;

import com.brewstack.api.model.OrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "recipeIds must not be empty")
        List<Long> recipeIds,

        @NotNull(message = "baristaId must not be null")
        Long baristaId,

        // Nullable: BrewService defaults to DINE_IN for backward compatibility
        OrderType orderType
) {}
```

---

### Step 3 — Modify `OrderSummaryDTO`

**Action:** REPLACE entire file content
**Path:** `src/main/java/com/brewstack/api/dto/OrderSummaryDTO.java`

Current content (for reference):
```java
public record OrderSummaryDTO(
        List<String> brewedRecipes,
        BigDecimal totalRevenue,
        int totalOrders,
        long baristaXp,
        int baristaLevel
) {}
```

New content:
```java
package com.brewstack.api.dto;

import com.brewstack.api.model.OrderType;
import java.math.BigDecimal;
import java.util.List;

public record OrderSummaryDTO(
        List<String> brewedRecipes,
        BigDecimal totalRevenue,
        int totalOrders,
        long baristaXp,
        int baristaLevel,
        OrderType orderType
) {}
```

> `orderType` is added as the **last** parameter to minimize deserialization breakage in existing clients that use positional mapping.

---

### Step 4 — Modify `BrewService.processOrder()`

**Action:** EDIT `src/main/java/com/brewstack/api/service/BrewService.java`

**Change 1 of 2:** Add import at the top of the imports block:
```java
import com.brewstack.api.model.OrderType;
```

**Change 2 of 2:** Inside `processOrder()`, apply two targeted edits:

- **After** the line `Barista barista = baristaRepository.findById(...)...` (line 41–42), insert:
```java
        OrderType orderType = request.orderType() != null ? request.orderType() : OrderType.DINE_IN;
```

- **Replace** the final `return` statement (currently line 122):
```java
// BEFORE
return new OrderSummaryDTO(brewedNames, orderRevenue, recipes.size(), barista.getTotalXp(), newLevel);

// AFTER
return new OrderSummaryDTO(brewedNames, orderRevenue, recipes.size(), barista.getTotalXp(), newLevel, orderType);
```

- **Replace** the `log.info` call (currently line 119–120):
```java
// BEFORE
log.info("Order processed: baristaId={} recipes={} revenue={} newLevel={}",
        request.baristaId(), brewedNames, orderRevenue, newLevel);

// AFTER
log.info("Order processed: baristaId={} recipes={} revenue={} newLevel={} orderType={}",
        request.baristaId(), brewedNames, orderRevenue, newLevel, orderType);
```

> `BrewController` requires **no changes**. It already delegates 100% to the service.

---

## FEATURE 2 — Pastry CRUD

### Step 5 — Create `Pastry` entity

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/model/Pastry.java`

```java
package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "pastries", indexes = {
    @Index(name = "idx_pastries_name", columnList = "name"),
    @Index(name = "idx_pastries_available", columnList = "available")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pastry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available = true;
}
```

---

### Step 6 — Create `PastryRepository`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/repository/PastryRepository.java`

```java
package com.brewstack.api.repository;

import com.brewstack.api.model.Pastry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PastryRepository extends JpaRepository<Pastry, Long> {

    boolean existsByName(String name);

    List<Pastry> findAllByAvailableTrue();
}
```

---

### Step 7 — Create `PastryNotFoundException`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/exception/PastryNotFoundException.java`

```java
package com.brewstack.api.exception;

public class PastryNotFoundException extends RuntimeException {

    public PastryNotFoundException(Long id) {
        super("Pastry not found with id: " + id);
    }
}
```

---

### Step 8 — Register exception in `GlobalExceptionHandler`

**Action:** EDIT `src/main/java/com/brewstack/api/exception/GlobalExceptionHandler.java`

Add the following import at the top of the imports block:
```java
import com.brewstack.api.exception.PastryNotFoundException;
```

Add the following handler **inside the `// ── 404 Not Found ──` block**, after the existing `handleIngredientNotFound` method:

```java
    @ExceptionHandler(PastryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePastryNotFound(PastryNotFoundException ex,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Pastry Not Found", ex.getMessage(), request.getRequestURI()));
    }
```

---

### Step 9 — Create `PastryDTO`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/dto/PastryDTO.java`

```java
package com.brewstack.api.dto;

import java.math.BigDecimal;

public record PastryDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean available
) {}
```

---

### Step 10 — Create `CreatePastryRequest`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/dto/CreatePastryRequest.java`

```java
package com.brewstack.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePastryRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        String description,

        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        BigDecimal price,

        boolean available
) {}
```

---

### Step 11 — Create `UpdatePastryRequest`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/dto/UpdatePastryRequest.java`

```java
package com.brewstack.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePastryRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        String description,

        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        BigDecimal price,

        boolean available
) {}
```

---

### Step 12 — Create `PastryService`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/service/PastryService.java`

```java
package com.brewstack.api.service;

import com.brewstack.api.dto.CreatePastryRequest;
import com.brewstack.api.dto.PastryDTO;
import com.brewstack.api.dto.UpdatePastryRequest;
import com.brewstack.api.exception.PastryNotFoundException;
import com.brewstack.api.model.Pastry;
import com.brewstack.api.repository.PastryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PastryService {

    private final PastryRepository pastryRepository;

    @Transactional(readOnly = true)
    public List<PastryDTO> findAll() {
        return pastryRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PastryDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Transactional
    public PastryDTO create(CreatePastryRequest request) {
        log.info("Creating pastry name='{}'", request.name());
        Pastry pastry = new Pastry(null, request.name(), request.description(),
                request.price(), request.available());
        PastryDTO created = toDTO(pastryRepository.save(pastry));
        log.info("Pastry created id={}", created.id());
        return created;
    }

    @Transactional
    public PastryDTO update(Long id, UpdatePastryRequest request) {
        log.info("Updating pastry id={}", id);
        Pastry pastry = findEntityById(id);
        pastry.setName(request.name());
        pastry.setDescription(request.description());
        pastry.setPrice(request.price());
        pastry.setAvailable(request.available());
        return toDTO(pastryRepository.save(pastry));
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting pastry id={}", id);
        Pastry pastry = findEntityById(id);
        pastryRepository.delete(pastry);
        log.info("Pastry id={} deleted", id);
    }

    private Pastry findEntityById(Long id) {
        return pastryRepository.findById(id)
                .orElseThrow(() -> new PastryNotFoundException(id));
    }

    private PastryDTO toDTO(Pastry pastry) {
        return new PastryDTO(pastry.getId(), pastry.getName(), pastry.getDescription(),
                pastry.getPrice(), pastry.isAvailable());
    }
}
```

---

### Step 13 — Create `PastryController`

**Action:** CREATE new file
**Path:** `src/main/java/com/brewstack/api/controller/PastryController.java`

```java
package com.brewstack.api.controller;

import com.brewstack.api.dto.CreatePastryRequest;
import com.brewstack.api.dto.PastryDTO;
import com.brewstack.api.dto.UpdatePastryRequest;
import com.brewstack.api.service.PastryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pastries")
@RequiredArgsConstructor
public class PastryController {

    private final PastryService pastryService;

    @PostMapping
    public ResponseEntity<PastryDTO> create(@Valid @RequestBody CreatePastryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pastryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PastryDTO>> getAll() {
        return ResponseEntity.ok(pastryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PastryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pastryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PastryDTO> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdatePastryRequest request) {
        return ResponseEntity.ok(pastryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pastryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

### Step 14 — Create Flyway migration `V5`

**Action:** CREATE new file
**Path:** `src/main/resources/db/migration/V5__add_pastries_table.sql`

```sql
-- V5__add_pastries_table.sql
-- Creates the pastries table.
-- NOTE: orderType is NOT stored in daily_balances because a daily balance
-- aggregates many orders of both types; orderType lives in the DTO layer only.

CREATE TABLE IF NOT EXISTS pastries (
    id          BIGSERIAL       NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(10, 2)  NOT NULL,
    available   BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_pastries PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_pastries_name      ON pastries (name);
CREATE INDEX IF NOT EXISTS idx_pastries_available ON pastries (available);
```

> Indexes are created explicitly in SQL because `ddl-auto=none` — Hibernate annotations are documentation only.

---

## Step 15 — Verify

```bash
# Compile
make compile

# Run all tests (fix any OrderSummaryDTO constructor calls in tests first)
make test

# Start DB and app, then smoke test
make db
make run

# Create a pastry
curl -s -X POST http://localhost:8181/api/pastries \
  -H "Content-Type: application/json" \
  -d '{"name":"Croissant","description":"Butter croissant","price":3.50,"available":true}' | jq .

# Get all pastries
curl -s http://localhost:8181/api/pastries | jq .

# Place an order with orderType
curl -s -X POST http://localhost:8181/api/brew/order \
  -H "Content-Type: application/json" \
  -d '{"recipeIds":[1],"baristaId":1,"orderType":"TAKE_AWAY"}' | jq .

# Verify orderType appears in response and defaults to DINE_IN when omitted
curl -s -X POST http://localhost:8181/api/brew/order \
  -H "Content-Type: application/json" \
  -d '{"recipeIds":[1],"baristaId":1}' | jq .orderType
# Expected: "DINE_IN"
```

---

## File Summary

### Files to CREATE (10)

| # | Path | Type |
|---|---|---|
| 1 | `model/OrderType.java` | Enum |
| 2 | `model/Pastry.java` | JPA Entity |
| 3 | `repository/PastryRepository.java` | JPA Repository |
| 4 | `exception/PastryNotFoundException.java` | Exception |
| 5 | `dto/PastryDTO.java` | Record DTO |
| 6 | `dto/CreatePastryRequest.java` | Record DTO |
| 7 | `dto/UpdatePastryRequest.java` | Record DTO |
| 8 | `service/PastryService.java` | Service |
| 9 | `controller/PastryController.java` | Controller |
| 10 | `db/migration/V5__add_pastries_table.sql` | Flyway SQL |

### Files to MODIFY (4)

| # | Path | Change |
|---|---|---|
| 1 | `dto/OrderRequest.java` | Add `OrderType orderType` field (nullable) |
| 2 | `dto/OrderSummaryDTO.java` | Add `OrderType orderType` as last field |
| 3 | `service/BrewService.java` | Resolve default + propagate `orderType` to return |
| 4 | `exception/GlobalExceptionHandler.java` | Add handler for `PastryNotFoundException` |

---

## REST API Surface

### Modified endpoint

```
POST /api/brew/order
Request:  { "recipeIds": [1], "baristaId": 1, "orderType": "TAKE_AWAY" }
          orderType is optional → defaults to DINE_IN
Response: { ..., "orderType": "TAKE_AWAY" }
```

### New endpoints

```
POST   /api/pastries        201 + PastryDTO
GET    /api/pastries        200 + List<PastryDTO>
GET    /api/pastries/{id}   200 + PastryDTO   |  404 PastryNotFoundException
PUT    /api/pastries/{id}   200 + PastryDTO   |  404 PastryNotFoundException
DELETE /api/pastries/{id}   204 No Content    |  404 PastryNotFoundException
```
