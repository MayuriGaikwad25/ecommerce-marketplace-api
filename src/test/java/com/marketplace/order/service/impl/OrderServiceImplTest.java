package com.marketplace.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.common.exception.EmptyCartException;
import com.marketplace.common.exception.InsufficientStockException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.repository.OrderItemRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.user.entity.Role;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User customer;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id("user-1")
                .email("customer@example.com")
                .fullName("Test Customer")
                .role(Role.CUSTOMER)
                .build();

        product = Product.builder()
                .id("product-1")
                .name("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(5)
                .sku("MOUSE-001")
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder().product(product).quantity(2).build();
        cart = Cart.builder().user(customer).items(new ArrayList<>(List.of(cartItem))).build();
    }

    @Test
    void placeOrder_decrementsStockAndReturnsOrderWithCorrectTotal() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response =
                orderService.placeOrder("customer@example.com", new PlaceOrderRequest("123 Main St, Dubai"));

        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.totalAmount()).isEqualByComparingTo("59.98");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);
        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(cart.getItems()).isEmpty();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrder_emptyCartItems_throwsEmptyCartExceptionAndNeverSaves() {
        cart.setItems(new ArrayList<>());
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.placeOrder("customer@example.com", new PlaceOrderRequest("addr")))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_noCartExistsYet_throwsEmptyCartException() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("customer@example.com", new PlaceOrderRequest("addr")))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void placeOrder_requestedQuantityExceedsStock_throwsInsufficientStockAndNeverSaves() {
        product.setStockQuantity(1);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder("customer@example.com", new PlaceOrderRequest("addr")))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getById_orderBelongsToDifferentUser_throwsResourceNotFoundToHideExistence() {
        User otherUser = User.builder().id("user-2").email("other@example.com").role(Role.CUSTOMER).build();
        Order othersOrder = Order.builder()
                .id("order-1")
                .user(otherUser)
                .status(OrderStatus.PLACED)
                .totalAmount(BigDecimal.TEN)
                .shippingAddress("Somewhere")
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(othersOrder));

        assertThatThrownBy(() -> orderService.getById("customer@example.com", "order-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
