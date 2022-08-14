#!/bin/bash

ssh scottvorthmann@sandy.dreamhost.com \
  sed -i &quot;s/BUILD7NUM=[0-9]*/BUILD7NUM=${buildNum}/&quot; vzome.com/.htaccess
  