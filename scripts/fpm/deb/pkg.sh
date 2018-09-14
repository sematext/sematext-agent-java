OUTDIR=$1
VERSION=$2

PKG_HOME=scripts/fpm/
PKG_COMMON_HOME=$PKG_HOME/pkg-common/

. $PKG_HOME/spm-client-env.sh

INSTALL_HEAD="$PKG_COMMON_HOME/postinstall/after-install-head.sh"
INSTALL_MIGRATION="$PKG_COMMON_HOME/postinstall/migrate-from-old.sh"
INSTALL_BOTTOM="$PKG_COMMON_HOME/postinstall/after-install-bottom.sh"

INSTALL_SCRIPT="$PKG_COMMON_HOME/postinstall/after-install.sh"

cat $INSTALL_HEAD $INSTALL_MIGRATION $INSTALL_BOTTOM > $INSTALL_SCRIPT

fpm -s dir \
  -t deb \
  -a all \
  -f \
  -n "$PKG_NAME" \
  -v "$VERSION" \
  -m "$PKG_MAINTANER" \
  --epoch 0 \
  --vendor "$PKG_VENDOR" \
  --description "$PKG_DESCRIPTION" \
  --url "$PKG_URL" \
  --before-install $PKG_COMMON_HOME/preinstall/before-install.sh \
  --after-install $INSTALL_SCRIPT \
  --before-remove $PKG_COMMON_HOME/preremove/before-remove.sh \
  --template-scripts \
  --template-value pkg_type=deb \
  --template-value pkg_type=debian \
  --template-value init=init \
  --config-files /opt/spm/properties/java.properties \
  --config-files /opt/spm/spm-monitor/collectors \
  -d 'cron' \
  -d 'ntp | chrony' \
  -d 'python' \
  -C $OUTDIR/image/ \
  -p $OUTDIR/$PKG_NAME-$VERSION.noarch.deb \
  opt/ \
  var/ \
  etc/ \
  lib/

rm $INSTALL_SCRIPT
