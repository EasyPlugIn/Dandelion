cp -r "${1}"/* "${2}"

cd "${2}"

find . -type f -name '*.jar' | xargs -I{} sh -c 'tar xvf "{}" && rm "{}" && rm -rf META-INF'
