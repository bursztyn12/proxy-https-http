# proxy-https-http
Student:18911
Group:14

Additional assumption
1)Handling only HTTPS, HTTP/1.0, HTTP/1.1
2)Labor in "transparent proxy" mode(without browsing / tampering with the content of the message - except for obtaining the FQDN
  server from the Request type query, which also results in handling 400 and 500 error messages)
3)Support for multiple sessions simultaneously (multi-threading)

How it works
1)In the main method of the Proxy class, we create a new object and pass the argument to the constructor
  the value the port number on which the proxy should listen for connections from clients.
2)The proxyOn method from the Proxy class is called to accept connections from clients
  and initiate their service (the result is that we created Socket object for the client session).
  To do this, the ClientHandler class object is created in this method.
  To the constructor of this class, we pass the previously created Socket object used for communication with the client who initiated
  session.
3)The ClinetHandler class implements the Runnable interface, which allows multiple threads to be created concurrently
  performing tasks that were commissioned by individual clients.
4)An object of the ClinetHandler class calls the start method, and the start method starts the run method defined in this class.
5)There are method calls:
-> requestFromClient (): creates an input stream of the InputStream class associated with previously
  said Socket object passed to the construct and receives a request from the client.
  He checks with which method we are dealing with CONNECT or GET (description of their operation in item 6).
-> processConnect (): returns the name and address of the server the client wants to access (with this address and the appropriate port
  the socketProxyServer object of the socket class used to communicate with the server is initialized).
-> processGet (): returns the name and address of the server the client wants to access (with this address and the appropriate port
  the socketProxyServer object of the Socket class used to communicate with the server is initialized).
(The difference between processConnect () and processGet () is the regex used to extract information from 1 customer request line)
-> readHttpsFromClient (): reads the client request, which it immediately sends to the server.
-> readHttpsFromServer (): reads the response from the server, which it immediately sends to the appropriate client.
-> closeStreamsHttpsFromClient (); closes the open socket and input / output streams for communication over HTTPS with the client.
-> closeStreamsHttpsFromServer (); closes an open socket and input / output streams for communication over HTTPS with the server.
-> sendHttpToServer (): sends a request to the server.
-> readHttpFromServer (): reads the response from the server and immediately sends it to the client.
-> closeSocketsAndStreams (): closes open sockets and I/O streams for HTTP communication with client and server.
6)If we are dealing with CONNECT, we first get the address and port number using the processConnect () method and these values
  we initialize the socket (socketProxyServer) used to communicate with the server. Then an output stream (connectionEstablished) is created
  which sends information to the client about the correct connection to the server. Next 2 threads are created, the first is for reading
  from the client, and sending this information to the server, and the other to send responses from the server to the client. After that, the methods are called
  closeStreamsHttpsFromClient () and closeStreamsHttpsFromServer ().
  In the case of a GET method, it is first checked which HTTP version we are dealing with. For HTTP / 1.1, the proxy waits for the client
  terminates the connection (line = readFromClient.readLine ())! = null) and if this happens, the sockets responsible for communication
  with the client and target server and all I / O streams are closed. For HTTP / 1.0, immediately after upload
  to the client HTTP message received from the target server, sockets responsible for communication
  with the client and server and all input / output streams are closed.

For testing this proxy I recommend using firefox browser or curl command(curl -v --proxy ip:port https://www.google.pl)
