# UCD-onbarding

Change params for seed-ucd.groovy

Create top level Resource and update topLevelGroupName

Create Component template with a dummy deployment process called "Deploy"

groovy -cp lib/http-builder-0.7.2.jar:lib/dependencies/* seed-ucd.groovy

Troubleshooting:
If you get: 

Caught: java.lang.NoSuchMethodError: org.codehaus.groovy.runtime.DefaultGroovyMethods.leftShift(Ljava/io/Writer;Ljava/lang/Object;)Ljava/io/Writer;
java.lang.NoSuchMethodError: org.codehaus.groovy.runtime.DefaultGroovyMethods.leftShift(Ljava/io/Writer;Ljava/lang/Object;)Ljava/io/Writer;

...use and older Groovy such as 2.5.3"

http://groovy-lang.org/download.html

groovy-2.5.3/bin/groovy -cp lib/http-builder-0.7.2.jar:lib/dependencies/* seed-ucd.groovy
