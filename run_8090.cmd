@echo off
cd /d "C:\Users\Administrateur\Desktop\Projet Stage\collab-task-api"
mvn -o -q spring-boot:run -Dserver.port=8090 >> "C:\Users\Administrateur\Desktop\Projet Stage\collab-task-api\backend_8090.log" 2>>"C:\Users\Administrateur\Desktop\Projet Stage\collab-task-api\backend_8090.err.log"

