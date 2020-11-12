#!/bin/bash

let "nextParam = 1"
for ((cmd = 1; cmd <= $#; cmd++)) do

    (( nextParam++ ))

    case "${!cmd}" in

    "help")
      echo ""
      echo  "Helper: (tip: you can chain parameters, e.g.: \"./do w 3.6.0 c t\")"
      echo  ""
      echo  "t            test plugin"
      echo  "b            compile"
      echo  "c            clean"
      echo  "i            install plugin's jar, sources and javadoc in local repository"
      echo  "w \$V         set or upgrade Maven wrapper to version \$V"
      echo  "cv           check plugins and dependencies versions"
      echo  "uv           update plugins and dependencies versions"
      echo  "dt           show dependencies tree"
      ;;

    "t")
      sh ./mvnw verify
      ;;

    "b")
      sh ./mvnw compile -DskipTests
      ;;

    "c")
      sh ./mvnw clean
      ;;

    "i")
      sh ./mvnw install -DskipTests
      ;;

    "w")
      mvn -N io.takari:maven:wrapper -Dmaven=${!nextParam}
      ;;

    "cv")
      sh ./mvnw versions:display-property-updates
      ;;

    "uv")
      sh ./mvnw versions:update-properties
      ;;

    "dt")
      sh ./mvnw dependency:tree
      ;;

    esac

done
