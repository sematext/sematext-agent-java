if [ -z "$SPM_HOME"]; then
   SPM_HOME=/opt/spm
fi

for f in $SPM_HOME/spm-monitor/lib/*.jar; do
   unver=$(echo $f | sed -E 's/(.*)(\-[[:digit:]]+\.[[:digit:]]+\.[[:digit:]])(\-SNAPSHOT){0,1}(\-withdeps)(\.jar)/\1\5/g')
   mv $f $unver
done

unver=$(ls $SPM_HOME/spm-monitor/lib/internal/common/spm-client-common-libs-*.jar | sed -E 's/(.*)(\-[[:digit:]]+\.[[:digit:]]+\.[[:digit:]])(\-SNAPSHOT){0,1}(\-withdeps)(\.jar)/\1\5/g')
mv $SPM_HOME/spm-monitor/lib/internal/common/spm-client-common-libs-*.jar $unver

