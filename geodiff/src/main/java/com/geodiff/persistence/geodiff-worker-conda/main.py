#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def main():
    """Main entry point to the program."""

    # Get the location of the AMQP broker (RabbitMQ server) from
    # an environment variable
    amqp_url = os.environ['AMQP_URL']
    task_queue = os.environ['TASK_QUEUE']
    result_queue = os.environ['RES_QUEUE']
    worker = GeoDiffWorker(amqp_url, task_queue, result_queue)
    worker.start()


if __name__ == '__main__':
    main()
