rem del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.6\*.class
rem del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.8\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.1\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.2\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_c\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c\*.class


rem -g:none unknown
rem "C:\Program Files (x86)\Java\jdk1.1.6\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.6

rem "C:\Program Files (x86)\Java\jdk1.1.8\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.8


rem default target is 1.1 (45.3)
"C:\Program Files (x86)\Java\jdk1.2.1\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.1
"C:\Program Files (x86)\Java\jdk1.2.1\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.1

"C:\Program Files (x86)\Java\jdk1.2.2\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.2
"C:\Program Files (x86)\Java\jdk1.2.2\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.2


rem default target is 1.1 (45.3)
"C:\Program Files (x86)\Java\jdk1.3\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3
"C:\Program Files (x86)\Java\jdk1.3\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3
"C:\Program Files (x86)\Java\jdk1.3\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3

"C:\Program Files (x86)\Java\jdk1.3.1_20\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20
"C:\Program Files (x86)\Java\jdk1.3.1_20\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20
"C:\Program Files (x86)\Java\jdk1.3.1_20\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20


rem default target is 1.2 (46)
"C:\Program Files (x86)\Java\j2sdk1.4.0\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0
"C:\Program Files (x86)\Java\j2sdk1.4.0\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0
"C:\Program Files (x86)\Java\j2sdk1.4.0\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0
"C:\Program Files (x86)\Java\j2sdk1.4.0\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0

"C:\Program Files (x86)\Java\j2sdk1.4.2_19\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19
"C:\Program Files (x86)\Java\j2sdk1.4.2_19\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19
"C:\Program Files (x86)\Java\j2sdk1.4.2_19\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19
"C:\Program Files (x86)\Java\j2sdk1.4.2_19\bin\javac" -source 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19


"C:\Program Files (x86)\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0
"C:\Program Files (x86)\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0
"C:\Program Files (x86)\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0
"C:\Program Files (x86)\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0
"C:\Program Files (x86)\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0

"C:\Program Files (x86)\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_c
"C:\Program Files (x86)\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_c
"C:\Program Files (x86)\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_c
"C:\Program Files (x86)\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_c
"C:\Program Files (x86)\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_c


"C:\Program Files (x86)\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files (x86)\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files (x86)\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files (x86)\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files (x86)\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files (x86)\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0

"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_c


"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files (x86)\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk7\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0

"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c
"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c
"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c
"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c
"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c
"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c
"C:\Program Files (x86)\Java\jdk1.7.0_10\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk7\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_c


"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk7\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk8\*.java -g:none -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0

"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk7\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c
"C:\Program Files (x86)\Java\jdk1.8.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk8\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.8.0_c


mkdir D:\Data\Decomp\workspace\DecoJerTest\dex\bin

"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk3\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk4\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin

"C:\Program Files (x86)\Java\jdk1.6.0_38\bin\jar" -cf D:\Data\Decomp\workspace\DecoJerTest\dex\classes.jar -C D:\Data\Decomp\workspace\DecoJerTest\dex\bin org/decojer/cavaj/test

rmdir /S/Q D:\Data\Decomp\workspace\DecoJerTest\dex\bin

"D:\Data\Decomp\android-sdk-windows\platform-tools\dx" --dex --output=D:\Data\Decomp\workspace\DecoJerTest\dex\classes.dex D:\Data\Decomp\workspace\DecoJerTest\dex\classes.jar
