# Scala Exercise

This project demonstrates a simple order system in a restaurant.

In this restaurant, there is a server to receive order requests from restaurant staffs. 
For clients, staffs can use the tablet to create, list, query, and delete order.
Server will keep the order before the order have been served.

## Installation

There are two APPs in this project. One is the restaurant server, another is simulated clients.

### Restaurant Server

This server is build by [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html). 
Enter the `sbt shell` and execute `runMain com.github.neofelis.exercise.controller.RestaurantServer` to start.

```shell script
[IJ]sbt:exercise> runMain com.github.neofelis.exercise.controller.RestaurantServer 
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list
[info] running com.github.neofelis.exercise.controller.RestaurantServer 
00:54:08.650 [default-akka.actor.default-dispatcher-5] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
Server online at http://localhost:8080/
Press RETURN to stop...
```

### Staff Client

This client is also build by [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html). 
By sending order requests in parallel to simulate the situation of multiple staffs serve many tables at the same time. 
Enter the `sbt shell` and execute `runMain com.github.neofelis.exercise.StaffClient` to start.

```shell script
[IJ]sbt:exercise> runMain com.github.neofelis.exercise.app.StaffClient
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list
[info] running com.github.neofelis.exercise.app.StaffClient 
00:55:49.858 [default-akka.actor.default-dispatcher-4] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
order: Order(List(Item(2,5), Item(2,19), Item(2,31), Item(2,47), Item(2,6), Item(2,43), Item(2,2), Item(2,40), Item(2,44), Item(2,7))) created: HttpResponse(200 OK,List(Server: akka-http/10.1.12, Date: Wed, 29 Jul 2020 17:55:51 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,13 bytes total),HttpProtocol(HTTP/1.1))
order: Order(List(Item(5,18), Item(5,19), Item(5,48), Item(5,10), Item(5,22), Item(5,45), Item(5,5), Item(5,11), Item(5,6), Item(5,25))) created: HttpResponse(200 OK,List(Server: akka-http/10.1.12, Date: Wed, 29 Jul 2020 17:55:51 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,13 bytes total),HttpProtocol(HTTP/1.1))
order: Order(List(Item(3,6), Item(3,23), Item(3,9), Item(3,18), Item(3,2), Item(3,22), Item(3,13), Item(3,3), Item(3,26), Item(3,1))) created: HttpResponse(200 OK,List(Server: akka-http/10.1.12, Date: Wed, 29 Jul 2020 17:55:51 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,13 bytes total),HttpProtocol(HTTP/1.1))
...
```

### Configuration

This project provides the configuration settings to tweak the behaviors of server and client. 
By editing this file: `src/main/resources/application.conf` we can change the behavior of server, or simulate the different situation of clients.

## Usage

The client APP only provides the simulation of creating orders. 
Because the restaurant server provides RESTful APIs, we can use CLI or Postman to complete other operations, e.x., list order by the table, delete order...etc. 

This project includes the Postman script to test or operate the APIs. Import this file `postman/scala_exercise.postman_collection.json` into Postman:

![import script into postman](https://user-images.githubusercontent.com/13026209/88796842-8ef25080-d1cc-11ea-8573-fdec8c74c5f7.png)

Then we can get the settings corresponding to the four endpoints:

![4 endpoints](https://user-images.githubusercontent.com/13026209/88797117-032cf400-d1cd-11ea-9f41-29a356f0e0f5.png)

### Create Order

An order contains one or many items, and we have to specify the `tableId` and `menuId` for each item.

To create a new order:

```shell script
$ curl --location --request POST 'http://localhost:8080/order' \
--header 'Content-Type: application/json' \
--data-raw '{
    "items": [
        {
            "tableId": 1,
            "menuId": 1
        },
        {
            "tableId": 1,
            "menuId": 2
        },
        {
            "tableId": 1,
            "menuId": 3
        }
    ]
}'
```

### List Order

By specifying the table ID, we can list all waiting items in orders.

```shell script
$ curl --location --request GET 'http://localhost:8080/order/:table_id'
```

If there are any waiting items, the server will return something like this:

```json
[
    {
        "expectedServingAt": 1596015645800,
        "id": "f3a8590e-cc9d-4aa4-85e4-38df47028120",
        "menuId": 48,
        "tableId": 2
    },
    {
        "expectedServingAt": 1596015632690,
        "id": "a3eae248-2916-4486-9451-eaed42cb81cf",
        "menuId": 6,
        "tableId": 2
    },
    {
        "expectedServingAt": 1596015651831,
        "id": "71bed766-4e79-4821-bd08-676f25dfd3fc",
        "menuId": 8,
        "tableId": 2
    }
]
```

### Get the Details of Item

By specifying the table ID and item ID, we can get the details of specific item.

```shell script
$ curl --location --request GET 'http://localhost:8080/order/:table_id/:item_id'
```

If the item is waiting, e.g., item id `07b1a5bf-b6ca-47f6-b3ed-8adf2505890b`, and the server will return something like this:

```json
{
    "expectedServingAt": 1596024453241,
    "id": "07b1a5bf-b6ca-47f6-b3ed-8adf2505890b",
    "menuId": 3,
    "tableId": 1
}
```

### Delete an Item

Before the item have been served, we can delete it. By specifying the `tableId` and `itemId` then we can remove the item from its order.

```shell script
curl --location --request DELETE 'http://localhost:8080/order/:table_id/:item_id'
``` 

## The Idea about Design and Implementation

![system diagram](https://user-images.githubusercontent.com/13026209/88793161-94e53300-d1c6-11ea-9a67-829e64a895c2.png)

### Cache with TTL Mechanism

According to the simple system diagram, the first thing we need is a cache for storing the orders. This cache needs to have the abilities of `thread-safe` and `time-to-live`.
In addition, staffs will manipulate the orders by `table` or `item`, so the random access performance is important.

Here I choose [Scala TrieMap](https://www.scala-lang.org/api/2.12.2/scala/collection/concurrent/TrieMap.html). Base on it I implement the simple, passive TTL mechanism. 
Passive means that there is no agent to clean the expired item time by time. Instead, this cache check the expired item and clean it whenever there is a new request come in.

I use cache here because there is no requirement to persist the orders. So this design can provide a minimal viable product with better performance.
If the requirement changed in the future, we can consider to use `Akka Persistence` or the other kind of database usage.

### RESTful API Server

There are many ways to communicate between the client and server. Here I choose RESTful is because simple and easy to achieve.
To get together with an old friend is comfortable especially there is the time limit.   

### Menu Items

Originally I want to pre-generate the menu items into memory cache. Due to I ran out of time, so I used a simple random generator.

### Client APP

Here I only implement the simulation of bulk order requests in parallel. Because I think it's hard to use if I mix this feature with other operations, i.e., list and delete.
I leave other operations in RESTful APIs, and provide the ready-to-use Postman script. If there is more time I will try to implement a multi-function interactive CLI.

## To Be Improve

* Decouple the route settings with HTTP server.
* Understand more about Akka Actor System.
* Data persistence.
* Testing for the HTTP routes.
* Better client APP.