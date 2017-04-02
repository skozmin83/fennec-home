#!/bin/bash
rm -rf dist
mkdir -p dist/js
mkdir -p dist/css
mkdir -p dist/img
browserify js/index.js > dist/js/index.js
rework -v webkit,moz < css/style.css > dist/css/style.css
cp -R img/* dist/img/.
cp index.html dist/index.html