del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.6\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.8\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.1\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.2\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_22\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_29\*.class

del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0\*.class
del /S/Q D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_01\*.class


"C:\Program Files\Java\jdk1.1.6\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.6

"C:\Program Files\Java\jdk1.1.8\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.1.8


rem default target is 1.1 (45.3)
"C:\Program Files\Java\jdk1.2.1\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.1
"C:\Program Files\Java\jdk1.2.1\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.1

"C:\Program Files\Java\jdk1.2.2\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.2
"C:\Program Files\Java\jdk1.2.2\bin\javac" -target 1.2 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.2.2


rem default target is 1.1 (45.3)
"C:\Program Files\Java\jdk1.3\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3
"C:\Program Files\Java\jdk1.3\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3

"C:\Program Files\Java\jdk1.3.1_20\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20
"C:\Program Files\Java\jdk1.3.1_20\bin\javac" -target 1.3 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.3.1_20


rem default target is 1.2 (46)
"C:\Program Files\Java\j2sdk1.4.0\bin\javac" -target 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0
"C:\Program Files\Java\j2sdk1.4.0\bin\javac" -target 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.0

"C:\Program Files\Java\j2sdk1.4.2_19\bin\javac" -target 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19
"C:\Program Files\Java\j2sdk1.4.2_19\bin\javac" -target 1.4 D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_j2sdk1.4.2_19


"C:\Program Files\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0
"C:\Program Files\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0
"C:\Program Files\Java\jdk1.5.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0

"C:\Program Files\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_22
"C:\Program Files\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_22
"C:\Program Files\Java\jdk1.5.0_22\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.5.0_22


"C:\Program Files\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0
"C:\Program Files\Java\jdk1.6.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0

"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_29
"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_29
"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_29
"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.6.0_29


"C:\Program Files\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0
"C:\Program Files\Java\jdk1.7.0\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk7\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0

"C:\Program Files\Java\jdk1.7.0_01\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_01
"C:\Program Files\Java\jdk1.7.0_01\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_01
"C:\Program Files\Java\jdk1.7.0_01\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_01
"C:\Program Files\Java\jdk1.7.0_01\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_01
"C:\Program Files\Java\jdk1.7.0_01\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk7\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\bin_jdk1.7.0_01


mkdir D:\Data\Decomp\workspace\DecoJerTest\dex\bin

"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk2\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk5\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin
"C:\Program Files\Java\jdk1.6.0_29\bin\javac" D:\Data\Decomp\workspace\DecoJerTest\src\org\decojer\cavaj\test\jdk6\*.java -g -d D:\Data\Decomp\workspace\DecoJerTest\dex\bin

"C:\Program Files\Java\jdk1.6.0_29\bin\jar" -cf D:\Data\Decomp\workspace\DecoJerTest\dex\classes.jar -C D:\Data\Decomp\workspace\DecoJerTest\dex\bin org/decojer/cavaj/test

rmdir /S/Q D:\Data\Decomp\workspace\DecoJerTest\dex\bin

"D:\Data\Decomp\android-sdk-windows\platform-tools\dx" --dex --output=D:\Data\Decomp\workspace\DecoJerTest\dex\classes.dex D:\Data\Decomp\workspace\DecoJerTest\dex\classes.jar
