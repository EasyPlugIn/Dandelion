BUILD_OUTPUT = build
JAR_FILES = libs/CSMAPI.jar:libs/DANAPI.jar:libs/json-20160212.jar:libs/processing/core.jar
CLASSPATH = lib-classes/
SRC_DIR = src
MAIN_FILE = Dandelion.java
MANIFEST_FILE = Dandelion.mf

all: classes jar

classes:
	# javac -classpath libs/CSMAPI.jar:libs/DANAPI.jar:libs/json-20160212.jar:libs/processing/core.jar: -d build src/*.java
	mkdir $(BUILD_OUTPUT)
	javac -classpath $(CLASSPATH) -d $(BUILD_OUTPUT) -sourcepath $(SRC_DIR) $(SRC_DIR)/$(MAIN_FILE)

jar:
	echo "Manifest-Version: 1.0" > $(BUILD_OUTPUT)/$(MANIFEST_FILE)
	echo "Class-Path: ." >> $(BUILD_OUTPUT)/$(MANIFEST_FILE)
	echo "Main-Class: Dandelion" >> $(BUILD_OUTPUT)/$(MANIFEST_FILE)
	cp -r lib-classes/* $(BUILD_OUTPUT)
	cd $(BUILD_OUTPUT) && jar cfm output2.jar Dandelion.mf *.class */*.class */**/*.class

clean:
	rm -rf build
