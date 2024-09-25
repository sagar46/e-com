package com.ecommerce.project.services.impl;

import com.ecommerce.project.dto.CartDTO;
import com.ecommerce.project.dto.ProductDTO;
import com.ecommerce.project.entities.Cart;
import com.ecommerce.project.entities.CartItem;
import com.ecommerce.project.entities.Product;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.services.CartService;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CartServiceImpl implements CartService {


    private final CartRepository cartRepository;
    private final AuthUtil authUtil;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public CartDTO addProductToCart(Long productId, Integer quantity) {

        log.debug("CartServiceImpl.addProductToCart call started with productId: {} and quantity: {}", productId, quantity);
        Cart cart = createCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                            log.error("CartServiceImpl.addProductToCart failed with product not found");
                            return new ResourceNotFoundException("Product with productId " + productId + " not found.");
                        }
                );
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem != null) {
            log.error("CartServiceImpl.addProductToCart failed with Product already exist in cart");
            throw new APIException("Product " + product.getProductName() + " already exists in the cart.");
        }
        if (product.getQuantity() == 0) {
            log.error("CartServiceImpl.addProductToCart failed with: {} had no quantity", product.getProductName());
            throw new APIException("Product " + product.getProductName() + " has no quantity.");
        }
        if (product.getQuantity() < quantity) {
            log.error("CartServiceImpl.addProductToCart failed with: {} had no enough quantity", product.getProductName());
            throw new APIException("Product " + product.getProductName() + " has no enough quantity.");
        }
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setCart(cart);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getPrice());
        CartItem savedCartItem = cartItemRepository.save(newCartItem);

        product.setQuantity(product.getQuantity());
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cart.getCartItems().add(savedCartItem);
        cartRepository.save(cart);

        CartDTO savedCart = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        savedCart.setProducts(productDTOStream.toList());
        log.debug("CartServiceImpl.addProductToCart call completed with: {}", savedCart);
        return savedCart;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        log.debug("CartService.getAllCarts started...");
        List<Cart> carts = cartRepository.findAll();
        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
            cart.getCartItems().forEach(cartItem -> cartItem.getProduct().setQuantity(cartItem.getQuantity()));
            List<ProductDTO> products = cart.getCartItems().stream()
                    .map(
                            product ->
                                    modelMapper.map(product.getProduct(), ProductDTO.class))
                    .toList();
            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();
        log.debug("CartServiceImpl.getAllCarts call completed with: {}", cartDTOS);
        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        log.debug("CartServiceImpl.getCart call started with emailId: {} and cartId: {}", emailId, cartId);
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart not found with emailId " + emailId + " and cartId " + cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cart.getCartItems().forEach(cartItem -> cartItem.getProduct().setQuantity(cartItem.getQuantity()));
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(cartItem ->
                        modelMapper.map(cartItem.getProduct(), ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        log.debug("CartServiceImpl.getCart call completed with: {}", cartDTO);
        return cartDTO;
    }


    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        log.debug("CartServiceImpl.updateProductQuantityInCart call started with productId: {} and delete: {}", productId, quantity);
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id " + cartId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        if (product.getQuantity() == 0) {
            throw new APIException("Product " + product.getProductName() + " has no quantity");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Product " + product.getProductName() + " has no enough quantity");
        }
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " has no enough quantity in the cart");
        }

        int newQuantity = cartItem.getQuantity() + quantity;
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative.");
        }

        if (newQuantity == 0) {
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * cartItem.getQuantity()));
            cartRepository.save(cart);
        }
        CartItem updatedItem = cartItemRepository.save(cartItem);
        if (updatedItem.getQuantity() == 0) {
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });

        cartDTO.setProducts(productDTOStream.toList());
        log.debug("CartServiceImpl.updateProductQuantityInCart call completed with: {}", cartDTO);
        return cartDTO;
    }

    @Override
    @Transactional
    public String deleteProductFromCart(Long cartId, Long productId) {
        log.debug("CartServiceImpl.deleteProductFromCart call started with productId: {}", productId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id " + cartId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("Cart item not found with cartId " + cartId + " and productId " + productId);
        }
        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        cartRepository.save(cart);

        log.debug("CartServiceImpl.deleteProductFromCart call completed with: {}", cart);
        return "Product " + cartItem.getProduct().getProductName() + " deleted successfully.";
    }

    @Override
    public void updateProductInCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id " + cartId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Cart item not found with cartId " + cartId);
        }

        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItem = cartItemRepository.save(cartItem);
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return userCart;
        }
        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
}
