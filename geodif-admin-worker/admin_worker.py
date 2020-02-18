#!/usr/bin/env python
from datetime import datetime,date, time, timedelta
import pika
import docker
import random
import threading
import pandas
import json

# Constants
max_timeout = '00:10:00'  # 10 segundos
last_timestamps = dict()  # ultimos keep alives de cada worker |  par  id_container : timestamp
#last_timestamps = {233333: '13/02/2020 18:24:00', 4455555: '14/02/2020 18:25:00', 667777: '14/02/2020 18:25:50'}
queue = "KEEP_ALIVE_QUEUE"
image = "dockerdeploy_worker:latest"
host = "localhost"

""" Procedures -------------------------------------------------------------------------------- """

def callback_keep_alive_queue(ch, method, properties, body):
    data = json.loads(body)
    # if last_timestamps.get(data['id'], 'none') == 'none':  # si nunca recibio mensajes de ese container, lo agrega.
    last_timestamps[data['id']] = data['timestamp']


def calculate_timeout_workers():
    # Calculo si algun worker lleva demasiado tiempo sin responder
    while (True):  # recorro array con workers y sus tiempos.
        for id_container in last_timestamps:
            timeout_worker = datetime.strptime(last_timestamps[id_container], '%d/%m/%Y %H:%M:%S')
            now = datetime.now()
            diff = now - timeout_worker
            print('diferencia ' + str(diff))
            if diff > pandas.to_timedelta(max_timeout): # si la diferencia de tiempo es mayor a MAX_TIMEOUT
                container = client.containers.get(id_container)
                container.stop()
                name_worker = prox_worker()
                new_container = client.containers.run(image, name=name_worker, tty=True, detach=True, stdin_open=True)
                # last_timestamps[new_container.attrs['id']] = str(datetime.now())  # actualizo lista de containers.


def prox_worker():
    return 'worker_' + str(len(last_timestamps))


""" ------------------------------------------------------------------------------------------- """

# Create docker client
client = docker.from_env()
connection = pika.BlockingConnection(pika.ConnectionParameters(host=host))
channel = connection.channel()

# Create the queue and start consuming
keep_alive_queue = channel.queue_declare(queue=queue)
channel.basic_consume(queue=queue, on_message_callback=callback_keep_alive_queue, auto_ack=False)

# En un hilo escucha los mensajes de la cola y en el principal, compara los timers.
thread1 = threading.Thread(target=channel.start_consuming)
thread1.start()
calculate_timeout_workers()


# print(' Administrador de Workers UP ')
