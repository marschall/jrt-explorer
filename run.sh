#!/bin/bash
#mvn -o -nsu package && \
#cp target/jrt-explorer-1.0.0-SNAPSHOT.jar mods/ && \
java --module-path mods/ -m com.github.marschall.jrtexplorer/com.github.marschall.jrtexplorer.Explorer
# java -modulepath mods/ -m com.github.marschall.jrtexplorer/com.github.marschall.jrtexplorer.Main

