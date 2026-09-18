# Good and Bad Tests

## Good Tests

**Integration-style**: Test through real interfaces, not mocks of internal parts.

```java
// GOOD: Tests observable behavior
@Test
void userCanCheckoutWithValidCart() {
    Cart cart = new Cart();
    cart.add(product);
    CheckoutResult result = checkoutService.checkout(cart, paymentMethod);
    assertThat(result.getStatus()).isEqualTo(CheckoutStatus.CONFIRMED);
}
```

Characteristics:

- Tests behavior users/callers care about
- Uses public API only
- Survives internal refactors
- Describes WHAT, not HOW
- One logical assertion per test

## Bad Tests

**Implementation-detail tests**: Coupled to internal structure.

```java
// BAD: Tests implementation details
@Test
void checkoutCallsPaymentServiceProcess() {
    PaymentService mockPayment = mock(PaymentService.class);
    CheckoutService service = new CheckoutService(mockPayment);
    service.checkout(cart, paymentMethod);
    verify(mockPayment).process(cart.getTotal());
}
```

Red flags:

- Mocking internal collaborators
- Testing private methods
- Asserting on call counts/order
- Test breaks when refactoring without behavior change
- Test name describes HOW not WHAT
- Verifying through external means instead of interface

```java
// BAD: Bypasses interface to verify
@Test
void createUserSavesToDatabase() {
    userService.createUser(new CreateUserRequest("Alice"));
    Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM users WHERE name = ?", "Alice");
    assertThat(row).isNotNull();
}

// GOOD: Verifies through interface
@Test
void createUserMakesUserRetrievable() {
    User user = userService.createUser(new CreateUserRequest("Alice"));
    User retrieved = userService.getUser(user.getId());
    assertThat(retrieved.getName()).isEqualTo("Alice");
}
```

**Tautological tests**: Expected value restates the implementation, so the test passes by construction.

```java
// BAD: Expected value is recomputed the way the code computes it
@Test
void calculateTotalSumsLineItems() {
    List<Item> items = List.of(new Item(10), new Item(5));
    int expected = items.stream().mapToInt(Item::getPrice).sum();
    assertThat(calculator.calculateTotal(items)).isEqualTo(expected);
}

// GOOD: Expected value is an independent, known literal
@Test
void calculateTotalSumsLineItems() {
    List<Item> items = List.of(new Item(10), new Item(5));
    assertThat(calculator.calculateTotal(items)).isEqualTo(15);
}
```
