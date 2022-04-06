# Penn-CIS555
Here are my CIS555 (Internet and Web Systems) coursework projects in 2022 Spring.

## HW1: Web and Microservice Framework

This homework is composed with 3 Milestones.

In Milestone 0, I

• used an existing application server framework, which helped to understand how modern web frameworks operate;

• used Spark Java (not Apache Spark), which implemented a similar model to Django (Python), Node.js (JavaScript), and many
other similar platforms.

In Milestone 1, I

• implemented a simple HTTP server for static content (i.e., files like images, style
sheets, and HTML pages).
This web server allows me to get a nice, limited-scale introduction to
building a server system as it requires careful attention to concurrency issues and well-designed
programming abstractions to support extensibility.

In Milestone 2, I

• expanded the webserver to handle Web service calls.
Such services were written by attaching handlers (functions called by the Web server) to various routes (URL paths and patterns).
I implemented a microservices framework that emulates the Spark API.

## HW2: Web Crawling and Stream Processing

This homework is composed with 2 Milestones.

In Milestone 1, I

• expanded a dynamic Web application, which runs on the Spark Java framework and allows users to (1) create user identities, (2) define topic-specific "channels" defined by a set of XPath expressions, and (3) to display documents that match a channel;

• implemented and expanded a persistent data store (using Oracle Berkeley DB) to hold retrieved HTML/XML documents and channel definitions;

• fleshed out a crawler that traverses the Web, looking for HTML and XML documents that match one of the patterns.

In Milestone 2, I

• refactored the crawler to fit into a stream processing system's basic abstractions;

• routed documents from the crawler through a stream engine for processing one at a time;

• wrote a state machine-based pattern matcher that determines if an HTML or XML document
matches one of a set of patterns.

## HW3: MapReduce, Parallelism, and Stream Processing with Punctuation
