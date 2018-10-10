SPM_CLIENT_MODULES="es hbase jvm solr hadoop solrcloud zk redis storm cassandra mysql nginx-plus kafka spark haproxy tomcat clickhouse"
SPM_CLIENT_PACKED_MONITORS="generic storm redis haproxy"
SPM_CLIENT_JAVA_BASED="cassandra es hadoop hbase jvm kafka solr solrcloud spark storm tomcat zk"
SPM_CLIENT_COMMON_LIBS_MODULE="spm-client-common-libs"
SPM_PACKAGE_TYPES="deb rpm-centos-5 rpm-centos-6 rpm-centos-7 rpm-redhat-5 rpm-redhat-6 rpm-redhat-7 rpm-fedora rpm-suse"

SPM_CLIENT_PKG_ROOT="/opt"

PKG_NAME="sematext-agent-java"
PKG_MAINTANER="spm-client@sematext.com"
PKG_DESCRIPTION="Sematext Agent Java"
PKG_URL="http://sematext.com"
PKG_VENDOR="sematext.com"
