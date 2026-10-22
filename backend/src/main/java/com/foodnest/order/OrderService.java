package com.foodnest.order;

import com.foodnest.cart.CartCalculationItemResponse;
import com.foodnest.cart.CartCalculationResponse;
import com.foodnest.cart.CartService;
import com.foodnest.common.BadRequestException;
import com.foodnest.common.ForbiddenException;
import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.customer.Customer;
import com.foodnest.customer.CustomerRepository;
import com.foodnest.delivery.AssignmentStatus;
import com.foodnest.delivery.DeliveryAssignment;
import com.foodnest.delivery.DeliveryAssignmentDto;
import com.foodnest.delivery.DeliveryAssignmentRepository;
import com.foodnest.delivery.DeliveryAssignmentService;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.dish.Dish;
import com.foodnest.dish.DishRepository;
import com.foodnest.payment.PaymentRecord;
import com.foodnest.payment.PaymentRepository;
import com.foodnest.restaurant.Restaurant;
import com.foodnest.restaurant.RestaurantRepository;
import com.foodnest.security.UserPrincipal;
import com.foodnest.security.UserRole;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryManRepository deliveryManRepository;
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CustomerRepository customerRepository,
                        RestaurantRepository restaurantRepository,
                        DishRepository dishRepository,
                        DeliveryAssignmentRepository assignmentRepository,
                        DeliveryManRepository deliveryManRepository,
                        PaymentRepository paymentRepository,
                        CartService cartService,
                        DeliveryAssignmentService deliveryAssignmentService,
                        SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.assignmentRepository = assignmentRepository;
        this.deliveryManRepository = deliveryManRepository;
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public OrderDto createOrder(UserPrincipal principal, CreateOrderRequest req) {
        if (principal == null || principal.getRole() != UserRole.CUSTOMER) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Only authenticated customers can place food orders.");
        }
        Customer customer = customerRepository.findById(principal.getId())
                .orElseGet(() -> customerRepository.findByEmail(principal.getUsername())
                        .orElseThrow(() -> new ResourceNotFoundException("Customer account not found for: " + principal.getUsername())));

        return createOrderForCustomer(customer, req);
    }

    @Transactional
    public OrderDto createOrder(Long customerId, CreateOrderRequest req) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return createOrderForCustomer(customer, req);
    }

    private OrderDto createOrderForCustomer(Customer customer, CreateOrderRequest req) {
        // Recheck dish availability, pricing, single-restaurant rule via server CartService
        CartCalculationResponse cartCalc = cartService.calculateCart(req.getItems());

        Restaurant restaurant = restaurantRepository.findById(cartCalc.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Order order = new Order();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        order.setOrderNumber("ORD-" + dateStr + "-" + randSuffix);

        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setCustomerNotes(req.getCustomerNotes());
        order.setSubtotal(cartCalc.getSubtotal());
        order.setTaxAmount(cartCalc.getTaxAmount());
        order.setDeliveryFee(cartCalc.getDeliveryFee());
        order.setTotalAmount(cartCalc.getTotalAmount());
        order.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH_ON_DELIVERY");
        order.setPaymentStatus("PENDING");

        for (CartCalculationItemResponse itemCalc : cartCalc.getItems()) {
            Dish dish = dishRepository.findById(itemCalc.getDishId()).orElse(null);
            if (dish != null) {
                // Decrement stock
                int remaining = Math.max(0, dish.getAvailableStock() - itemCalc.getQuantity());
                dish.setAvailableStock(remaining);
                dishRepository.save(dish);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setDish(dish);
            orderItem.setDishName(itemCalc.getName());
            orderItem.setUnitPrice(itemCalc.getUnitPrice());
            orderItem.setQuantity(itemCalc.getQuantity());
            orderItem.setItemTotal(itemCalc.getItemTotal());
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Initial Delivery Assignment placeholder
        DeliveryAssignment initialAssignment = new DeliveryAssignment();
        initialAssignment.setOrder(savedOrder);
        initialAssignment.setStatus(AssignmentStatus.AWAITING_ASSIGNMENT);
        assignmentRepository.save(initialAssignment);

        // Record Initial Payment Record
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrder(savedOrder);
        paymentRecord.setAmount(savedOrder.getTotalAmount());
        paymentRecord.setPaymentMethod(savedOrder.getPaymentMethod());
        paymentRecord.setPaymentStatus("PENDING");
        paymentRecord.setTransactionReference("COD-" + savedOrder.getOrderNumber());
        paymentRepository.save(paymentRecord);

        OrderDto orderDto = enrichOrderDto(savedOrder);

        // Broadcast to restaurant orders channel
        try {
            messagingTemplate.convertAndSend("/topic/restaurants/" + restaurant.getId() + "/orders", orderDto);
        } catch (Exception ignored) {
        }

        return orderDto;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getCustomerOrdersByPrincipal(UserPrincipal principal) {
        if (principal == null || principal.getRole() != UserRole.CUSTOMER) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Customer account required.");
        }
        return getCustomerOrders(principal.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::enrichOrderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getCustomerOrderById(Long customerId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or does not belong to your account."));
        return enrichOrderDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getRestaurantOrders(Long restaurantId, OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(restaurantId, status);
        } else {
            orders = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        }
        return orders.stream()
                .map(this::enrichOrderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getRestaurantOrderById(Long restaurantId, Long orderId) {
        Order order = orderRepository.findByIdAndRestaurantId(orderId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or does not belong to your restaurant."));
        return enrichOrderDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatusByRestaurant(Long restaurantId, Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndRestaurantId(orderId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or does not belong to your restaurant."));

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException("Illegal order status transition from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.PICKED_UP || newStatus == OrderStatus.OUT_FOR_DELIVERY || newStatus == OrderStatus.DELIVERED) {
            throw new ForbiddenException("Restaurant cannot mark an order as " + newStatus + ". This action is reserved for delivery partners.");
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // Automatic delivery partner assignment when restaurant ACCEPTS or marks food READY!
        if (newStatus == OrderStatus.ACCEPTED || newStatus == OrderStatus.READY) {
            deliveryAssignmentService.assignDeliveryMan(savedOrder);
        }

        OrderDto dto = enrichOrderDto(savedOrder);

        try {
            messagingTemplate.convertAndSend("/topic/orders/" + savedOrder.getId(), dto);
        } catch (Exception ignored) {
        }

        return dto;
    }

    @Transactional
    public OrderDto cancelOrderByPrincipal(UserPrincipal principal, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isAuthorized = false;
        if (principal.getRole() == UserRole.CUSTOMER && order.getCustomer().getId().equals(principal.getId())) {
            isAuthorized = true;
        } else if (order.getCustomer() != null && order.getCustomer().getEmail().equalsIgnoreCase(principal.getUsername())) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            throw new ForbiddenException("Order does not belong to your account.");
        }

        return cancelOrderInternal(order);
    }

    @Transactional
    public OrderDto cancelOrderByCustomer(Long customerId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or does not belong to your account."));
        return cancelOrderInternal(order);
    }

    private OrderDto cancelOrderInternal(Order order) {
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("Order cannot be cancelled in state: " + order.getStatus() + ". Only newly placed orders can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Restore stock
        for (OrderItem item : savedOrder.getItems()) {
            if (item.getDish() != null) {
                Dish d = item.getDish();
                d.setAvailableStock(d.getAvailableStock() + item.getQuantity());
                dishRepository.save(d);
            }
        }

        OrderDto dto = enrichOrderDto(savedOrder);
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + savedOrder.getId(), dto);
        } catch (Exception ignored) {
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByIdForAuthorizedUser(Long orderId, UserPrincipal principal) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isAuthorized = false;
        if (principal.getRole() == UserRole.CUSTOMER && order.getCustomer().getId().equals(principal.getId())) {
            isAuthorized = true;
        } else if (order.getCustomer() != null && order.getCustomer().getEmail().equalsIgnoreCase(principal.getUsername())) {
            isAuthorized = true;
        } else if (principal.getRole() == UserRole.RESTAURANT && order.getRestaurant().getId().equals(principal.getId())) {
            isAuthorized = true;
        } else if (principal.getRole() == UserRole.DELIVERY_MAN) {
            Optional<DeliveryAssignment> assignmentOpt = assignmentRepository.findByOrderId(orderId);
            if (assignmentOpt.isPresent()) {
                DeliveryAssignment da = assignmentOpt.get();
                if ((da.getDeliveryMan() != null && da.getDeliveryMan().getId().equals(principal.getId()))
                        || (da.getStatus() == AssignmentStatus.AWAITING_ASSIGNMENT)) {
                    isAuthorized = true;
                }
            }
        }

        if (!isAuthorized) {
            throw new ForbiddenException("Access denied: You are not authorized to view this order.");
        }

        return enrichOrderDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatusByDeliveryMan(Long deliveryManId, Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No delivery assignment found for order id: " + orderId));

        if (assignment.getDeliveryMan() == null || !assignment.getDeliveryMan().getId().equals(deliveryManId)) {
            throw new ForbiddenException("Access denied: You are not the assigned delivery partner for this order.");
        }

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException("Illegal order status transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.PICKED_UP) {
            assignment.setStatus(AssignmentStatus.PICKED_UP);
            assignment.setPickedUpAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            assignment.setStatus(AssignmentStatus.ACCEPTED);
        } else if (newStatus == OrderStatus.DELIVERED) {
            assignment.setStatus(AssignmentStatus.DELIVERED);
            assignment.setDeliveredAt(LocalDateTime.now());

            // Release delivery man availability and touch lastActiveAt
            DeliveryMan rider = assignment.getDeliveryMan();
            rider.setIsAvailable(true);
            rider.setLastActiveAt(LocalDateTime.now());
            deliveryManRepository.save(rider);

            // Mark COD payment paid
            order.setPaymentStatus("PAID");
            List<PaymentRecord> payments = paymentRepository.findByOrderId(order.getId());
            for (PaymentRecord pr : payments) {
                pr.setPaymentStatus("PAID");
                paymentRepository.save(pr);
            }

            // Retry any waiting assignments
            deliveryAssignmentService.retryAwaitingAssignments();
        }

        assignmentRepository.save(assignment);
        Order savedOrder = orderRepository.save(order);

        OrderDto dto = enrichOrderDto(savedOrder);
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + savedOrder.getId(), dto);
        } catch (Exception ignored) {
        }

        return dto;
    }

    private OrderDto enrichOrderDto(Order order) {
        OrderDto dto = OrderDto.fromEntity(order);
        assignmentRepository.findByOrderId(order.getId())
                .ifPresent(assignment -> dto.setDeliveryAssignment(DeliveryAssignmentDto.fromEntity(assignment)));
        return dto;
    }
}
