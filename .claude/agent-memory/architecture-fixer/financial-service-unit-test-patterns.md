---
name: financial-service-unit-test-patterns
description: Patterns and decisions for FinancialServiceTest unit tests (R59)
type: project
---

## FinancialService Unit Test Patterns (R59 — completed)

### Key implementation decisions

**LocalDate.now() inside the method under test:**
`getDailyReport()` calls `LocalDate.now()` internally — it is not injected and cannot be controlled from outside.
The stub must use `any(LocalDate.class)` instead of a fixed date to avoid a timing race between test setup and method execution.
The assertion `result.date().isEqualTo(LocalDate.now())` is still safe because both the stub call and the assertion resolve `LocalDate.now()` within the same JVM second.

**No-balance fallback path:**
`getDailyReport()` uses `orElse(new DailyBalance(today, BigDecimal.ZERO, 0))` — NOT `orElseGet`.
The `DailyBalance` all-args constructor must be called with `(LocalDate, BigDecimal, Integer)`.
The zero-values test stubs `findById` to return `Optional.empty()` and then asserts `totalRevenue.isEqualByComparingTo(BigDecimal.ZERO)` and `totalOrders == 0`.

**getHistory pagination:**
`findAllByOrderByDateDesc(Pageable)` returns a `Page<DailyBalance>`. The service calls `.map(this::toDTO)` — PageImpl's `.map()` returns a new `PageImpl` with the same page metadata.
Test fixtures use `new PageImpl<>(content, pageable, totalElements)` — the three-argument constructor preserves total count independently of content list size, which is needed for the pagination metadata test.

**Verify call pattern:**
For `getDailyReport`, `verify(dailyBalanceRepository).findById(LocalDate.now())` works because verify resolves at assertion time (after the method has run), not at stub time.
For `getHistory`, `verify(dailyBalanceRepository).findAllByOrderByDateDesc(pageable)` with the exact `Pageable` instance confirms delegation.

### Test count: 8
- getDailyReport_noBalanceForToday_returnsZeroValues
- getDailyReport_noBalanceForToday_callsFindById
- getDailyReport_balanceExists_returnsAccumulatedValues
- getDailyReport_balanceExists_mapsAllDTOFields
- getHistory_delegatesToRepositoryWithCorrectPageable
- getHistory_mapsDailyBalanceToDTOCorrectly
- getHistory_returnsEmptyPage_whenNoRecordsExist
- getHistory_respectsPaginationMetadata
