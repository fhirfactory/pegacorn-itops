# Builder
FROM fhirfactory/pegacorn-base-communicate-app-web:1.0.0 as builder

# Conditionally configure npm to use https proxy. Based on: 
# 1. https://www.dev-diaries.com/social-posts/conditional-logic-in-dockerfile/
ARG HTTP_PROXY
RUN if [ -n "$HTTP_PROXY" ] ; then \
    npm set proxy ${HTTP_PROXY} \ 
 && npm set strict-ssl false \
 ; fi

ENV NODE_ENV production

WORKDIR /src
COPY . /src

ENV PUBLIC_URL "/"
ENV WDS_SOCKET_PATH "/sockjs-node"

RUN npm install \
 && npm run build
	
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html

COPY --from=builder /src/build /usr/share/nginx/html/

# Insert wasm type into Nginx mime.types file so they load correctly.
RUN sed -i '3i\ \ \ \ application/wasm wasm\;' /etc/nginx/mime.types

ARG IMAGE_BUILD_TIMESTAMP
ENV REACT_APP_IMAGE_BUILD_TIMESTAMP=${IMAGE_BUILD_TIMESTAMP}
RUN echo REACT_APP_IMAGE_BUILD_TIMESTAMP=${REACT_APP_IMAGE_BUILD_TIMESTAMP}
