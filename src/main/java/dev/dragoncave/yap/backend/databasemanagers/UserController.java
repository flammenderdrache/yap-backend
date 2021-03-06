package dev.dragoncave.yap.backend.databasemanagers;

import com.google.gson.Gson;
import dev.dragoncave.yap.backend.databasemanagers.connections.ConnectionController;
import dev.dragoncave.yap.backend.rest.objects.User;
import dev.dragoncave.yap.backend.rest.security.PasswordUtils;

import java.security.NoSuchAlgorithmException;
import java.sql.*;

@SuppressWarnings("SqlResolve")
public class UserController {
	private UserController() {

	}

	public static void updateLastLoginTime(long user_id, long newTime) throws SQLException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement updateTime = dbcon.prepareStatement(
						"UPDATE users SET last_login = ? WHERE user_id = ?"
				)
		) {
			updateTime.setLong(1, newTime);
			updateTime.setLong(2, user_id);
			updateTime.execute();
		}
	}

	public static void updateLastLoginTime(long user_id) throws SQLException {
		updateLastLoginTime(user_id, System.currentTimeMillis());
	}

	public static boolean passwordMatches(String email_address, String password) throws SQLException, NoSuchAlgorithmException {
		long user_id = getUserIdFromEmailAddress(email_address);
		return passwordMatches(user_id, password);
	}

	//TODO Optimize into single query
	public static boolean passwordMatches(long user_id, String password) throws SQLException, NoSuchAlgorithmException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement getPasswordStatement = dbcon.prepareStatement(
						"SELECT password FROM users WHERE user_id = ?"
				);
				PreparedStatement getSaltStatement = dbcon.prepareStatement(
						"SELECT salt FROM password_salts WHERE user_id = ?"
				)

		) {
			getPasswordStatement.setLong(1, user_id);
			getSaltStatement.setLong(1, user_id);

			try (
					ResultSet passwordResultSet = getPasswordStatement.executeQuery();
					ResultSet saltResultSet = getSaltStatement.executeQuery()
			) {
				if (passwordResultSet.next() && saltResultSet.next()) {
					String passwordHash = passwordResultSet.getString("password");
					String base64Salt = saltResultSet.getString("salt");
					return PasswordUtils.isExpectedPassword(password, base64Salt, passwordHash);
				}
			}

		}
		return false;
	}

	//TODO use single query
	public static void updatePassword(long user_id, String newPassword) throws SQLException, NoSuchAlgorithmException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement changeUserPasswordStatement = dbcon.prepareStatement(
						"UPDATE users SET password = ? WHERE user_id = ?"
				);
				PreparedStatement changeSaltStatement = dbcon.prepareStatement(
						"UPDATE password_salts SET salt = ? WHERE user_id = ?"
				)
		) {
			String newSalt = PasswordUtils.getBase64Salt();
			String newPasswordHash = PasswordUtils.getHash(newPassword, newSalt);

			changeUserPasswordStatement.setString(1, newPasswordHash);
			changeUserPasswordStatement.setLong(2, user_id);

			changeSaltStatement.setString(1, newSalt);
			changeSaltStatement.setLong(2, user_id);

			changeUserPasswordStatement.execute();
			changeSaltStatement.execute();
		}
	}

	public static User getUserFromEmailAddress(String email_address) throws SQLException {
		return getUserByID(getUserIdFromEmailAddress(email_address));
	}

	public static long getUserIdFromEmailAddress(String email_address) throws SQLException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement getIdStatement = dbcon.prepareStatement(
						"SELECT user_id FROM users WHERE email_address = LOWER(?)"
				)
		) {

			getIdStatement.setString(1, email_address);

			try (ResultSet userIdResultSet = getIdStatement.executeQuery()) {
				if (userIdResultSet.next()) {
					return userIdResultSet.getLong("user_id");
				}
			}
		}
		return -1;
	}

	public static void updateUser(User user) throws SQLException {
		User oldUser = getUserByID(user.getUserID());
		String username = user.getUsername();
		String emailAddress = user.getEmailAddress();

		if (username == null) {
			username = oldUser.getUsername();
		}
		if (emailAddress == null) {
			emailAddress = oldUser.getEmailAddress();
		}

		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement statement = dbcon.prepareStatement(
						"UPDATE users " +
								"SET username = ?, email_address = LOWER(?)" +
								"WHERE user_id = ?"
				)
		) {
			statement.setString(1, username);
			statement.setString(2, emailAddress);
			statement.setLong(3, user.getUserID());

			statement.execute();
		}
	}

	//returns the ID of the just created user or -1 if something went wrong
	public static long createUser(String username, String password, long create_date, long last_login, String email_address) throws SQLException, NoSuchAlgorithmException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement userInsertionStatement = dbcon.prepareStatement(
						"INSERT INTO users (username, password, create_date, last_login, email_address)" +
								"VALUES (?, ?, ?, ?, LOWER(?))",
						Statement.RETURN_GENERATED_KEYS //Add to prepared statement as second argument next to the sql because we otherwise don't get the generated ID unlike with MySQL or SQLITE
				);
				PreparedStatement passwordSaltInsertionStatement = dbcon.prepareStatement(
						"INSERT INTO password_salts VALUES(?, ?)"
				)
		) {

			String salt = PasswordUtils.getBase64Salt();
			String hashedPassword = PasswordUtils.getHash(password, salt);

			userInsertionStatement.setString(1, username);
			userInsertionStatement.setString(2, hashedPassword);
			userInsertionStatement.setLong(3, create_date);
			userInsertionStatement.setLong(4, last_login);
			userInsertionStatement.setString(5, email_address);
			userInsertionStatement.execute();

			long newUserId = 0;

			try (ResultSet newUserIdResultSet = userInsertionStatement.getGeneratedKeys()) {
				if (newUserIdResultSet.next()) {
					newUserId = newUserIdResultSet.getLong(1);
				}
			}

			if (newUserId != 0) {
				passwordSaltInsertionStatement.setLong(1, newUserId);
				passwordSaltInsertionStatement.setString(2, salt);
				passwordSaltInsertionStatement.execute();
				return newUserId;
			}

			return -1;
		}
	}

	public static long createUser(User newUser) throws SQLException, NoSuchAlgorithmException {
		return createUser(newUser.getUsername(), newUser.getPassword(), System.currentTimeMillis(), System.currentTimeMillis(), newUser.getEmailAddress());
	}

	public static boolean userExists(long user_id) throws SQLException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement statement = dbcon.prepareStatement(
						"SELECT user_id, username, create_date, last_login, email_address" +
								" FROM users" +
								" WHERE user_id = ?"
				)
		) {
			statement.setLong(1, user_id);
			statement.execute();

			try (ResultSet resultSet = statement.getResultSet()) {
				return resultSet.next();
			}
		}
	}

	public static User getUserByID(long user_id) throws SQLException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement statement = dbcon.prepareStatement("SELECT * FROM users WHERE user_id = ?")

		) {
			statement.setLong(1, user_id);

			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return new User(
							resultSet.getInt("user_id"),
							resultSet.getString("username"),
							resultSet.getLong("create_date"),
							resultSet.getLong("last_login"),
							resultSet.getString("email_address")
					);
				}
			}
		}
		return null;
	}

	public static void deleteUser(long user_id) throws SQLException {
		try (
				Connection dbcon = ConnectionController.getConnection();
				PreparedStatement statement = dbcon.prepareStatement(
						"DELETE FROM users WHERE user_id = ?"
				)
		) {
			statement.setLong(1, user_id);
			statement.execute();
		}
	}

	public static String getUserJson(long user_id) throws SQLException {
		Gson gson = new Gson();
		return gson.toJson(getUserByID(user_id));
	}
}