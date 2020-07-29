# Chronic Reader

![chronic reader logo](src/main/resources/static/gold_logo.png)

## Description

This is a server application that you can use to access your 
collection of ebooks and comic books remotely, read them and
keep read progress across multiple devices. The application
is browser based and designed to work well with both desktop
and mobile devices. 

## Features

- supports reading CBR, CBZ and EPUB files
- scan and monitor a library folder for supported file types
- open and display supported file types in browser
- keep track of position user has reached in comic book or ebook
- continue reading from latest position
- keep track of read ebooks and comic books
- organize books and comic books by collections (the subfolders they
are stored in on disk)
- search for comic books and ebooks
- add multiple users
- import or export users and book progress information
- change password functionality

## Prerequisites

Download and install the latest java for your operating system. Once all is done, open a command line and verify that
java is installed correctly with the following command:

```
java -version
```

## Installation instructions

- download latest release from [releases](./releases)
- copy release to folder
- create the `application.properties` file and using a text editor
add the following:

```

```

- open a command line and run: `java -jar `

## Creating users and other admin functions

## Configuration properties

## Resetting everything

## Updating the service

- export users and progress
- stop application (service, jar)
- copy new version
- start application with new version
- same database files should be used, check to see that the users and progress have been maintained
- if data was corrupted, import users and progress saved at first step

## Installing as a service on Windows

## Installing as a service on Linux

## Installing as a service on Mac OS

## Other considerations

- make it available on the web by updating your IP to a domain name
- add https, do https offloading using a server like nginx