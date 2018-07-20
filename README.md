# Link State Routing Simulation

> This project implements a simulator to the simplified Link-State Routing Protocol with the user space processes in Java.

It simulates the process of generating connection table for each router with one additional heartbeat feature ensures the lost neighbors are detected timely via socket and computes the shortest path from the router to all the other routers using the Dijkstra algorithm.

* Heartbeat mechanism [link](https://en.wikipedia.org/wiki/Heartbeat_(computing))

## Configuration

we have a predefined configuration entry named socs.network.router.ip defining the simulated ip address of the router instance. You can add other entries like the ip address and the port if you prefer.

## Set up

To compile package and program into one jar file
```
mvn compile
mvn compile assembly:single
```
Run the program by
```
java -cp router.jar socs.network.Main conf[\d].conf
```
## How to Use

### Commands
`attach [Process IP] [Process Port] [IP Address] [Link Weight]`
establish a link to the remote router which is identified by [IP Address]

`start`
start this router and initialize the database synchronization process

`connect [Process IP] [Process Port] [IP Address] [Link Weight]`
similar to attach command, but it directly triggers the database synchronization without the necessary to run start

`disconnect [Port Number]`
remove the link between this router and the remote one which is connected at port [Port Number]

`detect [IP Address]`
output the routing path from this router to the destination router which is identified by [IP Address].

`neighbors`
output the IP Addresses of all neighbors of the router where you run this command

`quit`
exit the program. NOTE, this will trigger the synchronization of link state database

## Built With

* Maven [link](https://maven.apache.org/)
