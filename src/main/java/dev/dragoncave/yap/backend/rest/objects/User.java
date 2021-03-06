package dev.dragoncave.yap.backend.rest.objects;

import com.google.gson.Gson;
import dev.dragoncave.yap.backend.rest.UserUtils;

import java.util.Objects;

public class User {
	private long userID = -1;
	private String username;
	private String password;
	private long createDate = -1;
	private long lastLogin = -1;
	private String emailAddress;

	public User() {

	}

	public User(long userid, String username, long createDate, long lastLogin, String emailAddress) {
		this.userID = userid;
		this.username = username;
		this.createDate = createDate;
		this.lastLogin = lastLogin;
		this.emailAddress = emailAddress;
	}

	public long getUserID() {
		return userID;
	}

	public void setUserID(long userID) {
		this.userID = userID;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(long createDate) {
		this.createDate = createDate;
	}

	public long getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String toJson() {
		return this.toString();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	//Hashcode and Equals Methods; Auto-Generated
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof User)) return false;
		User user = (User) o;
		return getUserID() == user.getUserID() && getCreateDate() == user.getCreateDate() && getLastLogin() == user.getLastLogin() && getUsername().equals(user.getUsername()) && getEmailAddress().equals(user.getEmailAddress());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUserID(), getUsername(), getCreateDate(), getLastLogin(), getEmailAddress());
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public boolean isInvalid() {
		return username == null || !UserUtils.emailIsValid(emailAddress);
	}
}