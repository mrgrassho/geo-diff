
# SETEAR API KEY
# set -x NASA_API_KEY 6k9ilibCQcusmZl9RRczizWjFC7K0gkviEt2G4Qa

from nasa import earth
assets = earth.assets(lat=1.5, lon=100.75, begin='2014-02-01', end='2014-06-01')
[(a.date, a.id) for a in assets]
image = assets[0].get_asset_image()
image.id
image.image.save("tsdad.png")
