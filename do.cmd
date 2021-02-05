@echo off

if [%1] == [help] (
  echo  t       test plugin
  echo  b       compile
  echo  c       clean
  echo  i       install plugin's jar, sources and javadoc in local repository
  echo  w $V    set or upgrade Maven wrapper to version $V
  echo  cv      check plugins and dependencies versions
  echo  uv      update plugins and dependencies versions
  echo  dt      show dependencies tree
  echo  pub     build, sign, then upload to Maven Central
)

if [%1] == [t] (
  mvnw --no-transfer-progress --show-version clean verify
)
if [%1] == [b] (
  mvnw clean compile -DskipTests
)
if [%1] == [c] (
  mvnw clean
)
if [%1] == [i] (
  mvnw clean install -DskipTests
)
if [%1] == [w] (
  mvn -N io.takari:maven:wrapper -Dmaven=%2
)
if [%1] == [cv] (
  mvnw versions:display-property-updates
)
if [%1] == [uv] (
  mvnw versions:update-properties
)
if [%1] == [dt] (
  mvnw dependency:tree
)
if [%1] == [pub] (
  mvn clean release:prepare release:perform -P ossrh
)
