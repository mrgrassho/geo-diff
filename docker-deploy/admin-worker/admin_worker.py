#!/usr/bin/env python
from datetime import datetime, date, timedelta
import time
import pika
import docker
import random
import threading
import pandas
import json
import os


class AdminWorker(object):

    def __init__(self, reconection_time=10, prefetch_count=1, debug=True):
        self._amqp_url = os.environ['AMQP_URL']
        self._docker_image = os.environ['IMAGE']
        self._keep_alive_queue = os.environ['KEEP_ALIVE_QUEUE']
        self._reconection_time = reconection_time # 10 seconds
        self._last_timestamps = dict()
        self._max_timeout = os.environ['MAX_TIMEOUT']
        self._prefetch_count = prefetch_count
        self._connection = None
        self._channel = None
        self._docker_client = docker.from_env()
        self._debug = debug


    def callback_keep_alive_queue(ch, method, properties, body):
        data = json.loads(body)
        # if LAST_TIMESTAMPS.get(data['id'], 'none') == 'none':  # si nunca recibio mensajes de ese container, lo agrega.
        self._last_timestamps[data['id']] = data['timestamp']


    def calculate_timeout_workers():
        # Calculo si algun worker lleva demasiado tiempo sin responder
        while (True):  # recorro array con workers y sus tiempos.
            for id_container in self._last_timestamps:
                timeout_worker = datetime.strptime(self._last_timestamps[id_container], '%d/%m/%Y %H:%M:%S')
                now = datetime.now()
                diff = now - timeout_worker
                if (self._debug):
                    print(' [+] Worker {} has been inactive {} seconds'.format(id_container, str(diff)))
                if diff > pandas.to_timedelta(self._max_timeout): # si la diferencia de tiempo es mayor a MAX_TIMEOUT
                    if (self._debug):
                        print(' [!] Worker {} is NOT responding.'.format(id_container))
                    container = self._docker_client.containers.get(id_container)
                    if (self._debug):
                        print(' [+] Worker {} stopped.'.format(id_container))
                    container.stop()
                    name_worker = prox_worker()
                    if (self._debug):
                        print(' [+] Crafting {}...'.format(name_worker))
                    new_container = self._docker_client.containers.run(self._docker_image, name=name_worker, tty=True, detach=True, stdin_open=True)
                    # LAST_TIMESTAMPS[new_container.attrs['id']] = str(datetime.now())  # actualizo lista de containers.


    def prox_worker():
        return 'worker_' + str(len(self._last_timestamps))


    def on_open_connection(self, _unused_frame):
        if (self._debug):
            print(" [x] Connected - Connection state: OPEN. ")
        self._connection.channel(on_open_callback=self.on_channel_open)


    def on_channel_open(self, channel):
        """Callback when we have successfully declared the channel."""
        if (self._debug):
            print(" [x] Channel - Channel state: OPEN. ")
        self._channel = channel
        cb = functools.partial(self.on_queue_declareok, userdata=self._keep_alive_queue)
        self._channel.queue_declare(queue=self._keep_alive_queue, durable=True, callback=cb)


    def on_queue_declareok(self, _unused_frame, userdata):
        """Callback when we have successfully declared the queue.

        This call tells the server to send us self._prefetch_count message in advance.
        This helps overall throughput, but it does require us to deal with the messages
        we have promptly.
        """
        queue_name = userdata
        if (self._debug):
            print(" [x] Queue declared - Queue: {}".format(queue_name))
        self._channel.basic_qos(prefetch_count=self._prefetch_count, callback=self.on_qos)


    def on_qos(self, _unused_frame):
        """Callback when Basic.QOS has completed."""
        if (self._debug):
            print(" [x] QOS set to {}".format(self._prefetch_count))
        self.start_consuming()


    def start_consuming(self):
        """Start consuming from task queue."""
        if (self._debug):
            print(" [x] Waiting for messages...")
        self._channel.basic_consume(self._keep_alive_queue, self.callback_keep_alive_queue, auto_ack=False)


    def start(self):
        """Start worker."""
        # Define connection
        while True:
            try:
                self._connection = pika.SelectConnection(pika.URLParameters(self._amqp_url), on_open_callback=self.on_open_connection)
                self._connection.ioloop.start()
            # Catch a Keyboard Interrupt to make sure that the connection is closed cleanly
            except KeyboardInterrupt:
                # Gracefully close the connection
                self._connection.close()
                # Start the IOLoop again so Pika can communicate, it will stop on its own when the connection is closed
                self._connection.ioloop.start()
            except :
                if (self._debug):
                    print(" [!] RabbitMQ Host Unrecheable. Reconecting in {} seconds...".format(self._reconection_time))
                time.sleep(self._reconection_time)


def main():
    """Main entry point to the program."""
    admin = AdminWorker()
    admin.start()
    admin.calculate_timeout_workers()


if __name__ == '__main__':
    main()
