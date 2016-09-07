OUTPUT_FOLDER = output
JAR_FOLDER = libs
CLASSPATH = ${OUTPUT_FOLDER}
SRC_DIR = src
MAIN_JAVA = Dandelion.java
MANIFEST_FILE = Dandelion.mf
OUTPUT_JAR = dandelion.jar


${OUTPUT_JAR}: ${OUTPUT_FOLDER} LIB_CLASS_FILES SOURCE_CLASS_FILES ${MANIFEST_FILE}
	cd ${OUTPUT_FOLDER} && jar cfm ${OUTPUT_JAR} ${MANIFEST_FILE} *.class */*.class */**/*.class

${OUTPUT_FOLDER}:
	mkdir $@

LIB_CLASS_FILES: ${OUTPUT_FOLDER}
	sh build-tools/setup-jar.sh ${JAR_FOLDER} ${OUTPUT_FOLDER}

SOURCE_CLASS_FILES: ${OUTPUT_FOLDER}
	javac -classpath ${CLASSPATH} -d ${OUTPUT_FOLDER} -sourcepath ${SRC_DIR} ${SRC_DIR}/${MAIN_JAVA}

${MANIFEST_FILE}: ${OUTPUT_FOLDER}
	sh build-tools/setup-manifest.sh ${OUTPUT_FOLDER}/${MANIFEST_FILE}

clean:
	rm -rf ${OUTPUT_FOLDER}
