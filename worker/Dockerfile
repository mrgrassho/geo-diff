FROM continuumio/miniconda3

RUN mkdir /worker
WORKDIR /worker

RUN apt-get update \
  && apt-get install -y build-essential libssl-dev libffi-dev \
  && pip install cryptography \
  && apt-get purge -y build-essential libssl-dev libffi-dev \
                      dpkg-dev fakeroot libfakeroot:amd64 \
  && apt-get autoremove -y \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

RUN conda install geopandas

COPY requirements.txt /tmp/

RUN apt-get update -y && \
    apt-get install -y gcc

RUN pip install -r /tmp/requirements.txt

ENV AMQP_URL amqp://guest:guest@rabbitmq-server:5672/%2f
ENV TASK_QUEUE TASK_QUEUE
ENV RES_QUEUE RESULT_QUEUE

COPY geodiff_worker.py  main.py potrace  ./
