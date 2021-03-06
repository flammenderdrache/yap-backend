package dev.dragoncave.yap.backend.rest.controllers;

import dev.dragoncave.yap.backend.databasemanagers.EntryController;
import dev.dragoncave.yap.backend.databasemanagers.FileDatabaseController;
import dev.dragoncave.yap.backend.databasemanagers.UserController;
import dev.dragoncave.yap.backend.rest.ProfilePictureStore;
import dev.dragoncave.yap.backend.rest.objects.User;
import dev.dragoncave.yap.backend.rest.security.PasswordUtils;
import dev.dragoncave.yap.backend.rest.security.tokens.DatabaseTokenStore;
import dev.dragoncave.yap.backend.rest.security.tokens.Tokenstore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;

@RestController
@RequestMapping("/user")
public class RestUserController {
	Tokenstore tokenstore = new DatabaseTokenStore();

	@GetMapping()
	public ResponseEntity<?> getUser(@RequestHeader(value = "Token") String token) {
		try {
			if (!tokenstore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			long userId = tokenstore.getUserIdByToken(token);
			return new ResponseEntity<>(UserController.getUserJson(userId), HttpStatus.OK);
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@GetMapping("/entries")
	public ResponseEntity<?> getEntries(@RequestHeader(value = "Token") String token) {
		try {
			if (!tokenstore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			long userId = tokenstore.getUserIdByToken(token);
			return new ResponseEntity<>(EntryController.getUserEntries(userId), HttpStatus.OK);
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PutMapping()
	public ResponseEntity<?> putEntry(@RequestHeader(value = "Token") String token, @RequestBody User user) {
		try {
			if (!tokenstore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			long userID = tokenstore.getUserIdByToken(token);
			if (tokenstore.getUserIdByToken(token) != userID) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			user.setUserID(userID);

			if (user.isInvalid()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			UserController.updateUser(user);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PostMapping(
			consumes = {MediaType.APPLICATION_JSON_VALUE},
			produces = {MediaType.APPLICATION_JSON_VALUE}
	)
	public ResponseEntity<?> newUser(@RequestBody User newUser) {
		try {
			if (newUser.isInvalid()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			if (UserController.getUserIdFromEmailAddress(newUser.getEmailAddress()) != -1) {
				return new ResponseEntity<>("User already exists", HttpStatus.CONFLICT);
			}

			if (!PasswordUtils.isValidPassword(newUser.getPassword())) {
				return new ResponseEntity<>("Invalid Password supplied", HttpStatus.BAD_REQUEST);
			}

			long newUserId = UserController.createUser(newUser);
			return new ResponseEntity<>(newUserId, HttpStatus.CREATED);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PostMapping(
			path = {"/profilePicture"}
	)
	public ResponseEntity<?> uploadProfilePicture(@Nullable @RequestParam(value = "file") MultipartFile multipartFile, @RequestHeader(value = "Token") String token) {
		try {
			if (multipartFile == null || multipartFile.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			long userID = tokenstore.getUserIdByToken(token);
			ProfilePictureStore.storeProfilePicture(multipartFile, userID);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (IllegalArgumentException illegalArgumentException) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@GetMapping("/{userID}/profilePicture")
	public ResponseEntity<?> getProfilePicture(@PathVariable Long userID) {
		try {
			if (!FileDatabaseController.userHasProfilePicture(userID)) {
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			String profilePictureLocation = ProfilePictureStore.getProfilePictureLocation(userID);

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setLocation(URI.create(profilePictureLocation));

			return new ResponseEntity<>(responseHeaders, HttpStatus.TEMPORARY_REDIRECT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@DeleteMapping()
	public ResponseEntity<?> deleteUser(@RequestHeader(value = "Token") String token, @RequestBody HashMap<String, String> requestBody) {
		try {
			if (!tokenstore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			if (!requestBody.containsKey("password")) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			long userId = tokenstore.getUserIdByToken(token);

			if (!UserController.passwordMatches(userId, requestBody.get("password"))) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			tokenstore.invalidateAllUserTokens(userId);
			UserController.deleteUser(userId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (SQLException | NoSuchAlgorithmException exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}