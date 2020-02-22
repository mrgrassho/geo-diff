#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pika
import json
import time
import cv2
import numpy as np
import base64
import string
import random
import functools
import http.client
import mimetypes
from datetime import datetime
import concurrent.futures

class GeoDiffWorker(object):
    """docstring for GeoDiffWorker."""
    OPENCV_MAX_HUE = 179
    OPENCV_MAX_SAT = 255
    OPENCV_MAX_VAL = 255
    GOOGLE_MAX_HUE = 360
    GOOGLE_MAX_SAT = 100
    GOOGLE_MAX_VAL = 100

    def __init__(self, amqp_url, task_queue, res_xchg, keep_alive_queue, prefetch_count=1, reconection_time=10, debug=True):
        self._id_worker = self.id_generator()
        self._amqp_url = amqp_url
        self._task_queue = task_queue
        self._res_xchg = res_xchg
        self._keep_alive_queue = keep_alive_queue
        self._channel = None
        self._debug = debug
        self._connection = None
        self._prefetch_count = prefetch_count
        self._reconection_time = reconection_time


    def applyFilter(self, image, filter='DEFORESTATION'):
        """
        Apply Filter to Image (.png).

        :param image: Base64 enconded image.
        :type image: :class:`string`

        :param image: Filter to apply.
        :type image: :class:`string`
        """
        if (self._debug):
            print(" [x] Applying {} filter to Image".format(filter))
        if (filter == 'DEFORESTATION'):
            # Filtra gama de verdes
            #lower_hsv = self.normalize_HSV(np.array([90,0,0]))
            #upper_hsv = self.normalize_HSV(np.array([150,100,100]))
            lower_hsv = np.array([36, 0, 0])
            upper_hsv = np.array([86, 255, 255])
        elif (filter == 'DROUGHT'):
            # Filtra gama de amarrillos / rojizos
            #lower_hsv = self.normalize_HSV(np.array([40,46,54]))
            #upper_hsv = self.normalize_HSV(np.array([65,100,100]))
            lower_hsv = np.array([80, 0, 0])
            upper_hsv = np.array([135, 255, 255])
        elif (filter == 'FLOODING'):
            # Filtra gama de azules / celestes
            #lower_hsv = self.normalize_HSV(np.array([183,41,51]))
            #upper_hsv = self.normalize_HSV(np.array([254,100,100]))
            lower_hsv = np.array([0, 100, 20])
            upper_hsv = np.array([23, 255, 255])
        else:
            # Filtra gama del blanco al negro
            lower_hsv = self.normalize_HSV(np.array([0,40,100]))
            upper_hsv = self.normalize_HSV(np.array([360,0,100]))
        img = self.data_uri_to_cv2_img(image)
        # blurredImage = cv2.blur(img,(2,2))
        hsv = cv2.cvtColor(img, cv2.COLOR_RGB2HSV)
        mask = cv2.inRange(hsv, lower_hsv, upper_hsv)
        res = cv2.bitwise_and(img,img, mask= mask)
        # cv2.imwrite("tsti.png", img)
        # cv2.imwrite("tsti.png", img)
        if (self._debug):
            print(" [x] Filter applied in hsv.")

        return res


    def normalize_HSV(self, ndarr):
        # For HSV, Hue range is [0,179], Saturation range is [0,255]
        # and Value range is [0,255]. Different softwares use different scales.
        # So if you are comparing OpenCV values with them, you need to normalize
        # these ranges.
        hue = 0 if (ndarr[0] == 0) else self.OPENCV_MAX_HUE / (self.GOOGLE_MAX_HUE / ndarr[0])
        sat = 0 if (ndarr[1] == 0) else self.OPENCV_MAX_SAT / (self.GOOGLE_MAX_SAT / ndarr[1])
        val = 0 if (ndarr[2] == 0) else self.OPENCV_MAX_VAL / (self.GOOGLE_MAX_VAL / ndarr[2])
        return np.array([hue, sat, val])


    def id_generator(self, size=28, chars=string.ascii_uppercase + string.digits):
        return ''.join(random.choice(chars) for _ in range(size))


    def paint_square(self, img, k, cols, rows, elem, i, j):
        for ii in range(i-k,i+k):
            for jj in range(j-k,j+k):
                if (ii < rows and jj < cols):
                    img[ii, jj] = elem
        return img


    def erase_clouds(self, img):
        """Replace white spots (clouds) with the colors around."""
        rows, cols, _ = img.shape
        last_color = [random.randrange(rows), random.randrange(cols)]
        step = 2
        for i in range(0, rows, step):
            for j in range(0, cols, step):
                if (img[i,j] > [55, 55, 55]).all():
                    x = last_color[0]
                    y = last_color[1]
                    elem = img[x, y]
                    if (x >= rows): x = rows-1
                    if (y >= cols): y = cols-1
                    img = self.paint_square(img, step, cols, rows, elem, i, j)
                else:
                    last_color = [i, j]
        return img


    def set_transparent_background(self, img):
        """Sets Alpha Channel from black to transparent."""
        *_, alpha = cv2.split(img)
        alpha[np.where(alpha != 0)] = 1000
        return cv2.merge((img, alpha))


    def img_to_base64(self, res):
        retval, buffer = cv2.imencode('.png', res)
        return "data:image/png;base64," + base64.b64encode(buffer).decode("utf-8", "ignore")

    def apply_kmeans(self, _K, img):
        Z = img.reshape((-1,3))
        Z = np.float32(Z)
        criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 10, 1.0)
        K = _K
        ret,label,center = cv2.kmeans(Z,K,None,criteria,10,cv2.KMEANS_RANDOM_CENTERS)
        center = np.uint8(center)
        res = center[label.flatten()]
        res = res.reshape((img.shape))
        return res

    def download_img(self, url):
        if (self._debug):
            print(" [x] Downloading Image...")
        url = url.split('https://')[1]
        host = url.split('/')[0]
        resource = url.split('.com')[1]
        conn = http.client.HTTPSConnection(host)
        payload = ''
        headers = {}
        conn.request("GET", resource, payload, headers)
        res = conn.getresponse()
        data = res.read()
        if (data == ''):
            raise Exception("Empty Response.")
        return "data:image/png;base64," + base64.b64encode(data).decode("utf-8", "ignore")


    def data_uri_to_cv2_img(self, uri):
        if (self._debug):
            print(" [x] Converting Base64 String to image...")
        e = uri.split(',')[1]
        encoded_data = bytearray(e, 'utf-8')
        nparr = np.fromstring(base64.b64decode(encoded_data), np.uint8)
        # nparr = nparr.reshape(nparr.shape[0],1,)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if (self._debug):
            print(" [x] Conversion ready.")
        return img


    def send_to_xchg(self, message, reply_to):
        """
        Send message to RabbitMQ Exchange.
        """
        json_msg = json.dumps(message)
        self._channel.exchange_declare(exchange=self._res_xchg, exchange_type='fanout')
        self._channel.basic_publish(
            exchange=self._res_xchg,
            routing_key='',
            body=json_msg,
            properties=pika.BasicProperties(
                delivery_mode=2,  # make message persistent
                reply_to=reply_to
            )
        )
        if (self._debug):
            print(" [x] Image sent to XCHG: {} - Body: {} ".format(self._res_xchg, json_msg[:140]))


    def send_to_queue(self, message, queue):
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
            print(" [x] Image sent to {} - Body: {} ".format(queue, json_msg[:10]))

    def process_image(self, data, filter):
        process_time = time.time()
        filteredImage = self.applyFilter(data['earthImage']['rawImage'], filter)
        #geoOutput = self.pngToGeoJson(filteredImage)
        if (self._debug):
            print(" [+] Filter applied! Process took {} seconds.".format(time.time() - process_time))
        process_time = time.time()
        filteredImage = self.erase_clouds(filteredImage)
        if (self._debug):
            print(" [+] Erase Clouds done! Process took {} seconds.".format(time.time() - process_time))
        process_time = time.time()
        filteredImage = self.apply_kmeans(8, filteredImage)
        if (self._debug):
            print(" [+] Apply KMeans done! Process took {} seconds.".format(time.time() - process_time))
        filteredImage = self.set_transparent_background(filteredImage)
        if (self._debug):
            print(" [+] Set Background done! Process took {} seconds.".format(time.time() - process_time))
        return {
            'id': self.id_generator(),
            'filterName': filter.lower(),
            'vectorImage': self.img_to_base64(filteredImage)
        }


    def callback_task_queue(self, _unused_channel, delivery, properties, body):
        """
        Callback triggered when message is received.
        """
        if (self._debug):
            print(" [x] Received - Body: {}".format(body[:140]))
        start_time = time.time()
        data = json.loads(body)
        # Descargamos la imagen si no esta
        if (not 'rawImage' in data['earthImage']):
            data['earthImage']['rawImage'] = self.download_img(data['earthImage']['url'])

        filters = ['DEFORESTATION', 'FLOODING', 'DROUGHT']
        return_values = list()

        for filter in filters:
            with concurrent.futures.ThreadPoolExecutor() as executor:
                future = executor.submit(self.process_image, data, filter)
                return_values.append(future.result())

        data['filteredImages'] = return_values
        if (self._debug):
            print(" [x] Job done! Image processing took {} seconds.".format(time.time() - start_time))
        self._channel.basic_ack(delivery_tag=delivery.delivery_tag)
        self.send_to_xchg(data, properties.reply_to)
        #  send keep alive message to admin worker.
        message_ka = {'id':self._id_worker,'timestamp':str(datetime.now())}
        self.send_to_queue(message_ka,self._keep_alive_queue)


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
            except :
                if (self._debug):
                    print(" [!] RabbitMQ Host Unrecheable. Reconecting in {} seconds...".format(self._reconection_time))
                time.sleep(self._reconection_time)
