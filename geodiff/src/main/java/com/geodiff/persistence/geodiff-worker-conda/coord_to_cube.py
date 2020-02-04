#!/usr/bin/env python3
# -*- coding: utf-8 -*-

STEP = 0.025
DEFAULT_POLYGON = {
    'shape': [
        [0.0125, 0.0125],
        [-0.0125, 0.0125],
        [-0.0125, -0.0125],
        [0.0125, -0.0125]
    ],
    'center': [0, 0]
}

def coord_to_cube(x, y, n=STEP):
    n = n / 2
    l = []
    l.append([x+n,y+n])
    l.append([x-n,y+n])
    l.append([x-n,y-n])
    l.append([x+n,y-n])
    l.append([x+n,y+n])
    return l

def divide_coord(coord):
    """Returns coord divide by STEP size."""
    return [coord[0] // STEP, coord[1] // STEP]

def polygon_center(pol):
    return [ divide_coord(i) for i in pol ]

    
