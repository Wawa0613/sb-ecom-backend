package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    CartRepository cartRepository;
    @Autowired
   private AuthUtil authUtil;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {

        Cart cart=createdCart();

        Product product=productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

      CartItem newCartItem= cartItemRepository.findCartItemByProductIdAndCartId(
                cart.getCartId(),
                productId
        );

      if(newCartItem!=null){
          throw new APIException("product"+product.getProductName()+"already exits in tht cart");
      }

      if(product.getQuantity()==0){
          throw new APIException(product.getProductName()+"is not available in stock");
      }

      if(product.getQuantity()<quantity){
          throw new APIException("Please make an order of the"+product.getProductName()+"less than or equal to"+product.getQuantity());
      }
       newCartItem = new CartItem();
       newCartItem.setProduct(product);
       newCartItem.setCart(cart);
       newCartItem.setQuantity(quantity);
       newCartItem.setDiscount(product.getDiscount());
       newCartItem.setProductPrice(product.getSpecialPrice());
       cartItemRepository.save(newCartItem);
       product.setQuantity(product.getQuantity());
       cart.setTotalPrice(cart.getTotalPrice()+(product.getSpecialPrice()*quantity));
       cartRepository.save(cart);
       CartDTO cartDTO=modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems=cart.getCartItems();

        Stream<ProductDTO>productStream=cartItems.stream().map(item->{
            ProductDTO map= modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });

        cartDTO.setProducts(productStream.toList());


    return cartDTO;

    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart>carts=cartRepository.findAll();
        if(carts.size()==0){
            throw new APIException("No cart exist");
        }
        List<CartDTO>cartDTOS=carts.stream()
                .map(cart -> {
                    CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
                    List<ProductDTO>products=cart.getCartItems().stream()
                            .map(p->modelMapper.map(p.getProduct(),ProductDTO.class))
                            .collect(Collectors.toList());
                    cartDTO.setProducts(products);
                    return cartDTO;
                }).collect(Collectors.toList());

        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart=cartRepository.findCartItemByEmailAndCartId(emailId,cartId);
        if(cart==null){
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }
        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
        cart.getCartItems().forEach(c->c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO>products=cart.getCartItems().stream()
                .map(p->modelMapper.map(p.getProduct(),ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional//
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId= authUtil.loggedInEmail();
        Cart userCart=cartRepository.findCartByEmail(emailId);
        Long cartId=userCart.getCartId();
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()->new ResourceNotFoundException("Cart","cartId",cartId));

        Product product=productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        if(product.getQuantity()==0){
            throw new APIException(product.getProductName()+"is not available in stock");
        }

        if(product.getQuantity()<quantity){
            throw new APIException("Please make an order of the"+product.getProductName()+"less than or equal to"+product.getQuantity());
        }

       CartItem cartItem= cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);
        if(cartItem==null){
            throw new APIException("Product"+product.getProductName()+"does not available in the cart");
        }

        int newQuantity=cartItem.getQuantity()+quantity;
        if(newQuantity<0){
            throw new APIException("The resulting quantity cannot be negative.");
        }
        if (newQuantity > product.getQuantity()) {
            throw new APIException("Sorry, only " + product.getQuantity() + " units of " + product.getProductName() + " are left in stock.");
        }

        if(newQuantity==0){
            deleteProductFromCart(cartId, productId);
        }else {

            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }
        CartItem updateItem=cartItemRepository.save(cartItem);

        if(updateItem.getQuantity()==0){
            cartItemRepository.deleteById(updateItem.getCartItemId());
        }
        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
        List<CartItem>cartItems=cart.getCartItems();

        /*
        cartItems.stream()：将 cartItems 转换为流。
map(item -> {...})：对流中的每个 CartItem 执行映射操作。
使用 modelMapper.map(item.getProduct(), ProductDTO.class) 将 CartItem 中的 Product 转换为 ProductDTO。
设置 ProductDTO 的 quantity 为 CartItem 的 quantity。
productStream.toList()：将映射后的 ProductDTO 流转换为列表。
最终结果是一个包含 ProductDTO 的列表，其中每个 ProductDTO 都包含对应的 Product 信息和数量。
         */

        Stream<ProductDTO>productStream=cartItems.stream().map(item->{
            ProductDTO prd=modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        });
        cartDTO.setProducts(productStream.toList());
        return cartDTO;

    }
    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() -
                (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()->new ResourceNotFoundException("Cart","cartId",cartId));

        Product product=productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));
        CartItem cartItem= cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);
        if(cartItem==null){
            throw new APIException("Product"+product.getProductName()+"does not available in the cart");
        }
        double cartPrice=cart.getTotalPrice()-(cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItem=cartItemRepository.save(cartItem);

    }

    private Cart createdCart(){
        Cart userCart=cartRepository.findCartByEmail(authUtil.loggedInEmail());

        if(userCart!=null){
            return userCart;
        }

        Cart cart=new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart=cartRepository.save(cart);
        return newCart;

    }
}
