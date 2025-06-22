package com.ecommerce.service;

import java.util.List;

import com.ecommerce.model.UserDtls;

public interface UserService {
	
	public UserDtls saveUser(UserDtls user);
	
	public UserDtls getUserByEmail(String email);
	
	public List<UserDtls> getUsers(String role);

	public Boolean updateAccountStatus(Integer id, Boolean status);
	
	public void increaseFailedAttempt(UserDtls user);
	
	public void userAccountLock(UserDtls user);
	
	public boolean unlockAccountTimeExpired(UserDtls user);
	
	public void resetAttempt(int userId);

	public void updateUserResetToken(String email, String resetToken);
	
	public UserDtls getUserByToken(String token);
	
	public UserDtls uptateUser(UserDtls user);
	
	public UserDtls updateuserProfile(UserDtls user);
	
	public UserDtls saveAdmin(UserDtls user);
	
	public Boolean existsEmail(String email);

}
