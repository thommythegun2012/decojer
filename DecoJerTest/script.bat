set DecoJerTest=D:\Data\Decomp\workspace\DecoJerTest
set Java=D:\Data\Decomp\Java

del /S/Q %DecoJerTest%\bin_jdk1.1.3\*.class
del /S/Q %DecoJerTest%\bin_jdk1.1.8\*.class

del /S/Q %DecoJerTest%\bin_jdk1.2.1\*.class
del /S/Q %DecoJerTest%\bin_jdk1.2.2\*.class

del /S/Q %DecoJerTest%\bin_jdk1.3\*.class
del /S/Q %DecoJerTest%\bin_jdk1.3.1_20\*.class

del /S/Q %DecoJerTest%\bin_j2sdk1.4.0\*.class
del /S/Q %DecoJerTest%\bin_j2sdk1.4.2_19\*.class

del /S/Q %DecoJerTest%\bin_jdk1.5.0\*.class
del /S/Q %DecoJerTest%\bin_jdk1.5.0_c\*.class

del /S/Q %DecoJerTest%\bin_jdk1.6.0\*.class
del /S/Q %DecoJerTest%\bin_jdk1.6.0_c\*.class

del /S/Q %DecoJerTest%\bin_jdk1.7.0\*.class
del /S/Q %DecoJerTest%\bin_jdk1.7.0_c\*.class

del /S/Q %DecoJerTest%\bin_jdk1.8.0\*.class
del /S/Q %DecoJerTest%\bin_jdk1.8.0_c\*.class


rem -g:none unknown
"%Java%\jdk1.1.3\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -d %DecoJerTest%\bin_jdk1.1.3

"%Java%\jdk1.1.8\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.1.8


rem default target is 1.1 (45.3)
"%Java%\jdk1.2.1\bin\javac" -target 1.2 %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_jdk1.2.1
"%Java%\jdk1.2.1\bin\javac" -target 1.2 %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_jdk1.2.1

"%Java%\jdk1.2.2\bin\javac" -target 1.2 %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.2.2
"%Java%\jdk1.2.2\bin\javac" -target 1.2 %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_jdk1.2.2


rem default target is 1.1 (45.3)
"%Java%\jdk1.3\bin\javac" -target 1.3 %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_jdk1.3
"%Java%\jdk1.3\bin\javac" -target 1.3 %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_jdk1.3
"%Java%\jdk1.3\bin\javac" -target 1.3 %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_jdk1.3

"%Java%\jdk1.3.1_20\bin\javac" -target 1.3 %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.3.1_20
"%Java%\jdk1.3.1_20\bin\javac" -target 1.3 %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_jdk1.3.1_20
"%Java%\jdk1.3.1_20\bin\javac" -target 1.3 %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_jdk1.3.1_20


rem default target is 1.2 (46)
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_j2sdk1.4.0
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_j2sdk1.4.0
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_j2sdk1.4.0
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_j2sdk1.4.0

"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_j2sdk1.4.2_19


"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_jdk1.5.0

"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_jdk1.5.0_c


"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g:none -d %DecoJerTest%\bin_jdk1.6.0

"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\bin_jdk1.6.0_c


"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk7\*.java -g:none -d %DecoJerTest%\bin_jdk1.7.0

"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_51\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk7\*.java -g -d %DecoJerTest%\bin_jdk1.7.0_c


"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk7\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk8\*.java -g:none -d %DecoJerTest%\bin_jdk1.8.0

"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk7\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk8\*.java -g -d %DecoJerTest%\bin_jdk1.8.0_c


mkdir %DecoJerTest%\dex\bin

"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\dex\bin
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\dex\bin
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\dex\bin
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\dex\bin
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\dex\bin
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\dex\bin

"%Java%\jdk1.6.0_45\bin\jar" -cf %DecoJerTest%\dex\classes.jar -C %DecoJerTest%\dex\bin org/decojer/cavaj/test

rmdir /S/Q %DecoJerTest%\dex\bin

"D:\Data\Decomp\android-sdk-windows\build-tools\19.0.1\dx" --dex --output=%DecoJerTest%\dex\classes.dex %DecoJerTest%\dex\classes.jar
