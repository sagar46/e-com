package com.ecommerce.project.services.impl;

import com.ecommerce.project.dto.CartDTO;
import com.ecommerce.project.dto.ProductDTO;
import com.ecommerce.project.dto.ProductResponse;
import com.ecommerce.project.entities.Cart;
import com.ecommerce.project.entities.Category;
import com.ecommerce.project.entities.Product;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.services.CartService;
import com.ecommerce.project.services.FileService;
import com.ecommerce.project.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final FileService fileService;
    private final CartRepository cartRepository;
    private final CartService cartService;


    @Value("${project.image}")
    private String path;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        log.debug("ProductService.addProduct call started...");
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (Objects.isNull(category)) {
            log.debug("ProductService.addProduct call failed...");
            throw new ResourceNotFoundException("Category not found");
        }

        boolean isProductNotPresent = true;
        List<Product> products = category.getProducts();
        for (Product product : products) {
            if (product.getProductName().equals(productDTO.getProductName())) {
                isProductNotPresent = false;
                break;
            }
        }

        if (isProductNotPresent) {
            Product product = modelMapper.map(productDTO, Product.class);
            double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);
            product.setCategory(category);
            product.setImage("default.png");
            Product savedProduct = productRepository.save(product);
            log.debug("ProductService.addProduct call completed...");
            return modelMapper.map(savedProduct, ProductDTO.class);
        } else {
            log.debug("ProductService.addProduct call failed...");
            throw new APIException("Product already exists");
        }
    }

    @Override
    public ProductResponse getAllProduct(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.debug("ProductService.getAllProduct call started...");
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productPage = productRepository.findAll(pageDetails);
        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContents(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        productResponse.setTotalPages(productPage.getTotalPages());
        log.debug("ProductService.getAllProduct call completed...");
        return productResponse;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.debug("ProductService.searchByCategory call started...");
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (Objects.isNull(category)) {
            log.debug("ProductService.searchByCategory category call failed...");
            throw new ResourceNotFoundException("Category not found");
        }
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageDetails);
        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContents(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        productResponse.setTotalPages(productPage.getTotalPages());

        log.debug("ProductService.searchByCategory call completed...");
        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.debug("ProductService.searchProductByKeyword call started...");
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageDetails);
        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContents(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        productResponse.setTotalPages(productPage.getTotalPages());

        log.debug("ProductService.searchProductByKeyword call completed...");
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        log.debug("ProductService.updateProduct call started...");
        Product product = productRepository.findById(productId).orElse(null);
        if (Objects.isNull(product)) {
            log.debug("ProductService.updateProduct call failed...");
            throw new ResourceNotFoundException("Product not found");
        }
        product.setProductName(productDTO.getProductName());
        product.setDescription(productDTO.getDescription());
        product.setDiscount(productDTO.getDiscount());
        product.setQuantity(productDTO.getQuantity());
        product.setPrice(productDTO.getPrice());
        product.setSpecialPrice(product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice()));
        Product savedProduct = productRepository.save(product);

        List<Cart> carts = cartRepository.findCartByProductId(productId);

        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
                    CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
                    List<ProductDTO> productDTOList = cart.getCartItems().stream().map(prod -> modelMapper.map(prod, ProductDTO.class)).toList();
                    cartDTO.setProducts(productDTOList);
                    return cartDTO;
                }
        ).toList();
        cartDTOS.forEach(cartDTO -> cartService.updateProductInCart(cartDTO.getCartId(), productId));
        log.debug("ProductService.updateProduct call completed...");
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        log.debug("ProductService.deleteProduct call started...");
        Product product = productRepository.findById(productId).orElse(null);
        if (Objects.isNull(product)) {
            log.debug("ProductService.deleteProduct call failed...");
            throw new ResourceNotFoundException("Product not found");
        }
        List<Cart> carts = cartRepository.findCartByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));
        productRepository.delete(product);
        log.debug("ProductService.deleteProduct call completed...");
        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        log.debug("ProductService.updateProductImage call started...");
        Product product = productRepository.findById(productId).orElse(null);
        if (Objects.isNull(product)) {
            log.debug("ProductService.updateProductImage call failed...");
            throw new ResourceNotFoundException("Product not found");
        }

        String filename = fileService.uploadImage(path, image);
        product.setImage(filename);

        Product updatedProduct = productRepository.save(product);
        log.debug("ProductService.updateProductImage call completed...");
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }


}
