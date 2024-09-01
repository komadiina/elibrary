# elibrary

an online library system with GUI applications for managing members, books, and book reservations, utilizing RESTful endpoints, secure socket communication, and multicast messaging. It includes separate applications for library members and book suppliers, with functionalities for book proposals, orders, and chat communication.

### Java
- **JavaFX**: Used for building the GUI applications for the library, members, and suppliers. JavaFX provides a rich set of UI controls and multimedia capabilities.
- **Java RMI (Remote Method Invocation)**: Used for communication between the supplier and the accounting service. RMI allows invoking methods on remote objects.
- **Java Sockets**: Used for traditional socket communication between the library and suppliers, as well as for secure socket communication between library clients.
- **Java MulticastSocket**: Used for sending and receiving multicast messages for book proposals and notifications.
- **Java SSL/TLS**: Used for secure socket communication to ensure encrypted data transfer between clients and servers.

### Libraries
- **JSON (org.json)**: Used for parsing and generating JSON data, which is commonly used in RESTful APIs and socket communication.
- **SendGrid**: Used for sending emails to users with information about downloaded books. The API key and email origin are configured in the properties file.
- **Redis**: Used as the database for storing book information. Redis is a fast, in-memory key-value store.
- **Loggers (java.util.logging)**: Used for logging exceptions and other significant events in the application.

### Configuration and Properties
- **Properties Files**: Used for storing configuration settings such as file paths, API keys, and server ports. This allows for easy modification of settings without changing the code.
- **XML**: Used for storing user account information in [`users.xml`].

### Project Structure
- **Maven/Gradle**: The project structure suggests the use of a build automation tool like Maven or Gradle, although specific files are not listed.
- **Eclipse/IntelliJ IDEA**: The presence of [`.classpath`], [`.project`], and [`.idea`] directories indicates that the project is set up to be compatible with Eclipse and IntelliJ IDEA IDEs.

### Example Files and Directories
- **[`src/`]**: Contains the source code for the application, including controllers, models, middleware, and server classes.
- **[`resources/`]**: Contains configuration files, properties files, and other resources like book text files and SSL certificates.
- **[`WebContent/`]**: Likely contains web-related resources, possibly for a web-based component of the application.

### Example Code Excerpts
- **[`SupplierServerThread.java`]**: Handles socket communication with suppliers.
- **[`AccountingInterface.java`]**: Defines the RMI interface for the accounting service.
- **[`project.properties`]**: Contains various configuration settings for the application.
- **[`librarian-screen.fxml`]** Defines the layout for the librarian's GUI screen using JavaFX.

This combination of technologies and libraries provides a robust and scalable foundation for the online library system, ensuring secure communication, efficient data handling, and a rich user interface.
