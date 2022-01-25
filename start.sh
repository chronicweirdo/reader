sed -i 's/ADMIN_PASSWORD/'$ADMIN_PASSWORD'/' application.properties
sed -i 's/SERVER_PORT/'$SERVER_PORT'/' application.properties
sed -i 's/DEBUG/'$DEBUG'/' application.properties
sed -i 's/LOG_LEVEL/'$LOG_LEVEL'/' application.properties
sed -i 's/SCANNER_WORKERS/'$SCANNER_WORKERS'/' application.properties

java -XX:ActiveProcessorCount=$PROCESSORS -Dcom.sun.media.imageio.disableCodecLib=true -jar reader.jar