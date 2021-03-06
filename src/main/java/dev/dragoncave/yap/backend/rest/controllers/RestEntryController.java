package dev.dragoncave.yap.backend.rest.controllers;

import dev.dragoncave.yap.backend.databasemanagers.EntryController;
import dev.dragoncave.yap.backend.rest.objects.Entry;
import dev.dragoncave.yap.backend.rest.security.tokens.DatabaseTokenStore;
import dev.dragoncave.yap.backend.rest.security.tokens.Tokenstore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@RequestMapping("/entry")
public class RestEntryController {
	Tokenstore tokenStore = new DatabaseTokenStore();

	@GetMapping("/{entryId}")
	public ResponseEntity<?> getEntry(@RequestHeader(value = "Token") String token, @PathVariable Long entryId) {
		try {
			if (!tokenStore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			if (!EntryController.entryExists(entryId)) {
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			long userID = tokenStore.getUserIdByToken(token);
			if (!EntryController.userHasAccessToEntry(userID, entryId)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			return new ResponseEntity<>(EntryController.getEntryJson(entryId), HttpStatus.OK);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PutMapping("/{entryID}")
	public ResponseEntity<?> putEntry(@RequestHeader(value = "Token") String token, @PathVariable Long entryID, @RequestBody Entry entry) {
		try {
			if (!tokenStore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			if (entry.isInvalid()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			entry.setEntryID(entryID);
			if (!EntryController.entryExists(entryID)) {
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			long userID = tokenStore.getUserIdByToken(token);
			if (!EntryController.userHasAccessToEntry(userID, entryID)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			EntryController.updateEntry(entry);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@DeleteMapping("/{entryId}")
	public ResponseEntity<?> deleteEntry(@RequestHeader(value = "Token") String token, @PathVariable Long entryId) {
		try {
			if (!tokenStore.tokenIsValid(token)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}

			if (!EntryController.entryExists(entryId)) {
				return new ResponseEntity<>("Entry does not exist", HttpStatus.NO_CONTENT);
			}

			long userID = tokenStore.getUserIdByToken(token);
			if (!EntryController.userHasAccessToEntry(userID, entryId)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			EntryController.deleteEntry(entryId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

}