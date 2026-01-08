```shell
docker ps
docker images
docker compose -f docker/docker-compose/production/docker-compose.yml --env-file .env up

docker container prune -f
docker image prune -f
docker volume prune -f
docker network prune -f
docker system prune -f
docker system prune -a -f

docker compose -f docker/docker-compose/docker/docker-compose.yaml down
docker compose -f docker/docker-compose/default/docker-compose.yaml up -d

docker exec -it <frontend_container_name> /bin/sh
curl -X POST http://fit-verse-backend:8181/api/v1.0.0/cms/users/authenticate -H "Content-Type: application/json" -d '{"username": "dulal.rupesh@gmail.com", "password": "Rupesh@123"}'
curl -X POST http://localhost:8080/api/users/authenticate -H "Content-Type: application/json" -d '{"username": "dulal.rupesh@gmail.com", "password": "Rupesh@123"}'
docker exec -it 6ce8c223f8ec  curl -X POST http://fit-verse-backend:8181/api/v1.0.0/cms/users/authenticate -H "Content-Type: application/json" -d '{"username": "dulal.rupesh@gmail.com", "password": "Rupesh@123"}'
```