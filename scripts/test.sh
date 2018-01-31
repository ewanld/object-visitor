#! /bin/bash

pushd ..
	mvn test
popd
diff -u ../src/test/resources/TestObjectVisitor-ref.txt ../target/test-classes/TestObjectVisitor-last.txt --color=auto
