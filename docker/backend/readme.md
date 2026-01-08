1. Build image
   docker build -t rupesh1997/fitverse-backend:1.0.1 \
   --build-arg ACTIVE_PROFILE=docker \
   --build-arg PROJECT_VERSION=1.0.1 \
   -f ../docker/backend/Dockerfile .

2. Create network
   docker network create fitverse-network

3. Run mysql docker image
   docker run -d \
   --name mysql \
   -e MYSQL_ROOT_PASSWORD=root \
   -e MYSQL_DATABASE=fitverse \
   -p 3306:3306 \
   --network fitverse-network \
   --health-cmd="mysqladmin ping -h localhost -uroot -proot" \
   --health-interval=10s \
   --health-retries=5 \
   --health-start-period=60s \
   mysql:8.0

4. Run backend docker image
   docker run -d \
   --name fitverse-backend \
   --network fitverse-network \
   -e SPRING_APPLICATION_NAME="fitverse-backend" \
   -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/ fitverse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
   -e SPRING_DATASOURCE_USERNAME="root" \
   -e SPRING_DATASOURCE_PASSWORD="root" \
   -e SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.MySQL8Dialect" \
   -p 9090:9090 \
   rupesh1997/fitverse-backend:1.0.0

