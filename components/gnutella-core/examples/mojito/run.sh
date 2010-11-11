#!/bin/bash

ulimit -n 1024
PATH_SEPARATOR=":"
ROOT="../../../.."
MOJITO="mojito-ui"


CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}."
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${ROOT}/core"
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${ROOT}/lib/messagebundles"

PLATFORM=`uname`
case ${PLATFORM} in 
    CYGWIN*)
        PATH_SEPARATOR=";"        
    ;;
    *)
esac


COMPONENTS="${ROOT}/components"
for COMPONENT in $(ls ${COMPONENTS}); do
	if [ -d "${COMPONENTS}/${COMPONENT}/build/classes" ]
	then
		CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${COMPONENTS}/${COMPONENT}/build/classes"
		CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${COMPONENTS}/${COMPONENT}/src/main/resources"

	    if [ ${COMPONENT} = $MOJITO ]; then
        CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${COMPONENTS}/${COMPONENT}/build/misc"
    	fi
	fi
done

for JAR in $(find ${ROOT}/lib/jars -name *.jar); do 
   CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${JAR}"
done

for JAR in $(find ${COMPONENTS}/mojito-ui/misc/lib -name *.jar); do 
   CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${JAR}"
done

export CLASSPATH
javac org/limewire/mojito/Main.java
java -ea org.limewire.mojito.Main $*

exit 0
