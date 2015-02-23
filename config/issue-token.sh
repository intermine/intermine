# Issue a JWT for someone@somewhere.com
ant -f testmodel/webapp/main/build.xml issue-jwt \
    -Drelease=demo         \
    -Dkeystore="$KEYSTORE" \
    -Dexp=100000           \
    -Dsub=someone          \
    -Demail='someone@somewhere.com'

mv testmodel/webapp/main/build/someone.jwt ./someone.jwt

