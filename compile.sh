#!/bin/bash

function compile {

	mkdir bin
	javac $(find ./* | grep .java) -d bin
	cp ./client.keys ./bin
	cp ./server.keys ./bin
	cp ./truststore ./bin
}

compile
