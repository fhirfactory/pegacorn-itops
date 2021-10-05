# Builder
FROM fhirfactory/pegacorn-base-communicate-app-web:1.0.0 as builder

# Conditionally copy certificate, as it won't exist outside the proxy (based on https://forums.docker.com/t/copy-only-if-file-exist/3781/3)
#COPY /README.md /PRDPKICA.crt* /opt/

#ENV NODE_EXTRA_CA_CERTS=/opt/PRDPKICA.crt

# Conditionally configure npm to use https proxy. Based on: 
# 1. https://www.dev-diaries.com/social-posts/conditional-logic-in-dockerfile/
ARG HTTP_PROXY
RUN if [ -n "$HTTP_PROXY" ] ; then \
    npm set proxy ${HTTP_PROXY} \ 
 && npm set strict-ssl false \
 ; fi
# && npm set cafile /opt/PRDPKICA.crt \

ENV NODE_ENV production

WORKDIR /src
COPY . /src

ENV PUBLIC_URL "/role-selection"
ENV WDS_SOCKET_PATH "/role-selection/sockjs-node"

RUN npm install \
 && npm run build

#RUN if [ -n "$HTTP_PROXY" ] ; then \
#    rm /opt/PRDPKICA.crt \
# ; fi
	
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html

COPY --from=builder /src/build /usr/share/nginx/html/role-selection

# Insert wasm type into Nginx mime.types file so they load correctly.
RUN sed -i '3i\ \ \ \ application/wasm wasm\;' /etc/nginx/mime.types

ARG IMAGE_BUILD_TIMESTAMP
ENV REACT_APP_IMAGE_BUILD_TIMESTAMP=${IMAGE_BUILD_TIMESTAMP}
RUN echo REACT_APP_IMAGE_BUILD_TIMESTAMP=${REACT_APP_IMAGE_BUILD_TIMESTAMP}
