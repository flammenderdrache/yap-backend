# Add Admin to a board

### Mapping /boards/{boardID}/admin

* Method: POST

* Pathvariable: boardID

* Header: "Token $token"

* Requestbody:
    * emailAddress

* Response:
    * HttpStatus (409, 403, 401, 400, 200)

#### Description:

Accepts an email Address and makes the user behind it admin
