package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {
     @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id=?1 and ci.product.id=?2")
     CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);
   // @Transactional// 开启事务：方法内的数据库操作要么全部成功，要么全部失败回滚
     @Modifying// 标记为修改操作：用于执行更新或删除语句
     @Query("DELETE FROM CartItem ci WHERE ci.cart.id = ?1 AND ci.product.id = ?2")
     void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);
     @Modifying
     @Query("DELETE FROM CartItem ci WHERE ci.cart.id = ?1")
     List<Cart> deleteAllByCartId(Long cartId);
}
