# Delete user

### Mapping /user

* Method: DELETE 

* Header: "Token $token"

* Requestbody:
    * Json:
        * password

* Response:
    * HttpStatus (401, 200)

#### Description:

Takes in a token to identify the user to delete it. Requires the password of the user.