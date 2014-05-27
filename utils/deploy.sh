#!/bin/sh

PROJ=rfi_gen

cd .. 
echo `date` > conf/date.info
git rev-list HEAD --count > conf/minorBuild.info
cat conf/minorBuild.info conf/date.info conf/revision.info > conf/build.info
find -iname "*~" -exec rm '{}' ';'
play clean && play war --exclude app/controllers:.git:app/helpers:app/models:app/notifiers:tmp:logs:utils:test:attachments --%prod -o /tmp/$PROJ --zip

cd utils
