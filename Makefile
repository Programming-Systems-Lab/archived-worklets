#
# Makefile for psl.worklets
# 

CLASSES=Worklet.class \
	SerializedWorklet.class \
	WVM.class \
	WVMInteract.class \
	GenericConsole.class \
	WVMConsole.class \
	WVMInterpreter.class \
	WorkletEventQueue.class

#	examples/testdriver/TestDriver.class \
#	rmi/IWVM.class \
#	rmi/IWVMimpl.class \
#	rmi/WVMauth.class \
#	rmi/WVMrmi.class

NETCLASSES=net/WVMTransport.class \
           net/WVMTransportException.class \
	   net/WorkletSender.class \
	   net/WorkletReceiver.class \
	   net/WorkletSendMode.class \
	   net/WorkletReceiveMode.class \
	   net/GenericReceiveMode.class \
	   net/GenericSendMode.class \
	   net/HTTPReceiveMode.class \
	   net/HTTPSendMode.class \
	   net/NoReceiveModesException.class \
	   net/NoSendModesException.class \
	   net/ReceiverException.class \
	   net/SendFailureException.class 


all: $(CLASSES) net

#rmi: rmi\IWVMimpl_Skel.class
#
#rmi\IWVMimpl_Skel.class: rmi\IWVMimpl.class
#	cd rmi
#	C:\jdk1.2\bin\rmic -classpath .;C:\jdk1.2\jre\lib\rt.jar;$(CLASSPATH) psl.worklets.rmi.IWVMimpl

net: $(NETCLASSES)

include ../../Makefiles/Makefile.conf
