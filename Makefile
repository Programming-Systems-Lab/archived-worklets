# java related stuff
JCC=javac
RMI=rmic
JCFLAGS=

# LaTeX compilation stuff
name=
SRC = ${name}.tex
TEX=latex
DVIPSFLAGS=-K


all:  wkl rmi

wkl:  WVM.java WVM_Host.java WVM_Transporter.java Worklet.java WorkletJunction.java
	# Compiling ... 
	$(JCC) -g ${JCFLAGS} *.java http/*.java WVMRSL/*.java

rmi: WVM_RMI_Transporter.class
	# Creating rmi stubs/skeletons ... 
	$(RMI) -d ../.. -classpath ../.. psl.worklets.WVM_RMI_Transporter.RTU psl.worklets.RTU_RegistrarImpl

dm:    WKL_Demo_Sender.java WKL_Demo_Target.java
	$(JCC) ${JCFLAGS} WKL_Demo_Sender.java WKL_Demo_Target.java

docs: *.java
	javadoc -private *.java http/*.java WVMRSL/*.java ../probelets/Probeable.java

# LaTeX compilation stuff
ps: ${name}.ps
dvi: ${name}.dvi
pdf: ${name}.pdf

%.ps: %.dvi; dvips $(DVIPSFLAGS) $* -o
# %.pdf: %.tex %.dvi; pdflatex $*
%.pdf: %.ps; ps2pdf $< $@

clean:
	# Deleting class files ... 
	rm -f *.class http/*.class WVMRSL/*.class ${name}.{dvi,ps,log,aux}
