#! /bin/bash
# chmod +x runserver.sh
# ./runserver.sh
# for vm deployment: #! /bin/bash
                     #export GOOGLE_APPLICATION_CREDENTIALS=/var/server/<service key>.json
                     #java -jar /var/server/Server.jar 8000
export GOOGLE_APPLICATION_CREDENTIALS=<service key>.json
java -jar Server.jar 8000
