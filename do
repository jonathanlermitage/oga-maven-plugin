#!/bin/bash

let "nextParam = 1"
for ((cmd = 1; cmd <= $#; cmd++)) do

    (( nextParam++ ))

    case "${!cmd}" in

    "help")
      echo ""
      echo  "Helper: (tip: you can chain parameters, e.g.: \"./do w 3.6.0 c t\")"
      echo  ""
      echo  "t            test"
      echo  "b            compile"
      echo  "c            clean"
      echo  "p            package"
      echo  "w \$V         set or upgrade Maven wrapper to version \$V"
      echo  "cv           check plugins and dependencies versions"
      echo  "uv           update plugins and dependencies versions"
      echo  "dt           show dependencies tree"
      ;;

    "t")
      sh ./mvnw verify
      ;;

    "b")
      sh ./mvnw compile -DskipTests -T1
      ;;

    "c")
      sh ./mvnw clean
      ;;

    "p")
      sh ./mvnw package -DskipTests -T1
      ;;

    "w")
      mvn -N io.takari:maven:wrapper -Dmaven=${!nextParam}
      ;;

    "cv")
      sh ./mvnw versions:display-property-updates -U -P coverage,jib,mig,spotbugs
      ;;

    "uv")
      sh ./mvnw versions:update-properties -U -P coverage,jib,mig,spotbugs
      ;;

    "dt")
      sh ./mvnw dependency:tree
      ;;

    esac

done
