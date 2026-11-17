## Summary of Changes
<!-- Briefly describe what this Pull Request implements and why -->

**Related Issue:** Closes #<!-- issue-number -->

## Affected Microservice / Module
- [ ] `platform-commons`
- [ ] `api-gateway`
- [ ] `user-service`
- [ ] `catalog-service`
- [ ] `inventory-service`
- [ ] `cart-service`
- [ ] `coupon-service`
- [ ] `order-service`
- [ ] `payment-service`
- [ ] `notification-service`
- [ ] `ci / infrastructure / terraform / k8s-gitops`

## Design Patterns & DSA Applied
<!-- Mention any GoF Design Patterns, Microservice Patterns (Saga, Outbox, CQRS), or Data Structures/Algorithms used -->
- 

## Scalability, Database & Security Impact (5M Customers)
- [ ] No hardcoded secrets, credentials, or tokens (uses `${}` environment placeholders)
- [ ] Flyway migration script added/verified (if schema changed)
- [ ] Queries indexed & HikariCP / transaction boundaries kept minimal
- [ ] BOLA/IDOR authorization checks enforced

## Verification Checklist
- [ ] Unit tests (`*Test.java`) added and passing locally
- [ ] Integration tests (`*IT.java` with Testcontainers) added and passing locally
- [ ] `./mvnw clean verify` passes with >= 80% coverage on new code
