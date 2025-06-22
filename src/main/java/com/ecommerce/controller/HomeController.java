package com.ecommerce.controller;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.CartService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import com.ecommerce.util.CommonUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {
	@Autowired
	private CategoryService categoryService;
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@ModelAttribute
	public void getUserDetails(Principal p,Model m) {
		
		if(p!=null) {
			
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user",userDtls);
			Integer countCart = cartService.getCountcart(userDtls.getId());
			m.addAttribute("countCart",countCart);
		}
		
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("category",allActiveCategory);
		
	}
	
	@GetMapping("/")
	public String index(Model m)
	{
		List<Category> allActiveCategory = categoryService.getAllActiveCategory().stream()
				.sorted((c1,c2)->c2.getId().compareTo(c1.getId()))
				.limit(6).toList();
		List<Product> allActiveProducts = productService.getAllActiveProducts("").stream().
				sorted((p1,p2)->p2.getId().compareTo(p1.getId())).limit(8).toList();
		m.addAttribute("category",allActiveCategory);
		m.addAttribute("products", allActiveProducts);
		
		
		return "index";
	}
	
	
	@GetMapping("/signin")
	public String login()
	{
		return "login";
	}
	
	
	@GetMapping("/register")
	public String register()
	{
		return "register";
	}
	
	@GetMapping("/products")
	public String products(Model m , @RequestParam(value="category",defaultValue = "")String category,
			@RequestParam(name = "pageNo",defaultValue="0") Integer pageNo,
			@RequestParam(name ="pageSize",defaultValue="2") Integer pageSize,@RequestParam(defaultValue = "") String ch)
	{
		List<Category> categories = categoryService.getAllActiveCategory();
//		List<Product> products = productService.getAllActiveProducts(category);
		m.addAttribute("categories", categories);
//		m.addAttribute("products", products);
		m.addAttribute("paramValue",category);
		
		Page<Product> page = null;
		if(StringUtils.isEmpty(ch)) {
		page = productService.getAllActiveProductPagination(pageNo,pageSize,category);
		}else {
			page = productService.searchActiveProductPagination(pageNo,pageSize,category,ch);
		}
		
		List<Product> products = page.getContent();
		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());
		
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		
		
		
		
		return "product";
	}
	
	@GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m)
	{
		Product productById = productService.getProductById(id);
		m.addAttribute("product",productById);
		
		
		return "view_product";
	}
	
//	@PostMapping("/saveUser")
//	public String saveUser(@ModelAttribute UserDtls user,@RequestParam("img")MultipartFile file,HttpSession session)throws IOException{
//		
////		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
////		user.setProfileImage(imageName);
//		UserDtls saveUser = userService.saveUser(user);
//		
//		if(!ObjectUtils.isEmpty(saveUser)) {
//			
//			if(!file.isEmpty()) {
//				
//				File saveFile =  new ClassPathResource("static/img").getFile();
//				
//			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator + file.getOriginalFilename());
//				
//				Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
//				session.setAttribute("succMsg","Registered successfully");
//			}
//			else 
//			{
//				session.setAttribute("errorMsg","something wrong on server");
//			}
//		}
//		
//		return "redirect:/register";
//	}
	
	@PostMapping("/saveUser")
	public String saveUser(@ModelAttribute UserDtls user, HttpSession session) {
	    // Save the user details
	    
	    Boolean existsEmail = userService.existsEmail(user.getEmail());
	    
	    if(existsEmail) {
	    	session.setAttribute("errorMsg","Email already exists");
	    }else {
	    	UserDtls savedUser = userService.saveUser(user);
	    	// Check if the user was successfully saved
		    if (!ObjectUtils.isEmpty(savedUser)) {
		        // Set a success message in the session
		        session.setAttribute("succMsg", "Registered successfully");
		    } else {
		        // Set an error message in the session
		        session.setAttribute("errorMsg", "Something went wrong on the server");
		    }
	    }

	    

	    // Redirect to the registration page
	    return "redirect:/register";
	}
	
	@GetMapping("/forgot_password")
	public String showForgotPassword() {
		
		return "forgot_password";
	}
	
	@PostMapping("/forgot_password")
	public String processForgotPassword(@RequestParam String email , HttpSession session,HttpServletRequest request) throws UnsupportedEncodingException, MessagingException {
		
		UserDtls userByEmail = userService.getUserByEmail(email);
		
		if(ObjectUtils.isEmpty(userByEmail)) {
			session.setAttribute("errorMsg","Invalid email");
		}else {
			
			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email,resetToken);
			
//			Generate url : http://localhost:8080/request-password?token=sfgdbgfswegfdbfewgvseg
			
			String url = CommonUtil.generateUrl(request)+"/reset_password?token="+resetToken;
			
			Boolean sendMail = commonUtil.sendMail(url,email);
			
			if(sendMail) {
				session.setAttribute("succMsg", "Please check your email . Password Reset link sent");
			}else{
				session.setAttribute("errorMsg", "Something wrong on server ! Email not sent");
			}
		}
		
		return "redirect:/forgot_password";
	}
	
//	@PostMapping("/forgot_password")
//	public String processForgotPassword(
//	        @RequestParam String email,
//	        HttpSession session,
//	        HttpServletRequest request) throws UnsupportedEncodingException, MessagingException {
//
//	    try {
//	        UserDtls optionalUser = userService.getUserByEmail(email);
//
//	        if (optionalUser.isPresent(optionalUser)) {
//	            // Generate a secure reset token
//	            String resetToken = UUID.randomUUID().toString();
//
//	            // Update the reset token in the database
//	            userService.updateUserResetToken(email, resetToken);
//
//	            // Generate the reset URL
//	            String resetUrl = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;
//
//	            // Send reset email
//	            boolean emailSent = commonUtil.sendMail(resetUrl, email);
//
//	            if (emailSent) {
//	                session.setAttribute("succMsg", "If this email exists, a password reset link has been sent.");
//	            } else {
//	                session.setAttribute("errorMsg", "Unable to send the email. Please try again later.");
//	            }
//	        } else {
//	            // Generic message to avoid email enumeration
//	            session.setAttribute("succMsg", "If this email exists, a password reset link has been sent.");
//	        }
//	    } catch (Exception ex) {
//	        session.setAttribute("errorMsg", "An unexpected error occurred. Please try again.");
//	    }
//
//	    return "redirect:/forgot_password";
//	}

	
	@GetMapping("/reset_password")
	public String shoeResetPassword(@RequestParam String token, HttpSession session,Model m) {
		
		UserDtls userByToken = userService.getUserByToken(token);
		
		if(userByToken == null)
		{
			m.addAttribute("errorMsg","Your link is invalid or expired");
			return "message";
		}
		m.addAttribute("token",token);
		
		return "reset_password";
	}
	
	
	@PostMapping("/reset_password")
	public String resetPassword(@RequestParam String token,@RequestParam String password, HttpSession session,Model m) {
		
		UserDtls userByToken = userService.getUserByToken(token);
		if(userByToken == null)
		{
			m.addAttribute("errorMsg","Your link is invalid or expired");
			return "message";
		}else {
			userByToken.setPassword(passwordEncoder.encode(password));
			userByToken.setResetToken(null);
			session.setAttribute("succMsg", "Password Changed Successfully");
			m.addAttribute("msg", "Password Changed Successfully");
			return "message";
		}
		
		
		
	}
	
	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch,Model m) {
		
		List<Product> searchProducts = productService.searchProduct(ch);
		m.addAttribute("products", searchProducts);
		 
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("categories", categories);
		
		return "product";
	}


}
