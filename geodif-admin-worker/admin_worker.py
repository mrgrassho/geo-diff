#!/usr/bin/env python
import pika
import docker
import random
#constantes a cambiar
queue = "hello"
imagen = "centos:7"
host = "localhost"

client = docker.from_env()
connection = pika.BlockingConnection(
    pika.ConnectionParameters(host=host))
channel = connection.channel()

cola = channel.queue_declare(queue=queue)


def callback(ch, method, properties, body):
    nombre = "worker"  + str(random.uniform(0,1))
    client.containers.run(imagen,name=nombre, tty=True, detach = True, stdin_open = True)


channel.basic_consume(queue=queue, on_message_callback=callback, auto_ack=False)

print(' administrador worker UP')

channel.start_consuming()
