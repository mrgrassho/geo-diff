#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from geodiff_worker import GeoDiffWorker
import os

def main():
    """Main entry point to the program."""

    # Get the location of the AMQP broker (RabbitMQ server) from
    # an environment variable
    amqp_url = os.environ['AMQP_URL']
    task_queue = os.environ['TASK_QUEUE']
    result_xchg = os.environ['RES_XCHG']
    keep_alive_queue = os.environ['KEEP_ALIVE_QUEUE']
    worker = GeoDiffWorker(amqp_url, task_queue, result_xchg, keep_alive_queue, debug=True)
    worker.start()


if __name__ == '__main__':
    main()
