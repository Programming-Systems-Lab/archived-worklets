@echo off
SET OLD_DIR=%CD%
cd\pslcvs

echo Compiling Worklets
javac -classpath .;c:\pslcvs c:\pslcvs\psl\worklets\*.java c:\pslcvs\psl\worklets\http\*.java

echo Creating stubs for WVM_RMI_Transporter.RTU
rmic psl.worklets.WVM_RMI_Transporter.RTU

cd %OLD_DIR%
