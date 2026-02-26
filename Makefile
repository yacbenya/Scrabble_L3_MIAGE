JC = javac
JCFLAGS = -d build -sourcepath src -encoding UTF-8
JVM = java
JVMFLAGS = -ea -classpath build
ENTRY = view.Main

compile:
	mkdir -p build
	$(JC) $(JCFLAGS) $(shell find src -name "*.java")

run: compile
	$(JVM) $(JVMFLAGS) $(ENTRY)

clean:
	rm -rf build