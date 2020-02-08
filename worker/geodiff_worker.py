#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import geopandas
from shapely.geometry.multipolygon import MultiPolygon
from shapely.geometry.point import Point
import pika
import json
import sys
import time
import cv2
import numpy as np
import base64
import subprocess
from PIL import Image
import string
import random
import functools


class GeoDiffWorker(object):
    """docstring for GeoDiffWorker."""

    def __init__(self, amqp_url, task_queue, res_queue, prefetch_count=1, reconection_time=10, debug=True):
        self._amqp_url = amqp_url
        self._task_queue = task_queue
        self._res_queue = res_queue
        self._channel = None
        self._debug = debug
        self._connection = None
        self._prefetch_count = prefetch_count
        self._reconection_time = reconection_time


    def applyFilter(self, image, filter):
        """
        Apply Filter to Image (.png).

        :param image: Base64 enconded image.
        :type image: :class:`string`

        :param image: Filter to apply.
        :type image: :class:`string`
        """
        if (filter == 'DEFORESTATION'):
            # Filtra gama de verdes
            lower_hsv = np.array([70,40,40])
            upper_hsv = np.array([175,100,100])
        elif (filter == 'DROUGHT'):
            # Filtra gama de amarrillos / rojizos
            lower_hsv = np.array([40,46,54])
            upper_hsv = np.array([65,100,100])
        elif (filter == 'FLOODING'):
            # Filtra gama de azules / celestes
            lower_hsv = np.array([183,41,51])
            upper_hsv = np.array([254,100,100])
        else:
            # Filtra gama del blanco al negro
            lower_hsv = np.array([0,40,100])
            upper_hsv = np.array([360,0,100])
        img = self.data_uri_to_cv2_img(image)
        blurredImage = cv2.blur(img,(5,5))
        hsv = cv2.cvtColor(blurredImage,cv2.COLOR_RGB2HSV)
        mask = cv2.inRange(hsv, lower_hsv, upper_hsv)
        return mask


    def id_generator(self, size=28, chars=string.ascii_uppercase + string.digits):
        return ''.join(random.choice(chars) for _ in range(size))


    def pngToGeoJson(self, image_data, tmp=""):
        tmp = self.id_generator()
        Image.frombytes('RGBA', (128,128), image_data, 'raw').save(tmp, format="bmp")
        outputTmp = tmp+".geojson"
        bashCommand = "./potrace -b geojson -i "+tmp+".bmp -o "+outputTmp
        subprocess.check_output(['bash','-c', bashCommand])
        data = json.load(outputTmp)
        subprocess.check_output(['bash','-c', "rm -rf "+tmp+" "+outputTmp])
        return data


    def scale(self, polygons, scale_x, center=Point([0,0])):
        """
        Scale and center polygons, finally write a resulting Geojson file.

        :param polygons: Polygons Series.
        :type polygons: :class:`geopandas.GeoSeries`

        :param scale_x: Degrees to scale the Polygons.
        :type scale_x: :class:`float`

        :param center: Coordinates Point([lon, lat]) to center polygons.
        :type center: :class:`shapely.geometry.Point`
        """
        m = MultiPolygon([polygons.loc[i].geometry for i in range(polygons.size)])
        g = geopandas.GeoSeries([m])
        x = g.centroid.x - center.x if (g.centroid.x >= center.x) else center.x - g.centroid.x
        y = g.centroid.y - center.y if (g.centroid.y >= center.y) else center.y - g.centroid.y
        origin = Point([x, y])
        g = g.scale(scale_x, scale_x, origin=origin)
        return g.to_json()


    def data_uri_to_cv2_img(self, uri):
        encoded_data = uri.split(',')[1]
        nparr = np.fromstring(encoded_data.decode('base64'), np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        return img


    def send(self, message, queue):
        """
        Send message to RabbitMQ Queue.
        """
        json_msg = json.dumps(message)
        self._channel.queue_declare(queue=queue, durable=True)
        self._channel.basic_publish(
            exchange='',
            routing_key=queue,
            body=json_msg,
            properties=pika.BasicProperties(
                delivery_mode=2,  # make message persistent
            )
        )
        if (self._debug):
            print(" [x] Sent {}".format(json_msg))


    def callback_task_queue(self, _unused_channel, delivery, properties, body):
        """
        Callback triggered when message is received.
        """
        if (self._debug):
            print(" [x] Received - Body: {}".format(body))
        start_time = time.time()
        data = json.loads(body)
        filteredImage = self.applyFilter(data['earthImage']['rawImage'])
        geoOutput = self.pngToGeoJson(filteredImage)
        data['vectorImage'] = self.scale(geoOutput, data['earthImage']['dim'], Point(data['earthImage']['coordinate']['longitude'], data['earthImage']['coordinate']['latitude']) )
        if (self._debug):
            print(" [x] Job done! Image processing took {} seconds.".format(time.time() - start_time))
        self._channel.basic_ack(delivery_tag=delivery.delivery_tag)
        self.send(data, self._res_queue)


    def on_open_connection(self, _unused_frame):
        if (self._debug):
            print(" [x] Connected - Connection state: OPEN. ")
        self._connection.channel(on_open_callback=self.on_channel_open)


    def on_channel_open(self, channel):
        """Callback when we have successfully declared the channel."""
        if (self._debug):
            print(" [x] Channel - Channel state: OPEN. ")
        self._channel = channel
        cb = functools.partial(self.on_queue_declareok, userdata=self._task_queue)
        self._channel.queue_declare(queue=self._task_queue, durable=True, callback=cb)


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
        self._channel.basic_consume(self._task_queue, self.callback_task_queue)


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
            if (self._debug):
                print(" [!] Host Unrecheable. Reconecting in {} seconds...".format(self._reconection_time))
            time.sleep(self._reconection_time)
