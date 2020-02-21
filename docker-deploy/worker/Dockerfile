FROM continuumio/miniconda3

RUN mkdir /worker
WORKDIR /worker

RUN conda install numpy

COPY requirements.txt /tmp/

RUN pip install -r /tmp/requirements.txt

ENV AMQP_URL amqp://guest:guest@rabbitmq-server:5672/%2f
ENV TASK_QUEUE TASK_QUEUE
ENV RES_QUEUE RESULT_QUEUE
ENV KEEP_ALIVE_QUEUE KEEP_ALIVE_QUEUE

COPY geodiff_worker.py  main.py  ./

CMD ["python","main.py"]
