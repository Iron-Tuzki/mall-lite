$env:JAVA_HOME = "C:\Users\Tuzki\.jdks\corretto-21.0.10"
$env:MAVEN_HOME = "C:\Users\Tuzki\Tools\apache-maven-3.9.15"
$env:M2_HOME = $env:MAVEN_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"
java -version
mvn -version
