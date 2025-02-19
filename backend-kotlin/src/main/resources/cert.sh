rm server.*

# 生成私钥
openssl genrsa -out server.key 2048

# 生成证书签名请求
openssl req -new -key server.key -out server.csr \
    -subj "/C=XX/ST=SomeState/L=SomeCity/O=QuickHub/OU=Home/CN=quickhub.local"

# 生成自签名证书
echo "
[v3_req]
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
DNS.2 = quickhub.local
IP.1 = 127.0.0.1
IP.2 = ::1
" | openssl x509 -req -days 825 -in server.csr -signkey server.key -out server.crt -extensions v3_req -extfile -
# -addext "subjectAltName = DNS:localhost"

# 将证书和私钥合并为PKCS12格式
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 \
    -name "quickhub" -passout pass:changeit

rm server.crt server.csr server.key

openssl req -x509 -out localhost.crt -keyout localhost.key \
  -newkey rsa:2048 -nodes -sha256 \
  -subj '/CN=localhost' -extensions EXT -config <( \
   printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
