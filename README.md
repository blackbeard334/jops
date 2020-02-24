[![Build Status](https://travis-ci.com/blackbeard334/jops.svg?token=bpmypf3quXHRf7DniJui&branch=master)](https://travis-ci.com/blackbeard334/jops)

# JOPS
**J**ava **O**perator Overloading **P**lugin

Originally JOPS stood for "Java Opcodes", but that didn't make sense anymore, so now it stands for **J**ava **O**perator Overloading **P**lugin.

I know I know, it should be called JOOP instead, but I like JOPS better. Oh yeah, the 'S' stands for _Syntactic Sugar Edition_. 


## How to use
- add [@OperatorOverloading](https://github.com/blackbeard334/jops/blob/master/jops_plugin/src/main/java/com/jops/annotation/OperatorOverloading.java) annotation to the class with overloaded operators
- overload the operators with the following C++ syntax:
```java
@OperatorOverloading
public class Bla {       
    public Bla operator+(Bla b) {
        return ...//something..or nothing
    }                 
}
```              
- ...
- write some code, and behold:
```java
public class BlaTest {       
    public static void main(String[]args) {
        Bla a = new Bla();
        Bla b = new Bla();
        Bla c = a + b;
        System.out.println("operator overloading banzai!");
    }                     
}
```
##### Currently only the following **single operand** operators are supported:
1) `operator+`
1) `operator-`
1) `operator*`
1) `operator/`
1) `operator+=`
1) `operator-=`
1) `operator*=`
1) `operator/=`

## How to build/compile
### Maven
- include the _jops_plugin_ as a dependency(we need this for the _@OperatorOverloading_ annotation)
```xml       
<dependencies>
    <dependency>
        <groupId>io.github.blackbeard334</groupId>
        <artifactId>jops_plugin</artifactId>
        <version>${jops_plugin.version}</version>
    </dependency> 
</dependencies>
```
- next you need to add the _jops_plugin_ as a compiler arg, and as a dependency for the _maven-compiler_plugin_
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>11</source>
                <target>11</target>
                <compilerArgs>
                    <arg>-Xplugin:JOPSPlugin</arg>
                </compilerArgs>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>io.github.blackbeard334</groupId>
                    <artifactId>jops_plugin</artifactId>
                    <version>${jops_plugin.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```              
### Mavenless
* `javac -cp /path/jops -Xplugin:JOPSPlugin HelloWorld.java`
    1) first we need to include the _jops_plugin_ in the classpath `-cp /path/jops`
    1) then we tell the compiler that we want to use the _jops_plugin_ with the `-Xplugin:JOPSPlugin` switch
    3) enjoy...  

###### References
* [The Hitchhiker's Guide to javac](https://openjdk.java.net/groups/compiler/doc/hhgtjavac/index.html#source)
* [Java Compiler API - Compile Code to a File and Load it using Default Class Loader](https://www.logicbig.com/tutorials/core-java-tutorial/java-se-compiler-api/compiler-api-string-source.html)
* [ANNOTATION PROCESSING 101](http://hannesdorfmann.com/annotation-processing/annotationprocessing101)
* [Extending Java and Javac](https://blog.blackhc.net/2009/06/extending-java-and-javac/)
* [Creating a Java Compiler Plugin](https://www.baeldung.com/java-build-compiler-plugin)
* [Parser for C and Objective-C.](https://github.com/gcc-mirror/gcc/blob/master/gcc/c/c-parser.c)