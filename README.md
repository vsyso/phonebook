# Phonebook REST API

Allows to perform basic operations with a Phonebook via **GET**, **POST**, **PUT** and **DELETE** methods, there is a list of supported operations:

 - `GET /contacts` lists all contacts and phone numbers from the database 
 - `GET /contact/{id}` lists specific contact and phone number by id 
 - `GET /contacts/find_by_number?{phone_number}&match` lists contacts by a phone number or its part, depending on `match` paramenter which is optional but set to false by default. 
 - `POST /contact` accepts XML or JSON body with `firstName` and `lastName` nodes to create a new contact, returns an url to the created contact in the Location header.
 - `POST /contact/{id}/add_number` accepts XML or JSON body with `number` and `type` nodes to add a phone number to specific contact by id.
  - `PUT /contact/{id}` accepts XML or JSON body with `firstName` and/or `lastName` nodes to update specific contact by id or creates one if the contact cannot be found by provided id, returns an url to newly created contact in the Location header.
  - `DELETE /contact/{id}` removes a contact by id
  - `DELETE /contact/{id}/{phone_number}` removes a phone number from specific contact by id
  
The projects contains three implemenations of the API:
  - Spring MVC
  - Jersey
  - Servlet

### Spring MVC implementation

Technologies used:
- Spring Web MVC framework
- MySQL via Hibernate
- Jackson Project for JSON data binding
- JUnit tests with RestTemplate web client

Spring implementation uses Hibernate for ORM data access, all CRUD operations preform via PhonebookService class.

### Jersey implementation

Technologies used:
- Jersey RESTful Web Services framework
- MySQL via EclipseLink
- JUnit tests with Apache HttpClient and Hamcrest framework (common for both Jersey and Servlet)

All CRUD operations preform via PhonebookService class.

### Serlvet implementation

Technologies used:
- Java Servlet API
- MySQL via EclipseLink
- JUnit tests with Apache HttpClient and Hamcrest framework (common for both Jersey and Servlet)

Servlet uses JAXBContext as entry point to JAXB API via JAXBMapper class to marshal and unmarshal data.
All CRUD operations preform via PhonebookService class.

----

Jersey and Servlet implemenations placed into single project an separated by packages `org.syso.phonebook.controllers.jersey` and `org.syso.phonebook.controllers.servlet` accordingly.
Also Jersey facade can be accessed from {BASE_URL}/jersey and Servlet from {BASE_URL}/servlet.
They are both use common PhonebookService class for CRUD operations.
Unit tests are common for both implementations and run with different URLs.

License
----

MIT
