package com.ecommerce.project.controllers;

import com.ecommerce.project.dto.CartDTO;
import com.ecommerce.project.entities.Cart;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.services.CartService;
import com.ecommerce.project.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CartController {


    private final CartService cartService;
    private final AuthUtil authUtil;
    private final CartRepository cartRepository;


    @PostMapping("/cart/product/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long productId, @PathVariable Integer quantity) {
        log.debug("CartController.addProductToCart call started with productId: {}, quantity: {}", productId, quantity);
        CartDTO cartDTO = cartService.addProductToCart(productId, quantity);
        log.debug("CartController.addProductToCart call completed with cartDTO: {}", cartDTO);
        return ResponseEntity.status(HttpStatus.OK).body(cartDTO);
    }

    @GetMapping("/carts")
    public ResponseEntity<List<CartDTO>> getAllCarts() {
        log.debug("CartController.getAllCarts call started...");
        List<CartDTO> cartDTOS = cartService.getAllCarts();
        if (cartDTOS.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        log.debug("CartController.getAllCarts call completed with cartDTOS: {}", cartDTOS);
        return ResponseEntity.status(HttpStatus.OK).body(cartDTOS);
    }

    @GetMapping("/carts/user/cart")
    public ResponseEntity<CartDTO> getCartById() {
        log.debug("CartController.getCartById call started...");
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);
        CartDTO cartDTO = cartService.getCart(emailId, cart.getCartId());
        log.debug("CartController.getCartById call completed with cartDTO: {}", cartDTO);
        return ResponseEntity.status(HttpStatus.OK).body(cartDTO);
    }

    @PutMapping("/carts/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateCartProduct(@PathVariable Long productId, @PathVariable String operation) {
        log.debug("CartController.updateCartProduct call started...");
        CartDTO cartDTO = cartService.updateProductQuantityInCart(
                productId,
                operation.equalsIgnoreCase("delete") ? -1 : 1);
        log.debug("CartController.updateCartProduct call completed with cartDTO: {}", cartDTO);
        return ResponseEntity.status(HttpStatus.OK).body(cartDTO);
    }

    @DeleteMapping("carts/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId,
                                                        @PathVariable Long productId) {
        log.debug("CartController.deleteProductFromCart call started with cartId: {} and productId: {}", cartId, productId);
        String status = cartService.deleteProductFromCart(cartId, productId);
        log.debug("CartController.deleteProductFromCart call completed with cartDTO: {}", status);
        return ResponseEntity.status(HttpStatus.OK).body(status);
    }
}
