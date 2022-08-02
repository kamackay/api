image=docker.keith.sh/api:$1

docker build . --file ./Dockerfile -t $image && \
    docker push $image && \
    kubectl --context do-nyc3-keithmackay-cluster -n webpage \
    set image deployment/api api=$image

sleep 2
ATTEMPTS=0
ROLLOUT_STATUS_CMD="kubectl --context do-nyc3-keithmackay-cluster -n webpage rollout status deployment/api"
until $ROLLOUT_STATUS_CMD || [ $ATTEMPTS -eq 60 ]; do
  $ROLLOUT_STATUS_CMD
  ATTEMPTS=$((ATTEMPTS + 1))
  sleep 2
done

ECHO "Successfully deployed" $1