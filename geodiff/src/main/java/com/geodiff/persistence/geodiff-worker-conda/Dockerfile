FROM continuumio/miniconda3

RUN mkdir /geodiff-worker-conda
WORKDIR /geodiff-worker-conda

RUN apt-get update \
  && apt-get install -y build-essential libssl-dev libffi-dev \
  && pip install cryptography \
  && apt-get purge -y build-essential libssl-dev libffi-dev \
                      dpkg-dev fakeroot libfakeroot:amd64 \
  && apt-get autoremove -y \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

RUN conda install geopandas

COPY requirements.txt ./

RUN pip install -r requirements.txt

COPY . .
