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

1. Download the latest release from [releases](../../releases).
2. Copy the release JAR to a folder on disk. This is where the application will start in and will create the database
files.
3. Create an `application.properties` file and, using a text editor, configure your app as described in the code below.
Set the `<library_folder>` to an address on disk where your ebook and comic book collection is. Set a strong
`<administrator_password>`, you will use this for the first login and creating users for your application. Set
a `<port>` that is unoccupied on your machine, a good option is `8080`.

```
library.location=<library_folder>
adminPass=<administrator_password>
server.port=<port>
```

4. Start the application by opening a command line and running: `java -jar reader-<version>.jar` (be sure to
replace `version` with the version you downloaded).

## Creating users and other admin functions

Once you have started the application, you can navigate in a browser to `localhost:<port>` address (use the port you
configured). There you should see the login page. Log in with the `admin` username and the `<administrator_password>`
you configured in the properties file.

<video src="instruction_resources/create_first_users.mp4" controls></video>

On the main page, click the settings button. In the settings screen, click on "Import data". This is the administration
functions screen. Only the admin user has access to this screen. You can create the users
you need for your app in this screen by writing a username, a comma and a password for each user you want to create,
on different lines. The users will have the option to change their password to what they prefer.

## Configuration properties

- `keepEbookStyles=false`
    - It is recommended to set the `keepEbookStyles` setting to false, to get a more consistent experience with ebooks,
    but you can experiment with this and change it to true.

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