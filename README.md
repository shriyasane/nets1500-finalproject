# Kalshi Market Graph
Without Maven - 
To compile: javac $(find src/main/java -name "*.java")
To run: java -cp src/main/java edu.upenn.nets1500.kalshi.App

Currently, we have a working similarity component that can compare any two Market objects and produce a normalized score in [0.0, 1.0]. This is what will be used when building edges in the graph objects.