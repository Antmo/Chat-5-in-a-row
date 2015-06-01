# chat
Chat client including a game of five-in-a-row for a university course.

Instructions below, courtesy of https://www.ida.liu.se/~TDTS04/labs/2015/Chat/lab5.html

How to compile and run: 
% make target
% make clobber
% make idl
/usr/bin/idlj -fall Chat.idl
% make c
/usr/bin/javac ChatClient.java ChatApp/
*.java
% make s
/usr/bin/javac ChatServer.java ChatApp/*.java

Start the name server in the first window. The example below assumes that you have chosen port number 1057.

% make orbd
orbd -ORBInitialPort 1057 -ORBInitialHost
localhost
Start the chat server in the second terminal window:

% make server
/usr/bin/java ChatServer -ORBInitialPort 1057
-ORBInitialHost localhost
ChatServer ready and waiting ...
Finally, start up the chat client in the third window:

% make client
/usr/bin/java ChatClient -ORBInitialPort 1057
-ORBInitialHost localhost
Hello....
....Goodbye!
