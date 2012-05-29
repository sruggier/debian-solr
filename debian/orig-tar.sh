#/bin/sh -e

VERSION=$2
TAR=../lucene-solr_$VERSION.orig.tar.gz
DIR=lucene-solr-$VERSION
mkdir -p $DIR

# Unpack ready fo re-packing
tar -xzf $TAR -C $DIR --strip-components=1

# Clean up tarball
find $DIR -name \*.jar -delete
find $DIR -name \*.war -delete
fgrep -lR "Generated By:JavaCC:" $DIR | xargs -n5 rm

# Repack excluding stuff we don't need
GZIP=--best tar -czf $TAR $DIR

rm -rf $DIR
