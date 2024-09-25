package com.ecommerce.project.services.impl;

import com.ecommerce.project.dto.OrderDTO;
import com.ecommerce.project.dto.OrderItemDTO;
import com.ecommerce.project.dto.ProductDTO;
import com.ecommerce.project.entities.*;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.repositories.*;
import com.ecommerce.project.services.CartService;
import com.ecommerce.project.services.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ModelMapper modelMapper;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId,
                               Long addressId,
                               String paymentMethod,
                               String pgName,
                               String pgPaymentId,
                               String pgStatus,
                               String pgResponseMessage) {
        log.debug("OrderServiceImpl.placeOrder call started...");
        // validations for cart
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            log.error("Cart not found with email: {}", emailId);
            throw new ResourceNotFoundException("Cart not found with email: " + emailId);
        }
        // validations for address
        Address address = addressRepository.findById(addressId)
                .orElseThrow(
                        () -> {
                            log.debug("Address not found with id: {}", addressId);
                            return new ResourceNotFoundException("Address not found with id: " + addressId);
                        });

        // validations for cartItem whether
        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()) {
            log.error("Cart is empty");
            throw new APIException("Cart is empty");
        }

        // create the order
        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted !");
        order.setAddress(address);

        // create the payment and save into database
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentMethod);
        payment.setPgPaymentId(pgPaymentId);
        payment.setPgStatus(pgStatus);
        payment.setPgResponseMessage(pgResponseMessage);
        payment.setPgName(pgName);
        payment.setOrder(order);
        payment = paymentRepository.save(payment);

        // saved payment should be set against the order
        order.setPayment(payment);

        // save the order
        Order savedOrder = orderRepository.save(order);

        // mapping cartItem to orderItem
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        // saving the orderItem
        orderItems = orderItemRepository.saveAll(orderItems);

        //updating the stock for product
        cart.getCartItems().forEach(item -> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();

            //Reduce stock quantity
            product.setQuantity(product.getQuantity() - quantity);

            // save product back to database
            productRepository.save(product);

            //Remove item from cart
            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
        });

        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(orderItem -> {
            OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);
            orderItemDTO.setProduct(modelMapper.map(orderItem.getProduct(), ProductDTO.class));
                    orderDTO.getOrderItems().add(orderItemDTO);
                }
        );

        orderDTO.setAddressId(addressId);
        log.debug("OrderServiceImpl.placeOrder call completed with: {}", orderDTO);
        return orderDTO;
    }
}
