JCC=javac
RMI=rmic
JCFLAGS=

all:  wkl rmi

wkl:  WVM.java WVM_Host.java WVM_Transporter.java Worklet.java WorkletJunction.java
	# Compiling ... 
	$(JCC) -g ${JCFLAGS} *.java http/*.java

rmi: WVM_RMI_Transporter.class
	# Creating rmi stubs/skeletons ... 
	$(RMI) -d ../.. -classpath ../.. psl.worklets.WVM_RMI_Transporter.RTU

dm:    WKL_Demo_Sender.java WKL_Demo_Target.java
	$(JCC) ${JCFLAGS} WKL_Demo_Sender.java WKL_Demo_Target.java

clean:
	# Deleting class files ... 
	rm -f *.class http/*.class

