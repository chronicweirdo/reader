sed -i 's/ADMIN_PASSWORD/'$ADMIN_PASSWORD'/' application.properties
sed -i 's/SERVER_PORT/'$SERVER_PORT'/' application.properties
sed -i 's/DEBUG/'$DEBUG'/' application.properties
sed -i 's/LOG_LEVEL/'$LOG_LEVEL'/' application.properties
sed -i 's/ENABLE_FOLDER_WATCHING/'$ENABLE_FOLDER_WATCHING'/' application.properties
sed -i 's/VERIFY_ON_INITIAL_SCAN/'$VERIFY_ON_INITIAL_SCAN'/' application.properties

java -jar reader.jar