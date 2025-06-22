package com.ecommerce.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.model.Cart;
import com.ecommerce.model.Category;
import com.ecommerce.model.OrderRequest;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.CartService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import com.ecommerce.util.CommonUtil;
import com.ecommerce.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CategoryService categoryService;
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	
	
	@GetMapping("/")
	public String home() {
		
		return "user/home";
		
	}

	@ModelAttribute
	public void getUserDetails(Principal p,Model m) {
		if(p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user",userDtls);
			Integer countCart = cartService.getCountcart(userDtls.getId());
			m.addAttribute("countCart",countCart);
		}
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("category",allActiveCategory);
	}
	
	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid,@RequestParam Integer uid,HttpSession session) {
		
		Cart saveCart = cartService.saveCart(pid, uid);
		
		if(ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg","Product add to cart failed" );
			
		}else {
			session.setAttribute("succMsg", "Product added to cart");
		}
		
		
		return "redirect:/product/" + pid;
	}
	
	
	  @GetMapping("/cart")
	  public String loadCartPage(Principal p, Model m) {
	  
	  UserDtls user = getLoggedInUserDetails(p); List<Cart> carts =
	  cartService.getCartsByUser(user.getId());
	  
	  m.addAttribute("carts", carts); if(carts.size() > 0) { Double
	  totalOrderAmount = carts.get(carts.size()-1).getTotalOrderAmount();
	  m.addAttribute("totalOrderAmount",totalOrderAmount); }
	  
	  return "/user/cart";
	 
	
	
	}

	
	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy,@RequestParam Integer cid)
	{
		 cartService.updateQuantity(sy,cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		
		
		return userDtls;
	}
	
	@GetMapping("/orders")
	public String orderPage(Principal p , Model m) {
		
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts",carts);
		if(carts.size() > 0) {
			Double orderPrice = carts.get(carts.size()-1).getTotalOrderAmount();
			Double totalOrderPrice= carts.get(carts.size()-1).getTotalOrderAmount()+ 25 +10;
			m.addAttribute("orderPrice",orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		
		
		return "/user/order";
	}
	
 
	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p) throws Exception {
		
		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveOrder(user.getId(), request);
		System.out.println("inuserColtroller="+request.toString());
		
		return "redirect:/user/success";
	}
	
	@GetMapping("/success")
	public String loadSuccess() {
		
		
		return "/user/success";
	}
	
	@GetMapping("/user-orders")
	public String myOrders(Model m ,Principal p) {
		
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());
		m.addAttribute("orders",orders);
		
		
		return "/user/my_orders";
	}
	
	@GetMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id,@RequestParam Integer st,HttpSession session) {
		
		OrderStatus[] values = OrderStatus.values();
		String status = null;
		
		for(OrderStatus orderSt : values) {
			if(orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}
		}
		
		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
		
		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		
		if(!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		}else {
			session.setAttribute("errorMsg", "status not updated");
		}
		
		return "redirect:/user/user-orders";
	}
	
	@GetMapping("/profile")
	public String profile() {
		
		return "/user/profile";
	}
	
	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user,HttpSession session) {
		
		UserDtls updateuserProfile = userService.updateuserProfile(user);
		
		if(ObjectUtils.isEmpty(updateuserProfile))
		{
			session.setAttribute("errorMsg", "Profile not updated");
		}else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		
		return "redirect:/user/profile";
	}
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword,@RequestParam String currentpassword,Principal p,HttpSession session) {
		
		UserDtls loggedInUserDtls = getLoggedInUserDetails(p);
		
		boolean matches = passwordEncoder.matches(currentpassword, loggedInUserDtls.getPassword());
		
		if(matches) {
			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDtls.setPassword(encodePassword);
			UserDtls updateUser = userService.uptateUser(loggedInUserDtls);
			
			if(ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated !! Error in server");
				
			}else {
				session.setAttribute("succMsg", "Current Password incorrect");
			}
			
		}else {
			session.setAttribute("errorMsg", "Current Password incorrect");
		}
		
		return "redirect:/user/profile";
	}
	

}
