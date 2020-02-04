#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import geopandas
from shapely.geometry.multipolygon import MultiPolygon
import rabbitmq
import json
import sys

class GeoDiffWorker(object):
    """docstring for GeoDiffWorker."""

    def __init__(self, ampq_url, task_queue, res_queue, scale_x=0.0001, scale_y=0.0001, debug=False):
        super(GeoDiffWorker, self).__init__()
        self.ampq_url = ampq_url
        self.scale_x = scale_x
        self.scale_y = scale_y
        self.task_queue = task_queue
        self.res_queue = res_queue
        self.channel = None


    def scale(self, polygons, origin=None, output_file="tmp-file.geojson"):
        """Scale and center polygons, finally write a resulting Geojson file.

        :param polygons: Polygons Series.
        :type polygons: :class:`geopandas.GeoSeries`

        :param origin: Coordinates tuple (lon, lat) to center polygons.
        :type origin: :class:`tuple`

        :param output_file: Output file.
        :type output_file: :class:`str`
        """
        m = MultiPolygon([polygons.loc[i].geometry for i in range(polygons.size)])
        g = geopandas.GeoSeries([m])
        if (origin is None):
            g = g.scale(self.scale_x, self.scale_y)
        else:
            g = g.scale(self.scale_x, self.scale_y, origin=origin)
        g.to_file(output_file, driver='GeoJSON')


    def send(self, message, queue):
        """Send message to RabbitMQ Queue."""

        self.channel.queue_declare(queue=queue, durable=True)
        self.channel.basic_publish(
            exchange='',
            routing_key=queue,
            body=message,
            properties=pika.BasicProperties(
                delivery_mode=2,  # make message persistent
            )
        )
        if (debug):
            print(" [x] Sent %r" % message)


    def callback(self, ch, method, properties, body):
        """Callback triggered when message is received."""

        if (debug):
            print(" [x] Received %r" % body)
        time.sleep(body.count(b'.'))
        # TODO:
        #       - Definir el formato del mensaje
        data = json.loads(body)
        # ---
        # SI RECIBIMOS JPEG SE DEBE DECODEAR Y LUEGO PROCESAR CON convert y potrace
        # ---
        # data.polygons = self.process_img(data)
        # self.scale(data.polygons)
        result = "{}"
        if (debug):
            print(" [x] Done")
        ch.basic_ack(delivery_tag=method.delivery_tag)
        self.send(result, self.res_queue)


    def start(self):
        """Start worker."""

        parameters = pika.URLParameters(self.amqp_url)
        connection = pika.SelectConnection(parameters, on_open_callback=on_open)
        self.channel = connection.channel()
        self.channel.queue_declare(queue=self.task_queue, durable=True)
        if (debug):
            print(' [*] Waiting for messages. To exit press CTRL+C')
        self.channel.basic_qos(prefetch_count=1)
        self.channel.basic_consume(queue=self.task_queue, on_message_callback=callback)
        self.channel.start_consuming()
