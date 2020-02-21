FROM frolvlad/alpine-miniconda3

ENV AMQP_URL amqp://guest:guest@rabbitmq-server:5672/%2f
ENV MAX_TIMEOUT 00:30:00
ENV IMAGE docker-deploy_worker:latest
ENV KEEP_ALIVE_QUEUE KEEP_ALIVE_QUEUE

COPY . .

RUN apk update \
    && conda install pandas \
    && pip install -r requirements.txt

CMD ["python","admin_worker.py"]
