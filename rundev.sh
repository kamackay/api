docker-compose build
docker-compose up -d
while inotifywait -e close_write -r ./; do
    sleep 2
    docker-compose build && docker-compose down -v && docker-compose up -d && docker image prune -f
done
