function compile {

	mkdir bin
	javac $(find ./src/* | grep .java) -d bin
	cp -r ./src/TestFiles ./bin
}

compile
