@echo off

if [%1] == [help] (
  echo  t       test
  echo  b       compile
  echo  c       clean
  echo  p       package
  echo  w $V    set or upgrade Maven wrapper to version $V
  echo  cv      check plugins and dependencies versions
  echo  uv      update plugins and dependencies versions
  echo  dt      show dependencies tree
)

if [%1] == [t] (
  mvnw clean verify
)
if [%1] == [b] (
  mvnw clean compile -DskipTests -T1
)
if [%1] == [c] (
  mvnw clean
)
if [%1] == [p] (
  mvnw clean package -DskipTests -T1
)
if [%1] == [w] (
  mvn -N io.takari:maven:wrapper -Dmaven=%2
)
if [%1] == [cv] (
  mvnw versions:display-property-updates -U
)
if [%1] == [uv] (
  mvnw versions:update-properties -U -P
)
if [%1] == [dt] (
  mvnw dependency:tree
)
