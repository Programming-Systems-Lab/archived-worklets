JCC=javac

all:	wkl

wkl:	WVM.java WVM_Host.java WVM_Transporter.java Worklet.java WorkletJunction.java
			$(JCC) -g *.java http/*.java

dm:		WKL_Demo_Sender.java WKL_Demo_Target.java
			$(JCC) WKL_Demo_Sender.java WKL_Demo_Target.java

clean:
	rm -f *.class http/*.class

