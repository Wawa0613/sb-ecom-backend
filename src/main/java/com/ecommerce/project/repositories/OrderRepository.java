package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Order;
import com.ecommerce.project.payload.OrderDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
