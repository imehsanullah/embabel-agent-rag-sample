
# Compiling and running the software
cd agent
mvn -o -Dmaven.repo.local=../.m2 -DskipTests compile
mvn -o -Dmaven.repo.local=../.m2 spring-boot:run

# To ingest the documents from the data directory
curl -u jettro:password -X POST http://localhost:8080/ingest
