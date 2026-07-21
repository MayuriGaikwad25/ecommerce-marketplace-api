package com.marketplace.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.category.entity.Category;
import com.marketplace.category.repository.CategoryRepository;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.service.OrderService;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.user.entity.Role;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * This test intentionally does NOT use Spring's test-managed @Transactional. That annotation
 * would run the whole test in one outer transaction on one connection, which defeats the purpose
 * here: we need two genuinely independent transactions racing against each other, the same way
 * two real HTTP requests would. Test data is cleaned up manually in @BeforeEach instead.
 */
@SpringBootTest
@Testcontainers
class OrderConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product lastUnitProduct;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User vendor = userRepository.save(newUser("vendor@test.com", Role.VENDOR));
        Category category = categoryRepository.save(
                Category.builder().name("Electronics").build());

        lastUnitProduct = productRepository.save(Product.builder()
                .name("Last Unit Item")
                .description("Only one left")
                .price(new BigDecimal("99.00"))
                .stockQuantity(1)
                .sku("RACE-SKU-001")
                .active(true)
                .category(category)
                .vendor(vendor)
                .build());

        User customerA = userRepository.save(newUser("customerA@test.com", Role.CUSTOMER));
        User customerB = userRepository.save(newUser("customerB@test.com", Role.CUSTOMER));

        giveCartWithProduct(customerA, lastUnitProduct);
        giveCartWithProduct(customerB, lastUnitProduct);
    }

    @Test
    void twoCustomersRacingForTheLastUnit_onlyOneCheckoutSucceeds() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<OrderResponse> checkoutAsCustomerA = () -> {
            readyLatch.countDown();
            startLatch.await();
            return orderService.placeOrder("customerA@test.com", new PlaceOrderRequest("Address A, Dubai"));
        };
        Callable<OrderResponse> checkoutAsCustomerB = () -> {
            readyLatch.countDown();
            startLatch.await();
            return orderService.placeOrder("customerB@test.com", new PlaceOrderRequest("Address B, Dubai"));
        };

        Future<OrderResponse> futureA = executor.submit(checkoutAsCustomerA);
        Future<OrderResponse> futureB = executor.submit(checkoutAsCustomerB);

        // Wait until both threads are parked at the gate, then release them at the same instant
        // so both read the product's stock before either one commits.
        readyLatch.await();
        startLatch.countDown();

        int successCount = 0;
        int conflictCount = 0;

        for (Future<OrderResponse> future : List.of(futureA, futureB)) {
            try {
                future.get(15, TimeUnit.SECONDS);
                successCount++;
            } catch (ExecutionException e) {
                conflictCount++;
            } catch (TimeoutException e) {
                throw new AssertionError("Checkout did not complete within 15s — possible deadlock", e);
            }
        }
        executor.shutdown();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        Product refreshed =
                productRepository.findById(lastUnitProduct.getId()).orElseThrow();
        assertThat(refreshed.getStockQuantity()).isZero();
    }

    private User newUser(String email, Role role) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .role(role)
                .enabled(true)
                .build();
    }

    private void giveCartWithProduct(User user, Product product) {
        Cart cart = cartRepository.save(Cart.builder().user(user).build());
        cart.getItems().add(CartItem.builder().cart(cart).product(product).quantity(1).build());
        cartRepository.save(cart);
    }
}
