package com.ecommerce.project.controllers;

import com.ecommerce.project.dto.OrderDTO;
import com.ecommerce.project.dto.OrderRequestDTO;
import com.ecommerce.project.services.OrderService;
import com.ecommerce.project.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderController {

    private final OrderService orderService;
    private final AuthUtil authUtil;

    @PostMapping("/orders/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod,
                                                  @RequestBody OrderRequestDTO orderRequestDTO) {
        log.debug("OrderController.orderProducts call started with paymentMethod: {} and orderRequestDTO: {}", paymentMethod, orderRequestDTO);
        String emailId = authUtil.loggedInEmail();
        OrderDTO orderDTO = orderService.placeOrder(
                emailId,
                orderRequestDTO.getAddressId(),
                paymentMethod,
                orderRequestDTO.getPgName(),
                orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(),
                orderRequestDTO.getPgResponseMessage()
        );
        log.debug("OrderController.orderProducts call completed with: {}", orderDTO);
        return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
    }

}
