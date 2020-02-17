#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from geodiff_worker import GeoDiffWorker
import os
import random
from datetime import datetime

def generate_id():
    id = ''.join(random.choice('0123456789ABCDEF') for i in range(32))
    return id

def main():
    """Main entry point to the program."""
    # Get the location of the AMQP broker (RabbitMQ server) from
    # an environment variable
    # print(str(datetime.now()))

    amqp_url = os.environ['AMQP_URL']
    task_queue = os.environ['TASK_QUEUE']
    result_queue = os.environ['RES_QUEUE']
    keep_alive_queue = os.environ['KEEP_ALIVE_QUEUE']
    worker = GeoDiffWorker(generate_id(), amqp_url, task_queue, result_queue,keep_alive_queue, debug=True)
    worker.start()


if __name__ == '__main__':
    main()
