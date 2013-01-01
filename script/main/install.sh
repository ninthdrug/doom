#!/bin/bash
mkdir -p $DOOMHOME/bin
mkdir -p $DOOMHOME/conf
mkdir -p $DOOMHOME/lib
mkdir -p $DOOMHOME/log
mkdir -p $DOOMHOME/web
cp -r bin/* $DOOMHOME/bin
cp -r lib/* $DOOMHOME/lib
echo "$DOOMHOME/conf/doom.conf" > $HOME/.ninthdrug
cp conf/doom.conf $DOOMHOME/conf
cp -r conf/doom $DOOMHOME/conf
rsync --delete -av web/ $DOOMHOME/web

