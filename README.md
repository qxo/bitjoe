bitjoe
======

Bitjoe is the gateway to Tradle transaction network based on bitcoin. In this sense it is a first point of contact for new blockchain apps. Bitjoe is a RESTful server, and is called like this:

`http://127.0.0.1:8080?transaction=json-formatted-data&to=pubKey1,pubKey2,...`

(The default port is 8080, you can adjust it in conf/config.json)

Learn about Bitjoe architecture in [Tradle wiki](https://github.com/urbien/Tradle/wiki)

Installation
============

1. Clone this repo from github
2. Copy the conf directory into the build directory
3. Run java -jar bitjoe.jar from the build directory

OR, if you don't need the source code, download and run install.sh
