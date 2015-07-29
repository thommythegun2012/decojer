set DecoJerTest=D:\Data\Decomp\workspace\DecoJerTest
set Java=D:\Data\Decomp\Java

rd /S/Q %DecoJerTest%\bin_tests\jdk1.1.3
md %DecoJerTest%\bin_tests\jdk1.1.3
rem -g:none unknown
"%Java%\jdk1.1.3\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -d %DecoJerTest%\bin_tests\jdk1.1.3

rd /S/Q %DecoJerTest%\bin_tests\jdk1.1.8
md %DecoJerTest%\bin_tests\jdk1.1.8
"%Java%\jdk1.1.8\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.1.8


rd /S/Q %DecoJerTest%\bin_tests\jdk1.2.1
md %DecoJerTest%\bin_tests\jdk1.2.1
rem default target is 1.1 (45.3)
"%Java%\jdk1.2.1\bin\javac" -target 1.2 %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.2.1
"%Java%\jdk1.2.1\bin\javac" -target 1.2 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.2.1

rd /S/Q %DecoJerTest%\bin_tests\jdk1.2.2
md %DecoJerTest%\bin_tests\jdk1.2.2
"%Java%\jdk1.2.2\bin\javac" -target 1.2 %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.2.2
"%Java%\jdk1.2.2\bin\javac" -target 1.2 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\jdk1.2.2


rd /S/Q %DecoJerTest%\bin_tests\jdk1.3
md %DecoJerTest%\bin_tests\jdk1.3
rem default target is 1.1 (45.3)
"%Java%\jdk1.3\bin\javac" -target 1.3 %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.3
"%Java%\jdk1.3\bin\javac" -target 1.3 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.3
"%Java%\jdk1.3\bin\javac" -target 1.3 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.3

rd /S/Q %DecoJerTest%\bin_tests\jdk1.3.1_20
md %DecoJerTest%\bin_tests\jdk1.3.1_20
"%Java%\jdk1.3.1_20\bin\javac" -target 1.3 %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.3.1_20
"%Java%\jdk1.3.1_20\bin\javac" -target 1.3 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\jdk1.3.1_20
"%Java%\jdk1.3.1_20\bin\javac" -target 1.3 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_tests\jdk1.3.1_20


rd /S/Q %DecoJerTest%\bin_tests\j2sdk1.4.0
md %DecoJerTest%\bin_tests\j2sdk1.4.0
rem default target is 1.2 (46)
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\j2sdk1.4.0
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\j2sdk1.4.0
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_tests\j2sdk1.4.0
"%Java%\j2sdk1.4.0\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_tests\j2sdk1.4.0

rd /S/Q %DecoJerTest%\bin_tests\j2sdk1.4.2_19
md %DecoJerTest%\bin_tests\j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_tests\j2sdk1.4.2_19
"%Java%\j2sdk1.4.2_19\bin\javac" -source 1.4 %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_tests\j2sdk1.4.2_19


rd /S/Q %DecoJerTest%\bin_tests\jdk1.5.0
md %DecoJerTest%\bin_tests\jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.5.0
"%Java%\jdk1.5.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.5.0

rd /S/Q %DecoJerTest%\bin_tests\jdk1.5.0_c
md %DecoJerTest%\bin_tests\jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_tests\jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_tests\jdk1.5.0_c
"%Java%\jdk1.5.0_22\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_tests\jdk1.5.0_c


rd /S/Q %DecoJerTest%\bin_tests\jdk1.6.0
md %DecoJerTest%\bin_tests\jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.6.0
"%Java%\jdk1.6.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk6\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.6.0

rd /S/Q %DecoJerTest%\bin_tests\jdk1.6.0_c
md %DecoJerTest%\bin_tests\jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_tests\jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_tests\jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_tests\jdk1.6.0_c
"%Java%\jdk1.6.0_45\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\bin_tests\jdk1.6.0_c


rd /S/Q %DecoJerTest%\bin_tests\jdk1.7.0
md %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk6\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0
"%Java%\jdk1.7.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk7\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.7.0

rd /S/Q %DecoJerTest%\bin_tests\jdk1.7.0_c
md %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c
"C:\Program Files\Java\jdk1.7.0_80\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk7\*.java -g -d %DecoJerTest%\bin_tests\jdk1.7.0_c


rd /S/Q %DecoJerTest%\bin_tests\jdk1.8.0
md %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk6\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk7\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0
"%Java%\jdk1.8.0\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk8\*.java -g:none -d %DecoJerTest%\bin_tests\jdk1.8.0

rd /S/Q %DecoJerTest%\bin_tests\jdk1.8.0_c
md %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk2\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk3\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk4\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk5\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk6\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk7\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c
"C:\Program Files\Java\jdk1.8.0_51\bin\javac" %DecoJerTest%\src_tests\org\decojer\cavaj\test\jdk8\*.java -g -d %DecoJerTest%\bin_tests\jdk1.8.0_c


"%Java%\jdk1.6.0_45\bin\jar" cf %DecoJerTest%\bin_tests\jdk1.6.0_c\classes.jar -C %DecoJerTest%\bin_tests\jdk1.6.0_c .
"D:\Data\Decomp\android-sdk\build-tools\20.0.0\dx" --dex --output=%DecoJerTest%\bin_tests\jdk1.6.0_c\classes.dex %DecoJerTest%\bin_tests\jdk1.6.0_c\classes.jar
del %DecoJerTest%\bin_tests\jdk1.6.0_c\classes.jar
