# When to Mock

Mock at **system boundaries** only:

- External APIs (payment, email, etc.)
- Databases (sometimes - prefer test DB)
- Time/randomness
- File system (sometimes)

Don't mock:

- Your own classes/modules
- Internal collaborators
- Anything you control

## Designing for Mockability

At system boundaries, design interfaces that are easy to mock:

**1. Use dependency injection**

Pass external dependencies in rather than creating them internally:

```java
// Easy to mock
public class PaymentService {
    private final PaymentClient paymentClient;

    public PaymentService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    public PaymentResult processPayment(Order order) {
        return paymentClient.charge(order.getTotal());
    }
}

// Hard to mock
public class PaymentService {
    public PaymentResult processPayment(Order order) {
        PaymentClient client = new StripeClient(System.getenv("STRIPE_KEY"));
        return client.charge(order.getTotal());
    }
}
```

**2. Prefer SDK-style interfaces over generic fetchers**

Create specific methods for each external operation instead of one generic method with conditional logic:

```java
// GOOD: Each method is independently mockable
public interface UserApiClient {
    User getUser(String id);
    List<Order> getOrders(String userId);
    Order createOrder(CreateOrderRequest request);
}

// BAD: Mocking requires conditional logic inside the mock
public interface GenericApiClient {
    <T> HttpResponse<T> execute(HttpRequest request, Class<T> responseType);
}
```

The SDK approach means:
- Each mock returns one specific type
- No conditional logic in test setup
- Easier to see which endpoints a test exercises
- Type safety per endpoint
